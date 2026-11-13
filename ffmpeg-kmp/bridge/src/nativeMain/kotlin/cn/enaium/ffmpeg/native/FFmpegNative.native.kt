@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.native

import ffmpeg_kmp.*
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeNullPtr
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value

private fun Long.asPointer(): CPointer<out CPointed>? =
    if (this == 0L) null else interpretCPointer<COpaque>(nativeNullPtr.plus(this))

private fun CPointer<out CPointed>?.asHandle(): Long = this?.rawValue?.toLong() ?: 0L

private fun packRational(num: Int, den: Int): Long =
    (num.toLong() shl 32) or (den.toLong() and 0xFFFFFFFFL)

private inline fun <R> ByteArray.withPinnedBytes(block: (CPointer<UByteVar>?, Int) -> R): R =
    if (isEmpty()) {
        block(null, 0)
    } else {
        usePinned { pinned ->
            block(pinned.addressOf(0).reinterpret(), size)
        }
    }

/** cinterop implementation of [FFmpegNative]. */
public actual object FFmpegNative {
    public actual fun versionInfo(): String = fk_version_info()?.toKString().orEmpty()
    public actual fun configuration(): String = fk_configuration()?.toKString().orEmpty()
    public actual fun license(): String = fk_license()?.toKString().orEmpty()
    public actual fun lastError(): Int = fk_last_error()

    public actual fun avutilVersion(): Int = fk_avutil_version().toInt()
    public actual fun avcodecVersion(): Int = fk_avcodec_version().toInt()
    public actual fun avformatVersion(): Int = fk_avformat_version().toInt()
    public actual fun avfilterVersion(): Int = fk_avfilter_version().toInt()
    public actual fun swscaleVersion(): Int = fk_swscale_version().toInt()
    public actual fun swresampleVersion(): Int = fk_swresample_version().toInt()
    public actual fun avdeviceVersion(): Int = fk_avdevice_version().toInt()

    public actual fun logLevel(): Int = fk_log_level()
    public actual fun setLogLevel(level: Int): Unit = fk_log_set_level(level)
    public actual fun logMessage(level: Int, message: String): Unit =
        fk_log_message(level, message)

    public actual fun errorEagain(): Int = fk_error_eagain()
    public actual fun errorEof(): Int = fk_error_eof()
    public actual fun errorString(error: Int): String =
        fk_error_string(error)?.toKString().orEmpty()

    public actual fun pixFmtName(format: Int): String? = fk_pix_fmt_name(format)?.toKString()
    public actual fun pixFmtGet(name: String): Int = fk_pix_fmt_get(name)
    public actual fun sampleFmtName(format: Int): String? =
        fk_sample_fmt_name(format)?.toKString()

    public actual fun sampleFmtGet(name: String): Int = fk_sample_fmt_get(name)
    public actual fun sampleFmtBytes(format: Int): Int = fk_sample_fmt_bytes(format)

    public actual fun dictCreate(): Long = fk_dict_create().asHandle()
    public actual fun dictFree(dict: Long): Unit = fk_dict_free(dict.asPointer())
    public actual fun dictSet(dict: Long, key: String, value: String): Int =
        fk_dict_set(dict.asPointer(), key, value)

    public actual fun dictSetInt(dict: Long, key: String, value: Long): Int =
        fk_dict_set_int(dict.asPointer(), key, value)

    public actual fun dictGet(dict: Long, key: String): String? =
        fk_dict_get(dict.asPointer(), key)?.toKString()

    public actual fun dictGetInt(dict: Long, key: String, fallback: Long): Long =
        fk_dict_get_int(dict.asPointer(), key, fallback)

    public actual fun dictCount(dict: Long): Int = fk_dict_count(dict.asPointer())

    public actual fun frameAlloc(): Long = fk_frame_alloc().asHandle()
    public actual fun frameFree(frame: Long): Unit = fk_frame_free(frame.asPointer())
    public actual fun frameUnref(frame: Long): Unit = fk_frame_unref(frame.asPointer())
    public actual fun frameRef(dst: Long, src: Long): Int =
        fk_frame_ref(dst.asPointer(), src.asPointer())

    public actual fun frameGetBuffer(frame: Long, align: Int): Int =
        fk_frame_get_buffer(frame.asPointer(), align)

    public actual fun frameMakeWritable(frame: Long): Int =
        fk_frame_make_writable(frame.asPointer())

    public actual fun frameSetVideo(frame: Long, width: Int, height: Int, format: Int): Int =
        fk_frame_set_video(frame.asPointer(), width, height, format)

    public actual fun frameSetAudio(
        frame: Long,
        sampleRate: Int,
        format: Int,
        nbSamples: Int,
        channels: Int,
    ): Int = fk_frame_set_audio(frame.asPointer(), sampleRate, format, nbSamples, channels)

    public actual fun frameWidth(frame: Long): Int = fk_frame_get_width(frame.asPointer())
    public actual fun frameHeight(frame: Long): Int = fk_frame_get_height(frame.asPointer())
    public actual fun frameFormat(frame: Long): Int = fk_frame_get_format(frame.asPointer())
    public actual fun framePts(frame: Long): Long = fk_frame_get_pts(frame.asPointer())
    public actual fun setFramePts(frame: Long, pts: Long): Unit =
        fk_frame_set_pts(frame.asPointer(), pts)

    public actual fun framePktDts(frame: Long): Long = fk_frame_get_pkt_dts(frame.asPointer())
    public actual fun frameDuration(frame: Long): Long = fk_frame_get_duration(frame.asPointer())
    public actual fun frameNbSamples(frame: Long): Int = fk_frame_get_nb_samples(frame.asPointer())
    public actual fun frameSampleRate(frame: Long): Int = fk_frame_get_sample_rate(frame.asPointer())
    public actual fun frameChannels(frame: Long): Int = fk_frame_get_channels(frame.asPointer())

    public actual fun frameIsKey(frame: Long): Boolean =
        fk_frame_is_key(frame.asPointer()) != 0

    public actual fun frameSampleAspectRatioNum(frame: Long): Int =
        fk_frame_get_sample_aspect_ratio_num(frame.asPointer())

    public actual fun frameSampleAspectRatioDen(frame: Long): Int =
        fk_frame_get_sample_aspect_ratio_den(frame.asPointer())

    public actual fun setFrameSampleAspectRatio(frame: Long, num: Int, den: Int): Unit =
        fk_frame_set_sample_aspect_ratio(frame.asPointer(), num, den)

    public actual fun frameLinesize(frame: Long, plane: Int): Int =
        fk_frame_get_linesize(frame.asPointer(), plane)

    public actual fun framePlaneSize(frame: Long, plane: Int): Int =
        fk_frame_get_plane_size(frame.asPointer(), plane)

    public actual fun frameCopyPlane(frame: Long, plane: Int, out: ByteArray): Int =
        out.withPinnedBytes { data, size ->
            fk_frame_copy_plane(frame.asPointer(), plane, data, size)
        }

    public actual fun frameWritePlane(frame: Long, plane: Int, data: ByteArray): Int =
        data.withPinnedBytes { bytes, size ->
            fk_frame_write_plane(frame.asPointer(), plane, bytes, size)
        }

    public actual fun avctxGetInt(ctx: Long, field: Int): Int =
        fk_avctx_get_int(ctx.asPointer(), field)

    public actual fun avctxSetInt(ctx: Long, field: Int, value: Int): Int =
        fk_avctx_set_int(ctx.asPointer(), field, value)

    public actual fun avctxGetInt64(ctx: Long, field: Int): Long =
        fk_avctx_get_int64(ctx.asPointer(), field)

    public actual fun avctxSetInt64(ctx: Long, field: Int, value: Long): Int =
        fk_avctx_set_int64(ctx.asPointer(), field, value)

    public actual fun avctxGetRational(ctx: Long, field: Int): Long = memScoped {
        val num = alloc<IntVar>()
        val den = alloc<IntVar>()
        if (fk_avctx_get_rational(ctx.asPointer(), field, num.ptr, den.ptr) < 0) {
            0L
        } else {
            packRational(num.value, den.value)
        }
    }

    public actual fun avctxSetRational(ctx: Long, field: Int, num: Int, den: Int): Int =
        fk_avctx_set_rational(ctx.asPointer(), field, num, den)

    public actual fun avctxSetExtradata(ctx: Long, data: ByteArray): Int =
        data.withPinnedBytes { bytes, size ->
            fk_avctx_set_extradata(ctx.asPointer(), bytes, size)
        }

    public actual fun avctxExtradataSize(ctx: Long): Int =
        fk_avctx_get_extradata_size(ctx.asPointer())

    public actual fun avctxCopyExtradata(ctx: Long, out: ByteArray): Int =
        out.withPinnedBytes { bytes, size ->
            fk_avctx_copy_extradata(ctx.asPointer(), bytes, size)
        }

    public actual fun findDecoderByName(name: String): Long =
        fk_find_decoder_by_name(name).asHandle()

    public actual fun findEncoderByName(name: String): Long =
        fk_find_encoder_by_name(name).asHandle()

    public actual fun findDecoder(id: Int): Long = fk_find_decoder(id).asHandle()
    public actual fun findEncoder(id: Int): Long = fk_find_encoder(id).asHandle()
    public actual fun codecName(codec: Long): String? =
        fk_codec_get_name(codec.asPointer())?.toKString()

    public actual fun codecId(codec: Long): Int = fk_codec_get_id(codec.asPointer())
    public actual fun codecType(codec: Long): Int = fk_codec_get_type(codec.asPointer())
    public actual fun codecIsDecoder(codec: Long): Boolean =
        fk_codec_is_decoder(codec.asPointer()) != 0

    public actual fun codecIsEncoder(codec: Long): Boolean =
        fk_codec_is_encoder(codec.asPointer()) != 0

    public actual fun codecIdFromName(name: String): Int = fk_codec_id_from_name(name)

    public actual fun avctxAlloc(codec: Long): Long = fk_avctx_alloc(codec.asPointer()).asHandle()
    public actual fun avctxFree(ctx: Long): Unit = fk_avctx_free(ctx.asPointer())
    public actual fun avctxOpen2(ctx: Long, codec: Long, dict: Long): Int =
        fk_avctx_open2(ctx.asPointer(), codec.asPointer(), dict.asPointer())

    public actual fun avctxSendPacket(ctx: Long, packet: Long): Int =
        fk_avctx_send_packet(ctx.asPointer(), packet.asPointer())

    public actual fun avctxReceiveFrame(ctx: Long, frame: Long): Int =
        fk_avctx_receive_frame(ctx.asPointer(), frame.asPointer())

    public actual fun avctxSendFrame(ctx: Long, frame: Long): Int =
        fk_avctx_send_frame(ctx.asPointer(), frame.asPointer())

    public actual fun avctxReceivePacket(ctx: Long, packet: Long): Int =
        fk_avctx_receive_packet(ctx.asPointer(), packet.asPointer())

    public actual fun avctxFlush(ctx: Long): Unit = fk_avctx_flush(ctx.asPointer())

    public actual fun packetAlloc(): Long = fk_packet_alloc().asHandle()
    public actual fun packetFree(packet: Long): Unit = fk_packet_free(packet.asPointer())
    public actual fun packetUnref(packet: Long): Unit = fk_packet_unref(packet.asPointer())
    public actual fun packetAllocData(packet: Long, size: Int): Int =
        fk_packet_alloc_data(packet.asPointer(), size)

    public actual fun packetSetData(packet: Long, data: ByteArray): Int =
        data.withPinnedBytes { bytes, size ->
            fk_packet_set_data(packet.asPointer(), bytes, size)
        }

    public actual fun packetSize(packet: Long): Int = fk_packet_get_size(packet.asPointer())

    public actual fun packetCopyData(packet: Long, out: ByteArray): Int =
        out.withPinnedBytes { bytes, size ->
            fk_packet_copy_data(packet.asPointer(), bytes, size)
        }

    public actual fun packetGetInt64(packet: Long, field: Int): Long =
        fk_packet_get_int64(packet.asPointer(), field)

    public actual fun packetSetInt64(packet: Long, field: Int, value: Long): Unit =
        fk_packet_set_int64(packet.asPointer(), field, value)

    public actual fun packetRescaleTs(
        packet: Long,
        srcNum: Int,
        srcDen: Int,
        dstNum: Int,
        dstDen: Int,
    ): Unit = fk_packet_rescale_ts(packet.asPointer(), srcNum, srcDen, dstNum, dstDen)

    public actual fun codecparAlloc(): Long = fk_codecpar_alloc().asHandle()
    public actual fun codecparFree(par: Long): Unit = fk_codecpar_free(par.asPointer())
    public actual fun codecparFromContext(par: Long, ctx: Long): Int =
        fk_codecpar_from_context(par.asPointer(), ctx.asPointer())

    public actual fun codecparToContext(ctx: Long, par: Long): Int =
        fk_codecpar_to_context(ctx.asPointer(), par.asPointer())

    public actual fun codecparCopy(dst: Long, src: Long): Int =
        fk_codecpar_copy(dst.asPointer(), src.asPointer())

    public actual fun codecparGetInt(par: Long, field: Int): Int =
        fk_codecpar_get_int(par.asPointer(), field)

    public actual fun codecparSetInt(par: Long, field: Int, value: Int): Int =
        fk_codecpar_set_int(par.asPointer(), field, value)

    public actual fun codecparGetInt64(par: Long, field: Int): Long =
        fk_codecpar_get_int64(par.asPointer(), field)

    public actual fun codecparSetInt64(par: Long, field: Int, value: Long): Int =
        fk_codecpar_set_int64(par.asPointer(), field, value)

    public actual fun codecparGetRational(par: Long, field: Int): Long = memScoped {
        val num = alloc<IntVar>()
        val den = alloc<IntVar>()
        if (fk_codecpar_get_rational(par.asPointer(), field, num.ptr, den.ptr) < 0) {
            0L
        } else {
            packRational(num.value, den.value)
        }
    }

    public actual fun codecparSetRational(par: Long, field: Int, num: Int, den: Int): Int =
        fk_codecpar_set_rational(par.asPointer(), field, num, den)

    public actual fun codecparSetExtradata(par: Long, data: ByteArray): Int =
        data.withPinnedBytes { bytes, size ->
            fk_codecpar_set_extradata(par.asPointer(), bytes, size)
        }

    public actual fun codecparExtradataSize(par: Long): Int =
        fk_codecpar_get_extradata_size(par.asPointer())

    public actual fun codecparCopyExtradata(par: Long, out: ByteArray): Int =
        out.withPinnedBytes { bytes, size ->
            fk_codecpar_copy_extradata(par.asPointer(), bytes, size)
        }

    public actual fun formatFindInputFormat(name: String): Long =
        fk_format_find_input_format(name).asHandle()

    public actual fun formatFindOutputFormat(name: String): Long =
        fk_format_find_output_format(name).asHandle()

    public actual fun inputFormatName(format: Long): String? =
        fk_input_format_get_name(format.asPointer())?.toKString()

    public actual fun outputFormatName(format: Long): String? =
        fk_output_format_get_name(format.asPointer())?.toKString()

    public actual fun inputOpen(url: String, dict: Long): Long =
        fk_input_open(url, dict.asPointer()).asHandle()

    public actual fun inputOpenFormat(url: String, formatName: String, dict: Long): Long =
        fk_input_open_format(url, formatName, dict.asPointer()).asHandle()

    public actual fun inputFindStreamInfo(ctx: Long, dict: Long): Int =
        fk_input_find_stream_info(ctx.asPointer(), dict.asPointer())

    public actual fun inputClose(ctx: Long): Unit = fk_input_close(ctx.asPointer())
    public actual fun inputReadFrame(ctx: Long, packet: Long): Int =
        fk_input_read_frame(ctx.asPointer(), packet.asPointer())

    public actual fun inputSeek(ctx: Long, streamIndex: Int, timestamp: Long, flags: Int): Int =
        fk_input_seek(ctx.asPointer(), streamIndex, timestamp, flags)

    public actual fun formatNbStreams(ctx: Long): Int = fk_format_get_nb_streams(ctx.asPointer())
    public actual fun formatGetStream(ctx: Long, index: Int): Long =
        fk_format_get_stream(ctx.asPointer(), index).asHandle()

    public actual fun formatStartTime(ctx: Long): Long = fk_format_get_start_time(ctx.asPointer())
    public actual fun formatDuration(ctx: Long): Long = fk_format_get_duration(ctx.asPointer())
    public actual fun formatName(ctx: Long): String? =
        fk_format_get_format_name(ctx.asPointer())?.toKString()

    public actual fun outputOpen(formatName: String, filename: String): Long =
        fk_output_open(formatName, filename).asHandle()

    public actual fun outputWriteHeader(ctx: Long, dict: Long): Int =
        fk_output_write_header(ctx.asPointer(), dict.asPointer())

    public actual fun outputWritePacket(ctx: Long, packet: Long): Int =
        fk_output_write_packet(ctx.asPointer(), packet.asPointer())

    public actual fun outputWriteTrailer(ctx: Long): Int =
        fk_output_write_trailer(ctx.asPointer())

    public actual fun outputClose(ctx: Long): Unit = fk_output_close(ctx.asPointer())

    public actual fun formatNewStream(ctx: Long, codec: Long): Long =
        fk_format_new_stream(ctx.asPointer(), codec.asPointer()).asHandle()

    public actual fun streamIndex(stream: Long): Int = fk_stream_get_index(stream.asPointer())
    public actual fun streamTimeBase(stream: Long): Long = memScoped {
        val num = alloc<IntVar>()
        val den = alloc<IntVar>()
        if (fk_stream_get_time_base(stream.asPointer(), num.ptr, den.ptr) < 0) {
            0L
        } else {
            packRational(num.value, den.value)
        }
    }

    public actual fun streamSetTimeBase(stream: Long, num: Int, den: Int): Int =
        fk_stream_set_time_base(stream.asPointer(), num, den)

    public actual fun streamDuration(stream: Long): Long =
        fk_stream_get_duration(stream.asPointer())

    public actual fun streamAvgFrameRate(stream: Long): Long = memScoped {
        val num = alloc<IntVar>()
        val den = alloc<IntVar>()
        if (fk_stream_get_avg_frame_rate(stream.asPointer(), num.ptr, den.ptr) < 0) {
            0L
        } else {
            packRational(num.value, den.value)
        }
    }

    public actual fun streamCodecpar(stream: Long): Long =
        fk_stream_get_codecpar(stream.asPointer()).asHandle()

    public actual fun streamSetCodecpar(stream: Long, par: Long): Int =
        fk_stream_set_codecpar(stream.asPointer(), par.asPointer())

    public actual fun graphAlloc(): Long = fk_graph_alloc().asHandle()
    public actual fun graphFree(graph: Long): Unit = fk_graph_free(graph.asPointer())

    public actual fun graphCreateFilter(
        graph: Long,
        name: String,
        instanceName: String,
        args: String?,
    ): Long = fk_graph_create_filter(graph.asPointer(), name, instanceName, args).asHandle()

    public actual fun graphLink(src: Long, srcPad: Int, dst: Long, dstPad: Int): Int =
        fk_graph_link(src.asPointer(), srcPad, dst.asPointer(), dstPad)

    public actual fun graphConfig(graph: Long): Int = fk_graph_config(graph.asPointer())
    public actual fun buffersrcAddFrame(src: Long, frame: Long): Int =
        fk_buffersrc_add_frame(src.asPointer(), frame.asPointer())

    public actual fun buffersinkGetFrame(sink: Long, frame: Long): Int =
        fk_buffersink_get_frame(sink.asPointer(), frame.asPointer())

    public actual fun swsGetContext(
        srcWidth: Int,
        srcHeight: Int,
        srcFormat: Int,
        dstWidth: Int,
        dstHeight: Int,
        dstFormat: Int,
        flags: Int,
    ): Long = fk_sws_get_context(
        srcWidth,
        srcHeight,
        srcFormat,
        dstWidth,
        dstHeight,
        dstFormat,
        flags,
    ).asHandle()

    public actual fun swsFreeContext(ctx: Long): Unit = fk_sws_free_context(ctx.asPointer())
    public actual fun swsScaleFrame(ctx: Long, dst: Long, src: Long): Int =
        fk_sws_scale_frame(ctx.asPointer(), dst.asPointer(), src.asPointer())

    public actual fun swrAllocSetOpts(
        outSampleRate: Int,
        outFormat: Int,
        outChannels: Int,
        inSampleRate: Int,
        inFormat: Int,
        inChannels: Int,
    ): Long = fk_swr_alloc_set_opts(
        outSampleRate,
        outFormat,
        outChannels,
        inSampleRate,
        inFormat,
        inChannels,
    ).asHandle()

    public actual fun swrInit(ctx: Long): Int = fk_swr_init(ctx.asPointer())
    public actual fun swrConvertFrame(ctx: Long, out: Long, input: Long): Int =
        fk_swr_convert_frame(ctx.asPointer(), out.asPointer(), input.asPointer())

    public actual fun swrOutSamples(ctx: Long, inSamples: Int): Int =
        fk_swr_get_out_samples(ctx.asPointer(), inSamples)

    public actual fun swrFree(ctx: Long): Unit = fk_swr_free(ctx.asPointer())

    public actual fun deviceRegisterAll(): Unit = fk_device_register_all()
}
