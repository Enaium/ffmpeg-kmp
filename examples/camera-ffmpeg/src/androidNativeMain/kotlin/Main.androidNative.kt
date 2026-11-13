/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

import cn.enaium.ffmpeg.example.CameraFFmpegDemo
import cn.enaium.ffmpeg.example.core.DEFAULT_BITRATE_KBPS
import cn.enaium.ffmpeg.example.core.DemoOptions
import cn.enaium.ffmpeg.example.core.FilterPreset
import cn.enaium.ffmpeg.example.core.RecordingFormat
import cn.enaium.ffmpeg.example.core.SourceKind
import cn.enaium.sdl.SDL
import kotlin.native.CName
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.toKString
import platform.posix.getenv

/**
 * Android entry point. The SDL activity loads `libmain.so` and calls the
 * exported `SDL_main` symbol on its own thread; the camera permission has
 * already been requested by [cn.enaium.ffmpeg.example.MainActivity].
 */
@CName("SDL_main")
fun sdlMain(argc: Int, argv: CPointer<CPointerVar<ByteVar>>?): Int {
    fun env(name: String): String? = getenv(name)?.toKString()
    val record = env("CAMERA_FFMPEG_RECORD") == "1"
    val options = DemoOptions(
        frames = env("CAMERA_FFMPEG_FRAMES")?.toIntOrNull() ?: 0,
        source = SourceKind.parse(env("CAMERA_FFMPEG_SOURCE")),
        headless = false,
        width = env("CAMERA_FFMPEG_WIDTH")?.toIntOrNull() ?: 640,
        height = env("CAMERA_FFMPEG_HEIGHT")?.toIntOrNull() ?: 480,
        frameRate = env("CAMERA_FFMPEG_FPS")?.toIntOrNull() ?: 30,
        bitrateKbps = env("CAMERA_FFMPEG_BITRATE")?.toIntOrNull() ?: DEFAULT_BITRATE_KBPS,
        filter = FilterPreset.parse(env("CAMERA_FFMPEG_FILTER")),
        scalePercent = env("CAMERA_FFMPEG_SCALE")?.removeSuffix("%")?.toIntOrNull() ?: 100,
        container = RecordingFormat.parse(env("CAMERA_FFMPEG_CONTAINER")),
        recording = record,
        recordingPath = env("CAMERA_FFMPEG_RECORD_PATH") ?: "camera-ffmpeg-android.mp4",
        quitAfterMillis = 0,
    )
    val uiScale = env("CAMERA_FFMPEG_DPI_SCALE")?.toFloatOrNull() ?: 1f
    println(
        "ffmpeg-kmp camera example: ${options.width}x${options.height}@" +
            "${options.frameRate}, filter=${options.filter.label}, scale=${options.scalePercent}%",
    )
    CameraFFmpegDemo.run(options, uiScale)
    SDL.quit()
    return 0
}
