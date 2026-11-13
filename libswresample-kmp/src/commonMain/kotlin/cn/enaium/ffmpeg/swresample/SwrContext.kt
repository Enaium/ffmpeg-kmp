/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.swresample

import cn.enaium.ffmpeg.avutil.AvError
import cn.enaium.ffmpeg.avutil.AvException
import cn.enaium.ffmpeg.avutil.AvFrame
import cn.enaium.ffmpeg.avutil.AvVersion
import cn.enaium.ffmpeg.native.FFmpegNative

/** libswresample version. */
public object Swr {
    public val version: Int get() = FFmpegNative.swresampleVersion()
    public val versionString: String get() = AvVersion.toString(version)
}

/**
 * `SwrContext`: converts audio sample rate, sample format and channel count.
 *
 * The output frame must be configured (rate/format/channels, and `nb_samples`
 * large enough for [outSamples]) before [convert].
 */
public class SwrContext private constructor(
    handle: Long,
    public val inputSampleRate: Int,
    public val inputFormat: Int,
    public val inputChannels: Int,
    public val outputSampleRate: Int,
    public val outputFormat: Int,
    public val outputChannels: Int,
) : AutoCloseable {
    public var handle: Long = handle
        private set

    public fun convert(output: AvFrame, input: AvFrame?) {
        AvError.check(
            FFmpegNative.swrConvertFrame(handle, output.handle, input?.handle ?: 0L),
            "swr_convert_frame",
        )
    }

    /** Upper bound on the output samples for [inputSamples] input samples. */
    public fun outSamples(inputSamples: Int): Int =
        AvError.check(
            FFmpegNative.swrOutSamples(handle, inputSamples),
            "swr_get_out_samples",
        )

    override fun close() {
        if (handle != 0L) {
            FFmpegNative.swrFree(handle)
            handle = 0L
        }
    }

    public companion object {
        public fun create(
            inputSampleRate: Int,
            inputFormat: Int,
            inputChannels: Int,
            outputSampleRate: Int,
            outputFormat: Int,
            outputChannels: Int,
        ): SwrContext {
            val handle = FFmpegNative.swrAllocSetOpts(
                outputSampleRate,
                outputFormat,
                outputChannels,
                inputSampleRate,
                inputFormat,
                inputChannels,
            )
            if (handle == 0L) {
                throw AvException(
                    "swr_alloc_set_opts2 failed",
                    FFmpegNative.lastError(),
                )
            }
            AvError.check(FFmpegNative.swrInit(handle), "swr_init")
            return SwrContext(
                handle,
                inputSampleRate,
                inputFormat,
                inputChannels,
                outputSampleRate,
                outputFormat,
                outputChannels,
            )
        }
    }
}
