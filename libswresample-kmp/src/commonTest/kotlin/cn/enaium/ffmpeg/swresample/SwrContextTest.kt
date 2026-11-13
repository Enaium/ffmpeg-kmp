/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.swresample

import cn.enaium.ffmpeg.avutil.AvFrame
import cn.enaium.ffmpeg.avutil.AvSampleFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwrContextTest {
    @Test
    fun resamplesS16StereoToFltpStereo() {
        assertTrue(Swr.version > 0)

        AvFrame.audio(44_100, AvSampleFormat.S16, 1024, 2).use { input ->
            val samples = ByteArray(input.planeSize(0))
            for (index in samples.indices step 2) {
                samples[index] = 0
                samples[index + 1] = (index % 255).toByte()
            }
            input.writePlane(0, samples)

            SwrContext.create(
                inputSampleRate = 44_100,
                inputFormat = AvSampleFormat.S16,
                inputChannels = 2,
                outputSampleRate = 48_000,
                outputFormat = AvSampleFormat.FLTP,
                outputChannels = 2,
            ).use { resampler ->
                val outSamples = resampler.outSamples(1024)
                assertTrue(outSamples > 0)

                AvFrame.create().use { output ->
                    output.setAudio(48_000, AvSampleFormat.FLTP, outSamples, 2)
                    resampler.convert(output, input)
                    assertTrue(output.nbSamples > 0)
                    assertEquals(48_000, output.sampleRate)
                    assertEquals(2, output.channels)
                    assertTrue(output.planeSize(0) > 0)
                }
            }
        }
    }

    @Test
    fun convertsPlanarFloatToPackedS16() {
        AvFrame.audio(48_000, AvSampleFormat.FLTP, 512, 2).use { input ->
            input.writePlane(0, ByteArray(input.planeSize(0)))
            input.writePlane(1, ByteArray(input.planeSize(1)))

            SwrContext.create(
                48_000,
                AvSampleFormat.FLTP,
                2,
                48_000,
                AvSampleFormat.S16,
                2,
            ).use { resampler ->
                AvFrame.create().use { output ->
                    output.setAudio(48_000, AvSampleFormat.S16, 512, 2)
                    resampler.convert(output, input)
                    assertTrue(output.nbSamples > 0)
                    assertEquals(AvSampleFormat.S16, output.format)
                }
            }
        }
    }
}
