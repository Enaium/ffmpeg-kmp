/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example.core

import cn.enaium.camera.I420Frame
import cn.enaium.ffmpeg.AvFrame
import cn.enaium.ffmpeg.AvPixelFormat

/** Copies a camera frame into an FFmpeg YUV420P frame with allocated planes. */
internal fun I420Frame.toAvFrame(): AvFrame =
    AvFrame.video(width, height, AvPixelFormat.YUV420P).apply {
        writePlane(0, y)
        writePlane(1, u)
        writePlane(2, v)
    }
