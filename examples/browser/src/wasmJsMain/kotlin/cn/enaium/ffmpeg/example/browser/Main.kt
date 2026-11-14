/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example.browser

import cn.enaium.ffmpeg.example.CameraFFmpegDemo
import cn.enaium.ffmpeg.example.core.DemoOptions
import cn.enaium.ffmpeg.example.core.SourceKind
import kotlinx.browser.window

/**
 * Browser entry point. SDL and Dear ImGui are initialized by the page before
 * this module is imported; the UI itself is the shared [CameraFFmpegDemo].
 *
 * The synthetic test pattern is used by default because browsers do not grant
 * camera access without HTTPS/user consent. The UI can switch to the camera
 * source when `getUserMedia` is available.
 */
fun main() {
    val options = DemoOptions(
        source = SourceKind.SYNTHETIC,
        width = 640,
        height = 480,
        frameRate = 30,
    )
    CameraFFmpegDemo.run(
        options = options,
        driver = CameraFFmpegDemo.FrameDriver { step, done ->
            fun tick() {
                if (step()) {
                    window.requestAnimationFrame { tick() }
                } else {
                    done()
                }
            }
            window.requestAnimationFrame { tick() }
        },
    )
}
