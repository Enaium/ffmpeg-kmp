/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avcodec

import cn.enaium.ffmpeg.avutil.AvDictionary
import cn.enaium.ffmpeg.avutil.AvError
import cn.enaium.ffmpeg.avutil.AvException
import cn.enaium.ffmpeg.avutil.AvFrame
import cn.enaium.ffmpeg.avutil.AvRational
import cn.enaium.ffmpeg.native.FFmpegNative

/**
 * `AVCodecContext`: decoder/encoder state.
 *
 * Use [open] before sending packets/frames. Decoding follows FFmpeg's
 * send/receive contract: [sendPacket] then [receiveFrame] until it returns
 * `null`; encoding is [sendFrame] then [receivePacket] until `null`.
 */
public class AvCodecContext private constructor(handle: Long) : AutoCloseable {
    public var handle: Long = handle
        private set

    public var width: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.WIDTH)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.WIDTH, value) }
    public var height: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.HEIGHT)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.HEIGHT, value) }
    /** Video pixel format. */
    public var pixelFormat: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.PIXEL_FORMAT)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.PIXEL_FORMAT, value) }
    /** Audio sample format. */
    public var sampleFormat: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.SAMPLE_FORMAT)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.SAMPLE_FORMAT, value) }
    public var sampleRate: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.SAMPLE_RATE)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.SAMPLE_RATE, value) }
    public var channels: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.CHANNELS)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.CHANNELS, value) }
    public var profile: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.PROFILE)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.PROFILE, value) }
    public var level: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.LEVEL)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.LEVEL, value) }
    public var frameSize: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.FRAME_SIZE)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.FRAME_SIZE, value) }
    public var threadCount: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.THREAD_COUNT)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.THREAD_COUNT, value) }
    public var flags: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.FLAGS)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.FLAGS, value) }
    public var flags2: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.FLAGS2)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.FLAGS2, value) }
    public var gopSize: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.GOP_SIZE)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.GOP_SIZE, value) }
    public var maxBFrames: Int
        get() = FFmpegNative.avctxGetInt(handle, AvCodecContextField.MAX_B_FRAMES)
        set(value) { FFmpegNative.avctxSetInt(handle, AvCodecContextField.MAX_B_FRAMES, value) }
    public var bitRate: Long
        get() = FFmpegNative.avctxGetInt64(handle, AvCodecContextInt64Field.BIT_RATE)
        set(value) { FFmpegNative.avctxSetInt64(handle, AvCodecContextInt64Field.BIT_RATE, value) }
    public var timeBase: AvRational
        get() = rational(AvCodecContextRationalField.TIME_BASE)
        set(value) { setRational(AvCodecContextRationalField.TIME_BASE, value) }
    public var framerate: AvRational
        get() = rational(AvCodecContextRationalField.FRAMERATE)
        set(value) { setRational(AvCodecContextRationalField.FRAMERATE, value) }
    public var sampleAspectRatio: AvRational
        get() = rational(AvCodecContextRationalField.SAMPLE_ASPECT_RATIO)
        set(value) { setRational(AvCodecContextRationalField.SAMPLE_ASPECT_RATIO, value) }
    public val extradata: ByteArray
        get() {
            val size = FFmpegNative.avctxExtradataSize(handle)
            if (size <= 0) return ByteArray(0)
            val out = ByteArray(size)
            FFmpegNative.avctxCopyExtradata(handle, out)
            return out
        }

    public fun setExtradata(data: ByteArray) {
        AvError.check(FFmpegNative.avctxSetExtradata(handle, data), "avctx_set_extradata")
    }

    public fun open(codec: AvCodec? = null, options: AvDictionary? = null) {
        AvError.check(
            FFmpegNative.avctxOpen2(handle, codec?.handle ?: 0L, options?.handle ?: 0L),
            "avcodec_open2",
        )
    }

    /** Sends a compressed packet; pass `null` to enter draining mode. */
    public fun sendPacket(packet: AvPacket?) {
        AvError.check(
            FFmpegNative.avctxSendPacket(handle, packet?.handle ?: 0L),
            "avcodec_send_packet",
        )
    }

    /** Returns the next decoded frame, or `null` on `EAGAIN`/`EOF`. */
    public fun receiveFrame(): AvFrame? {
        val frame = AvFrame.create()
        val result = FFmpegNative.avctxReceiveFrame(handle, frame.handle)
        if (result == AvError.EAGAIN || result == AvError.EOF) {
            frame.close()
            return null
        }
        if (result < 0) {
            frame.close()
            AvError.check(result, "avcodec_receive_frame")
        }
        return frame
    }

    /** Sends a raw frame; pass `null` to enter draining mode. */
    public fun sendFrame(frame: AvFrame?) {
        AvError.check(
            FFmpegNative.avctxSendFrame(handle, frame?.handle ?: 0L),
            "avcodec_send_frame",
        )
    }

    /** Returns the next encoded packet, or `null` on `EAGAIN`/`EOF`. */
    public fun receivePacket(): AvPacket? {
        val packet = AvPacket.create()
        val result = FFmpegNative.avctxReceivePacket(handle, packet.handle)
        if (result == AvError.EAGAIN || result == AvError.EOF) {
            packet.close()
            return null
        }
        if (result < 0) {
            packet.close()
            AvError.check(result, "avcodec_receive_packet")
        }
        return packet
    }

    public fun flush() {
        FFmpegNative.avctxFlush(handle)
    }

    /** Copies the current configuration into a new [AvCodecParameters]. */
    public fun codecParameters(): AvCodecParameters {
        val parameters = AvCodecParameters.create()
        try {
            parameters.copyFrom(this)
        } catch (e: Throwable) {
            parameters.close()
            throw e
        }
        return parameters
    }

    /** `avcodec_parameters_to_context`. */
    public fun applyParameters(parameters: AvCodecParameters) {
        AvError.check(
            FFmpegNative.codecparToContext(handle, parameters.handle),
            "avcodec_parameters_to_context",
        )
    }

    private fun rational(field: Int): AvRational {
        val packed = FFmpegNative.avctxGetRational(handle, field)
        return AvRational((packed shr 32).toInt(), packed.toInt())
    }

    private fun setRational(field: Int, value: AvRational): Unit {
        AvError.check(
            FFmpegNative.avctxSetRational(handle, field, value.numerator, value.denominator),
            "avctx_set_rational",
        )
    }

    override fun close() {
        if (handle != 0L) {
            FFmpegNative.avctxFree(handle)
            handle = 0L
        }
    }

    public companion object {
        public fun create(codec: AvCodec? = null): AvCodecContext {
            val handle = FFmpegNative.avctxAlloc(codec?.handle ?: 0L)
            if (handle == 0L) throw AvException("avcodec_alloc_context3 failed")
            return AvCodecContext(handle)
        }
    }
}
