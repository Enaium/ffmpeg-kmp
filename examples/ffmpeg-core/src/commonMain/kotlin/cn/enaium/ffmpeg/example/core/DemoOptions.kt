/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example.core

/** Camera input or the deterministic test pattern. */
enum class SourceKind {
    CAMERA,
    SYNTHETIC;

    companion object {
        fun parse(value: String?): SourceKind = when (value?.lowercase()) {
            "synthetic", "pattern", "test" -> SYNTHETIC
            else -> CAMERA
        }
    }
}

/** Filters exposed in the UI. They all keep the YUV420P format the encoder needs. */
enum class FilterPreset(
    val label: String,
    val filterName: String?,
    val args: String?,
) {
    NONE("None", null, null),
    HFLIP("Horizontal flip", "hflip", null),
    VFLIP("Vertical flip", "vflip", null),
    NEGATE("Negate", "negate", null),
    // boxblur and eq are GPL-only in FFmpeg 9; gblur and hue are LGPL.
    GBLUR("Gaussian blur", "gblur", "sigma=2:steps=1"),
    HUE("Hue + saturation", "hue", "s=1.4:b=0.05"),
    EDGEDETECT("Edge detect", "edgedetect", null);

    companion object {
        fun parse(value: String?): FilterPreset = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true) ||
                it.label.equals(value, ignoreCase = true)
        } ?: NONE
    }
}

/** Muxers exposed by the recording UI. All of them accept MPEG-4 Part 2. */
enum class RecordingFormat(
    val label: String,
    val muxer: String,
    val extension: String,
    /** AVFMT_GLOBALHEADER: extradata in codecpar instead of in-band packets. */
    val globalHeader: Boolean,
) {
    MP4("MP4", "mp4", "mp4", true),
    MKV("Matroska", "matroska", "mkv", true),
    MOV("QuickTime", "mov", "mov", true),
    MPEG_TS("MPEG-TS", "mpegts", "ts", false),
    MPEG_PS("MPEG-PS", "mpeg", "mpg", false),
    AVI("AVI", "avi", "avi", false),
    THREE_GP("3GP", "3gp", "3gp", true);

    companion object {
        fun parse(value: String?): RecordingFormat = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true) ||
                it.muxer.equals(value, ignoreCase = true) ||
                it.extension.equals(value, ignoreCase = true) ||
                it.label.equals(value, ignoreCase = true)
        } ?: MP4
    }
}

/** Scale percentages offered in the UI. */
object ScaleOptions {
    val values: IntArray = intArrayOf(25, 50, 75, 100, 150)
    val labels: Array<String> = values.map { "$it%" }.toTypedArray()

    fun indexOf(percent: Int): Int =
        values.indexOf(percent).takeIf { it >= 0 } ?: values.indexOf(100)
}

/**
 * Command line / environment options shared by the desktop and Android entry
 * points.
 */
data class DemoOptions(
    val frames: Int = 0,
    val source: SourceKind = SourceKind.CAMERA,
    val headless: Boolean = false,
    val width: Int = 640,
    val height: Int = 480,
    val frameRate: Int = 30,
    val bitrateKbps: Int = DEFAULT_BITRATE_KBPS,
    val filter: FilterPreset = FilterPreset.NONE,
    val scalePercent: Int = 100,
    val recording: Boolean = false,
    val container: RecordingFormat = RecordingFormat.MP4,
    /** Base path of the recording; the pipeline adds a session suffix and the container's extension. */
    val recordingPath: String = "camera-ffmpeg.mp4",
    /** Closes the window after this many milliseconds; 0 keeps it open. */
    val quitAfterMillis: Long = 0,
) {
    init {
        require(width > 0 && height > 0) { "size must be positive" }
        require(frameRate > 0) { "frame rate must be positive" }
        require(bitrateKbps > 0) { "bitrate must be positive" }
    }
}

const val DEFAULT_BITRATE_KBPS: Int = 2_000
