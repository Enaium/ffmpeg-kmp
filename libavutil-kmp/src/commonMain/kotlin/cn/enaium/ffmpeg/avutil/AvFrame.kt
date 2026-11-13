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

/**
 * `AVFrame`: one decoded or to-be-encoded picture/frame.
 *
 * Frames returned by decoders and filters own their buffers; [close] releases
 * them. Use [allocateVideo]/[allocateAudio] (followed by
 * [allocateBuffer]) to create a frame for an encoder or a filter source.
 */
public class AvFrame private constructor(handle: Long) : AutoCloseable {
    public var handle: Long = handle
        private set

    public val width: Int get() = FFmpegNative.frameWidth(handle)
    public val height: Int get() = FFmpegNative.frameHeight(handle)

    /** Pixel format for video frames, sample format for audio frames. */
    public val format: Int get() = FFmpegNative.frameFormat(handle)

    public var pts: Long
        get() = FFmpegNative.framePts(handle)
        set(value) { FFmpegNative.setFramePts(handle, value) }
    public val pktDts: Long get() = FFmpegNative.framePktDts(handle)
    public val duration: Long get() = FFmpegNative.frameDuration(handle)
    public val nbSamples: Int get() = FFmpegNative.frameNbSamples(handle)
    public val sampleRate: Int get() = FFmpegNative.frameSampleRate(handle)
    public val channels: Int get() = FFmpegNative.frameChannels(handle)
    public val isKey: Boolean get() = FFmpegNative.frameIsKey(handle)

    public var sampleAspectRatio: AvRational
        get() = AvRational(
            FFmpegNative.frameSampleAspectRatioNum(handle),
            FFmpegNative.frameSampleAspectRatioDen(handle),
        )
        set(value) {
            FFmpegNative.setFrameSampleAspectRatio(
                handle,
                value.numerator,
                value.denominator,
            )
        }

    public fun linesize(plane: Int): Int = FFmpegNative.frameLinesize(handle, plane)

    public fun planeSize(plane: Int): Int = FFmpegNative.framePlaneSize(handle, plane)

    /** Copies one plane out of the frame. */
    public fun copyPlane(plane: Int): ByteArray {
        val size = planeSize(plane)
        if (size <= 0) return ByteArray(0)
        val out = ByteArray(size)
        val copied = FFmpegNative.frameCopyPlane(handle, plane, out)
        AvError.check(copied, "av_frame_copy_plane(plane=$plane)")
        return if (copied == size) out else out.copyOf(copied)
    }

    /** Copies [data] into one plane and returns the number of bytes written. */
    public fun writePlane(plane: Int, data: ByteArray): Int {
        val written = FFmpegNative.frameWritePlane(handle, plane, data)
        return AvError.check(written, "av_frame_write_plane(plane=$plane)")
    }

    public fun setVideo(width: Int, height: Int, format: Int) {
        AvError.check(
            FFmpegNative.frameSetVideo(handle, width, height, format),
            "av_frame_set_video",
        )
    }

    public fun setAudio(sampleRate: Int, format: Int, nbSamples: Int, channels: Int) {
        AvError.check(
            FFmpegNative.frameSetAudio(handle, sampleRate, format, nbSamples, channels),
            "av_frame_set_audio",
        )
    }

    public fun allocateBuffer(align: Int = 32) {
        AvError.check(FFmpegNative.frameGetBuffer(handle, align), "av_frame_get_buffer")
    }

    public fun makeWritable() {
        AvError.check(FFmpegNative.frameMakeWritable(handle), "av_frame_make_writable")
    }

    public fun unref() {
        FFmpegNative.frameUnref(handle)
    }

    /** Forwards `av_frame_ref`: makes [dst] reference this frame's buffers. */
    public fun ref(dst: AvFrame) {
        AvError.check(FFmpegNative.frameRef(dst.handle, handle), "av_frame_ref")
    }

    override fun close() {
        if (handle != 0L) {
            FFmpegNative.frameFree(handle)
            handle = 0L
        }
    }

    public companion object {
        public fun create(): AvFrame {
            val handle = FFmpegNative.frameAlloc()
            if (handle == 0L) throw AvException("av_frame_alloc failed")
            return AvFrame(handle)
        }

        /** Allocates a frame with a video buffer ready for writes. */
        public fun video(
            width: Int,
            height: Int,
            format: Int,
            align: Int = 32,
        ): AvFrame = create().apply {
            setVideo(width, height, format)
            allocateBuffer(align)
        }

        /** Allocates a frame with an audio buffer ready for writes. */
        public fun audio(
            sampleRate: Int,
            format: Int,
            nbSamples: Int,
            channels: Int,
            align: Int = 32,
        ): AvFrame = create().apply {
            setAudio(sampleRate, format, nbSamples, channels)
            allocateBuffer(align)
        }
    }
}
