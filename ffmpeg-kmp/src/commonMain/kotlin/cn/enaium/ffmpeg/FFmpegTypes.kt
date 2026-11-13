/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg

// One import surface for consumers of the aggregate artifact. The aliases live
// in the root package so `import cn.enaium.ffmpeg.*` is enough to use the whole
// binding.

public typealias AvException = cn.enaium.ffmpeg.avutil.AvException
public typealias AvError = cn.enaium.ffmpeg.avutil.AvError
public typealias AvRational = cn.enaium.ffmpeg.avutil.AvRational
public typealias AvDictionary = cn.enaium.ffmpeg.avutil.AvDictionary
public typealias AvFrame = cn.enaium.ffmpeg.avutil.AvFrame
public typealias AvPixelFormat = cn.enaium.ffmpeg.avutil.AvPixelFormat
public typealias AvSampleFormat = cn.enaium.ffmpeg.avutil.AvSampleFormat
public typealias AvLog = cn.enaium.ffmpeg.avutil.AvLog
public typealias AvLogLevel = cn.enaium.ffmpeg.avutil.AvLogLevel

public typealias AvCodec = cn.enaium.ffmpeg.avcodec.AvCodec
public typealias AvCodecContext = cn.enaium.ffmpeg.avcodec.AvCodecContext
public typealias AvCodecParameters = cn.enaium.ffmpeg.avcodec.AvCodecParameters
public typealias AvPacket = cn.enaium.ffmpeg.avcodec.AvPacket
public typealias AvCodecId = cn.enaium.ffmpeg.avcodec.AvCodecId
public typealias AvMediaType = cn.enaium.ffmpeg.avcodec.AvMediaType
public typealias AvPacketFlag = cn.enaium.ffmpeg.avcodec.AvPacketFlag

public typealias AvFormatContext = cn.enaium.ffmpeg.avformat.AvFormatContext
public typealias AvStream = cn.enaium.ffmpeg.avformat.AvStream
public typealias AvFormat = cn.enaium.ffmpeg.avformat.AvFormat
public typealias AvInputFormat = cn.enaium.ffmpeg.avformat.AvInputFormat
public typealias AvOutputFormat = cn.enaium.ffmpeg.avformat.AvOutputFormat
public typealias AvSeekFlag = cn.enaium.ffmpeg.avformat.AvSeekFlag

public typealias AvFilterGraph = cn.enaium.ffmpeg.avfilter.AvFilterGraph
public typealias AvFilterContext = cn.enaium.ffmpeg.avfilter.AvFilterContext
public typealias SwsContext = cn.enaium.ffmpeg.swscale.SwsContext
public typealias SwsFlag = cn.enaium.ffmpeg.swscale.SwsFlag
public typealias SwrContext = cn.enaium.ffmpeg.swresample.SwrContext
public typealias AvDevice = cn.enaium.ffmpeg.avdevice.AvDevice
