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
 * JNI declarations for the C shim. Not for application use: the public
 * modules go through [FFmpegNative], which checks error codes and wraps
 * handles in typed classes. Method names match `jni/ffmpeg_jni.c`.
 */
internal object FFmpegJni {
    external fun versionInfo(): String
    external fun configuration(): String
    external fun license(): String
    external fun lastError(): Int

    external fun avutilVersion(): Int
    external fun avcodecVersion(): Int
    external fun avformatVersion(): Int
    external fun avfilterVersion(): Int
    external fun swscaleVersion(): Int
    external fun swresampleVersion(): Int
    external fun avdeviceVersion(): Int

    external fun logLevel(): Int
    external fun setLogLevel(level: Int)
    external fun logMessage(level: Int, message: String)

    external fun errorEagain(): Int
    external fun errorEof(): Int
    external fun errorString(error: Int): String

    external fun pixFmtName(format: Int): String?
    external fun pixFmtGet(name: String): Int
    external fun sampleFmtName(format: Int): String?
    external fun sampleFmtGet(name: String): Int
    external fun sampleFmtBytes(format: Int): Int

    external fun dictCreate(): Long
    external fun dictFree(dict: Long)
    external fun dictSet(dict: Long, key: String, value: String): Int
    external fun dictSetInt(dict: Long, key: String, value: Long): Int
    external fun dictGet(dict: Long, key: String): String?
    external fun dictGetInt(dict: Long, key: String, fallback: Long): Long
    external fun dictCount(dict: Long): Int

    external fun frameAlloc(): Long
    external fun frameFree(frame: Long)
    external fun frameUnref(frame: Long)
    external fun frameRef(dst: Long, src: Long): Int
    external fun frameGetBuffer(frame: Long, align: Int): Int
    external fun frameMakeWritable(frame: Long): Int

    external fun frameSetVideo(frame: Long, width: Int, height: Int, format: Int): Int
    external fun frameSetAudio(
        frame: Long,
        sampleRate: Int,
        format: Int,
        nbSamples: Int,
        channels: Int,
    ): Int

    external fun frameWidth(frame: Long): Int
    external fun frameHeight(frame: Long): Int
    external fun frameFormat(frame: Long): Int
    external fun framePts(frame: Long): Long
    external fun setFramePts(frame: Long, pts: Long)
    external fun framePktDts(frame: Long): Long
    external fun frameDuration(frame: Long): Long
    external fun frameNbSamples(frame: Long): Int
    external fun frameSampleRate(frame: Long): Int
    external fun frameChannels(frame: Long): Int
    external fun frameIsKey(frame: Long): Boolean

    external fun frameSampleAspectRatioNum(frame: Long): Int
    external fun frameSampleAspectRatioDen(frame: Long): Int
    external fun setFrameSampleAspectRatio(frame: Long, num: Int, den: Int)
    external fun frameLinesize(frame: Long, plane: Int): Int
    external fun framePlaneSize(frame: Long, plane: Int): Int
    external fun frameCopyPlane(frame: Long, plane: Int, out: ByteArray): Int
    external fun frameWritePlane(frame: Long, plane: Int, data: ByteArray): Int

    external fun avctxGetInt(ctx: Long, field: Int): Int
    external fun avctxSetInt(ctx: Long, field: Int, value: Int): Int
    external fun avctxGetInt64(ctx: Long, field: Int): Long
    external fun avctxSetInt64(ctx: Long, field: Int, value: Long): Int
    external fun avctxGetRational(ctx: Long, field: Int): Long
    external fun avctxSetRational(ctx: Long, field: Int, num: Int, den: Int): Int
    external fun avctxSetExtradata(ctx: Long, data: ByteArray): Int
    external fun avctxExtradataSize(ctx: Long): Int
    external fun avctxCopyExtradata(ctx: Long, out: ByteArray): Int

    external fun findDecoderByName(name: String): Long
    external fun findEncoderByName(name: String): Long
    external fun findDecoder(id: Int): Long
    external fun findEncoder(id: Int): Long
    external fun codecName(codec: Long): String?
    external fun codecId(codec: Long): Int
    external fun codecType(codec: Long): Int
    external fun codecIsDecoder(codec: Long): Boolean
    external fun codecIsEncoder(codec: Long): Boolean
    external fun codecIdFromName(name: String): Int

    external fun avctxAlloc(codec: Long): Long
    external fun avctxFree(ctx: Long)
    external fun avctxOpen2(ctx: Long, codec: Long, dict: Long): Int
    external fun avctxSendPacket(ctx: Long, packet: Long): Int
    external fun avctxReceiveFrame(ctx: Long, frame: Long): Int
    external fun avctxSendFrame(ctx: Long, frame: Long): Int
    external fun avctxReceivePacket(ctx: Long, packet: Long): Int
    external fun avctxFlush(ctx: Long)

    external fun packetAlloc(): Long
    external fun packetFree(packet: Long)
    external fun packetUnref(packet: Long)
    external fun packetAllocData(packet: Long, size: Int): Int
    external fun packetSetData(packet: Long, data: ByteArray): Int
    external fun packetSize(packet: Long): Int
    external fun packetCopyData(packet: Long, out: ByteArray): Int
    external fun packetGetInt64(packet: Long, field: Int): Long
    external fun packetSetInt64(packet: Long, field: Int, value: Long)
    external fun packetRescaleTs(
        packet: Long,
        srcNum: Int,
        srcDen: Int,
        dstNum: Int,
        dstDen: Int,
    )

    external fun codecparAlloc(): Long
    external fun codecparFree(par: Long)
    external fun codecparFromContext(par: Long, ctx: Long): Int
    external fun codecparToContext(ctx: Long, par: Long): Int
    external fun codecparCopy(dst: Long, src: Long): Int
    external fun codecparGetInt(par: Long, field: Int): Int
    external fun codecparSetInt(par: Long, field: Int, value: Int): Int
    external fun codecparGetInt64(par: Long, field: Int): Long
    external fun codecparSetInt64(par: Long, field: Int, value: Long): Int
    external fun codecparGetRational(par: Long, field: Int): Long
    external fun codecparSetRational(par: Long, field: Int, num: Int, den: Int): Int
    external fun codecparSetExtradata(par: Long, data: ByteArray): Int
    external fun codecparExtradataSize(par: Long): Int
    external fun codecparCopyExtradata(par: Long, out: ByteArray): Int

    external fun formatFindInputFormat(name: String): Long
    external fun formatFindOutputFormat(name: String): Long
    external fun inputFormatName(format: Long): String?
    external fun outputFormatName(format: Long): String?

    external fun inputOpen(url: String, dict: Long): Long
    external fun inputOpenFormat(url: String, formatName: String, dict: Long): Long
    external fun inputFindStreamInfo(ctx: Long, dict: Long): Int
    external fun inputClose(ctx: Long)
    external fun inputReadFrame(ctx: Long, packet: Long): Int
    external fun inputSeek(ctx: Long, streamIndex: Int, timestamp: Long, flags: Int): Int

    external fun formatNbStreams(ctx: Long): Int
    external fun formatGetStream(ctx: Long, index: Int): Long
    external fun formatStartTime(ctx: Long): Long
    external fun formatDuration(ctx: Long): Long
    external fun formatName(ctx: Long): String?

    external fun outputOpen(formatName: String, filename: String): Long
    external fun outputWriteHeader(ctx: Long, dict: Long): Int
    external fun outputWritePacket(ctx: Long, packet: Long): Int
    external fun outputWriteTrailer(ctx: Long): Int
    external fun outputClose(ctx: Long)

    external fun formatNewStream(ctx: Long, codec: Long): Long
    external fun streamIndex(stream: Long): Int
    external fun streamTimeBase(stream: Long): Long
    external fun streamSetTimeBase(stream: Long, num: Int, den: Int): Int
    external fun streamDuration(stream: Long): Long
    external fun streamAvgFrameRate(stream: Long): Long
    external fun streamCodecpar(stream: Long): Long
    external fun streamSetCodecpar(stream: Long, par: Long): Int

    external fun graphAlloc(): Long
    external fun graphFree(graph: Long)
    external fun graphCreateFilter(
        graph: Long,
        name: String,
        instanceName: String,
        args: String?,
    ): Long
    external fun graphLink(src: Long, srcPad: Int, dst: Long, dstPad: Int): Int
    external fun graphConfig(graph: Long): Int
    external fun buffersrcAddFrame(src: Long, frame: Long): Int
    external fun buffersinkGetFrame(sink: Long, frame: Long): Int

    external fun swsGetContext(
        srcWidth: Int,
        srcHeight: Int,
        srcFormat: Int,
        dstWidth: Int,
        dstHeight: Int,
        dstFormat: Int,
        flags: Int,
    ): Long
    external fun swsFreeContext(ctx: Long)
    external fun swsScaleFrame(ctx: Long, dst: Long, src: Long): Int

    external fun swrAllocSetOpts(
        outSampleRate: Int,
        outFormat: Int,
        outChannels: Int,
        inSampleRate: Int,
        inFormat: Int,
        inChannels: Int,
    ): Long
    external fun swrInit(ctx: Long): Int
    external fun swrConvertFrame(ctx: Long, out: Long, input: Long): Int
    external fun swrOutSamples(ctx: Long, inSamples: Int): Int
    external fun swrFree(ctx: Long)

    external fun deviceRegisterAll()
}
