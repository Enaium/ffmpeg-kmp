/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avutil

import cn.enaium.ffmpeg.native.FFmpegNative

/** FFmpeg version numbers are packed as `major << 16 | minor << 8 | micro`. */
public object AvVersion {
    public fun major(version: Int): Int = version ushr 16
    public fun minor(version: Int): Int = (version ushr 8) and 0xFF
    public fun micro(version: Int): Int = version and 0xFF
    public fun toString(version: Int): String = "${major(version)}.${minor(version)}.${micro(version)}"
}

/** FFmpeg-wide build information. */
public object AvUtil {
    /** Binding version (FFmpeg 9.0.2, binding revision 1). */
    public const val BINDING_VERSION: String = "9.0.2.1"

    public val version: Int get() = FFmpegNative.avutilVersion()
    public val versionString: String get() = AvVersion.toString(version)
    public val configuration: String get() = FFmpegNative.configuration()
    public val license: String get() = FFmpegNative.license()

    /** Value reported by `av_version_info()`, normally the git describe string. */
    public val versionInfo: String get() = FFmpegNative.versionInfo()
}

/** `av_log` levels. */
public object AvLogLevel {
    public const val PANIC: Int = 0
    public const val FATAL: Int = 8
    public const val ERROR: Int = 16
    public const val WARNING: Int = 24
    public const val INFO: Int = 32
    public const val VERBOSE: Int = 40
    public const val DEBUG: Int = 48
    public const val TRACE: Int = 56

    /** `AV_LOG_QUIET`. */
    public const val QUIET: Int = -8
}

/** Process-wide FFmpeg log level. */
public object AvLog {
    public var level: Int
        get() = FFmpegNative.logLevel()
        set(value) { FFmpegNative.setLogLevel(value) }
    public fun log(level: Int, message: String): Unit = FFmpegNative.logMessage(level, message)
}
