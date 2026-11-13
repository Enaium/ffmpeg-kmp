/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example.core

import cn.enaium.camera.I420Frame
import cn.enaium.ffmpeg.AvCodec
import cn.enaium.ffmpeg.AvCodecContext
import cn.enaium.ffmpeg.avcodec.AvCodecFlag
import cn.enaium.ffmpeg.AvFilterContext
import cn.enaium.ffmpeg.AvFilterGraph
import cn.enaium.ffmpeg.AvFormatContext
import cn.enaium.ffmpeg.AvFrame
import cn.enaium.ffmpeg.AvPacket
import cn.enaium.ffmpeg.AvPixelFormat
import cn.enaium.ffmpeg.AvRational
import cn.enaium.ffmpeg.AvStream
import cn.enaium.ffmpeg.SwsContext
import cn.enaium.ffmpeg.SwsFlag
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.TimeSource

/** Runtime knobs owned by the UI / command line. */
data class PipelineSettings(
    val filter: FilterPreset = FilterPreset.NONE,
    val scalePercent: Int = 100,
    val record: Boolean = false,
    val recordingFormat: RecordingFormat = RecordingFormat.MP4,
    val recordingPath: String = "camera-ffmpeg.mp4",
)

/**
 * The FFmpeg half of the example:
 *
 * ```
 * I420 -> swscale (scale) -> avfilter (effect) -> avcodec (encode)
 *      -> avcodec (decode) -> swscale (RGBA) -> SDL texture
 * ```
 *
 * Encoded packets are optionally written to an MP4 file through libavformat.
 * The class is not thread-safe; one pipeline belongs to one worker thread.
 * Calling [process] with a different size or filter recreates the codec,
 * filter and scaler contexts (and starts a new recording file if recording is
 * enabled).
 */
class FFmpegPipeline(
    private val frameRate: Int,
    private val bitrateKbps: Int,
) : AutoCloseable {
    private val encoderCodec = AvCodec.encoder("mpeg4")
    private val decoderCodec = AvCodec.decoder("mpeg4")

    private var encoder: AvCodecContext? = null
    private var decoder: AvCodecContext? = null
    private var filterGraph: AvFilterGraph? = null
    private var filterSource: AvFilterContext? = null
    private var filterSink: AvFilterContext? = null
    private var scaleContext: SwsContext? = null
    private val displayContexts = HashMap<DisplayKey, SwsContext>()
    private var recorder: Recorder? = null
    private var recordingBase: String? = null
    private var recordingFormat: RecordingFormat? = null
    private var activeRecordingPath: String? = null
    private var pipelineKey: PipelineKey? = null
    private var lastDecoded: DisplayFrame? = null

    private var frameIndex = 0L
    private var inputFrames = 0L
    private var encodedFrames = 0L
    private var encodedBytes = 0L
    private var decodedFrames = 0L
    private var recordingSerial = 0
    private val sessionId = Random.nextInt(1, 1_000_000)
    private val started = TimeSource.Monotonic.markNow()
    private var closed = false

    /**
     * Processes one camera frame. [settings.recordingPath] is used as a base
     * name; a session/serial suffix is inserted before the extension so a
     * settings change never overwrites an earlier recording.
     */
    fun process(frame: I420Frame, settings: PipelineSettings): PipelineOutput {
        check(!closed) { "pipeline is closed" }
        val width = frame.width
        val height = frame.height
        val scaledWidth = even(width * settings.scalePercent / 100)
        val scaledHeight = even(height * settings.scalePercent / 100)

        ensurePipeline(
            width,
            height,
            scaledWidth,
            scaledHeight,
            settings.filter,
            settings.record && settings.recordingFormat.globalHeader,
        )
        ensureRecorder(settings)

        val source = frame.toAvFrame()
        val owned = mutableListOf(source)
        try {
            source.pts = frameIndex
            val camera = toDisplay(source, width, height)

            val scaled = if (scaledWidth == width && scaledHeight == height) {
                source
            } else {
                AvFrame.video(scaledWidth, scaledHeight, AvPixelFormat.YUV420P).also { destination ->
                    owned += destination
                    scaleContext!!.scale(destination, source)
                    destination.pts = frameIndex
                }
            }

            val filtered = if (filterGraph != null) {
                filterGraph!!.addFrame(filterSource!!, scaled)
                // A filter that buffers (or a chain that drops a frame) may
                // not produce output for every input; fall back to the scaled
                // picture instead of flushing the encoder.
                filterGraph!!.getFrame(filterSink!!)?.also { owned += it } ?: scaled
            } else {
                scaled
            }

            encoder!!.sendFrame(filtered)
            var last: AvFrame? = null
            while (true) {
                val packet = encoder!!.receivePacket() ?: break
                encodedFrames++
                encodedBytes += packet.size
                // The decoder does not consume the packet; the muxer does, so
                // decode first and write the (still intact) packet afterwards.
                decoder!!.sendPacket(packet)
                recorder?.write(packet)
                packet.close()

                while (true) {
                    val decoded = decoder!!.receiveFrame() ?: break
                    decodedFrames++
                    last?.close()
                    last = decoded
                }
            }

            val filteredDisplay = last?.let { decoded ->
                try {
                    toDisplay(decoded, decoded.width, decoded.height)
                } finally {
                    decoded.close()
                }
            }
            if (filteredDisplay != null) lastDecoded = filteredDisplay

            inputFrames++
            frameIndex++
            return PipelineOutput(
                camera = camera,
                filtered = filteredDisplay ?: lastDecoded,
                stats = stats(),
                recordingPath = activeRecordingPath,
            )
        } finally {
            owned.forEach { runCatching { it.close() } }
        }
    }

    private fun ensurePipeline(
        sourceWidth: Int,
        sourceHeight: Int,
        width: Int,
        height: Int,
        filter: FilterPreset,
        globalHeader: Boolean,
    ) {
        val key = PipelineKey(sourceWidth, sourceHeight, width, height, filter, globalHeader)
        if (key == pipelineKey) return
        closePipeline()

        encoder = AvCodecContext.create(encoderCodec).apply {
            this.width = width
            this.height = height
            pixelFormat = AvPixelFormat.YUV420P
            timeBase = AvRational(1, frameRate)
            bitRate = bitrateKbps.toLong() * 1000L
            gopSize = frameRate
            maxBFrames = 0
            flags = if (globalHeader) {
                flags or AvCodecFlag.GLOBAL_HEADER
            } else {
                flags and AvCodecFlag.GLOBAL_HEADER.inv()
            }
            open()
        }

        decoder = AvCodecContext.create(decoderCodec).apply {
            val parameters = encoder!!.codecParameters()
            try {
                applyParameters(parameters)
            } finally {
                parameters.close()
            }
            open()
        }

        scaleContext = SwsContext.create(
            sourceWidth,
            sourceHeight,
            AvPixelFormat.YUV420P,
            width,
            height,
            AvPixelFormat.YUV420P,
            SwsFlag.BILINEAR,
        )

        if (filter != FilterPreset.NONE) {
            val graph = AvFilterGraph.create()
            val source = graph.createFilter(
                "buffer",
                "in",
                "video_size=${width}x$height:pix_fmt=0:time_base=1/$frameRate:pixel_aspect=1/1",
            )
            val effect = graph.createFilter(filter.filterName!!, "effect", filter.args)
            // Some effects negotiate a different pixel format; the encoder
            // (and swscale) expect YUV420P.
            val format = graph.createFilter("format", "format", "pix_fmts=yuv420p")
            val sink = graph.createFilter("buffersink", "out")
            graph.link(source, 0, effect, 0)
            graph.link(effect, 0, format, 0)
            graph.link(format, 0, sink, 0)
            graph.configure()
            filterGraph = graph
            filterSource = source
            filterSink = sink
        }

        pipelineKey = key
        lastDecoded = null
        // The encoder/recording pair is new; start its timeline at zero.
        frameIndex = 0L
    }

    private fun ensureRecorder(settings: PipelineSettings) {
        if (!settings.record) {
            recorder?.close()
            recorder = null
            recordingBase = null
            recordingFormat = null
            activeRecordingPath = null
            return
        }

        if (recordingBase != settings.recordingPath ||
            recordingFormat != settings.recordingFormat
        ) {
            recorder?.close()
            recorder = null
            recordingBase = settings.recordingPath
            recordingFormat = settings.recordingFormat
        }
        if (recorder != null) return

        val path = uniqueRecordingPath(settings.recordingPath, settings.recordingFormat)
        val activeEncoder = encoder ?: return
        recorder = Recorder.create(path, activeEncoder, settings.recordingFormat)
        activeRecordingPath = path
        recordingSerial++
    }

    private fun uniqueRecordingPath(base: String, format: RecordingFormat): String {
        val slash = maxOf(base.lastIndexOf('/'), base.lastIndexOf('\\'))
        val dot = base.lastIndexOf('.')
        val stem = if (dot > slash) base.substring(0, dot) else base
        return "$stem-$sessionId-$recordingSerial.${format.extension}"
    }

    private fun toDisplay(frame: AvFrame, width: Int, height: Int): DisplayFrame {
        val key = DisplayKey(width, height, frame.format)
        val scaler = displayContexts.getOrPut(key) {
            SwsContext.create(
                width,
                height,
                frame.format,
                width,
                height,
                AvPixelFormat.RGBA,
                SwsFlag.BILINEAR,
            )
        }
        AvFrame.video(width, height, AvPixelFormat.RGBA).use { rgba ->
            scaler.scale(rgba, frame)
            return DisplayFrame(width, height, compactRgba(rgba, width, height))
        }
    }

    private fun compactRgba(frame: AvFrame, width: Int, height: Int): ByteArray {
        val stride = frame.linesize(0)
        val plane = frame.copyPlane(0)
        val out = ByteArray(width * height * 4)
        for (row in 0 until height) {
            plane.copyInto(
                destination = out,
                destinationOffset = row * width * 4,
                startIndex = row * stride,
                endIndex = row * stride + width * 4,
            )
        }
        return out
    }

    private fun stats(): PipelineStats {
        val elapsed = started.elapsedNow().inWholeMilliseconds / 1000.0
        val key = pipelineKey
        return PipelineStats(
            inputFrames = inputFrames,
            encodedFrames = encodedFrames,
            encodedBytes = encodedBytes,
            decodedFrames = decodedFrames,
            width = key?.width ?: 0,
            height = key?.height ?: 0,
            measuredFps = if (elapsed > 0) inputFrames / elapsed else 0.0,
        )
    }

    private fun closePipeline() {
        recorder?.close()
        recorder = null
        activeRecordingPath = null

        filterGraph?.close()
        filterGraph = null
        filterSource = null
        filterSink = null

        scaleContext?.close()
        scaleContext = null

        decoder?.close()
        decoder = null
        encoder?.close()
        encoder = null

        pipelineKey = null
        lastDecoded = null
    }

    override fun close() {
        if (closed) return
        closed = true

        // Flush the encoder before the muxer writes its trailer.
        runCatching {
            encoder?.sendFrame(null)
            while (true) {
                val packet = encoder?.receivePacket() ?: break
                encodedFrames++
                encodedBytes += packet.size
                recorder?.write(packet)
                packet.close()
            }
        }
        recorder?.close()
        recorder = null
        activeRecordingPath = null

        decoder?.close()
        decoder = null
        encoder?.close()
        encoder = null

        filterGraph?.close()
        filterGraph = null
        filterSource = null
        filterSink = null
        scaleContext?.close()
        scaleContext = null
        displayContexts.values.forEach { runCatching { it.close() } }
        displayContexts.clear()
        pipelineKey = null
    }

    private data class PipelineKey(
        val sourceWidth: Int,
        val sourceHeight: Int,
        val width: Int,
        val height: Int,
        val filter: FilterPreset,
        val globalHeader: Boolean,
    )

    private data class DisplayKey(
        val width: Int,
        val height: Int,
        val format: Int,
    )

    private class Recorder(
        private val output: AvFormatContext,
        private val stream: AvStream,
        private val encoderTimeBase: AvRational,
        val path: String,
    ) : AutoCloseable {
        private var closed = false

        fun write(packet: AvPacket) {
            packet.streamIndex = stream.index
            packet.rescaleTs(encoderTimeBase, stream.timeBase)
            output.writePacket(packet)
        }

        override fun close() {
            if (closed) return
            closed = true
            runCatching { output.writeTrailer() }
            output.close()
        }

        companion object {
            fun create(
                path: String,
                encoder: AvCodecContext,
                format: RecordingFormat,
            ): Recorder {
                val output = AvFormatContext.createOutput(format.muxer, path)
                try {
                    val stream = output.newStream()
                    stream.codecParameters.copyFrom(encoder)
                    stream.timeBase = encoder.timeBase
                    output.writeHeader()
                    return Recorder(output, stream, encoder.timeBase, path)
                } catch (error: Throwable) {
                    runCatching { output.close() }
                    throw error
                }
            }
        }
    }

    private companion object {
        fun even(value: Int): Int = max(2, value and 1.inv())
    }
}
