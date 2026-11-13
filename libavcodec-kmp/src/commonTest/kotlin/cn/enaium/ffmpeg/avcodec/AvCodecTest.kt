/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avcodec

import cn.enaium.ffmpeg.avutil.AvFrame
import cn.enaium.ffmpeg.avutil.AvPixelFormat
import cn.enaium.ffmpeg.avutil.AvRational
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AvCodecTest {
    @Test
    fun codecLookup() {
        val encoder = AvCodec.findEncoder("mpeg4")
        assertNotNull(encoder)
        assertEquals("mpeg4", encoder.name)
        assertEquals(AvCodecId.MPEG4, encoder.id)
        assertEquals(AvMediaType.VIDEO, encoder.mediaType)
        assertTrue(encoder.isEncoder)
        assertTrue(!encoder.isDecoder)

        val decoder = AvCodec.findDecoder("mpeg4")
        assertNotNull(decoder)
        assertTrue(decoder.isDecoder)
        assertEquals(AvCodecId.MPEG4, AvCodec.idFromName("mpeg4"))
        assertNull(AvCodec.findDecoder("definitely-not-a-codec"))
    }

    @Test
    fun encodeAndDecodeMpeg4() {
        val encoder = AvCodec.encoder("mpeg4")
        val encoded = AvCodecContext.create(encoder).use { context ->
            context.width = 32
            context.height = 32
            context.pixelFormat = AvPixelFormat.YUV420P
            context.timeBase = AvRational(1, 25)
            context.bitRate = 200_000
            context.gopSize = 10
            context.maxBFrames = 0
            context.open()

            assertEquals(AvRational(1, 25), context.timeBase)
            assertEquals(32, context.width)
            assertEquals(32, context.height)
            assertEquals(AvPixelFormat.YUV420P, context.pixelFormat)

            AvFrame.video(32, 32, AvPixelFormat.YUV420P).use { frame ->
                frame.writePlane(0, ByteArray(frame.planeSize(0)) { 16 })
                frame.writePlane(1, ByteArray(frame.planeSize(1)) { 128.toByte() })
                frame.writePlane(2, ByteArray(frame.planeSize(2)) { 128.toByte() })
                frame.pts = 0
                context.sendFrame(frame)
            }

            val packet = context.receivePacket()
            assertNotNull(packet)
            assertTrue(packet.size > 0)
            val bytes = packet.data()
            packet.close()

            context.sendFrame(null)
            while (true) {
                val remaining = context.receivePacket() ?: break
                remaining.close()
            }
            bytes
        }

        val decoder = AvCodec.decoder("mpeg4")
        AvCodecContext.create(decoder).use { context ->
            context.open()
            val packet = AvPacket.create()
            packet.setData(encoded)
            context.sendPacket(packet)
            val decoded = context.receiveFrame()
            assertNotNull(decoded)
            decoded.use { frame ->
                assertEquals(32, frame.width)
                assertEquals(32, frame.height)
                assertEquals(AvPixelFormat.YUV420P, frame.format)
            }
            packet.close()
        }
    }

    @Test
    fun packetFields() {
        AvPacket.create().use { packet ->
            val data = ByteArray(128) { it.toByte() }
            packet.allocateData(data.size)
            packet.setData(data)
            assertEquals(data.size, packet.size)
            assertEquals(data.toList(), packet.data().toList())

            packet.pts = 100
            packet.dts = 90
            packet.duration = 10
            packet.streamIndex = 2
            packet.flags = AvPacketFlag.KEY
            assertEquals(100, packet.pts)
            assertEquals(90, packet.dts)
            assertEquals(10, packet.duration)
            assertEquals(2, packet.streamIndex)
            assertTrue(packet.isKey)

            packet.rescaleTs(AvRational(1, 25), AvRational(1, 90000))
            assertEquals(360_000, packet.pts)
        }
    }

    @Test
    fun codecParametersRoundTrip() {
        val encoder = AvCodec.encoder("mpeg4")
        AvCodecContext.create(encoder).use { context ->
            context.width = 48
            context.height = 32
            context.pixelFormat = AvPixelFormat.YUV420P
            context.timeBase = AvRational(1, 25)
            context.bitRate = 100_000
            context.open()

            val parameters = context.codecParameters()
            assertEquals(48, parameters.width)
            assertEquals(32, parameters.height)
            assertEquals(AvCodecId.MPEG4, parameters.codecId)
            assertEquals(AvMediaType.VIDEO, parameters.codecType)

            AvCodecContext.create(AvCodec.decoder("mpeg4")).use { decoderContext ->
                decoderContext.applyParameters(parameters)
                assertEquals(48, decoderContext.width)
                assertEquals(32, decoderContext.height)
            }
            parameters.close()
        }
    }
}
