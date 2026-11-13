/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FFmpegWebTest {
    @Test
    fun emscriptenFileSystemRoundTrip() {
        val path = "ffmpeg-kmp-web-roundtrip.bin"
        val bytes = ByteArray(4096) { (it * 31).toByte() }
        FFmpegWeb.writeFile(path, bytes)
        assertTrue(FFmpegWeb.exists(path))
        assertContentEquals(bytes, FFmpegWeb.readFile(path))
        assertTrue(FFmpegWeb.deleteFile(path))
        assertFalse(FFmpegWeb.exists(path))
    }
}
