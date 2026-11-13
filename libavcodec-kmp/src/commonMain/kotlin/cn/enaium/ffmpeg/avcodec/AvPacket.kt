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

/** `AVPacket`: one compressed packet (or one encoded packet before muxing). */
public class AvPacket private constructor(handle: Long) : AutoCloseable {
    public var handle: Long = handle
        private set

    public val size: Int get() = FFmpegNative.packetSize(handle)

    public var pts: Long
        get() = FFmpegNative.packetGetInt64(handle, AvPacketField.PTS)
        set(value) { FFmpegNative.packetSetInt64(handle, AvPacketField.PTS, value) }
    public var dts: Long
        get() = FFmpegNative.packetGetInt64(handle, AvPacketField.DTS)
        set(value) { FFmpegNative.packetSetInt64(handle, AvPacketField.DTS, value) }
    public var duration: Long
        get() = FFmpegNative.packetGetInt64(handle, AvPacketField.DURATION)
        set(value) { FFmpegNative.packetSetInt64(handle, AvPacketField.DURATION, value) }
    public var streamIndex: Int
        get() = FFmpegNative.packetGetInt64(handle, AvPacketField.STREAM_INDEX).toInt()
        set(value) { FFmpegNative.packetSetInt64(handle, AvPacketField.STREAM_INDEX, value.toLong()) }
    public var flags: Int
        get() = FFmpegNative.packetGetInt64(handle, AvPacketField.FLAGS).toInt()
        set(value) { FFmpegNative.packetSetInt64(handle, AvPacketField.FLAGS, value.toLong()) }
    public val isKey: Boolean get() = (flags and AvPacketFlag.KEY) != 0

    public fun data(): ByteArray {
        val size = size
        if (size <= 0) return ByteArray(0)
        val out = ByteArray(size)
        val copied = FFmpegNative.packetCopyData(handle, out)
        AvError.check(copied, "av_packet_copy_data")
        return if (copied == size) out else out.copyOf(copied)
    }

    public fun setData(data: ByteArray) {
        AvError.check(FFmpegNative.packetSetData(handle, data), "av_packet_set_data")
    }

    public fun allocateData(size: Int) {
        AvError.check(FFmpegNative.packetAllocData(handle, size), "av_new_packet")
    }

    public fun unref() {
        FFmpegNative.packetUnref(handle)
    }

    public fun rescaleTs(source: AvRational, destination: AvRational) {
        FFmpegNative.packetRescaleTs(
            handle,
            source.numerator,
            source.denominator,
            destination.numerator,
            destination.denominator,
        )
    }

    override fun close() {
        if (handle != 0L) {
            FFmpegNative.packetFree(handle)
            handle = 0L
        }
    }

    public companion object {
        public fun create(): AvPacket {
            val handle = FFmpegNative.packetAlloc()
            if (handle == 0L) throw AvException("av_packet_alloc failed")
            return AvPacket(handle)
        }
    }
}
