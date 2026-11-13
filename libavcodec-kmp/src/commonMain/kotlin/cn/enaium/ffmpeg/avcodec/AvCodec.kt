/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avcodec

import cn.enaium.ffmpeg.avutil.AvException
import cn.enaium.ffmpeg.native.FFmpegNative

/**
 * `AVCodec`: a codec descriptor owned by libavcodec. Do not free it; handles
 * remain valid for the life of the process.
 */
public class AvCodec internal constructor(public val handle: Long) {
    public val name: String? get() = FFmpegNative.codecName(handle)
    public val id: Int get() = FFmpegNative.codecId(handle)
    public val mediaType: Int get() = FFmpegNative.codecType(handle)
    public val isDecoder: Boolean get() = FFmpegNative.codecIsDecoder(handle)
    public val isEncoder: Boolean get() = FFmpegNative.codecIsEncoder(handle)

    public companion object {
        public fun findDecoder(name: String): AvCodec? =
            FFmpegNative.findDecoderByName(name).takeIf { it != 0L }?.let(::AvCodec)

        public fun findEncoder(name: String): AvCodec? =
            FFmpegNative.findEncoderByName(name).takeIf { it != 0L }?.let(::AvCodec)

        public fun findDecoder(id: Int): AvCodec? =
            FFmpegNative.findDecoder(id).takeIf { it != 0L }?.let(::AvCodec)

        public fun findEncoder(id: Int): AvCodec? =
            FFmpegNative.findEncoder(id).takeIf { it != 0L }?.let(::AvCodec)

        /** `avcodec_find_decoder_by_name` or throws [AvException]. */
        public fun decoder(name: String): AvCodec =
            findDecoder(name) ?: throw AvException("No decoder named '$name'")

        /** `avcodec_find_encoder_by_name` or throws [AvException]. */
        public fun encoder(name: String): AvCodec =
            findEncoder(name) ?: throw AvException("No encoder named '$name'")

        /** `avcodec_find_decoder` or throws [AvException]. */
        public fun decoder(id: Int): AvCodec =
            findDecoder(id) ?: throw AvException("No decoder for codec id $id")

        /** `avcodec_find_encoder` or throws [AvException]. */
        public fun encoder(id: Int): AvCodec =
            findEncoder(id) ?: throw AvException("No encoder for codec id $id")

        public fun idFromName(name: String): Int = FFmpegNative.codecIdFromName(name)
    }
}
