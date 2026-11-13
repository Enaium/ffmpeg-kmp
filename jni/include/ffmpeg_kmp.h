/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

/*
 * Stable C ABI between FFmpeg and the Kotlin Multiplatform bindings.
 *
 * FFmpeg's public structs are not ABI-stable across releases and several APIs
 * use pointer-to-pointer out parameters that cinterop and JNI cannot express
 * ergonomically. Every object is therefore an opaque pointer owned by this
 * shim; accessors expose the handful of fields the Kotlin API needs. Handles
 * are plain pointers: on JVM they travel in a jlong, on Kotlin/Native they are
 * converted back through interpretCPointer().
 *
 * Strings returned by the shim are either static (version, format names) or a
 * per-thread buffer (error strings), and are copied by the caller's language
 * binding before the next call.
 */
#ifndef FFMPEG_KMP_H
#define FFMPEG_KMP_H

#include <stdint.h>

/*
 * 64-bit values cross the WebAssembly boundary as doubles: JavaScript has no
 * native 64-bit integer argument type, and timestamps/bitrates stay far below
 * 2^53. Native and JNI builds keep the exact int64_t representation.
 */
#ifdef __EMSCRIPTEN__
typedef double fk_i64;
#else
typedef int64_t fk_i64;
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* ==================== Versions and build information ==================== */

const char *fk_version_info(void);
const char *fk_configuration(void);
const char *fk_license(void);

/* Error code of the last call that can fail without returning one. */
int fk_last_error(void);

unsigned fk_avutil_version(void);
unsigned fk_avcodec_version(void);
unsigned fk_avformat_version(void);
unsigned fk_avfilter_version(void);
unsigned fk_swscale_version(void);
unsigned fk_swresample_version(void);
unsigned fk_avdevice_version(void);

/* ==================== libavutil ==================== */

int fk_log_level(void);
void fk_log_set_level(int level);
void fk_log_message(int level, const char *message);

int fk_error_eagain(void);
int fk_error_eof(void);
const char *fk_error_string(int error);

const char *fk_pix_fmt_name(int format);
int fk_pix_fmt_get(const char *name);
const char *fk_sample_fmt_name(int format);
int fk_sample_fmt_get(const char *name);
int fk_sample_fmt_bytes(int format);

void *fk_dict_create(void);
void fk_dict_free(void *dict);
int fk_dict_set(void *dict, const char *key, const char *value);
int fk_dict_set_int(void *dict, const char *key, fk_i64 value);
const char *fk_dict_get(void *dict, const char *key);
fk_i64 fk_dict_get_int(void *dict, const char *key, fk_i64 fallback);
int fk_dict_count(void *dict);

void *fk_frame_alloc(void);
void fk_frame_free(void *frame);
void fk_frame_unref(void *frame);
int fk_frame_ref(void *dst, void *src);
int fk_frame_get_buffer(void *frame, int align);
int fk_frame_make_writable(void *frame);

int fk_frame_set_video(void *frame, int width, int height, int format);
int fk_frame_set_audio(void *frame, int sample_rate, int format, int nb_samples, int channels);

int fk_frame_get_width(void *frame);
int fk_frame_get_height(void *frame);
int fk_frame_get_format(void *frame);
fk_i64 fk_frame_get_pts(void *frame);
void fk_frame_set_pts(void *frame, fk_i64 pts);
fk_i64 fk_frame_get_pkt_dts(void *frame);
fk_i64 fk_frame_get_duration(void *frame);
int fk_frame_get_nb_samples(void *frame);
int fk_frame_get_sample_rate(void *frame);
int fk_frame_get_channels(void *frame);
int fk_frame_is_key(void *frame);
int fk_frame_get_sample_aspect_ratio_num(void *frame);
int fk_frame_get_sample_aspect_ratio_den(void *frame);
void fk_frame_set_sample_aspect_ratio(void *frame, int num, int den);
int fk_frame_get_linesize(void *frame, int plane);
int fk_frame_get_plane_size(void *frame, int plane);
int fk_frame_copy_plane(void *frame, int plane, uint8_t *out, int out_size);
int fk_frame_write_plane(void *frame, int plane, const uint8_t *data, int data_size);

/* ==================== libavcodec ==================== */

/* FK_CODEC_CONTEXT_FIELD_* in the Kotlin API. */
int fk_avctx_get_int(void *ctx, int field);
int fk_avctx_set_int(void *ctx, int field, int value);
fk_i64 fk_avctx_get_int64(void *ctx, int field);
int fk_avctx_set_int64(void *ctx, int field, fk_i64 value);
int fk_avctx_get_rational(void *ctx, int field, int *num, int *den);
int fk_avctx_set_rational(void *ctx, int field, int num, int den);
int fk_avctx_set_extradata(void *ctx, const uint8_t *data, int size);
int fk_avctx_get_extradata_size(void *ctx);
int fk_avctx_copy_extradata(void *ctx, uint8_t *out, int out_size);

void *fk_find_decoder_by_name(const char *name);
void *fk_find_encoder_by_name(const char *name);
void *fk_find_decoder(int id);
void *fk_find_encoder(int id);
const char *fk_codec_get_name(void *codec);
int fk_codec_get_id(void *codec);
int fk_codec_get_type(void *codec);
int fk_codec_is_decoder(void *codec);
int fk_codec_is_encoder(void *codec);
int fk_codec_id_from_name(const char *name);

void *fk_avctx_alloc(void *codec);
void fk_avctx_free(void *ctx);
int fk_avctx_open2(void *ctx, void *codec, void *dict);
int fk_avctx_send_packet(void *ctx, void *packet);
int fk_avctx_receive_frame(void *ctx, void *frame);
int fk_avctx_send_frame(void *ctx, void *frame);
int fk_avctx_receive_packet(void *ctx, void *packet);
void fk_avctx_flush(void *ctx);

void *fk_packet_alloc(void);
void fk_packet_free(void *packet);
void fk_packet_unref(void *packet);
int fk_packet_alloc_data(void *packet, int size);
int fk_packet_set_data(void *packet, const uint8_t *data, int size);
int fk_packet_get_size(void *packet);
int fk_packet_copy_data(void *packet, uint8_t *out, int out_size);
fk_i64 fk_packet_get_int64(void *packet, int field);
void fk_packet_set_int64(void *packet, int field, fk_i64 value);
void fk_packet_rescale_ts(void *packet, int src_num, int src_den, int dst_num, int dst_den);

void *fk_codecpar_alloc(void);
void fk_codecpar_free(void *par);
int fk_codecpar_from_context(void *par, void *ctx);
int fk_codecpar_to_context(void *ctx, void *par);
int fk_codecpar_copy(void *dst, void *src);

/* FK_CODEC_PARAMETERS_FIELD_* in the Kotlin API. */
int fk_codecpar_get_int(void *par, int field);
int fk_codecpar_set_int(void *par, int field, int value);
fk_i64 fk_codecpar_get_int64(void *par, int field);
int fk_codecpar_set_int64(void *par, int field, fk_i64 value);
int fk_codecpar_get_rational(void *par, int field, int *num, int *den);
int fk_codecpar_set_rational(void *par, int field, int num, int den);
int fk_codecpar_set_extradata(void *par, const uint8_t *data, int size);
int fk_codecpar_get_extradata_size(void *par);
int fk_codecpar_copy_extradata(void *par, uint8_t *out, int out_size);

/* ==================== libavformat ==================== */

void *fk_format_find_input_format(const char *name);
void *fk_format_find_output_format(const char *name);
const char *fk_input_format_get_name(void *format);
const char *fk_output_format_get_name(void *format);

void *fk_input_open(const char *url, void *dict);
void *fk_input_open_format(const char *url, const char *format_name, void *dict);
int fk_input_find_stream_info(void *ctx, void *dict);
void fk_input_close(void *ctx);
int fk_input_read_frame(void *ctx, void *packet);
int fk_input_seek(void *ctx, int stream_index, fk_i64 timestamp, int flags);

int fk_format_get_nb_streams(void *ctx);
void *fk_format_get_stream(void *ctx, int index);
fk_i64 fk_format_get_start_time(void *ctx);
fk_i64 fk_format_get_duration(void *ctx);
const char *fk_format_get_format_name(void *ctx);

void *fk_output_open(const char *format_name, const char *filename);
int fk_output_write_header(void *ctx, void *dict);
int fk_output_write_packet(void *ctx, void *packet);
int fk_output_write_trailer(void *ctx);
void fk_output_close(void *ctx);

void *fk_format_new_stream(void *ctx, void *codec);
int fk_stream_get_index(void *stream);
int fk_stream_get_time_base(void *stream, int *num, int *den);
int fk_stream_set_time_base(void *stream, int num, int den);
fk_i64 fk_stream_get_duration(void *stream);
int fk_stream_get_avg_frame_rate(void *stream, int *num, int *den);
void *fk_stream_get_codecpar(void *stream);
int fk_stream_set_codecpar(void *stream, void *par);

/* ==================== libavfilter ==================== */

void *fk_graph_alloc(void);
void fk_graph_free(void *graph);
void *fk_graph_create_filter(void *graph, const char *name, const char *instance_name,
                             const char *args);
int fk_graph_link(void *src, int src_pad, void *dst, int dst_pad);
int fk_graph_config(void *graph);
int fk_buffersrc_add_frame(void *src, void *frame);
int fk_buffersink_get_frame(void *sink, void *frame);

/* ==================== libswscale ==================== */

void *fk_sws_get_context(int src_width, int src_height, int src_format,
                         int dst_width, int dst_height, int dst_format, int flags);
void fk_sws_free_context(void *ctx);
int fk_sws_scale_frame(void *ctx, void *dst, void *src);

/* ==================== libswresample ==================== */

void *fk_swr_alloc_set_opts(int out_sample_rate, int out_format, int out_channels,
                            int in_sample_rate, int in_format, int in_channels);
int fk_swr_init(void *ctx);
int fk_swr_convert_frame(void *ctx, void *out, void *in);
int fk_swr_get_out_samples(void *ctx, int in_samples);
void fk_swr_free(void *ctx);

/* ==================== libavdevice ==================== */

void fk_device_register_all(void);

#ifdef __cplusplus
}
#endif

#endif /* FFMPEG_KMP_H */
