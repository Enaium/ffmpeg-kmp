/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avdevice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AvDeviceTest {
    @Test
    fun registersAndFindsLavfi() {
        assertTrue(AvDevice.version > 0)
        AvDevice.registerAll()

        val lavfi = AvDevice.findInputFormat("lavfi")
        assertNotNull(lavfi)
        assertEquals("lavfi", lavfi.name)
    }
}
