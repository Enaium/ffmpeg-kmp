/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example.core

/** Tightly packed RGBA8888 pixels ready for an SDL streaming texture. */
class DisplayFrame(
    val width: Int,
    val height: Int,
    val rgba: ByteArray,
) {
    init {
        require(width > 0 && height > 0) { "display size must be positive" }
        require(rgba.size >= width * height * 4) { "RGBA buffer is too small" }
    }
}

/** Counters printed by the headless run and shown by the UI. */
data class PipelineStats(
    val inputFrames: Long = 0,
    val encodedFrames: Long = 0,
    val encodedBytes: Long = 0,
    val decodedFrames: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val measuredFps: Double = 0.0,
) {
    val averageFrameBytes: Long get() = if (encodedFrames == 0L) 0 else encodedBytes / encodedFrames

    override fun toString(): String =
        "${width}x$height, input=$inputFrames encoded=$encodedFrames decoded=$decodedFrames, " +
            "${averageFrameBytes} B/frame, ${(measuredFps * 10).toInt() / 10.0} fps"
}

/** One processed camera frame. */
data class PipelineOutput(
    val camera: DisplayFrame,
    val filtered: DisplayFrame?,
    val stats: PipelineStats,
    val recordingPath: String?,
)
