/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avutil

/** Thrown when an FFmpeg call returns a negative `AVERROR` code. */
public class AvException(
    message: String,
    public val errorCode: Int = 0,
) : Exception(
    if (errorCode == 0) {
        message
    } else {
        "$message: ${FFmpegNativeErrorText.of(errorCode)} (code $errorCode)"
    },
)

/**
 * Error helpers. The codes are platform-dependent (`AVERROR(EAGAIN)` is not
 * necessarily `-11`), so they are resolved from the native library instead of
 * being hard-coded.
 */
public object AvError {
    public val EAGAIN: Int get() = cn.enaium.ffmpeg.native.FFmpegNative.errorEagain()
    public val EOF: Int get() = cn.enaium.ffmpeg.native.FFmpegNative.errorEof()

    public fun string(error: Int): String = cn.enaium.ffmpeg.native.FFmpegNative.errorString(error)
    public fun isAgain(error: Int): Boolean = error == EAGAIN
    public fun isEof(error: Int): Boolean = error == EOF

    /** Throws [AvException] for a negative code and returns the code otherwise. */
    public fun check(error: Int, message: String): Int {
        if (error < 0) throw AvException(message, error)
        return error
    }
}

/** Keeps the error text formatter out of [AvException]'s public surface. */
private object FFmpegNativeErrorText {
    fun of(code: Int): String = cn.enaium.ffmpeg.native.FFmpegNative.errorString(code)
}
