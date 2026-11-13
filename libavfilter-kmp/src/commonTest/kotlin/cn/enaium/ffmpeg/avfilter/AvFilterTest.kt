/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avfilter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AvFilterTest {
    @Test
    fun colorSourceToBufferSink() {
        assertTrue(AvFilter.version > 0)

        AvFilterGraph.create().use { graph ->
            val source = graph.createFilter("color", "src", "c=red:s=32x32:r=1")
            assertEquals("src", source.instanceName)
            val sink = graph.createFilter("buffersink", "sink")
            graph.link(source, 0, sink, 0)
            graph.configure()

            val frame = graph.getFrame(sink)
            assertNotNull(frame)
            frame.use {
                assertEquals(32, it.width)
                assertEquals(32, it.height)
                assertTrue(it.format >= 0)
                assertTrue(it.planeSize(0) > 0)
            }
        }
    }

    @Test
    fun bufferSourcePushAndPull() {
        AvFilterGraph.create().use { graph ->
            val source = graph.createFilter(
                "buffer",
                "in",
                "video_size=64x48:pix_fmt=0:time_base=1/25:pixel_aspect=1/1",
            )
            val sink = graph.createFilter("buffersink", "out")
            graph.link(source, 0, sink, 0)
            graph.configure()

            cn.enaium.ffmpeg.avutil.AvFrame.video(
                64,
                48,
                cn.enaium.ffmpeg.avutil.AvPixelFormat.YUV420P,
            ).use { frame ->
                frame.writePlane(0, ByteArray(frame.planeSize(0)) { 128.toByte() })
                frame.writePlane(1, ByteArray(frame.planeSize(1)) { 128.toByte() })
                frame.writePlane(2, ByteArray(frame.planeSize(2)) { 128.toByte() })
                frame.pts = 0
                graph.addFrame(source, frame)
            }

            val filtered = graph.getFrame(sink)
            assertNotNull(filtered)
            filtered.use {
                assertEquals(64, it.width)
                assertEquals(48, it.height)
            }
        }
    }
}
