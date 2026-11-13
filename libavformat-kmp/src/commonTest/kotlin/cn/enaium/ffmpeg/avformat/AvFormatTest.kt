/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avformat

import cn.enaium.ffmpeg.avcodec.AvCodecId
import cn.enaium.ffmpeg.avcodec.AvCodecParameters
import cn.enaium.ffmpeg.avcodec.AvMediaType
import cn.enaium.ffmpeg.avutil.AvPixelFormat
import cn.enaium.ffmpeg.avutil.AvRational
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AvFormatTest {
    @Test
    fun formatsAndVersions() {
        assertTrue(AvFormat.version > 0)
        assertTrue(AvFormat.versionString.isNotEmpty())

        val mpegts = AvFormat.findOutputFormat("mpegts")
        assertNotNull(mpegts)
        assertEquals("mpegts", mpegts.name)
        assertNotNull(AvFormat.findInputFormat("mpegts"))
        assertNotNull(AvFormat.findOutputFormat("mp4"))
        assertNotNull(AvFormat.findInputFormat("matroska"))
    }

    @Test
    fun writesMpegTsHeaderAndTrailer() {
        val filename = "ffmpeg-format-test-${Random.nextInt(Int.MAX_VALUE)}.ts"
        AvCodecParameters.create().use { parameters ->
            parameters.codecId = AvCodecId.MPEG4
            parameters.codecType = AvMediaType.VIDEO
            parameters.width = 32
            parameters.height = 32
            parameters.format = AvPixelFormat.YUV420P

            AvFormatContext.createOutput("mpegts", filename).use { context ->
                assertTrue(context.isOutput)
                assertEquals("mpegts", context.formatName)
                val stream = context.newStream()
                stream.setCodecParameters(parameters)
                stream.timeBase = AvRational(1, 90_000)
                assertEquals(AvRational(1, 90_000), stream.timeBase)
                context.writeHeader()
                assertEquals(1, context.streams.size)
                assertEquals(0, context.streams.first().index)
                context.writeTrailer()
            }
        }
    }
}
