/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg

import cn.enaium.ffmpeg.avcodec.AvCodec
import cn.enaium.ffmpeg.avcodec.AvCodecContext
import cn.enaium.ffmpeg.avcodec.AvMediaType
import cn.enaium.ffmpeg.avcodec.AvPacket
import cn.enaium.ffmpeg.avformat.AvFormatContext
import cn.enaium.ffmpeg.avutil.AvFrame
import cn.enaium.ffmpeg.avutil.AvPixelFormat
import cn.enaium.ffmpeg.avutil.AvRational
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FFmpegIntegrationTest {
    @Test
    fun versionSurface() {
        assertEquals("9.0.2.1", FFmpeg.BINDING_VERSION)
        assertEquals("9.0.2", FFmpeg.FFMPEG_VERSION)
        assertTrue(FFmpeg.versionInfo.isNotEmpty())
        assertTrue(FFmpeg.avutilVersion.startsWith("61."))
        assertTrue(FFmpeg.avcodecVersion.startsWith("63."))
        assertTrue(FFmpeg.avformatVersion.startsWith("63."))
        assertTrue(FFmpeg.avfilterVersion.startsWith("12."))
        assertTrue(FFmpeg.swscaleVersion.startsWith("10."))
        assertTrue(FFmpeg.swresampleVersion.startsWith("7."))
        assertTrue(FFmpeg.avdeviceVersion.startsWith("63."))
    }

    @Test
    fun encodeMuxDemuxDecode() {
        val filename = "ffmpeg-kmp-integration-${Random.nextInt(Int.MAX_VALUE)}.ts"
        val width = 64
        val height = 48
        var packetsWritten = 0

        val encoder = AvCodec.encoder("mpeg4")
        AvCodecContext.create(encoder).use { encode ->
            encode.width = width
            encode.height = height
            encode.pixelFormat = AvPixelFormat.YUV420P
            encode.timeBase = AvRational(1, 25)
            encode.bitRate = 400_000
            encode.gopSize = 12
            encode.maxBFrames = 0
            encode.open()

            AvFormatContext.createOutput("mpegts", filename).use { output ->
                val stream = output.newStream(encoder)
                stream.codecParameters.copyFrom(encode)
                stream.timeBase = encode.timeBase
                output.writeHeader()

                fun writeAvailable() {
                    while (true) {
                        val packet = encode.receivePacket() ?: break
                        packet.use {
                            it.streamIndex = stream.index
                            it.rescaleTs(encode.timeBase, stream.timeBase)
                            output.writePacket(it)
                            packetsWritten++
                        }
                    }
                }

                for (index in 0 until 5) {
                    AvFrame.video(width, height, AvPixelFormat.YUV420P).use { frame ->
                        frame.writePlane(0, ByteArray(frame.planeSize(0)) { 96.toByte() })
                        frame.writePlane(1, ByteArray(frame.planeSize(1)) { 96.toByte() })
                        frame.writePlane(2, ByteArray(frame.planeSize(2)) { 160.toByte() })
                        frame.pts = index.toLong()
                        encode.sendFrame(frame)
                    }
                    writeAvailable()
                }
                encode.sendFrame(null)
                writeAvailable()
                output.writeTrailer()
            }
        }

        assertTrue(packetsWritten >= 1)

        var decodedFrames = 0
        AvFormatContext.openInput(filename, "mpegts").use { input ->
            input.findStreamInfo()
            val stream = input.streams.first {
                it.codecParameters.codecType == AvMediaType.VIDEO
            }
            val decoder = AvCodec.findDecoder(stream.codecParameters.codecId)
                ?: error("No decoder for codec id ${stream.codecParameters.codecId}")
            assertEquals(AvMediaType.VIDEO, decoder.mediaType)

            AvCodecContext.create(decoder).use { decode ->
                decode.applyParameters(stream.codecParameters)
                decode.open()

                val packet = AvPacket.create()
                while (input.readFrame(packet)) {
                    if (packet.streamIndex == stream.index) {
                        decode.sendPacket(packet)
                        while (true) {
                            val frame = decode.receiveFrame() ?: break
                            frame.use {
                                assertEquals(width, it.width)
                                assertEquals(height, it.height)
                            }
                            decodedFrames++
                        }
                    }
                    packet.unref()
                }
                packet.close()

                decode.sendPacket(null)
                while (true) {
                    val frame = decode.receiveFrame() ?: break
                    frame.close()
                    decodedFrames++
                }
            }
        }

        assertTrue(decodedFrames >= 1, "decoded $decodedFrames frames")
    }
}
