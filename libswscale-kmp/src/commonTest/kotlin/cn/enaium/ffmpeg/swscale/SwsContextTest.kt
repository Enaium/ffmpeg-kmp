/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.swscale

import cn.enaium.ffmpeg.avutil.AvFrame
import cn.enaium.ffmpeg.avutil.AvPixelFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwsContextTest {
    @Test
    fun scalesYuv420pToRgb24() {
        assertTrue(Sws.version > 0)

        AvFrame.video(64, 48, AvPixelFormat.YUV420P).use { source ->
            source.writePlane(0, ByteArray(source.planeSize(0)) { 128.toByte() })
            source.writePlane(1, ByteArray(source.planeSize(1)) { 64.toByte() })
            source.writePlane(2, ByteArray(source.planeSize(2)) { 192.toByte() })

            AvFrame.video(32, 24, AvPixelFormat.RGB24).use { destination ->
                SwsContext.create(
                    64,
                    48,
                    AvPixelFormat.YUV420P,
                    32,
                    24,
                    AvPixelFormat.RGB24,
                ).use { scaler ->
                    scaler.scale(destination, source)
                }

                assertEquals(32, destination.width)
                assertEquals(24, destination.height)
                assertEquals(AvPixelFormat.RGB24, destination.format)
                val rgb = destination.copyPlane(0)
                assertTrue(rgb.isNotEmpty())
                assertTrue(rgb.any { it != 0.toByte() })
            }
        }
    }

    @Test
    fun scalesRgb24ToYuv420p() {
        AvFrame.video(32, 32, AvPixelFormat.RGB24).use { source ->
            val rgb = ByteArray(source.planeSize(0))
            for (index in rgb.indices step 3) {
                rgb[index] = 0x20
                rgb[index + 1] = 0x80.toByte()
                rgb[index + 2] = 0xE0.toByte()
            }
            source.writePlane(0, rgb)

            AvFrame.video(16, 16, AvPixelFormat.YUV420P).use { destination ->
                SwsContext.create(
                    32,
                    32,
                    AvPixelFormat.RGB24,
                    16,
                    16,
                    AvPixelFormat.YUV420P,
                ).use { scaler ->
                    scaler.scale(destination, source)
                }
                assertTrue(destination.copyPlane(0).any { it != 0.toByte() })
            }
        }
    }
}
