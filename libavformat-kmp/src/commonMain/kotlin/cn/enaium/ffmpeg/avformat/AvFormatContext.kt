/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avformat

import cn.enaium.ffmpeg.avcodec.AvCodec
import cn.enaium.ffmpeg.avcodec.AvCodecParameters
import cn.enaium.ffmpeg.avcodec.AvPacket
import cn.enaium.ffmpeg.avutil.AvDictionary
import cn.enaium.ffmpeg.avutil.AvError
import cn.enaium.ffmpeg.avutil.AvException
import cn.enaium.ffmpeg.avutil.AvRational
import cn.enaium.ffmpeg.avutil.AvVersion
import cn.enaium.ffmpeg.native.FFmpegNative

/** libavformat version and format lookup. */
public object AvFormat {
    public val version: Int get() = FFmpegNative.avformatVersion()
    public val versionString: String get() = AvVersion.toString(version)

    /** `av_find_input_format`, including devices registered by libavdevice. */
    public fun findInputFormat(name: String): AvInputFormat? =
        FFmpegNative.formatFindInputFormat(name).takeIf { it != 0L }?.let(::AvInputFormat)

    /** `av_guess_format` with a short name. */
    public fun findOutputFormat(name: String): AvOutputFormat? =
        FFmpegNative.formatFindOutputFormat(name).takeIf { it != 0L }?.let(::AvOutputFormat)
}

/** An `AVInputFormat` descriptor owned by libavformat. */
public class AvInputFormat internal constructor(public val handle: Long) {
    public val name: String? get() = FFmpegNative.inputFormatName(handle)
}

/** An `AVOutputFormat` descriptor owned by libavformat. */
public class AvOutputFormat internal constructor(public val handle: Long) {
    public val name: String? get() = FFmpegNative.outputFormatName(handle)
}

/** `AVStream`. Owned by its [AvFormatContext]; do not free. */
public class AvStream internal constructor(
    public val handle: Long,
    public val context: AvFormatContext,
) {
    public val index: Int get() = FFmpegNative.streamIndex(handle)

    public var timeBase: AvRational
        get() {
            val packed = FFmpegNative.streamTimeBase(handle)
            return AvRational((packed shr 32).toInt(), packed.toInt())
        }
        set(value) { FFmpegNative.streamSetTimeBase(handle, value.numerator, value.denominator) }
    public val duration: Long get() = FFmpegNative.streamDuration(handle)

    public val averageFrameRate: AvRational
        get() {
            val packed = FFmpegNative.streamAvgFrameRate(handle)
            return AvRational((packed shr 32).toInt(), packed.toInt())
        }

    /** Views the stream's codec parameters; [close] on the view does not free them. */
    public val codecParameters: AvCodecParameters
        get() = AvCodecParameters.borrowed(FFmpegNative.streamCodecpar(handle))

    /** Copies [parameters] into the stream (`avcodec_parameters_copy`). */
    public fun setCodecParameters(parameters: AvCodecParameters) {
        AvError.check(
            FFmpegNative.streamSetCodecpar(handle, parameters.handle),
            "avcodec_parameters_copy(stream)",
        )
    }
}

/** `AVSEEK_FLAG_*`. */
public object AvSeekFlag {
    public const val BACKWARD: Int = 1
    public const val BYTE: Int = 2
    public const val ANY: Int = 4
    public const val FRAME: Int = 8
}

/**
 * `AVFormatContext`: an input demuxer or an output muxer.
 *
 * Input: [openInput] -> [findStreamInfo] -> [readFrame] -> [close].
 * Output: [createOutput] -> [newStream] -> [writeHeader] -> [writePacket] ->
 * [writeTrailer] -> [close].
 */
public class AvFormatContext private constructor(
    handle: Long,
    private val output: Boolean,
) : AutoCloseable {
    public var handle: Long = handle
        private set

    public val isOutput: Boolean get() = output
    public val isOpen: Boolean get() = handle != 0L

    public val formatName: String? get() = FFmpegNative.formatName(handle)
    public val startTime: Long get() = FFmpegNative.formatStartTime(handle)
    public val duration: Long get() = FFmpegNative.formatDuration(handle)

    public val streams: List<AvStream>
        get() = (0 until FFmpegNative.formatNbStreams(handle)).mapNotNull { index ->
            FFmpegNative.formatGetStream(handle, index)
                .takeIf { it != 0L }
                ?.let { AvStream(it, this) }
        }

    /** Creates a new output stream; [codec] may be null. */
    public fun newStream(codec: AvCodec? = null): AvStream {
        check(output) { "newStream() is only valid on an output context" }
        val stream = FFmpegNative.formatNewStream(handle, codec?.handle ?: 0L)
        if (stream == 0L) throw AvException("avformat_new_stream failed")
        return AvStream(stream, this)
    }

    /** `avformat_find_stream_info`; input only. */
    public fun findStreamInfo(options: AvDictionary? = null) {
        check(!output) { "findStreamInfo() is only valid on an input context" }
        AvError.check(
            FFmpegNative.inputFindStreamInfo(handle, options?.handle ?: 0L),
            "avformat_find_stream_info",
        )
    }

    /** Reads the next packet; returns `false` on `AVERROR_EOF`. */
    public fun readFrame(packet: AvPacket): Boolean {
        check(!output) { "readFrame() is only valid on an input context" }
        val result = FFmpegNative.inputReadFrame(handle, packet.handle)
        if (result == AvError.EOF) return false
        AvError.check(result, "av_read_frame")
        return true
    }

    /** `av_seek_frame`; input only. */
    public fun seek(streamIndex: Int, timestamp: Long, flags: Int = AvSeekFlag.BACKWARD) {
        check(!output) { "seek() is only valid on an input context" }
        AvError.check(
            FFmpegNative.inputSeek(handle, streamIndex, timestamp, flags),
            "av_seek_frame",
        )
    }

    /** `avformat_write_header`; output only. */
    public fun writeHeader(options: AvDictionary? = null) {
        check(output) { "writeHeader() is only valid on an output context" }
        AvError.check(
            FFmpegNative.outputWriteHeader(handle, options?.handle ?: 0L),
            "avformat_write_header",
        )
    }

    /**
     * Writes an interleaved packet (`av_interleaved_write_frame`); output only.
     * The packet is consumed by the muxer.
     */
    public fun writePacket(packet: AvPacket) {
        check(output) { "writePacket() is only valid on an output context" }
        AvError.check(
            FFmpegNative.outputWritePacket(handle, packet.handle),
            "av_interleaved_write_frame",
        )
    }

    /** `av_write_trailer`; output only. */
    public fun writeTrailer() {
        check(output) { "writeTrailer() is only valid on an output context" }
        AvError.check(FFmpegNative.outputWriteTrailer(handle), "av_write_trailer")
    }

    override fun close() {
        if (handle != 0L) {
            if (output) {
                FFmpegNative.outputClose(handle)
            } else {
                FFmpegNative.inputClose(handle)
            }
            handle = 0L
        }
    }

    public companion object {
        /** Opens a local file or URL for demuxing, probing the input format. */
        public fun openInput(url: String, options: AvDictionary? = null): AvFormatContext {
            val handle = FFmpegNative.inputOpen(url, options?.handle ?: 0L)
            if (handle == 0L) {
                throw AvException(
                    "avformat_open_input('$url') failed",
                    FFmpegNative.lastError(),
                )
            }
            return AvFormatContext(handle, output = false)
        }

        /**
         * Opens a local file or URL with an explicit demuxer short name.
         * Content probing misdetects short MPEG-TS files as MPEG-PS, so
         * container-aware callers should pass the format they know.
         */
        public fun openInput(
            url: String,
            formatName: String,
            options: AvDictionary? = null,
        ): AvFormatContext {
            val handle = FFmpegNative.inputOpenFormat(
                url,
                formatName,
                options?.handle ?: 0L,
            )
            if (handle == 0L) {
                throw AvException(
                    "avformat_open_input('$url', format='$formatName') failed",
                    FFmpegNative.lastError(),
                )
            }
            return AvFormatContext(handle, output = false)
        }

        /**
         * Allocates an output context and opens its IO. [formatName] is a
         * muxer short name such as `mp4`, `mpegts` or `matroska`.
         */
        public fun createOutput(
            formatName: String,
            filename: String,
        ): AvFormatContext {
            val handle = FFmpegNative.outputOpen(formatName, filename)
            if (handle == 0L) {
                throw AvException(
                    "avformat_alloc_output_context2('$formatName', '$filename') failed",
                    FFmpegNative.lastError(),
                )
            }
            return AvFormatContext(handle, output = true)
        }
    }
}
