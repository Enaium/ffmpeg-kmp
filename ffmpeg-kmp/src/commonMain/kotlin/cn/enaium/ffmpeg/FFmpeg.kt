/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg

import cn.enaium.ffmpeg.avutil.AvVersion
import cn.enaium.ffmpeg.native.FFmpegNative

/** Aggregate entry point for the ffmpeg-kmp bundle. */
public object FFmpeg {
    /** Binding version: FFmpeg 9.0.2, binding revision 1. */
    public const val BINDING_VERSION: String = "9.0.2.1"

    /** FFmpeg version this binding was generated for. */
    public const val FFMPEG_VERSION: String = "9.0.2"

    /** `av_version_info()`, normally the git describe string. */
    public val versionInfo: String get() = FFmpegNative.versionInfo()

    public val configuration: String get() = FFmpegNative.configuration()
    public val license: String get() = FFmpegNative.license()

    public val avutilVersion: String get() = AvVersion.toString(FFmpegNative.avutilVersion())
    public val avcodecVersion: String get() = AvVersion.toString(FFmpegNative.avcodecVersion())
    public val avformatVersion: String get() = AvVersion.toString(FFmpegNative.avformatVersion())
    public val avfilterVersion: String get() = AvVersion.toString(FFmpegNative.avfilterVersion())
    public val swscaleVersion: String get() = AvVersion.toString(FFmpegNative.swscaleVersion())
    public val swresampleVersion: String get() =
        AvVersion.toString(FFmpegNative.swresampleVersion())

    public val avdeviceVersion: String get() = AvVersion.toString(FFmpegNative.avdeviceVersion())
}
