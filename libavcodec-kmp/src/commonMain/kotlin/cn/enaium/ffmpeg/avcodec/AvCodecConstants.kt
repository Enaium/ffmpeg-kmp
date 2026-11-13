/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avcodec

/** Field selectors for [AvCodecContext]; order matches `jni/ffmpeg_kmp.c`. */
public object AvCodecContextField {
    public const val WIDTH: Int = 0
    public const val HEIGHT: Int = 1
    public const val PIXEL_FORMAT: Int = 2
    public const val SAMPLE_RATE: Int = 3
    public const val CHANNELS: Int = 4
    public const val PROFILE: Int = 5
    public const val LEVEL: Int = 6
    public const val FRAME_SIZE: Int = 7
    public const val THREAD_COUNT: Int = 8
    public const val FLAGS: Int = 9
    public const val FLAGS2: Int = 10
    public const val GOP_SIZE: Int = 11
    public const val MAX_B_FRAMES: Int = 12
    public const val SAMPLE_FORMAT: Int = 13
}

public object AvCodecContextInt64Field {
    public const val BIT_RATE: Int = 0
}

public object AvCodecContextRationalField {
    public const val TIME_BASE: Int = 0
    public const val FRAMERATE: Int = 1
    public const val SAMPLE_ASPECT_RATIO: Int = 2
}

/** Field selectors for [AvCodecParameters]. */
public object AvCodecParametersField {
    public const val WIDTH: Int = 0
    public const val HEIGHT: Int = 1
    public const val FORMAT: Int = 2
    public const val SAMPLE_RATE: Int = 3
    public const val CHANNELS: Int = 4
    public const val PROFILE: Int = 5
    public const val LEVEL: Int = 6
    public const val FRAME_SIZE: Int = 7
    public const val CODEC_ID: Int = 8
    public const val CODEC_TYPE: Int = 9
    public const val CODEC_TAG: Int = 10
}

public object AvCodecParametersInt64Field {
    public const val BIT_RATE: Int = 0
}

public object AvCodecParametersRationalField {
    public const val SAMPLE_ASPECT_RATIO: Int = 0
}

/** Field selectors for [AvPacket]. */
public object AvPacketField {
    public const val PTS: Int = 0
    public const val DTS: Int = 1
    public const val DURATION: Int = 2
    public const val STREAM_INDEX: Int = 3
    public const val FLAGS: Int = 4
}

/** Common `AV_CODEC_ID_*` values; any other id can be passed as an Int. */
public object AvCodecId {
    public const val NONE: Int = 0
    public const val MPEG4: Int = 12
    public const val H264: Int = 27
    public const val HEVC: Int = 172
    public const val MJPEG: Int = 7
    public const val RAWVIDEO: Int = 13
    public const val FFV1: Int = 33
    public const val VP8: Int = 139
    public const val VP9: Int = 166
    public const val AV1: Int = 222
    public const val AAC: Int = 86018
    public const val MP3: Int = 86017
    public const val OPUS: Int = 86076
    public const val PCM_S16LE: Int = 65536
    public const val PCM_S16BE: Int = 65537
}

/** `AVMediaType`. */
public object AvMediaType {
    public const val UNKNOWN: Int = -1
    public const val VIDEO: Int = 0
    public const val AUDIO: Int = 1
    public const val DATA: Int = 2
    public const val SUBTITLE: Int = 3
    public const val ATTACHMENT: Int = 4
}

/** `AV_PKT_FLAG_*`. */
public object AvPacketFlag {
    public const val KEY: Int = 0x0001
    public const val CORRUPT: Int = 0x0002
    public const val DISCARD: Int = 0x0004
    public const val TRUSTED: Int = 0x0008
}

/** `AV_CODEC_FLAG_*` values most callers need. */
public object AvCodecFlag {
    public const val UNALIGNED: Int = 1 shl 0
    public const val QSCALE: Int = 1 shl 1
    public const val GLOBAL_HEADER: Int = 1 shl 22
    public const val LOW_DELAY: Int = 1 shl 19
}

/** `AV_CODEC_FLAG2_*`. */
public object AvCodecFlag2 {
    public const val FAST: Int = 1 shl 0
    public const val NO_OUTPUT: Int = 1 shl 2
    public const val LOCAL_HEADER: Int = 1 shl 3
}
