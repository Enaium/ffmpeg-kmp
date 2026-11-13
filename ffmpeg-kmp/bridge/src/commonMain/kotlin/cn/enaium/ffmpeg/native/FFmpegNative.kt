/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.native

/**
 * Low-level bridge to the C shim (`jni/ffmpeg_kmp.c`).
 *
 * The high-level modules (`libavutil-kmp`, `libavcodec-kmp`, ...) own the
 * public API; this object is deliberately untyped: handles are `Long` values
 * (native pointers), strings are copied across the boundary, and every call
 * returns FFmpeg's own error code instead of throwing. JVM implements it with
 * JNI, Kotlin/Native with cinterop.
 *
 * A handle of `0` is the null pointer. Callers must close every handle they
 * create; the high-level classes do that in `close()`.
 */
public expect object FFmpegNative {
    // ==================== Versions and build information ====================

    public fun versionInfo(): String
    public fun configuration(): String
    public fun license(): String

    /** Error code of the last call that can fail without returning one. */
    public fun lastError(): Int

    public fun avutilVersion(): Int
    public fun avcodecVersion(): Int
    public fun avformatVersion(): Int
    public fun avfilterVersion(): Int
    public fun swscaleVersion(): Int
    public fun swresampleVersion(): Int
    public fun avdeviceVersion(): Int

    // ==================== libavutil ====================

    public fun logLevel(): Int
    public fun setLogLevel(level: Int)
    public fun logMessage(level: Int, message: String)

    public fun errorEagain(): Int
    public fun errorEof(): Int
    public fun errorString(error: Int): String

    public fun pixFmtName(format: Int): String?
    public fun pixFmtGet(name: String): Int
    public fun sampleFmtName(format: Int): String?
    public fun sampleFmtGet(name: String): Int
    public fun sampleFmtBytes(format: Int): Int

    public fun dictCreate(): Long
    public fun dictFree(dict: Long)
    public fun dictSet(dict: Long, key: String, value: String): Int
    public fun dictSetInt(dict: Long, key: String, value: Long): Int
    public fun dictGet(dict: Long, key: String): String?
    public fun dictGetInt(dict: Long, key: String, fallback: Long): Long
    public fun dictCount(dict: Long): Int

    public fun frameAlloc(): Long
    public fun frameFree(frame: Long)
    public fun frameUnref(frame: Long)
    public fun frameRef(dst: Long, src: Long): Int
    public fun frameGetBuffer(frame: Long, align: Int): Int
    public fun frameMakeWritable(frame: Long): Int

    public fun frameSetVideo(frame: Long, width: Int, height: Int, format: Int): Int
    public fun frameSetAudio(
        frame: Long,
        sampleRate: Int,
        format: Int,
        nbSamples: Int,
        channels: Int,
    ): Int

    public fun frameWidth(frame: Long): Int
    public fun frameHeight(frame: Long): Int
    public fun frameFormat(frame: Long): Int
    public fun framePts(frame: Long): Long
    public fun setFramePts(frame: Long, pts: Long)
    public fun framePktDts(frame: Long): Long
    public fun frameDuration(frame: Long): Long
    public fun frameNbSamples(frame: Long): Int
    public fun frameSampleRate(frame: Long): Int
    public fun frameChannels(frame: Long): Int
    public fun frameIsKey(frame: Long): Boolean

    public fun frameSampleAspectRatioNum(frame: Long): Int
    public fun frameSampleAspectRatioDen(frame: Long): Int
    public fun setFrameSampleAspectRatio(frame: Long, num: Int, den: Int)
    public fun frameLinesize(frame: Long, plane: Int): Int
    public fun framePlaneSize(frame: Long, plane: Int): Int
    public fun frameCopyPlane(frame: Long, plane: Int, out: ByteArray): Int
    public fun frameWritePlane(frame: Long, plane: Int, data: ByteArray): Int

    // ==================== libavcodec ====================

    /** [field]: `FK_CODEC_CONTEXT_FIELD_*` from `libavcodec-kmp`. */
    public fun avctxGetInt(ctx: Long, field: Int): Int
    public fun avctxSetInt(ctx: Long, field: Int, value: Int): Int

    /** [field]: `FK_CODEC_CONTEXT_INT64_FIELD_*` from `libavcodec-kmp`. */
    public fun avctxGetInt64(ctx: Long, field: Int): Long
    public fun avctxSetInt64(ctx: Long, field: Int, value: Long): Int

    /**
     * Packs `num` in the high 32 bits and `den` in the low 32 bits.
     * [field]: `FK_CODEC_CONTEXT_RATIONAL_FIELD_*`.
     */
    public fun avctxGetRational(ctx: Long, field: Int): Long
    public fun avctxSetRational(ctx: Long, field: Int, num: Int, den: Int): Int
    public fun avctxSetExtradata(ctx: Long, data: ByteArray): Int
    public fun avctxExtradataSize(ctx: Long): Int
    public fun avctxCopyExtradata(ctx: Long, out: ByteArray): Int

    public fun findDecoderByName(name: String): Long
    public fun findEncoderByName(name: String): Long
    public fun findDecoder(id: Int): Long
    public fun findEncoder(id: Int): Long
    public fun codecName(codec: Long): String?
    public fun codecId(codec: Long): Int
    public fun codecType(codec: Long): Int
    public fun codecIsDecoder(codec: Long): Boolean
    public fun codecIsEncoder(codec: Long): Boolean
    public fun codecIdFromName(name: String): Int

    public fun avctxAlloc(codec: Long): Long
    public fun avctxFree(ctx: Long)
    public fun avctxOpen2(ctx: Long, codec: Long, dict: Long): Int
    public fun avctxSendPacket(ctx: Long, packet: Long): Int
    public fun avctxReceiveFrame(ctx: Long, frame: Long): Int
    public fun avctxSendFrame(ctx: Long, frame: Long): Int
    public fun avctxReceivePacket(ctx: Long, packet: Long): Int
    public fun avctxFlush(ctx: Long)

    public fun packetAlloc(): Long
    public fun packetFree(packet: Long)
    public fun packetUnref(packet: Long)
    public fun packetAllocData(packet: Long, size: Int): Int
    public fun packetSetData(packet: Long, data: ByteArray): Int
    public fun packetSize(packet: Long): Int
    public fun packetCopyData(packet: Long, out: ByteArray): Int

    /** [field]: `FK_PACKET_FIELD_*` from `libavcodec-kmp`. */
    public fun packetGetInt64(packet: Long, field: Int): Long
    public fun packetSetInt64(packet: Long, field: Int, value: Long)
    public fun packetRescaleTs(
        packet: Long,
        srcNum: Int,
        srcDen: Int,
        dstNum: Int,
        dstDen: Int,
    )

    public fun codecparAlloc(): Long
    public fun codecparFree(par: Long)
    public fun codecparFromContext(par: Long, ctx: Long): Int
    public fun codecparToContext(ctx: Long, par: Long): Int
    public fun codecparCopy(dst: Long, src: Long): Int

    /** [field]: `FK_CODEC_PARAMETERS_FIELD_*` from `libavcodec-kmp`. */
    public fun codecparGetInt(par: Long, field: Int): Int
    public fun codecparSetInt(par: Long, field: Int, value: Int): Int

    /** [field]: `FK_CODEC_PARAMETERS_INT64_FIELD_*` from `libavcodec-kmp`. */
    public fun codecparGetInt64(par: Long, field: Int): Long
    public fun codecparSetInt64(par: Long, field: Int, value: Long): Int

    /** Packs `num` in the high 32 bits and `den` in the low 32 bits. */
    public fun codecparGetRational(par: Long, field: Int): Long
    public fun codecparSetRational(par: Long, field: Int, num: Int, den: Int): Int
    public fun codecparSetExtradata(par: Long, data: ByteArray): Int
    public fun codecparExtradataSize(par: Long): Int
    public fun codecparCopyExtradata(par: Long, out: ByteArray): Int

    // ==================== libavformat ====================

    public fun formatFindInputFormat(name: String): Long
    public fun formatFindOutputFormat(name: String): Long
    public fun inputFormatName(format: Long): String?
    public fun outputFormatName(format: Long): String?

    public fun inputOpen(url: String, dict: Long): Long
    public fun inputOpenFormat(url: String, formatName: String, dict: Long): Long
    public fun inputFindStreamInfo(ctx: Long, dict: Long): Int
    public fun inputClose(ctx: Long)
    public fun inputReadFrame(ctx: Long, packet: Long): Int
    public fun inputSeek(ctx: Long, streamIndex: Int, timestamp: Long, flags: Int): Int

    public fun formatNbStreams(ctx: Long): Int
    public fun formatGetStream(ctx: Long, index: Int): Long
    public fun formatStartTime(ctx: Long): Long
    public fun formatDuration(ctx: Long): Long
    public fun formatName(ctx: Long): String?

    public fun outputOpen(formatName: String, filename: String): Long
    public fun outputWriteHeader(ctx: Long, dict: Long): Int
    public fun outputWritePacket(ctx: Long, packet: Long): Int
    public fun outputWriteTrailer(ctx: Long): Int
    public fun outputClose(ctx: Long)

    public fun formatNewStream(ctx: Long, codec: Long): Long
    public fun streamIndex(stream: Long): Int
    public fun streamTimeBase(stream: Long): Long
    public fun streamSetTimeBase(stream: Long, num: Int, den: Int): Int
    public fun streamDuration(stream: Long): Long
    public fun streamAvgFrameRate(stream: Long): Long
    public fun streamCodecpar(stream: Long): Long
    public fun streamSetCodecpar(stream: Long, par: Long): Int

    // ==================== libavfilter ====================

    public fun graphAlloc(): Long
    public fun graphFree(graph: Long)
    public fun graphCreateFilter(graph: Long, name: String, instanceName: String, args: String?): Long
    public fun graphLink(src: Long, srcPad: Int, dst: Long, dstPad: Int): Int
    public fun graphConfig(graph: Long): Int
    public fun buffersrcAddFrame(src: Long, frame: Long): Int
    public fun buffersinkGetFrame(sink: Long, frame: Long): Int

    // ==================== libswscale ====================

    public fun swsGetContext(
        srcWidth: Int,
        srcHeight: Int,
        srcFormat: Int,
        dstWidth: Int,
        dstHeight: Int,
        dstFormat: Int,
        flags: Int,
    ): Long

    public fun swsFreeContext(ctx: Long)
    public fun swsScaleFrame(ctx: Long, dst: Long, src: Long): Int

    // ==================== libswresample ====================

    public fun swrAllocSetOpts(
        outSampleRate: Int,
        outFormat: Int,
        outChannels: Int,
        inSampleRate: Int,
        inFormat: Int,
        inChannels: Int,
    ): Long

    public fun swrInit(ctx: Long): Int
    public fun swrConvertFrame(ctx: Long, out: Long, input: Long): Int
    public fun swrOutSamples(ctx: Long, inSamples: Int): Int
    public fun swrFree(ctx: Long)

    // ==================== libavdevice ====================

    public fun deviceRegisterAll()
}
