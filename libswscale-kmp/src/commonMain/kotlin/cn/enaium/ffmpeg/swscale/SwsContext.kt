/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.swscale

import cn.enaium.ffmpeg.avutil.AvError
import cn.enaium.ffmpeg.avutil.AvException
import cn.enaium.ffmpeg.avutil.AvFrame
import cn.enaium.ffmpeg.avutil.AvVersion
import cn.enaium.ffmpeg.native.FFmpegNative

/** libswscale version and scaling flags. */
public object Sws {
    public val version: Int get() = FFmpegNative.swscaleVersion()
    public val versionString: String get() = AvVersion.toString(version)
}

/** `SWS_*` flags. */
public object SwsFlag {
    public const val FAST_BILINEAR: Int = 0x1
    public const val BILINEAR: Int = 0x2
    public const val BICUBIC: Int = 0x4
    public const val X: Int = 0x8
    public const val POINT: Int = 0x10
    public const val AREA: Int = 0x20
    public const val BICUBLIN: Int = 0x40
    public const val GAUSS: Int = 0x80
    public const val SINC: Int = 0x100
    public const val LANCZOS: Int = 0x200
    public const val SPLINE: Int = 0x400
    public const val ACCURATE_ROUNDING: Int = 1 shl 18
    public const val BITEXACT: Int = 1 shl 19
    public const val UNSTABLE: Int = 1 shl 20
}

/**
 * `SwsContext`: converts frames between pixel formats and sizes.
 *
 * Destination frames must be allocated with the target width, height and
 * format before calling [scale].
 */
public class SwsContext private constructor(
    handle: Long,
    public val sourceWidth: Int,
    public val sourceHeight: Int,
    public val sourceFormat: Int,
    public val destinationWidth: Int,
    public val destinationHeight: Int,
    public val destinationFormat: Int,
) : AutoCloseable {
    public var handle: Long = handle
        private set

    public fun scale(destination: AvFrame, source: AvFrame) {
        AvError.check(
            FFmpegNative.swsScaleFrame(handle, destination.handle, source.handle),
            "sws_scale_frame",
        )
    }

    override fun close() {
        if (handle != 0L) {
            FFmpegNative.swsFreeContext(handle)
            handle = 0L
        }
    }

    public companion object {
        public fun create(
            sourceWidth: Int,
            sourceHeight: Int,
            sourceFormat: Int,
            destinationWidth: Int,
            destinationHeight: Int,
            destinationFormat: Int,
            flags: Int = SwsFlag.BILINEAR,
        ): SwsContext {
            val handle = FFmpegNative.swsGetContext(
                sourceWidth,
                sourceHeight,
                sourceFormat,
                destinationWidth,
                destinationHeight,
                destinationFormat,
                flags,
            )
            if (handle == 0L) {
                throw AvException(
                    "sws_getContext(${sourceWidth}x$sourceHeight -> " +
                        "${destinationWidth}x$destinationHeight) failed",
                    FFmpegNative.lastError(),
                )
            }
            return SwsContext(
                handle,
                sourceWidth,
                sourceHeight,
                sourceFormat,
                destinationWidth,
                destinationHeight,
                destinationFormat,
            )
        }
    }
}
