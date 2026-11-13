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

/** `AVDictionary`: string key/value options passed to FFmpeg open calls. */
public class AvDictionary private constructor(handle: Long) : AutoCloseable {
    public var handle: Long = handle
        private set

    public val size: Int get() = FFmpegNative.dictCount(handle)

    public fun set(key: String, value: String) {
        AvError.check(FFmpegNative.dictSet(handle, key, value), "av_dict_set($key)")
    }

    public fun set(key: String, value: Long) {
        AvError.check(FFmpegNative.dictSetInt(handle, key, value), "av_dict_set_int($key)")
    }

    public fun get(key: String): String? = FFmpegNative.dictGet(handle, key)

    public fun getInt(key: String, fallback: Long = 0L): Long =
        FFmpegNative.dictGetInt(handle, key, fallback)

    override fun close() {
        if (handle != 0L) {
            FFmpegNative.dictFree(handle)
            handle = 0L
        }
    }

    public companion object {
        public fun create(): AvDictionary {
            val handle = FFmpegNative.dictCreate()
            if (handle == 0L) throw AvException("av_dict_create failed")
            return AvDictionary(handle)
        }
    }
}
