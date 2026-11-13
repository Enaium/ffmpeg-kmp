/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cn.enaium.ffmpeg.example.CameraFFmpegDemo
import cn.enaium.ffmpeg.example.core.DEFAULT_BITRATE_KBPS
import cn.enaium.ffmpeg.example.core.DemoOptions
import cn.enaium.ffmpeg.example.core.FilterPreset
import cn.enaium.ffmpeg.example.core.RecordingFormat
import cn.enaium.ffmpeg.example.core.SourceKind
import kotlinx.cinterop.toKString
import platform.posix.getenv

/**
 * Desktop native entry point. Options arrive through environment variables so
 * the same binary works in a terminal and in a headless CI job:
 *
 * `CAMERA_FFMPEG_FRAMES`, `CAMERA_FFMPEG_SOURCE` (`camera`/`synthetic`),
 * `CAMERA_FFMPEG_HEADLESS`, `CAMERA_FFMPEG_WIDTH`, `CAMERA_FFMPEG_HEIGHT`,
 * `CAMERA_FFMPEG_FPS`, `CAMERA_FFMPEG_BITRATE`, `CAMERA_FFMPEG_FILTER`,
 * `CAMERA_FFMPEG_SCALE`, `CAMERA_FFMPEG_RECORD`, `CAMERA_FFMPEG_RECORD_PATH`,
 * `CAMERA_FFMPEG_QUIT_AFTER_MS`.
 */
private fun nativeOptions(): DemoOptions {
    fun env(name: String): String? = getenv(name)?.toKString()
    val record = env("CAMERA_FFMPEG_RECORD") == "1"
    return DemoOptions(
        frames = env("CAMERA_FFMPEG_FRAMES")?.toIntOrNull() ?: 0,
        source = SourceKind.parse(env("CAMERA_FFMPEG_SOURCE")),
        headless = env("CAMERA_FFMPEG_HEADLESS") == "1",
        width = env("CAMERA_FFMPEG_WIDTH")?.toIntOrNull() ?: 640,
        height = env("CAMERA_FFMPEG_HEIGHT")?.toIntOrNull() ?: 480,
        frameRate = env("CAMERA_FFMPEG_FPS")?.toIntOrNull() ?: 30,
        bitrateKbps = env("CAMERA_FFMPEG_BITRATE")?.toIntOrNull() ?: DEFAULT_BITRATE_KBPS,
        filter = FilterPreset.parse(env("CAMERA_FFMPEG_FILTER")),
        scalePercent = env("CAMERA_FFMPEG_SCALE")?.removeSuffix("%")?.toIntOrNull() ?: 100,
        container = RecordingFormat.parse(env("CAMERA_FFMPEG_CONTAINER")),
        recording = record,
        recordingPath = env("CAMERA_FFMPEG_RECORD_PATH") ?: "camera-ffmpeg-native.mp4",
        quitAfterMillis = env("CAMERA_FFMPEG_QUIT_AFTER_MS")?.toLongOrNull() ?: 0,
    )
}

fun main() {
    val options = nativeOptions()
    println(
        "ffmpeg-kmp camera example: ${options.width}x${options.height}@" +
            "${options.frameRate}, filter=${options.filter.label}, scale=${options.scalePercent}%",
    )
    CameraFFmpegDemo.run(options)
}
