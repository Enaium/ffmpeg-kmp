/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example

import cn.enaium.ffmpeg.example.core.DEFAULT_BITRATE_KBPS
import cn.enaium.ffmpeg.example.core.DemoOptions
import cn.enaium.ffmpeg.example.core.FilterPreset
import cn.enaium.ffmpeg.example.core.RecordingFormat
import cn.enaium.ffmpeg.example.core.SourceKind

private fun parseOptions(args: Array<String>): DemoOptions {
    var frames = 0
    var source = SourceKind.CAMERA
    var headless = false
    var width = 640
    var height = 480
    var frameRate = 30
    var bitrateKbps = DEFAULT_BITRATE_KBPS
    var filter = FilterPreset.NONE
    var scalePercent = 100
    var container = RecordingFormat.MP4
    var recording = false
    var recordingPath = "camera-ffmpeg-${System.currentTimeMillis()}.mp4"
    var quitAfterMillis = 0L
    var index = 0
    while (index < args.size) {
        val arg = args[index]
        fun value(): String? = if (index + 1 < args.size) args[++index] else null
        when {
            arg == "--frames" -> frames = value()?.toIntOrNull() ?: 0
            arg.startsWith("--frames=") -> frames = arg.substringAfter('=').toIntOrNull() ?: 0
            arg == "--source" -> source = SourceKind.parse(value())
            arg.startsWith("--source=") -> source = SourceKind.parse(arg.substringAfter('='))
            arg == "--headless" -> headless = true
            arg == "--width" -> width = value()?.toIntOrNull() ?: width
            arg.startsWith("--width=") -> width = arg.substringAfter('=').toIntOrNull() ?: width
            arg == "--height" -> height = value()?.toIntOrNull() ?: height
            arg.startsWith("--height=") -> height = arg.substringAfter('=').toIntOrNull() ?: height
            arg == "--fps" -> frameRate = value()?.toIntOrNull() ?: frameRate
            arg.startsWith("--fps=") -> frameRate = arg.substringAfter('=').toIntOrNull() ?: frameRate
            arg == "--bitrate" -> bitrateKbps = value()?.toIntOrNull() ?: bitrateKbps
            arg.startsWith("--bitrate=") -> bitrateKbps = arg.substringAfter('=').toIntOrNull() ?: bitrateKbps
            arg == "--filter" -> filter = FilterPreset.parse(value())
            arg.startsWith("--filter=") -> filter = FilterPreset.parse(arg.substringAfter('='))
            arg == "--scale" -> scalePercent = value()?.removeSuffix("%")?.toIntOrNull() ?: scalePercent
            arg.startsWith("--scale=") ->
                scalePercent = arg.substringAfter('=').removeSuffix("%").toIntOrNull() ?: scalePercent
            arg == "--container" -> container = RecordingFormat.parse(value())
            arg.startsWith("--container=") -> container = RecordingFormat.parse(arg.substringAfter('='))
            arg == "--record" -> recording = true
            arg == "--record-path" -> {
                recordingPath = value() ?: recordingPath
                recording = true
            }

            arg.startsWith("--record-path=") -> {
                recordingPath = arg.substringAfter('=')
                recording = true
            }

            arg == "--quit-after-ms" -> quitAfterMillis = value()?.toLongOrNull() ?: quitAfterMillis
            arg.startsWith("--quit-after-ms=") ->
                quitAfterMillis = arg.substringAfter('=').toLongOrNull() ?: quitAfterMillis
        }
        index++
    }
    return DemoOptions(
        frames = frames,
        source = source,
        headless = headless,
        width = width,
        height = height,
        frameRate = frameRate,
        bitrateKbps = bitrateKbps,
        filter = filter,
        scalePercent = scalePercent,
        container = container,
        recording = recording,
        recordingPath = recordingPath,
        quitAfterMillis = quitAfterMillis,
    )
}

fun main(args: Array<String>) {
    val options = parseOptions(args)
    println(
        "ffmpeg-kmp camera example: ${options.width}x${options.height}@" +
            "${options.frameRate}, filter=${options.filter.label}, scale=${options.scalePercent}%",
    )
    CameraFFmpegDemo.run(options)
}
