/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avdevice

import cn.enaium.ffmpeg.avformat.AvFormat
import cn.enaium.ffmpeg.avformat.AvInputFormat
import cn.enaium.ffmpeg.avutil.AvVersion
import cn.enaium.ffmpeg.native.FFmpegNative

/**
 * libavdevice registration and device lookup.
 *
 * `avdevice_register_all()` is required before `AvFormat.findInputFormat`
 * can return device formats such as `avfoundation`, `dshow` or `lavfi`.
 */
public object AvDevice {
    public val version: Int get() = FFmpegNative.avdeviceVersion()
    public val versionString: String get() = AvVersion.toString(version)

    public fun registerAll() {
        FFmpegNative.deviceRegisterAll()
    }

    /** Registers every device and resolves an input format by short name. */
    public fun findInputFormat(name: String): AvInputFormat? {
        registerAll()
        return AvFormat.findInputFormat(name)
    }
}
