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
 * Web implementation of [FFmpegNative].
 *
 * The Emscripten module is instantiated synchronously on first use from the
 * embedded runtime and WebAssembly bytes, so the public API stays blocking on
 * JS and Wasm just like on the JVM and Kotlin/Native.
 *
 * Pointers are 32-bit WASM addresses; handles keep the common `Long` surface by
 * zero-extending them. 64-bit timestamps/bitrates are transported as JS numbers
 * (`fk_i64` is `double` under `__EMSCRIPTEN__`).
 */
public actual object FFmpegNative {
    init {
        WebGlue.init()
    }

    // ==================== Versions ====================

    public actual fun versionInfo(): String = WebGlue.readString(i("fk_version_info")) ?: ""
    public actual fun configuration(): String = WebGlue.readString(i("fk_configuration")) ?: ""
    public actual fun license(): String = WebGlue.readString(i("fk_license")) ?: ""
    public actual fun lastError(): Int = i("fk_last_error")

    public actual fun avutilVersion(): Int = i("fk_avutil_version")
    public actual fun avcodecVersion(): Int = i("fk_avcodec_version")
    public actual fun avformatVersion(): Int = i("fk_avformat_version")
    public actual fun avfilterVersion(): Int = i("fk_avfilter_version")
    public actual fun swscaleVersion(): Int = i("fk_swscale_version")
    public actual fun swresampleVersion(): Int = i("fk_swresample_version")
    public actual fun avdeviceVersion(): Int = i("fk_avdevice_version")

    // ==================== libavutil ====================

    public actual fun logLevel(): Int = i("fk_log_level")
    public actual fun setLogLevel(level: Int): Unit = unit("fk_log_set_level", level)
    public actual fun logMessage(level: Int, message: String): Unit =
        withString(message) { unit("fk_log_message", level, it) }

    public actual fun errorEagain(): Int = i("fk_error_eagain")
    public actual fun errorEof(): Int = i("fk_error_eof")
    public actual fun errorString(error: Int): String =
        WebGlue.readString(i("fk_error_string", error)) ?: ""

    public actual fun pixFmtName(format: Int): String? =
        WebGlue.readString(i("fk_pix_fmt_name", format))

    public actual fun pixFmtGet(name: String): Int = withString(name) { i("fk_pix_fmt_get", it) }
    public actual fun sampleFmtName(format: Int): String? =
        WebGlue.readString(i("fk_sample_fmt_name", format))

    public actual fun sampleFmtGet(name: String): Int = withString(name) { i("fk_sample_fmt_get", it) }
    public actual fun sampleFmtBytes(format: Int): Int = i("fk_sample_fmt_bytes", format)

    public actual fun dictCreate(): Long = i("fk_dict_create").handle()
    public actual fun dictFree(dict: Long): Unit = unit("fk_dict_free", dict.p())
    public actual fun dictSet(dict: Long, key: String, value: String): Int =
        withStrings(key, value) { k, v -> i("fk_dict_set", dict.p(), k, v) }

    public actual fun dictSetInt(dict: Long, key: String, value: Long): Int =
        withString(key) { k -> dd("fk_dict_set_int", dict.p().toDouble(), k.toDouble(), value.toDouble()).toInt() }

    public actual fun dictGet(dict: Long, key: String): String? =
        withString(key) { k -> WebGlue.readString(i("fk_dict_get", dict.p(), k)) }

    public actual fun dictGetInt(dict: Long, key: String, fallback: Long): Long =
        withString(key) { k ->
            dd("fk_dict_get_int", dict.p().toDouble(), k.toDouble(), fallback.toDouble()).toLong()
        }

    public actual fun dictCount(dict: Long): Int = i("fk_dict_count", dict.p())

    public actual fun frameAlloc(): Long = i("fk_frame_alloc").handle()
    public actual fun frameFree(frame: Long): Unit = unit("fk_frame_free", frame.p())
    public actual fun frameUnref(frame: Long): Unit = unit("fk_frame_unref", frame.p())
    public actual fun frameRef(dst: Long, src: Long): Int = i("fk_frame_ref", dst.p(), src.p())
    public actual fun frameGetBuffer(frame: Long, align: Int): Int =
        i("fk_frame_get_buffer", frame.p(), align)

    public actual fun frameMakeWritable(frame: Long): Int = i("fk_frame_make_writable", frame.p())

    public actual fun frameSetVideo(frame: Long, width: Int, height: Int, format: Int): Int =
        i("fk_frame_set_video", frame.p(), width, height, format)

    public actual fun frameSetAudio(
        frame: Long,
        sampleRate: Int,
        format: Int,
        nbSamples: Int,
        channels: Int,
    ): Int = i("fk_frame_set_audio", frame.p(), sampleRate, format, nbSamples, channels)

    public actual fun frameWidth(frame: Long): Int = i("fk_frame_get_width", frame.p())
    public actual fun frameHeight(frame: Long): Int = i("fk_frame_get_height", frame.p())
    public actual fun frameFormat(frame: Long): Int = i("fk_frame_get_format", frame.p())
    public actual fun framePts(frame: Long): Long = d("fk_frame_get_pts", frame.p()).toLong()
    public actual fun setFramePts(frame: Long, pts: Long): Unit =
        dd("fk_frame_set_pts", frame.p().toDouble(), pts.toDouble()).let { }

    public actual fun framePktDts(frame: Long): Long =
        d("fk_frame_get_pkt_dts", frame.p()).toLong()

    public actual fun frameDuration(frame: Long): Long =
        d("fk_frame_get_duration", frame.p()).toLong()

    public actual fun frameNbSamples(frame: Long): Int = i("fk_frame_get_nb_samples", frame.p())
    public actual fun frameSampleRate(frame: Long): Int = i("fk_frame_get_sample_rate", frame.p())
    public actual fun frameChannels(frame: Long): Int = i("fk_frame_get_channels", frame.p())
    public actual fun frameIsKey(frame: Long): Boolean = i("fk_frame_is_key", frame.p()) != 0

    public actual fun frameSampleAspectRatioNum(frame: Long): Int =
        i("fk_frame_get_sample_aspect_ratio_num", frame.p())

    public actual fun frameSampleAspectRatioDen(frame: Long): Int =
        i("fk_frame_get_sample_aspect_ratio_den", frame.p())

    public actual fun setFrameSampleAspectRatio(frame: Long, num: Int, den: Int): Unit =
        unit("fk_frame_set_sample_aspect_ratio", frame.p(), num, den)

    public actual fun frameLinesize(frame: Long, plane: Int): Int =
        i("fk_frame_get_linesize", frame.p(), plane)

    public actual fun framePlaneSize(frame: Long, plane: Int): Int =
        i("fk_frame_get_plane_size", frame.p(), plane)

    public actual fun frameCopyPlane(frame: Long, plane: Int, out: ByteArray): Int {
        if (out.isEmpty()) return i("fk_frame_copy_plane", frame.p(), plane, 0, 0)
        val pointer = WebGlue.alloc(out.size)
        try {
            val copied = i("fk_frame_copy_plane", frame.p(), plane, pointer, out.size)
            if (copied > 0) {
                WebGlue.readBytes(pointer, copied).copyInto(out)
            }
            return copied
        } finally {
            WebGlue.free(pointer)
        }
    }

    public actual fun frameWritePlane(frame: Long, plane: Int, data: ByteArray): Int {
        if (data.isEmpty()) return i("fk_frame_write_plane", frame.p(), plane, 0, 0)
        val pointer = WebGlue.writeBytes(data)
        try {
            return i("fk_frame_write_plane", frame.p(), plane, pointer, data.size)
        } finally {
            WebGlue.free(pointer)
        }
    }

    // ==================== libavcodec ====================

    public actual fun avctxGetInt(ctx: Long, field: Int): Int =
        i("fk_avctx_get_int", ctx.p(), field)

    public actual fun avctxSetInt(ctx: Long, field: Int, value: Int): Int =
        i("fk_avctx_set_int", ctx.p(), field, value)

    public actual fun avctxGetInt64(ctx: Long, field: Int): Long =
        d("fk_avctx_get_int64", ctx.p(), field).toLong()

    public actual fun avctxSetInt64(ctx: Long, field: Int, value: Long): Int =
        dd("fk_avctx_set_int64", ctx.p().toDouble(), field.toDouble(), value.toDouble()).toInt()

    public actual fun avctxGetRational(ctx: Long, field: Int): Long =
        readRational("fk_avctx_get_rational", ctx.p(), field)

    public actual fun avctxSetRational(ctx: Long, field: Int, num: Int, den: Int): Int =
        i("fk_avctx_set_rational", ctx.p(), field, num, den)

    public actual fun avctxSetExtradata(ctx: Long, data: ByteArray): Int =
        writeByteCall("fk_avctx_set_extradata", ctx.p(), data)

    public actual fun avctxExtradataSize(ctx: Long): Int =
        i("fk_avctx_get_extradata_size", ctx.p())

    public actual fun avctxCopyExtradata(ctx: Long, out: ByteArray): Int =
        copyBytes("fk_avctx_copy_extradata", ctx.p(), out)

    public actual fun findDecoderByName(name: String): Long =
        withString(name) { i("fk_find_decoder_by_name", it).handle() }

    public actual fun findEncoderByName(name: String): Long =
        withString(name) { i("fk_find_encoder_by_name", it).handle() }

    public actual fun findDecoder(id: Int): Long = i("fk_find_decoder", id).handle()
    public actual fun findEncoder(id: Int): Long = i("fk_find_encoder", id).handle()
    public actual fun codecName(codec: Long): String? =
        WebGlue.readString(i("fk_codec_get_name", codec.p()))

    public actual fun codecId(codec: Long): Int = i("fk_codec_get_id", codec.p())
    public actual fun codecType(codec: Long): Int = i("fk_codec_get_type", codec.p())
    public actual fun codecIsDecoder(codec: Long): Boolean = i("fk_codec_is_decoder", codec.p()) != 0
    public actual fun codecIsEncoder(codec: Long): Boolean = i("fk_codec_is_encoder", codec.p()) != 0
    public actual fun codecIdFromName(name: String): Int =
        withString(name) { i("fk_codec_id_from_name", it) }

    public actual fun avctxAlloc(codec: Long): Long = i("fk_avctx_alloc", codec.p()).handle()
    public actual fun avctxFree(ctx: Long): Unit = unit("fk_avctx_free", ctx.p())
    public actual fun avctxOpen2(ctx: Long, codec: Long, dict: Long): Int =
        i("fk_avctx_open2", ctx.p(), codec.p(), dict.p())

    public actual fun avctxSendPacket(ctx: Long, packet: Long): Int =
        i("fk_avctx_send_packet", ctx.p(), packet.p())

    public actual fun avctxReceiveFrame(ctx: Long, frame: Long): Int =
        i("fk_avctx_receive_frame", ctx.p(), frame.p())

    public actual fun avctxSendFrame(ctx: Long, frame: Long): Int =
        i("fk_avctx_send_frame", ctx.p(), frame.p())

    public actual fun avctxReceivePacket(ctx: Long, packet: Long): Int =
        i("fk_avctx_receive_packet", ctx.p(), packet.p())

    public actual fun avctxFlush(ctx: Long): Unit = unit("fk_avctx_flush", ctx.p())

    public actual fun packetAlloc(): Long = i("fk_packet_alloc").handle()
    public actual fun packetFree(packet: Long): Unit = unit("fk_packet_free", packet.p())
    public actual fun packetUnref(packet: Long): Unit = unit("fk_packet_unref", packet.p())
    public actual fun packetAllocData(packet: Long, size: Int): Int =
        i("fk_packet_alloc_data", packet.p(), size)

    public actual fun packetSetData(packet: Long, data: ByteArray): Int =
        writeByteCall("fk_packet_set_data", packet.p(), data)

    public actual fun packetSize(packet: Long): Int = i("fk_packet_get_size", packet.p())
    public actual fun packetCopyData(packet: Long, out: ByteArray): Int =
        copyBytes("fk_packet_copy_data", packet.p(), out)

    public actual fun packetGetInt64(packet: Long, field: Int): Long =
        d("fk_packet_get_int64", packet.p(), field).toLong()

    public actual fun packetSetInt64(packet: Long, field: Int, value: Long): Unit =
        dd("fk_packet_set_int64", packet.p().toDouble(), field.toDouble(), value.toDouble()).let { }

    public actual fun packetRescaleTs(
        packet: Long,
        srcNum: Int,
        srcDen: Int,
        dstNum: Int,
        dstDen: Int,
    ): Unit = unit("fk_packet_rescale_ts", packet.p(), srcNum, srcDen, dstNum, dstDen)

    public actual fun codecparAlloc(): Long = i("fk_codecpar_alloc").handle()
    public actual fun codecparFree(par: Long): Unit = unit("fk_codecpar_free", par.p())
    public actual fun codecparFromContext(par: Long, ctx: Long): Int =
        i("fk_codecpar_from_context", par.p(), ctx.p())

    public actual fun codecparToContext(ctx: Long, par: Long): Int =
        i("fk_codecpar_to_context", ctx.p(), par.p())

    public actual fun codecparCopy(dst: Long, src: Long): Int =
        i("fk_codecpar_copy", dst.p(), src.p())

    public actual fun codecparGetInt(par: Long, field: Int): Int =
        i("fk_codecpar_get_int", par.p(), field)

    public actual fun codecparSetInt(par: Long, field: Int, value: Int): Int =
        i("fk_codecpar_set_int", par.p(), field, value)

    public actual fun codecparGetInt64(par: Long, field: Int): Long =
        d("fk_codecpar_get_int64", par.p(), field).toLong()

    public actual fun codecparSetInt64(par: Long, field: Int, value: Long): Int =
        dd("fk_codecpar_set_int64", par.p().toDouble(), field.toDouble(), value.toDouble()).toInt()

    public actual fun codecparGetRational(par: Long, field: Int): Long =
        readRational("fk_codecpar_get_rational", par.p(), field)

    public actual fun codecparSetRational(par: Long, field: Int, num: Int, den: Int): Int =
        i("fk_codecpar_set_rational", par.p(), field, num, den)

    public actual fun codecparSetExtradata(par: Long, data: ByteArray): Int =
        writeByteCall("fk_codecpar_set_extradata", par.p(), data)

    public actual fun codecparExtradataSize(par: Long): Int =
        i("fk_codecpar_get_extradata_size", par.p())

    public actual fun codecparCopyExtradata(par: Long, out: ByteArray): Int =
        copyBytes("fk_codecpar_copy_extradata", par.p(), out)

    // ==================== libavformat ====================

    public actual fun formatFindInputFormat(name: String): Long =
        withString(name) { i("fk_format_find_input_format", it).handle() }

    public actual fun formatFindOutputFormat(name: String): Long =
        withString(name) { i("fk_format_find_output_format", it).handle() }

    public actual fun inputFormatName(format: Long): String? =
        WebGlue.readString(i("fk_input_format_get_name", format.p()))

    public actual fun outputFormatName(format: Long): String? =
        WebGlue.readString(i("fk_output_format_get_name", format.p()))

    public actual fun inputOpen(url: String, dict: Long): Long =
        withString(url) { i("fk_input_open", it, dict.p()).handle() }

    public actual fun inputOpenFormat(url: String, formatName: String, dict: Long): Long =
        withStrings(url, formatName) { u, f -> i("fk_input_open_format", u, f, dict.p()).handle() }

    public actual fun inputFindStreamInfo(ctx: Long, dict: Long): Int =
        i("fk_input_find_stream_info", ctx.p(), dict.p())

    public actual fun inputClose(ctx: Long): Unit = unit("fk_input_close", ctx.p())

    public actual fun inputReadFrame(ctx: Long, packet: Long): Int =
        i("fk_input_read_frame", ctx.p(), packet.p())

    public actual fun inputSeek(ctx: Long, streamIndex: Int, timestamp: Long, flags: Int): Int =
        dd("fk_input_seek", ctx.p().toDouble(), streamIndex.toDouble(), timestamp.toDouble(), flags.toDouble()).toInt()

    public actual fun formatNbStreams(ctx: Long): Int = i("fk_format_get_nb_streams", ctx.p())
    public actual fun formatGetStream(ctx: Long, index: Int): Long =
        i("fk_format_get_stream", ctx.p(), index).handle()

    public actual fun formatStartTime(ctx: Long): Long =
        d("fk_format_get_start_time", ctx.p()).toLong()

    public actual fun formatDuration(ctx: Long): Long =
        d("fk_format_get_duration", ctx.p()).toLong()

    public actual fun formatName(ctx: Long): String? =
        WebGlue.readString(i("fk_format_get_format_name", ctx.p()))

    public actual fun outputOpen(formatName: String, filename: String): Long =
        withStrings(formatName, filename) { f, n ->
            i("fk_output_open", f, n).handle()
        }

    public actual fun outputWriteHeader(ctx: Long, dict: Long): Int =
        i("fk_output_write_header", ctx.p(), dict.p())

    public actual fun outputWritePacket(ctx: Long, packet: Long): Int =
        i("fk_output_write_packet", ctx.p(), packet.p())

    public actual fun outputWriteTrailer(ctx: Long): Int = i("fk_output_write_trailer", ctx.p())
    public actual fun outputClose(ctx: Long): Unit = unit("fk_output_close", ctx.p())

    public actual fun formatNewStream(ctx: Long, codec: Long): Long =
        i("fk_format_new_stream", ctx.p(), codec.p()).handle()

    public actual fun streamIndex(stream: Long): Int = i("fk_stream_get_index", stream.p())
    public actual fun streamTimeBase(stream: Long): Long =
        readRational("fk_stream_get_time_base", stream.p())

    public actual fun streamSetTimeBase(stream: Long, num: Int, den: Int): Int =
        i("fk_stream_set_time_base", stream.p(), num, den)

    public actual fun streamDuration(stream: Long): Long =
        d("fk_stream_get_duration", stream.p()).toLong()

    public actual fun streamAvgFrameRate(stream: Long): Long =
        readRational("fk_stream_get_avg_frame_rate", stream.p())

    public actual fun streamCodecpar(stream: Long): Long =
        i("fk_stream_get_codecpar", stream.p()).handle()

    public actual fun streamSetCodecpar(stream: Long, par: Long): Int =
        i("fk_stream_set_codecpar", stream.p(), par.p())

    // ==================== libavfilter ====================

    public actual fun graphAlloc(): Long = i("fk_graph_alloc").handle()
    public actual fun graphFree(graph: Long): Unit = unit("fk_graph_free", graph.p())

    public actual fun graphCreateFilter(
        graph: Long,
        name: String,
        instanceName: String,
        args: String?,
    ): Long = withStrings(name, instanceName) { n, instance ->
        val a = if (args == null) 0 else WebGlue.writeString(args)
        try {
            i("fk_graph_create_filter", graph.p(), n, instance, a).handle()
        } finally {
            if (a != 0) WebGlue.free(a)
        }
    }

    public actual fun graphLink(src: Long, srcPad: Int, dst: Long, dstPad: Int): Int =
        i("fk_graph_link", src.p(), srcPad, dst.p(), dstPad)

    public actual fun graphConfig(graph: Long): Int = i("fk_graph_config", graph.p())

    public actual fun buffersrcAddFrame(src: Long, frame: Long): Int =
        i("fk_buffersrc_add_frame", src.p(), frame.p())

    public actual fun buffersinkGetFrame(sink: Long, frame: Long): Int =
        i("fk_buffersink_get_frame", sink.p(), frame.p())

    // ==================== libswscale ====================

    public actual fun swsGetContext(
        srcWidth: Int,
        srcHeight: Int,
        srcFormat: Int,
        dstWidth: Int,
        dstHeight: Int,
        dstFormat: Int,
        flags: Int,
    ): Long = i(
        "fk_sws_get_context",
        srcWidth,
        srcHeight,
        srcFormat,
        dstWidth,
        dstHeight,
        dstFormat,
        flags,
    ).handle()

    public actual fun swsFreeContext(ctx: Long): Unit = unit("fk_sws_free_context", ctx.p())
    public actual fun swsScaleFrame(ctx: Long, dst: Long, src: Long): Int =
        i("fk_sws_scale_frame", ctx.p(), dst.p(), src.p())

    // ==================== libswresample ====================

    public actual fun swrAllocSetOpts(
        outSampleRate: Int,
        outFormat: Int,
        outChannels: Int,
        inSampleRate: Int,
        inFormat: Int,
        inChannels: Int,
    ): Long = i(
        "fk_swr_alloc_set_opts",
        outSampleRate,
        outFormat,
        outChannels,
        inSampleRate,
        inFormat,
        inChannels,
    ).handle()

    public actual fun swrInit(ctx: Long): Int = i("fk_swr_init", ctx.p())
    public actual fun swrConvertFrame(ctx: Long, out: Long, input: Long): Int =
        i("fk_swr_convert_frame", ctx.p(), out.p(), input.p())

    public actual fun swrOutSamples(ctx: Long, inSamples: Int): Int =
        i("fk_swr_get_out_samples", ctx.p(), inSamples)

    public actual fun swrFree(ctx: Long): Unit = unit("fk_swr_free", ctx.p())

    // ==================== libavdevice ====================

    public actual fun deviceRegisterAll(): Unit = unit("fk_device_register_all")

    // ==================== helpers ====================

    private fun Long.p(): Int = toInt()
    private fun Int.handle(): Long = toLong() and 0xFFFFFFFFL

    private fun i(
        name: String,
        a: Int = 0,
        b: Int = 0,
        c: Int = 0,
        d: Int = 0,
        e: Int = 0,
        f: Int = 0,
        g: Int = 0,
    ): Int = WebGlue.callInt(name, a, b, c, d, e, f, g)

    private fun d(
        name: String,
        a: Int = 0,
        b: Int = 0,
        c: Int = 0,
        d: Int = 0,
        e: Int = 0,
        f: Int = 0,
        g: Int = 0,
    ): Double = WebGlue.callDouble(
        name,
        a.toDouble(),
        b.toDouble(),
        c.toDouble(),
        d.toDouble(),
        e.toDouble(),
        f.toDouble(),
        g.toDouble(),
    )

    private fun dd(
        name: String,
        a: Double = 0.0,
        b: Double = 0.0,
        c: Double = 0.0,
        d: Double = 0.0,
        e: Double = 0.0,
        f: Double = 0.0,
        g: Double = 0.0,
    ): Double = WebGlue.callDouble(name, a, b, c, d, e, f, g)

    private fun unit(
        name: String,
        a: Int = 0,
        b: Int = 0,
        c: Int = 0,
        d: Int = 0,
        e: Int = 0,
        f: Int = 0,
        g: Int = 0,
    ) {
        WebGlue.callInt(name, a, b, c, d, e, f, g)
    }

    private inline fun <R> withString(value: String, block: (Int) -> R): R {
        val pointer = WebGlue.writeString(value)
        try {
            return block(pointer)
        } finally {
            WebGlue.free(pointer)
        }
    }

    private inline fun <R> withStrings(a: String, b: String, block: (Int, Int) -> R): R =
        withString(a) { p1 -> withString(b) { p2 -> block(p1, p2) } }

    private fun writeByteCall(name: String, handle: Int, data: ByteArray): Int {
        if (data.isEmpty()) return i(name, handle, 0, 0)
        val pointer = WebGlue.writeBytes(data)
        try {
            return i(name, handle, pointer, data.size)
        } finally {
            WebGlue.free(pointer)
        }
    }

    private fun copyBytes(name: String, handle: Int, out: ByteArray): Int {
        if (out.isEmpty()) return i(name, handle, 0, 0)
        val pointer = WebGlue.alloc(out.size)
        try {
            val copied = i(name, handle, pointer, out.size)
            if (copied > 0) WebGlue.readBytes(pointer, copied).copyInto(out)
            return copied
        } finally {
            WebGlue.free(pointer)
        }
    }

    private fun readRational(name: String, handle: Int, field: Int = 0): Long {
        val pointer = WebGlue.alloc(8)
        try {
            val result = if (name == "fk_stream_get_time_base" || name == "fk_stream_get_avg_frame_rate") {
                i(name, handle, pointer, pointer + 4)
            } else {
                i(name, handle, field, pointer, pointer + 4)
            }
            if (result < 0) return 0L
            val num = WebGlue.readInt(pointer)
            val den = WebGlue.readInt(pointer + 4)
            return (num.toLong() shl 32) or (den.toLong() and 0xFFFFFFFFL)
        } finally {
            WebGlue.free(pointer)
        }
    }
}
