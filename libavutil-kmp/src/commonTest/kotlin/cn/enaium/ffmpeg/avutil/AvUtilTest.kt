/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avutil

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AvUtilTest {
    @Test
    fun versions() {
        assertTrue(AvUtil.version > 0)
        assertEquals("61.1.102", AvUtil.versionString)
        assertTrue(AvUtil.versionInfo.isNotEmpty())
        assertTrue(AvUtil.configuration.isNotEmpty())
        assertTrue(AvUtil.license.isNotEmpty())
    }

    @Test
    fun rational() {
        assertEquals(0.5, AvRational(1, 2).toDouble())
        assertEquals(AvRational(2, 1), AvRational(1, 2).inverse)
        assertEquals(0.0, AvRational(1, 0).toDouble())
    }

    @Test
    fun formats() {
        assertEquals(AvPixelFormat.YUV420P, AvPixelFormat.valueOf("yuv420p"))
        assertEquals("yuv420p", AvPixelFormat.name(AvPixelFormat.YUV420P))
        assertEquals(AvPixelFormat.NV12, AvPixelFormat.valueOf("nv12"))
        assertEquals("rgba", AvPixelFormat.name(AvPixelFormat.RGBA))
        assertEquals(AvPixelFormat.NONE, AvPixelFormat.valueOf("definitely-not-a-format"))

        assertEquals(AvSampleFormat.S16, AvSampleFormat.valueOf("s16"))
        assertEquals("fltp", AvSampleFormat.name(AvSampleFormat.FLTP))
        assertEquals(2, AvSampleFormat.bytesPerSample(AvSampleFormat.S16))
        assertEquals(4, AvSampleFormat.bytesPerSample(AvSampleFormat.FLTP))
    }

    @Test
    fun errors() {
        assertTrue(AvError.EAGAIN < 0)
        assertTrue(AvError.EOF < 0)
        assertTrue(AvError.isAgain(AvError.EAGAIN))
        assertTrue(AvError.isEof(AvError.EOF))
        assertTrue(AvError.string(AvError.EOF).isNotEmpty())
    }

    @Test
    fun dictionary() {
        AvDictionary.create().use { dictionary ->
            dictionary.set("alpha", "one")
            dictionary.set("beta", 42)
            assertEquals("one", dictionary.get("alpha"))
            assertEquals(42L, dictionary.getInt("beta"))
            assertEquals(7L, dictionary.getInt("missing", 7L))
            assertEquals(2, dictionary.size)
            assertEquals("one", dictionary.get("alpha"))
        }
    }

    @Test
    fun videoFramePlanes() {
        AvFrame.video(32, 32, AvPixelFormat.YUV420P).use { frame ->
            assertEquals(32, frame.width)
            assertEquals(32, frame.height)
            assertEquals(AvPixelFormat.YUV420P, frame.format)
            assertTrue(frame.linesize(0) >= 32)
            assertTrue(frame.planeSize(1) > 0)

            val luma = ByteArray(frame.planeSize(0)) { 16 }
            val chroma = ByteArray(frame.planeSize(1)) { 128.toByte() }
            assertEquals(luma.size, frame.writePlane(0, luma))
            assertEquals(chroma.size, frame.writePlane(1, chroma))
            assertContentEquals(luma, frame.copyPlane(0))
            assertContentEquals(chroma, frame.copyPlane(1))

            frame.pts = 1234
            assertEquals(1234, frame.pts)
            frame.sampleAspectRatio = AvRational(1, 1)
            assertEquals(AvRational(1, 1), frame.sampleAspectRatio)
        }
    }

    @Test
    fun audioFramePlanes() {
        AvFrame.audio(44100, AvSampleFormat.S16, 1024, 2).use { frame ->
            assertEquals(1024, frame.nbSamples)
            assertEquals(44100, frame.sampleRate)
            assertEquals(2, frame.channels)
            assertEquals(AvSampleFormat.S16, frame.format)
            assertEquals(4096, frame.planeSize(0))

            val samples = ByteArray(frame.planeSize(0))
            assertEquals(samples.size, frame.writePlane(0, samples))
            assertContentEquals(samples, frame.copyPlane(0))
            assertNotNull(frame)
        }
    }
}
