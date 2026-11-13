/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.native

/** JNI implementation of [FFmpegNative]. */
public actual object FFmpegNative {
    init {
        FFmpegJniLoader.load()
    }

    public actual fun versionInfo(): String = FFmpegJni.versionInfo()
    public actual fun configuration(): String = FFmpegJni.configuration()
    public actual fun license(): String = FFmpegJni.license()
    public actual fun lastError(): Int = FFmpegJni.lastError()

    public actual fun avutilVersion(): Int = FFmpegJni.avutilVersion()
    public actual fun avcodecVersion(): Int = FFmpegJni.avcodecVersion()
    public actual fun avformatVersion(): Int = FFmpegJni.avformatVersion()
    public actual fun avfilterVersion(): Int = FFmpegJni.avfilterVersion()
    public actual fun swscaleVersion(): Int = FFmpegJni.swscaleVersion()
    public actual fun swresampleVersion(): Int = FFmpegJni.swresampleVersion()
    public actual fun avdeviceVersion(): Int = FFmpegJni.avdeviceVersion()

    public actual fun logLevel(): Int = FFmpegJni.logLevel()
    public actual fun setLogLevel(level: Int): Unit = FFmpegJni.setLogLevel(level)
    public actual fun logMessage(level: Int, message: String): Unit =
        FFmpegJni.logMessage(level, message)

    public actual fun errorEagain(): Int = FFmpegJni.errorEagain()
    public actual fun errorEof(): Int = FFmpegJni.errorEof()
    public actual fun errorString(error: Int): String = FFmpegJni.errorString(error)

    public actual fun pixFmtName(format: Int): String? = FFmpegJni.pixFmtName(format)
    public actual fun pixFmtGet(name: String): Int = FFmpegJni.pixFmtGet(name)
    public actual fun sampleFmtName(format: Int): String? = FFmpegJni.sampleFmtName(format)
    public actual fun sampleFmtGet(name: String): Int = FFmpegJni.sampleFmtGet(name)
    public actual fun sampleFmtBytes(format: Int): Int = FFmpegJni.sampleFmtBytes(format)

    public actual fun dictCreate(): Long = FFmpegJni.dictCreate()
    public actual fun dictFree(dict: Long): Unit = FFmpegJni.dictFree(dict)
    public actual fun dictSet(dict: Long, key: String, value: String): Int =
        FFmpegJni.dictSet(dict, key, value)

    public actual fun dictSetInt(dict: Long, key: String, value: Long): Int =
        FFmpegJni.dictSetInt(dict, key, value)

    public actual fun dictGet(dict: Long, key: String): String? = FFmpegJni.dictGet(dict, key)
    public actual fun dictGetInt(dict: Long, key: String, fallback: Long): Long =
        FFmpegJni.dictGetInt(dict, key, fallback)

    public actual fun dictCount(dict: Long): Int = FFmpegJni.dictCount(dict)

    public actual fun frameAlloc(): Long = FFmpegJni.frameAlloc()
    public actual fun frameFree(frame: Long): Unit = FFmpegJni.frameFree(frame)
    public actual fun frameUnref(frame: Long): Unit = FFmpegJni.frameUnref(frame)
    public actual fun frameRef(dst: Long, src: Long): Int = FFmpegJni.frameRef(dst, src)
    public actual fun frameGetBuffer(frame: Long, align: Int): Int =
        FFmpegJni.frameGetBuffer(frame, align)

    public actual fun frameMakeWritable(frame: Long): Int = FFmpegJni.frameMakeWritable(frame)

    public actual fun frameSetVideo(frame: Long, width: Int, height: Int, format: Int): Int =
        FFmpegJni.frameSetVideo(frame, width, height, format)

    public actual fun frameSetAudio(
        frame: Long,
        sampleRate: Int,
        format: Int,
        nbSamples: Int,
        channels: Int,
    ): Int = FFmpegJni.frameSetAudio(frame, sampleRate, format, nbSamples, channels)

    public actual fun frameWidth(frame: Long): Int = FFmpegJni.frameWidth(frame)
    public actual fun frameHeight(frame: Long): Int = FFmpegJni.frameHeight(frame)
    public actual fun frameFormat(frame: Long): Int = FFmpegJni.frameFormat(frame)
    public actual fun framePts(frame: Long): Long = FFmpegJni.framePts(frame)
    public actual fun setFramePts(frame: Long, pts: Long): Unit = FFmpegJni.setFramePts(frame, pts)
    public actual fun framePktDts(frame: Long): Long = FFmpegJni.framePktDts(frame)
    public actual fun frameDuration(frame: Long): Long = FFmpegJni.frameDuration(frame)
    public actual fun frameNbSamples(frame: Long): Int = FFmpegJni.frameNbSamples(frame)
    public actual fun frameSampleRate(frame: Long): Int = FFmpegJni.frameSampleRate(frame)
    public actual fun frameChannels(frame: Long): Int = FFmpegJni.frameChannels(frame)
    public actual fun frameIsKey(frame: Long): Boolean = FFmpegJni.frameIsKey(frame)

    public actual fun frameSampleAspectRatioNum(frame: Long): Int =
        FFmpegJni.frameSampleAspectRatioNum(frame)

    public actual fun frameSampleAspectRatioDen(frame: Long): Int =
        FFmpegJni.frameSampleAspectRatioDen(frame)

    public actual fun setFrameSampleAspectRatio(frame: Long, num: Int, den: Int): Unit =
        FFmpegJni.setFrameSampleAspectRatio(frame, num, den)

    public actual fun frameLinesize(frame: Long, plane: Int): Int =
        FFmpegJni.frameLinesize(frame, plane)

    public actual fun framePlaneSize(frame: Long, plane: Int): Int =
        FFmpegJni.framePlaneSize(frame, plane)

    public actual fun frameCopyPlane(frame: Long, plane: Int, out: ByteArray): Int =
        FFmpegJni.frameCopyPlane(frame, plane, out)

    public actual fun frameWritePlane(frame: Long, plane: Int, data: ByteArray): Int =
        FFmpegJni.frameWritePlane(frame, plane, data)

    public actual fun avctxGetInt(ctx: Long, field: Int): Int = FFmpegJni.avctxGetInt(ctx, field)
    public actual fun avctxSetInt(ctx: Long, field: Int, value: Int): Int =
        FFmpegJni.avctxSetInt(ctx, field, value)

    public actual fun avctxGetInt64(ctx: Long, field: Int): Long = FFmpegJni.avctxGetInt64(ctx, field)
    public actual fun avctxSetInt64(ctx: Long, field: Int, value: Long): Int =
        FFmpegJni.avctxSetInt64(ctx, field, value)

    public actual fun avctxGetRational(ctx: Long, field: Int): Long =
        FFmpegJni.avctxGetRational(ctx, field)

    public actual fun avctxSetRational(ctx: Long, field: Int, num: Int, den: Int): Int =
        FFmpegJni.avctxSetRational(ctx, field, num, den)

    public actual fun avctxSetExtradata(ctx: Long, data: ByteArray): Int =
        FFmpegJni.avctxSetExtradata(ctx, data)

    public actual fun avctxExtradataSize(ctx: Long): Int = FFmpegJni.avctxExtradataSize(ctx)
    public actual fun avctxCopyExtradata(ctx: Long, out: ByteArray): Int =
        FFmpegJni.avctxCopyExtradata(ctx, out)

    public actual fun findDecoderByName(name: String): Long = FFmpegJni.findDecoderByName(name)
    public actual fun findEncoderByName(name: String): Long = FFmpegJni.findEncoderByName(name)
    public actual fun findDecoder(id: Int): Long = FFmpegJni.findDecoder(id)
    public actual fun findEncoder(id: Int): Long = FFmpegJni.findEncoder(id)
    public actual fun codecName(codec: Long): String? = FFmpegJni.codecName(codec)
    public actual fun codecId(codec: Long): Int = FFmpegJni.codecId(codec)
    public actual fun codecType(codec: Long): Int = FFmpegJni.codecType(codec)
    public actual fun codecIsDecoder(codec: Long): Boolean = FFmpegJni.codecIsDecoder(codec)
    public actual fun codecIsEncoder(codec: Long): Boolean = FFmpegJni.codecIsEncoder(codec)
    public actual fun codecIdFromName(name: String): Int = FFmpegJni.codecIdFromName(name)

    public actual fun avctxAlloc(codec: Long): Long = FFmpegJni.avctxAlloc(codec)
    public actual fun avctxFree(ctx: Long): Unit = FFmpegJni.avctxFree(ctx)
    public actual fun avctxOpen2(ctx: Long, codec: Long, dict: Long): Int =
        FFmpegJni.avctxOpen2(ctx, codec, dict)

    public actual fun avctxSendPacket(ctx: Long, packet: Long): Int =
        FFmpegJni.avctxSendPacket(ctx, packet)

    public actual fun avctxReceiveFrame(ctx: Long, frame: Long): Int =
        FFmpegJni.avctxReceiveFrame(ctx, frame)

    public actual fun avctxSendFrame(ctx: Long, frame: Long): Int =
        FFmpegJni.avctxSendFrame(ctx, frame)

    public actual fun avctxReceivePacket(ctx: Long, packet: Long): Int =
        FFmpegJni.avctxReceivePacket(ctx, packet)

    public actual fun avctxFlush(ctx: Long): Unit = FFmpegJni.avctxFlush(ctx)

    public actual fun packetAlloc(): Long = FFmpegJni.packetAlloc()
    public actual fun packetFree(packet: Long): Unit = FFmpegJni.packetFree(packet)
    public actual fun packetUnref(packet: Long): Unit = FFmpegJni.packetUnref(packet)
    public actual fun packetAllocData(packet: Long, size: Int): Int =
        FFmpegJni.packetAllocData(packet, size)

    public actual fun packetSetData(packet: Long, data: ByteArray): Int =
        FFmpegJni.packetSetData(packet, data)

    public actual fun packetSize(packet: Long): Int = FFmpegJni.packetSize(packet)
    public actual fun packetCopyData(packet: Long, out: ByteArray): Int =
        FFmpegJni.packetCopyData(packet, out)

    public actual fun packetGetInt64(packet: Long, field: Int): Long =
        FFmpegJni.packetGetInt64(packet, field)

    public actual fun packetSetInt64(packet: Long, field: Int, value: Long): Unit =
        FFmpegJni.packetSetInt64(packet, field, value)

    public actual fun packetRescaleTs(
        packet: Long,
        srcNum: Int,
        srcDen: Int,
        dstNum: Int,
        dstDen: Int,
    ): Unit = FFmpegJni.packetRescaleTs(packet, srcNum, srcDen, dstNum, dstDen)

    public actual fun codecparAlloc(): Long = FFmpegJni.codecparAlloc()
    public actual fun codecparFree(par: Long): Unit = FFmpegJni.codecparFree(par)
    public actual fun codecparFromContext(par: Long, ctx: Long): Int =
        FFmpegJni.codecparFromContext(par, ctx)

    public actual fun codecparToContext(ctx: Long, par: Long): Int =
        FFmpegJni.codecparToContext(ctx, par)

    public actual fun codecparCopy(dst: Long, src: Long): Int = FFmpegJni.codecparCopy(dst, src)
    public actual fun codecparGetInt(par: Long, field: Int): Int =
        FFmpegJni.codecparGetInt(par, field)

    public actual fun codecparSetInt(par: Long, field: Int, value: Int): Int =
        FFmpegJni.codecparSetInt(par, field, value)

    public actual fun codecparGetInt64(par: Long, field: Int): Long =
        FFmpegJni.codecparGetInt64(par, field)

    public actual fun codecparSetInt64(par: Long, field: Int, value: Long): Int =
        FFmpegJni.codecparSetInt64(par, field, value)

    public actual fun codecparGetRational(par: Long, field: Int): Long =
        FFmpegJni.codecparGetRational(par, field)

    public actual fun codecparSetRational(par: Long, field: Int, num: Int, den: Int): Int =
        FFmpegJni.codecparSetRational(par, field, num, den)

    public actual fun codecparSetExtradata(par: Long, data: ByteArray): Int =
        FFmpegJni.codecparSetExtradata(par, data)

    public actual fun codecparExtradataSize(par: Long): Int = FFmpegJni.codecparExtradataSize(par)
    public actual fun codecparCopyExtradata(par: Long, out: ByteArray): Int =
        FFmpegJni.codecparCopyExtradata(par, out)

    public actual fun formatFindInputFormat(name: String): Long =
        FFmpegJni.formatFindInputFormat(name)

    public actual fun formatFindOutputFormat(name: String): Long =
        FFmpegJni.formatFindOutputFormat(name)

    public actual fun inputFormatName(format: Long): String? = FFmpegJni.inputFormatName(format)
    public actual fun outputFormatName(format: Long): String? = FFmpegJni.outputFormatName(format)

    public actual fun inputOpen(url: String, dict: Long): Long = FFmpegJni.inputOpen(url, dict)
    public actual fun inputOpenFormat(url: String, formatName: String, dict: Long): Long =
        FFmpegJni.inputOpenFormat(url, formatName, dict)
    public actual fun inputFindStreamInfo(ctx: Long, dict: Long): Int =
        FFmpegJni.inputFindStreamInfo(ctx, dict)

    public actual fun inputClose(ctx: Long): Unit = FFmpegJni.inputClose(ctx)
    public actual fun inputReadFrame(ctx: Long, packet: Long): Int =
        FFmpegJni.inputReadFrame(ctx, packet)

    public actual fun inputSeek(ctx: Long, streamIndex: Int, timestamp: Long, flags: Int): Int =
        FFmpegJni.inputSeek(ctx, streamIndex, timestamp, flags)

    public actual fun formatNbStreams(ctx: Long): Int = FFmpegJni.formatNbStreams(ctx)
    public actual fun formatGetStream(ctx: Long, index: Int): Long =
        FFmpegJni.formatGetStream(ctx, index)

    public actual fun formatStartTime(ctx: Long): Long = FFmpegJni.formatStartTime(ctx)
    public actual fun formatDuration(ctx: Long): Long = FFmpegJni.formatDuration(ctx)
    public actual fun formatName(ctx: Long): String? = FFmpegJni.formatName(ctx)

    public actual fun outputOpen(formatName: String, filename: String): Long =
        FFmpegJni.outputOpen(formatName, filename)

    public actual fun outputWriteHeader(ctx: Long, dict: Long): Int =
        FFmpegJni.outputWriteHeader(ctx, dict)

    public actual fun outputWritePacket(ctx: Long, packet: Long): Int =
        FFmpegJni.outputWritePacket(ctx, packet)

    public actual fun outputWriteTrailer(ctx: Long): Int = FFmpegJni.outputWriteTrailer(ctx)
    public actual fun outputClose(ctx: Long): Unit = FFmpegJni.outputClose(ctx)

    public actual fun formatNewStream(ctx: Long, codec: Long): Long =
        FFmpegJni.formatNewStream(ctx, codec)

    public actual fun streamIndex(stream: Long): Int = FFmpegJni.streamIndex(stream)
    public actual fun streamTimeBase(stream: Long): Long = FFmpegJni.streamTimeBase(stream)
    public actual fun streamSetTimeBase(stream: Long, num: Int, den: Int): Int =
        FFmpegJni.streamSetTimeBase(stream, num, den)

    public actual fun streamDuration(stream: Long): Long = FFmpegJni.streamDuration(stream)
    public actual fun streamAvgFrameRate(stream: Long): Long =
        FFmpegJni.streamAvgFrameRate(stream)

    public actual fun streamCodecpar(stream: Long): Long = FFmpegJni.streamCodecpar(stream)
    public actual fun streamSetCodecpar(stream: Long, par: Long): Int =
        FFmpegJni.streamSetCodecpar(stream, par)

    public actual fun graphAlloc(): Long = FFmpegJni.graphAlloc()
    public actual fun graphFree(graph: Long): Unit = FFmpegJni.graphFree(graph)
    public actual fun graphCreateFilter(
        graph: Long,
        name: String,
        instanceName: String,
        args: String?,
    ): Long = FFmpegJni.graphCreateFilter(graph, name, instanceName, args)

    public actual fun graphLink(src: Long, srcPad: Int, dst: Long, dstPad: Int): Int =
        FFmpegJni.graphLink(src, srcPad, dst, dstPad)

    public actual fun graphConfig(graph: Long): Int = FFmpegJni.graphConfig(graph)
    public actual fun buffersrcAddFrame(src: Long, frame: Long): Int =
        FFmpegJni.buffersrcAddFrame(src, frame)

    public actual fun buffersinkGetFrame(sink: Long, frame: Long): Int =
        FFmpegJni.buffersinkGetFrame(sink, frame)

    public actual fun swsGetContext(
        srcWidth: Int,
        srcHeight: Int,
        srcFormat: Int,
        dstWidth: Int,
        dstHeight: Int,
        dstFormat: Int,
        flags: Int,
    ): Long = FFmpegJni.swsGetContext(
        srcWidth,
        srcHeight,
        srcFormat,
        dstWidth,
        dstHeight,
        dstFormat,
        flags,
    )

    public actual fun swsFreeContext(ctx: Long): Unit = FFmpegJni.swsFreeContext(ctx)
    public actual fun swsScaleFrame(ctx: Long, dst: Long, src: Long): Int =
        FFmpegJni.swsScaleFrame(ctx, dst, src)

    public actual fun swrAllocSetOpts(
        outSampleRate: Int,
        outFormat: Int,
        outChannels: Int,
        inSampleRate: Int,
        inFormat: Int,
        inChannels: Int,
    ): Long = FFmpegJni.swrAllocSetOpts(
        outSampleRate,
        outFormat,
        outChannels,
        inSampleRate,
        inFormat,
        inChannels,
    )

    public actual fun swrInit(ctx: Long): Int = FFmpegJni.swrInit(ctx)
    public actual fun swrConvertFrame(ctx: Long, out: Long, input: Long): Int =
        FFmpegJni.swrConvertFrame(ctx, out, input)

    public actual fun swrOutSamples(ctx: Long, inSamples: Int): Int =
        FFmpegJni.swrOutSamples(ctx, inSamples)

    public actual fun swrFree(ctx: Long): Unit = FFmpegJni.swrFree(ctx)

    public actual fun deviceRegisterAll(): Unit = FFmpegJni.deviceRegisterAll()
}
