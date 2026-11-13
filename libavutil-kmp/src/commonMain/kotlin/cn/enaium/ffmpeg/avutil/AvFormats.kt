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

/** `AVPixelFormat` values used by the examples; any other value can be passed as an Int. */
public object AvPixelFormat {
    public const val NONE: Int = -1
    public const val YUV420P: Int = 0
    public const val YUYV422: Int = 1
    public const val RGB24: Int = 2
    public const val BGR24: Int = 3
    public const val YUV422P: Int = 4
    public const val YUV444P: Int = 5
    public const val GRAY8: Int = 8
    public const val YUVJ420P: Int = 12
    public const val NV12: Int = 23
    public const val NV21: Int = 24
    public const val ARGB: Int = 25
    public const val RGBA: Int = 26
    public const val ABGR: Int = 27
    public const val BGRA: Int = 28
    public const val GRAY16BE: Int = 29
    public const val GRAY16LE: Int = 30
    public const val YUVA420P: Int = 33
    public const val YUV420P10LE: Int = 64
    public const val GBRP: Int = 73

    /** `av_get_pix_fmt_name`, or `null` for an unknown value. */
    public fun name(format: Int): String? = FFmpegNative.pixFmtName(format)

    /** `av_get_pix_fmt`, or [NONE] for an unknown name. */
    public fun valueOf(name: String): Int = FFmpegNative.pixFmtGet(name)
}

/** `AVSampleFormat` values. */
public object AvSampleFormat {
    public const val NONE: Int = -1
    public const val U8: Int = 0
    public const val S16: Int = 1
    public const val S32: Int = 2
    public const val FLT: Int = 3
    public const val DBL: Int = 4
    public const val U8P: Int = 5
    public const val S16P: Int = 6
    public const val S32P: Int = 7
    public const val FLTP: Int = 8
    public const val DBLP: Int = 9
    public const val S64: Int = 10
    public const val S64P: Int = 11

    public fun name(format: Int): String? = FFmpegNative.sampleFmtName(format)
    public fun valueOf(name: String): Int = FFmpegNative.sampleFmtGet(name)
    public fun bytesPerSample(format: Int): Int = FFmpegNative.sampleFmtBytes(format)
}
