/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example.core

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FFmpegPipelineTest {
    @Test
    fun filtersScalesEncodesDecodesAndRecords() {
        val pattern = TestPattern(160, 120, frameRate = 25)
        val pipeline = FFmpegPipeline(frameRate = 25, bitrateKbps = 600)
        try {
            var output: PipelineOutput? = null
            repeat(12) { index ->
                output = pipeline.process(
                    pattern.next(),
                    PipelineSettings(
                        filter = FilterPreset.NEGATE,
                        scalePercent = 50,
                        record = true,
                        recordingPath = "ffmpeg-example-core-test.mp4",
                    ),
                )
            }
            val last = assertNotNull(output)
            assertNotNull(last.filtered)
            assertTrue(last.filtered.width == 80)
            assertTrue(last.filtered.height == 60)
            assertTrue(last.stats.encodedFrames > 0)
            assertTrue(last.stats.decodedFrames > 0)
            assertNotNull(last.recordingPath)
        } finally {
            pipeline.close()
        }
    }

    @Test
    fun filterAndRecordingChangesRecreateThePipeline() {
        val pattern = TestPattern(96, 64, frameRate = 25)
        FFmpegPipeline(frameRate = 25, bitrateKbps = 500).use { pipeline ->
            repeat(4) {
                pipeline.process(
                    pattern.next(),
                    PipelineSettings(filter = FilterPreset.NONE, scalePercent = 100, record = true),
                )
            }
            repeat(4) {
                pipeline.process(
                    pattern.next(),
                    PipelineSettings(filter = FilterPreset.NEGATE, scalePercent = 50, record = false),
                )
            }
            val output = pipeline.process(
                pattern.next(),
                PipelineSettings(filter = FilterPreset.GBLUR, scalePercent = 50, record = true),
            )
            assertTrue(output.filtered == null || output.filtered!!.width == 48)
            assertTrue(output.stats.encodedFrames > 0)
        }
    }

    @Test
    fun everyRecordingContainerCreatesAFile() {
        val pattern = TestPattern(64, 48, frameRate = 25)
        FFmpegPipeline(frameRate = 25, bitrateKbps = 500).use { pipeline ->
            RecordingFormat.entries.forEach { format ->
                val output = pipeline.process(
                    pattern.next(),
                    PipelineSettings(
                        filter = FilterPreset.NONE,
                        scalePercent = 100,
                        record = true,
                        recordingFormat = format,
                        recordingPath = "ffmpeg-container-test.mp4",
                    ),
                )
                val path = assertNotNull(output.recordingPath)
                assertTrue(path.endsWith(".${format.extension}"), "unexpected path $path")
            }
        }
    }

    @Test
    fun noFilterAndFullScale() {
        val pattern = TestPattern(64, 48)
        FFmpegPipeline(frameRate = 30, bitrateKbps = 400).use { pipeline ->
            val output = pipeline.process(
                pattern.next(),
                PipelineSettings(filter = FilterPreset.NONE, scalePercent = 100),
            )
            assertTrue(output.camera.width == 64)
            assertTrue(output.camera.height == 48)
            assertTrue(output.stats.encodedFrames > 0)
        }
    }
}
