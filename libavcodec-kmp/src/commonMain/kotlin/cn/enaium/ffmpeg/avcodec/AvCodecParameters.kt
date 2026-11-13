/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avcodec

import cn.enaium.ffmpeg.avutil.AvError
import cn.enaium.ffmpeg.avutil.AvException
import cn.enaium.ffmpeg.avutil.AvRational
import cn.enaium.ffmpeg.native.FFmpegNative

/** `AVCodecParameters`: codec configuration stored in an `AVStream`. */
public class AvCodecParameters private constructor(
    handle: Long,
    private val owned: Boolean,
) : AutoCloseable {
    public var handle: Long = handle
        private set

    public var width: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.WIDTH)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.WIDTH, value) }
    public var height: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.HEIGHT)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.HEIGHT, value) }
    public var format: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.FORMAT)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.FORMAT, value) }
    public var sampleRate: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.SAMPLE_RATE)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.SAMPLE_RATE, value) }
    public var channels: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.CHANNELS)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.CHANNELS, value) }
    public var profile: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.PROFILE)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.PROFILE, value) }
    public var level: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.LEVEL)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.LEVEL, value) }
    public var frameSize: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.FRAME_SIZE)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.FRAME_SIZE, value) }
    public var codecId: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.CODEC_ID)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.CODEC_ID, value) }
    public var codecType: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.CODEC_TYPE)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.CODEC_TYPE, value) }
    public var codecTag: Int
        get() = FFmpegNative.codecparGetInt(handle, AvCodecParametersField.CODEC_TAG)
        set(value) { FFmpegNative.codecparSetInt(handle, AvCodecParametersField.CODEC_TAG, value) }
    public var bitRate: Long
        get() = FFmpegNative.codecparGetInt64(handle, AvCodecParametersInt64Field.BIT_RATE)
        set(value) {
            FFmpegNative.codecparSetInt64(handle, AvCodecParametersInt64Field.BIT_RATE, value)
        }

    public var sampleAspectRatio: AvRational
        get() {
            val packed = FFmpegNative.codecparGetRational(
                handle,
                AvCodecParametersRationalField.SAMPLE_ASPECT_RATIO,
            )
            return AvRational((packed shr 32).toInt(), packed.toInt())
        }
        set(value) {
            FFmpegNative.codecparSetRational(
                handle,
                AvCodecParametersRationalField.SAMPLE_ASPECT_RATIO,
                value.numerator,
                value.denominator,
            )
        }

    public val extradata: ByteArray
        get() {
            val size = FFmpegNative.codecparExtradataSize(handle)
            if (size <= 0) return ByteArray(0)
            val out = ByteArray(size)
            FFmpegNative.codecparCopyExtradata(handle, out)
            return out
        }

    public fun setExtradata(data: ByteArray) {
        AvError.check(
            FFmpegNative.codecparSetExtradata(handle, data),
            "avcodec_parameters_set_extradata",
        )
    }

    /** `avcodec_parameters_from_context`. */
    public fun copyFrom(context: AvCodecContext) {
        AvError.check(
            FFmpegNative.codecparFromContext(handle, context.handle),
            "avcodec_parameters_from_context",
        )
    }

    /** `avcodec_parameters_to_context`. */
    public fun applyTo(context: AvCodecContext) {
        AvError.check(
            FFmpegNative.codecparToContext(context.handle, handle),
            "avcodec_parameters_to_context",
        )
    }

    /** `avcodec_parameters_copy`. */
    public fun copyFrom(other: AvCodecParameters) {
        AvError.check(
            FFmpegNative.codecparCopy(handle, other.handle),
            "avcodec_parameters_copy",
        )
    }

    override fun close() {
        if (owned && handle != 0L) {
            FFmpegNative.codecparFree(handle)
            handle = 0L
        }
    }

    public companion object {
        public fun create(): AvCodecParameters {
            val handle = FFmpegNative.codecparAlloc()
            if (handle == 0L) throw AvException("avcodec_parameters_alloc failed")
            return AvCodecParameters(handle, owned = true)
        }

        /**
         * Wraps a codec-parameters pointer owned by another object (for example
         * `AVStream.codecpar`). [close] does not free it.
         */
        public fun borrowed(handle: Long): AvCodecParameters =
            AvCodecParameters(handle, owned = false)
    }
}
