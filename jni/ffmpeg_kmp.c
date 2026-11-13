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

#include "ffmpeg_kmp.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <libavcodec/avcodec.h>
#include <libavdevice/avdevice.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavformat/avformat.h>
#include <libavutil/channel_layout.h>
#include <libavutil/dict.h>
#include <libavutil/error.h>
#include <libavutil/frame.h>
#include <libavutil/imgutils.h>
#include <libavutil/log.h>
#include <libavutil/mem.h>
#include <libavutil/pixdesc.h>
#include <libavutil/samplefmt.h>
#include <libswresample/swresample.h>
#include <libswscale/swscale.h>

typedef struct fk_dict {
    AVDictionary *value;
} fk_dict;

/* FK_CODEC_CONTEXT_FIELD_* */
enum {
    FK_CTX_WIDTH = 0,
    FK_CTX_HEIGHT = 1,
    FK_CTX_FORMAT = 2,
    FK_CTX_SAMPLE_RATE = 3,
    FK_CTX_CHANNELS = 4,
    FK_CTX_PROFILE = 5,
    FK_CTX_LEVEL = 6,
    FK_CTX_FRAME_SIZE = 7,
    FK_CTX_THREAD_COUNT = 8,
    FK_CTX_FLAGS = 9,
    FK_CTX_FLAGS2 = 10,
    FK_CTX_GOP_SIZE = 11,
    FK_CTX_MAX_B_FRAMES = 12,
    FK_CTX_SAMPLE_FORMAT = 13,
};

/* FK_CODEC_CONTEXT_INT64_FIELD_* */
enum {
    FK_CTX_BIT_RATE = 0,
};

/* FK_CODEC_CONTEXT_RATIONAL_FIELD_* */
enum {
    FK_CTX_TIME_BASE = 0,
    FK_CTX_FRAMERATE = 1,
    FK_CTX_SAMPLE_ASPECT_RATIO = 2,
};

/* FK_CODEC_PARAMETERS_FIELD_* */
enum {
    FK_PAR_WIDTH = 0,
    FK_PAR_HEIGHT = 1,
    FK_PAR_FORMAT = 2,
    FK_PAR_SAMPLE_RATE = 3,
    FK_PAR_CHANNELS = 4,
    FK_PAR_PROFILE = 5,
    FK_PAR_LEVEL = 6,
    FK_PAR_FRAME_SIZE = 7,
    FK_PAR_CODEC_ID = 8,
    FK_PAR_CODEC_TYPE = 9,
    FK_PAR_CODEC_TAG = 10,
};

/* FK_CODEC_PARAMETERS_INT64_FIELD_* */
enum {
    FK_PAR_BIT_RATE = 0,
};

/* FK_CODEC_PARAMETERS_RATIONAL_FIELD_* */
enum {
    FK_PAR_SAMPLE_ASPECT_RATIO = 0,
};

/* FK_PACKET_FIELD_* */
enum {
    FK_PKT_PTS = 0,
    FK_PKT_DTS = 1,
    FK_PKT_DURATION = 2,
    FK_PKT_STREAM_INDEX = 3,
    FK_PKT_FLAGS = 4,
};

static _Thread_local int fk_last_error_value = 0;

static void fk_set_last_error(int error) { fk_last_error_value = error; }

static AVDictionary **fk_dict_options(void *dict) {
    return dict ? &((fk_dict *)dict)->value : NULL;
}

/* ==================== Versions ==================== */

const char *fk_version_info(void) { return av_version_info(); }
const char *fk_configuration(void) { return avutil_configuration(); }
const char *fk_license(void) { return avutil_license(); }

int fk_last_error(void) { return fk_last_error_value; }

unsigned fk_avutil_version(void) { return avutil_version(); }
unsigned fk_avcodec_version(void) { return avcodec_version(); }
unsigned fk_avformat_version(void) { return avformat_version(); }
unsigned fk_avfilter_version(void) { return avfilter_version(); }
unsigned fk_swscale_version(void) { return swscale_version(); }
unsigned fk_swresample_version(void) { return swresample_version(); }
unsigned fk_avdevice_version(void) { return avdevice_version(); }

/* ==================== libavutil ==================== */

int fk_log_level(void) { return av_log_get_level(); }
void fk_log_set_level(int level) { av_log_set_level(level); }
void fk_log_message(int level, const char *message) {
    av_log(NULL, level, "%s", message ? message : "");
}

int fk_error_eagain(void) { return AVERROR(EAGAIN); }
int fk_error_eof(void) { return AVERROR_EOF; }

const char *fk_error_string(int error) {
    static _Thread_local char buffer[AV_ERROR_MAX_STRING_SIZE];
    if (av_strerror(error, buffer, sizeof(buffer)) < 0) {
        snprintf(buffer, sizeof(buffer), "Unknown error %d", error);
    }
    return buffer;
}

const char *fk_pix_fmt_name(int format) { return av_get_pix_fmt_name(format); }
int fk_pix_fmt_get(const char *name) { return name ? av_get_pix_fmt(name) : AV_PIX_FMT_NONE; }
const char *fk_sample_fmt_name(int format) { return av_get_sample_fmt_name(format); }
int fk_sample_fmt_get(const char *name) {
    return name ? av_get_sample_fmt(name) : AV_SAMPLE_FMT_NONE;
}
int fk_sample_fmt_bytes(int format) { return av_get_bytes_per_sample(format); }

void *fk_dict_create(void) {
    return av_mallocz(sizeof(fk_dict));
}

void fk_dict_free(void *dict) {
    fk_dict *d = dict;
    if (!d) return;
    av_dict_free(&d->value);
    av_free(d);
}

int fk_dict_set(void *dict, const char *key, const char *value) {
    if (!dict || !key) return AVERROR(EINVAL);
    return av_dict_set(fk_dict_options(dict), key, value, 0);
}

int fk_dict_set_int(void *dict, const char *key, fk_i64 value) {
    if (!dict || !key) return AVERROR(EINVAL);
    return av_dict_set_int(fk_dict_options(dict), key, value, 0);
}

const char *fk_dict_get(void *dict, const char *key) {
    if (!dict || !key) return NULL;
    AVDictionaryEntry *entry = av_dict_get(fk_dict_options(dict)[0], key, NULL, 0);
    return entry ? entry->value : NULL;
}

fk_i64 fk_dict_get_int(void *dict, const char *key, fk_i64 fallback) {
    const char *value = fk_dict_get(dict, key);
    if (!value || !*value) return fallback;
    char *end = NULL;
    long long parsed = strtoll(value, &end, 10);
    return end == value ? fallback : (int64_t)parsed;
}

int fk_dict_count(void *dict) {
    return dict ? av_dict_count(fk_dict_options(dict)[0]) : 0;
}

void *fk_frame_alloc(void) { return av_frame_alloc(); }

void fk_frame_free(void *frame) {
    AVFrame *f = frame;
    if (!f) return;
    av_frame_free(&f);
}

void fk_frame_unref(void *frame) {
    if (frame) av_frame_unref(frame);
}

int fk_frame_ref(void *dst, void *src) {
    if (!dst || !src) return AVERROR(EINVAL);
    return av_frame_ref(dst, src);
}

int fk_frame_get_buffer(void *frame, int align) {
    if (!frame) return AVERROR(EINVAL);
    return av_frame_get_buffer(frame, align);
}

int fk_frame_make_writable(void *frame) {
    if (!frame) return AVERROR(EINVAL);
    return av_frame_make_writable(frame);
}

int fk_frame_set_video(void *frame, int width, int height, int format) {
    AVFrame *f = frame;
    if (!f || width <= 0 || height <= 0) return AVERROR(EINVAL);
    f->width = width;
    f->height = height;
    f->format = format;
    return 0;
}

int fk_frame_set_audio(void *frame, int sample_rate, int format, int nb_samples, int channels) {
    AVFrame *f = frame;
    if (!f || sample_rate <= 0 || nb_samples < 0 || channels <= 0) return AVERROR(EINVAL);
    f->sample_rate = sample_rate;
    f->format = format;
    f->nb_samples = nb_samples;
    av_channel_layout_default(&f->ch_layout, channels);
    return 0;
}

int fk_frame_get_width(void *frame) { return frame ? ((AVFrame *)frame)->width : 0; }
int fk_frame_get_height(void *frame) { return frame ? ((AVFrame *)frame)->height : 0; }
int fk_frame_get_format(void *frame) { return frame ? ((AVFrame *)frame)->format : -1; }
fk_i64 fk_frame_get_pts(void *frame) { return frame ? ((AVFrame *)frame)->pts : 0; }
void fk_frame_set_pts(void *frame, fk_i64 pts) {
    if (frame) ((AVFrame *)frame)->pts = pts;
}
fk_i64 fk_frame_get_pkt_dts(void *frame) { return frame ? ((AVFrame *)frame)->pkt_dts : 0; }
fk_i64 fk_frame_get_duration(void *frame) { return frame ? ((AVFrame *)frame)->duration : 0; }
int fk_frame_get_nb_samples(void *frame) { return frame ? ((AVFrame *)frame)->nb_samples : 0; }
int fk_frame_get_sample_rate(void *frame) { return frame ? ((AVFrame *)frame)->sample_rate : 0; }
int fk_frame_get_channels(void *frame) {
    return frame ? ((AVFrame *)frame)->ch_layout.nb_channels : 0;
}
int fk_frame_is_key(void *frame) {
    return frame && (((AVFrame *)frame)->flags & AV_FRAME_FLAG_KEY);
}
int fk_frame_get_sample_aspect_ratio_num(void *frame) {
    return frame ? ((AVFrame *)frame)->sample_aspect_ratio.num : 0;
}
int fk_frame_get_sample_aspect_ratio_den(void *frame) {
    return frame ? ((AVFrame *)frame)->sample_aspect_ratio.den : 0;
}
void fk_frame_set_sample_aspect_ratio(void *frame, int num, int den) {
    if (!frame) return;
    ((AVFrame *)frame)->sample_aspect_ratio.num = num;
    ((AVFrame *)frame)->sample_aspect_ratio.den = den;
}

int fk_frame_get_linesize(void *frame, int plane) {
    if (!frame || plane < 0 || plane >= AV_NUM_DATA_POINTERS) return 0;
    return ((AVFrame *)frame)->linesize[plane];
}

int fk_frame_get_plane_size(void *frame, int plane) {
    AVFrame *f = frame;
    if (!f || plane < 0 || plane >= AV_NUM_DATA_POINTERS) return 0;

    if (f->nb_samples > 0 && f->width <= 0) {
        int bytes = av_get_bytes_per_sample(f->format);
        int channels = f->ch_layout.nb_channels;
        if (bytes <= 0 || channels <= 0) return 0;
        if (av_sample_fmt_is_planar(f->format)) {
            return f->linesize[0] > 0 ? f->linesize[0] / channels : f->nb_samples * bytes;
        }
        return f->linesize[0] > 0 ? f->linesize[0] : f->nb_samples * bytes * channels;
    }

    const AVPixFmtDescriptor *desc = av_pix_fmt_desc_get(f->format);
    if (!desc || f->width <= 0 || f->height <= 0) return 0;
    int height = f->height;
    if (plane == 1 || plane == 2) {
        height = AV_CEIL_RSHIFT(height, desc->log2_chroma_h);
    }
    if (f->linesize[plane] <= 0) return 0;
    return f->linesize[plane] * height;
}

int fk_frame_copy_plane(void *frame, int plane, uint8_t *out, int out_size) {
    AVFrame *f = frame;
    if (!f || !out || out_size < 0 || plane < 0 || plane >= AV_NUM_DATA_POINTERS) {
        return AVERROR(EINVAL);
    }
    const uint8_t *src = f->extended_data ? f->extended_data[plane] : f->data[plane];
    if (!src) return 0;
    int size = fk_frame_get_plane_size(f, plane);
    if (size > out_size) size = out_size;
    if (size > 0) memcpy(out, src, (size_t)size);
    return size;
}

int fk_frame_write_plane(void *frame, int plane, const uint8_t *data, int data_size) {
    AVFrame *f = frame;
    if (!f || !data || data_size < 0 || plane < 0 || plane >= AV_NUM_DATA_POINTERS) {
        return AVERROR(EINVAL);
    }
    uint8_t *dst = f->extended_data ? f->extended_data[plane] : f->data[plane];
    if (!dst) return AVERROR(EINVAL);
    int size = fk_frame_get_plane_size(f, plane);
    if (data_size < size) size = data_size;
    if (size > 0) memcpy(dst, data, (size_t)size);
    return size;
}

/* ==================== libavcodec ==================== */

int fk_avctx_get_int(void *ctx, int field) {
    AVCodecContext *c = ctx;
    if (!c) return AVERROR(EINVAL);
    switch (field) {
        case FK_CTX_WIDTH: return c->width;
        case FK_CTX_HEIGHT: return c->height;
        case FK_CTX_FORMAT: return c->pix_fmt;
        case FK_CTX_SAMPLE_FORMAT: return c->sample_fmt;
        case FK_CTX_SAMPLE_RATE: return c->sample_rate;
        case FK_CTX_CHANNELS: return c->ch_layout.nb_channels;
        case FK_CTX_PROFILE: return c->profile;
        case FK_CTX_LEVEL: return c->level;
        case FK_CTX_FRAME_SIZE: return c->frame_size;
        case FK_CTX_THREAD_COUNT: return c->thread_count;
        case FK_CTX_FLAGS: return c->flags;
        case FK_CTX_FLAGS2: return c->flags2;
        case FK_CTX_GOP_SIZE: return c->gop_size;
        case FK_CTX_MAX_B_FRAMES: return c->max_b_frames;
        default: return AVERROR(EINVAL);
    }
}

int fk_avctx_set_int(void *ctx, int field, int value) {
    AVCodecContext *c = ctx;
    if (!c) return AVERROR(EINVAL);
    switch (field) {
        case FK_CTX_WIDTH: c->width = value; return 0;
        case FK_CTX_HEIGHT: c->height = value; return 0;
        case FK_CTX_FORMAT: c->pix_fmt = (enum AVPixelFormat)value; return 0;
        case FK_CTX_SAMPLE_FORMAT: c->sample_fmt = (enum AVSampleFormat)value; return 0;
        case FK_CTX_SAMPLE_RATE: c->sample_rate = value; return 0;
        case FK_CTX_CHANNELS:
            av_channel_layout_default(&c->ch_layout, value);
            return 0;
        case FK_CTX_PROFILE: c->profile = value; return 0;
        case FK_CTX_LEVEL: c->level = value; return 0;
        case FK_CTX_FRAME_SIZE: c->frame_size = value; return 0;
        case FK_CTX_THREAD_COUNT: c->thread_count = value; return 0;
        case FK_CTX_FLAGS: c->flags = value; return 0;
        case FK_CTX_FLAGS2: c->flags2 = value; return 0;
        case FK_CTX_GOP_SIZE: c->gop_size = value; return 0;
        case FK_CTX_MAX_B_FRAMES: c->max_b_frames = value; return 0;
        default: return AVERROR(EINVAL);
    }
}

fk_i64 fk_avctx_get_int64(void *ctx, int field) {
    AVCodecContext *c = ctx;
    if (!c) return AVERROR(EINVAL);
    switch (field) {
        case FK_CTX_BIT_RATE: return c->bit_rate;
        default: return AVERROR(EINVAL);
    }
}

int fk_avctx_set_int64(void *ctx, int field, fk_i64 value) {
    AVCodecContext *c = ctx;
    if (!c) return AVERROR(EINVAL);
    switch (field) {
        case FK_CTX_BIT_RATE: c->bit_rate = value; return 0;
        default: return AVERROR(EINVAL);
    }
}

int fk_avctx_get_rational(void *ctx, int field, int *num, int *den) {
    AVCodecContext *c = ctx;
    if (!c || !num || !den) return AVERROR(EINVAL);
    AVRational value;
    switch (field) {
        case FK_CTX_TIME_BASE: value = c->time_base; break;
        case FK_CTX_FRAMERATE: value = c->framerate; break;
        case FK_CTX_SAMPLE_ASPECT_RATIO: value = c->sample_aspect_ratio; break;
        default: return AVERROR(EINVAL);
    }
    *num = value.num;
    *den = value.den;
    return 0;
}

int fk_avctx_set_rational(void *ctx, int field, int num, int den) {
    AVCodecContext *c = ctx;
    if (!c) return AVERROR(EINVAL);
    switch (field) {
        case FK_CTX_TIME_BASE: c->time_base = (AVRational){num, den}; return 0;
        case FK_CTX_FRAMERATE: c->framerate = (AVRational){num, den}; return 0;
        case FK_CTX_SAMPLE_ASPECT_RATIO: c->sample_aspect_ratio = (AVRational){num, den}; return 0;
        default: return AVERROR(EINVAL);
    }
}

int fk_avctx_set_extradata(void *ctx, const uint8_t *data, int size) {
    AVCodecContext *c = ctx;
    if (!c || size < 0) return AVERROR(EINVAL);
    av_freep(&c->extradata);
    c->extradata_size = 0;
    if (!data || size == 0) return 0;
    c->extradata = av_mallocz((size_t)size + AV_INPUT_BUFFER_PADDING_SIZE);
    if (!c->extradata) return AVERROR(ENOMEM);
    memcpy(c->extradata, data, (size_t)size);
    c->extradata_size = size;
    return 0;
}

int fk_avctx_get_extradata_size(void *ctx) {
    return ctx ? ((AVCodecContext *)ctx)->extradata_size : 0;
}

int fk_avctx_copy_extradata(void *ctx, uint8_t *out, int out_size) {
    AVCodecContext *c = ctx;
    if (!c || !out || out_size < 0) return AVERROR(EINVAL);
    int size = c->extradata_size;
    if (size > out_size) size = out_size;
    if (size > 0 && c->extradata) memcpy(out, c->extradata, (size_t)size);
    return size;
}

void *fk_find_decoder_by_name(const char *name) {
    return name ? (void *)avcodec_find_decoder_by_name(name) : NULL;
}
void *fk_find_encoder_by_name(const char *name) {
    return name ? (void *)avcodec_find_encoder_by_name(name) : NULL;
}
void *fk_find_decoder(int id) { return (void *)avcodec_find_decoder((enum AVCodecID)id); }
void *fk_find_encoder(int id) { return (void *)avcodec_find_encoder((enum AVCodecID)id); }
const char *fk_codec_get_name(void *codec) {
    return codec ? ((AVCodec *)codec)->name : NULL;
}
int fk_codec_get_id(void *codec) { return codec ? ((AVCodec *)codec)->id : AV_CODEC_ID_NONE; }
int fk_codec_get_type(void *codec) { return codec ? ((AVCodec *)codec)->type : AVMEDIA_TYPE_UNKNOWN; }
int fk_codec_is_decoder(void *codec) { return codec ? av_codec_is_decoder(codec) : 0; }
int fk_codec_is_encoder(void *codec) { return codec ? av_codec_is_encoder(codec) : 0; }
int fk_codec_id_from_name(const char *name) {
    if (!name) return AV_CODEC_ID_NONE;
    const AVCodecDescriptor *desc = avcodec_descriptor_get_by_name(name);
    return desc ? desc->id : AV_CODEC_ID_NONE;
}

void *fk_avctx_alloc(void *codec) {
    return avcodec_alloc_context3(codec);
}

void fk_avctx_free(void *ctx) {
    AVCodecContext *c = ctx;
    if (!c) return;
    avcodec_free_context(&c);
}

int fk_avctx_open2(void *ctx, void *codec, void *dict) {
    if (!ctx) return AVERROR(EINVAL);
    return avcodec_open2(ctx, codec, fk_dict_options(dict));
}

int fk_avctx_send_packet(void *ctx, void *packet) {
    if (!ctx) return AVERROR(EINVAL);
    return avcodec_send_packet(ctx, packet);
}

int fk_avctx_receive_frame(void *ctx, void *frame) {
    if (!ctx || !frame) return AVERROR(EINVAL);
    return avcodec_receive_frame(ctx, frame);
}

int fk_avctx_send_frame(void *ctx, void *frame) {
    if (!ctx) return AVERROR(EINVAL);
    return avcodec_send_frame(ctx, frame);
}

int fk_avctx_receive_packet(void *ctx, void *packet) {
    if (!ctx || !packet) return AVERROR(EINVAL);
    return avcodec_receive_packet(ctx, packet);
}

void fk_avctx_flush(void *ctx) {
    if (ctx) avcodec_flush_buffers(ctx);
}

void *fk_packet_alloc(void) { return av_packet_alloc(); }

void fk_packet_free(void *packet) {
    AVPacket *p = packet;
    if (!p) return;
    av_packet_free(&p);
}

void fk_packet_unref(void *packet) {
    if (packet) av_packet_unref(packet);
}

int fk_packet_alloc_data(void *packet, int size) {
    if (!packet || size < 0) return AVERROR(EINVAL);
    return av_new_packet(packet, size);
}

int fk_packet_set_data(void *packet, const uint8_t *data, int size) {
    AVPacket *p = packet;
    if (!p || !data || size < 0) return AVERROR(EINVAL);
    av_packet_unref(p);
    int ret = av_new_packet(p, size);
    if (ret < 0) return ret;
    memcpy(p->data, data, (size_t)size);
    return 0;
}

int fk_packet_get_size(void *packet) { return packet ? ((AVPacket *)packet)->size : 0; }

int fk_packet_copy_data(void *packet, uint8_t *out, int out_size) {
    AVPacket *p = packet;
    if (!p || !out || out_size < 0) return AVERROR(EINVAL);
    int size = p->size;
    if (size > out_size) size = out_size;
    if (size > 0 && p->data) memcpy(out, p->data, (size_t)size);
    return size;
}

fk_i64 fk_packet_get_int64(void *packet, int field) {
    AVPacket *p = packet;
    if (!p) return AVERROR(EINVAL);
    switch (field) {
        case FK_PKT_PTS: return p->pts;
        case FK_PKT_DTS: return p->dts;
        case FK_PKT_DURATION: return p->duration;
        case FK_PKT_STREAM_INDEX: return p->stream_index;
        case FK_PKT_FLAGS: return p->flags;
        default: return AVERROR(EINVAL);
    }
}

void fk_packet_set_int64(void *packet, int field, fk_i64 value) {
    AVPacket *p = packet;
    if (!p) return;
    switch (field) {
        case FK_PKT_PTS: p->pts = value; break;
        case FK_PKT_DTS: p->dts = value; break;
        case FK_PKT_DURATION: p->duration = value; break;
        case FK_PKT_STREAM_INDEX: p->stream_index = (int)value; break;
        case FK_PKT_FLAGS: p->flags = (int)value; break;
        default: break;
    }
}

void fk_packet_rescale_ts(void *packet, int src_num, int src_den, int dst_num, int dst_den) {
    if (!packet) return;
    av_packet_rescale_ts(packet, (AVRational){src_num, src_den}, (AVRational){dst_num, dst_den});
}

void *fk_codecpar_alloc(void) { return avcodec_parameters_alloc(); }

void fk_codecpar_free(void *par) {
    AVCodecParameters *p = par;
    if (!p) return;
    avcodec_parameters_free(&p);
}

int fk_codecpar_from_context(void *par, void *ctx) {
    if (!par || !ctx) return AVERROR(EINVAL);
    return avcodec_parameters_from_context(par, ctx);
}

int fk_codecpar_to_context(void *ctx, void *par) {
    if (!ctx || !par) return AVERROR(EINVAL);
    return avcodec_parameters_to_context(ctx, par);
}

int fk_codecpar_copy(void *dst, void *src) {
    if (!dst || !src) return AVERROR(EINVAL);
    return avcodec_parameters_copy(dst, src);
}

int fk_codecpar_get_int(void *par, int field) {
    AVCodecParameters *p = par;
    if (!p) return AVERROR(EINVAL);
    switch (field) {
        case FK_PAR_WIDTH: return p->width;
        case FK_PAR_HEIGHT: return p->height;
        case FK_PAR_FORMAT: return p->format;
        case FK_PAR_SAMPLE_RATE: return p->sample_rate;
        case FK_PAR_CHANNELS: return p->ch_layout.nb_channels;
        case FK_PAR_PROFILE: return p->profile;
        case FK_PAR_LEVEL: return p->level;
        case FK_PAR_FRAME_SIZE: return p->frame_size;
        case FK_PAR_CODEC_ID: return p->codec_id;
        case FK_PAR_CODEC_TYPE: return p->codec_type;
        case FK_PAR_CODEC_TAG: return (int)p->codec_tag;
        default: return AVERROR(EINVAL);
    }
}

int fk_codecpar_set_int(void *par, int field, int value) {
    AVCodecParameters *p = par;
    if (!p) return AVERROR(EINVAL);
    switch (field) {
        case FK_PAR_WIDTH: p->width = value; return 0;
        case FK_PAR_HEIGHT: p->height = value; return 0;
        case FK_PAR_FORMAT: p->format = value; return 0;
        case FK_PAR_SAMPLE_RATE: p->sample_rate = value; return 0;
        case FK_PAR_CHANNELS:
            av_channel_layout_default(&p->ch_layout, value);
            return 0;
        case FK_PAR_PROFILE: p->profile = value; return 0;
        case FK_PAR_LEVEL: p->level = value; return 0;
        case FK_PAR_FRAME_SIZE: p->frame_size = value; return 0;
        case FK_PAR_CODEC_ID: p->codec_id = (enum AVCodecID)value; return 0;
        case FK_PAR_CODEC_TYPE: p->codec_type = (enum AVMediaType)value; return 0;
        case FK_PAR_CODEC_TAG: p->codec_tag = (uint32_t)value; return 0;
        default: return AVERROR(EINVAL);
    }
}

fk_i64 fk_codecpar_get_int64(void *par, int field) {
    AVCodecParameters *p = par;
    if (!p) return AVERROR(EINVAL);
    switch (field) {
        case FK_PAR_BIT_RATE: return p->bit_rate;
        default: return AVERROR(EINVAL);
    }
}

int fk_codecpar_set_int64(void *par, int field, fk_i64 value) {
    AVCodecParameters *p = par;
    if (!p) return AVERROR(EINVAL);
    switch (field) {
        case FK_PAR_BIT_RATE: p->bit_rate = value; return 0;
        default: return AVERROR(EINVAL);
    }
}

int fk_codecpar_get_rational(void *par, int field, int *num, int *den) {
    AVCodecParameters *p = par;
    if (!p || !num || !den) return AVERROR(EINVAL);
    switch (field) {
        case FK_PAR_SAMPLE_ASPECT_RATIO:
            *num = p->sample_aspect_ratio.num;
            *den = p->sample_aspect_ratio.den;
            return 0;
        default: return AVERROR(EINVAL);
    }
}

int fk_codecpar_set_rational(void *par, int field, int num, int den) {
    AVCodecParameters *p = par;
    if (!p) return AVERROR(EINVAL);
    switch (field) {
        case FK_PAR_SAMPLE_ASPECT_RATIO:
            p->sample_aspect_ratio = (AVRational){num, den};
            return 0;
        default: return AVERROR(EINVAL);
    }
}

int fk_codecpar_set_extradata(void *par, const uint8_t *data, int size) {
    AVCodecParameters *p = par;
    if (!p || size < 0) return AVERROR(EINVAL);
    av_freep(&p->extradata);
    p->extradata_size = 0;
    if (!data || size == 0) return 0;
    p->extradata = av_mallocz((size_t)size + AV_INPUT_BUFFER_PADDING_SIZE);
    if (!p->extradata) return AVERROR(ENOMEM);
    memcpy(p->extradata, data, (size_t)size);
    p->extradata_size = size;
    return 0;
}

int fk_codecpar_get_extradata_size(void *par) {
    return par ? ((AVCodecParameters *)par)->extradata_size : 0;
}

int fk_codecpar_copy_extradata(void *par, uint8_t *out, int out_size) {
    AVCodecParameters *p = par;
    if (!p || !out || out_size < 0) return AVERROR(EINVAL);
    int size = p->extradata_size;
    if (size > out_size) size = out_size;
    if (size > 0 && p->extradata) memcpy(out, p->extradata, (size_t)size);
    return size;
}

/* ==================== libavformat ==================== */

void *fk_format_find_input_format(const char *name) {
    return name ? (void *)av_find_input_format(name) : NULL;
}

void *fk_format_find_output_format(const char *name) {
    return name ? (void *)av_guess_format(name, NULL, NULL) : NULL;
}

const char *fk_input_format_get_name(void *format) {
    return format ? ((AVInputFormat *)format)->name : NULL;
}

const char *fk_output_format_get_name(void *format) {
    return format ? ((AVOutputFormat *)format)->name : NULL;
}

void *fk_input_open(const char *url, void *dict) {
    return fk_input_open_format(url, NULL, dict);
}

void *fk_input_open_format(const char *url, const char *format_name, void *dict) {
    if (!url) return NULL;
    const AVInputFormat *format = format_name ? av_find_input_format(format_name) : NULL;
    if (format_name && !format) {
        fk_set_last_error(AVERROR(EINVAL));
        return NULL;
    }
    AVFormatContext *ctx = NULL;
    AVDictionary **options = fk_dict_options(dict);
    int ret = avformat_open_input(&ctx, url, format, options);
    if (ret < 0) {
        fk_set_last_error(ret);
        avformat_close_input(&ctx);
        return NULL;
    }
    return ctx;
}

int fk_input_find_stream_info(void *ctx, void *dict) {
    if (!ctx) return AVERROR(EINVAL);
    return avformat_find_stream_info(ctx, fk_dict_options(dict));
}

void fk_input_close(void *ctx) {
    AVFormatContext *c = ctx;
    if (!c) return;
    avformat_close_input(&c);
}

int fk_input_read_frame(void *ctx, void *packet) {
    if (!ctx || !packet) return AVERROR(EINVAL);
    return av_read_frame(ctx, packet);
}

int fk_input_seek(void *ctx, int stream_index, fk_i64 timestamp, int flags) {
    if (!ctx) return AVERROR(EINVAL);
    return av_seek_frame(ctx, stream_index, timestamp, flags);
}

int fk_format_get_nb_streams(void *ctx) {
    return ctx ? (int)((AVFormatContext *)ctx)->nb_streams : 0;
}

void *fk_format_get_stream(void *ctx, int index) {
    AVFormatContext *c = ctx;
    if (!c || index < 0 || (unsigned)index >= c->nb_streams) return NULL;
    return c->streams[index];
}

fk_i64 fk_format_get_start_time(void *ctx) {
    return ctx ? ((AVFormatContext *)ctx)->start_time : 0;
}

fk_i64 fk_format_get_duration(void *ctx) {
    return ctx ? ((AVFormatContext *)ctx)->duration : 0;
}

const char *fk_format_get_format_name(void *ctx) {
    AVFormatContext *c = ctx;
    if (!c) return NULL;
    if (c->iformat) return c->iformat->name;
    if (c->oformat) return c->oformat->name;
    return NULL;
}

void *fk_output_open(const char *format_name, const char *filename) {
    AVFormatContext *ctx = NULL;
    int ret = avformat_alloc_output_context2(&ctx, NULL, format_name, filename);
    if (ret < 0 || !ctx) {
        fk_set_last_error(ret < 0 ? ret : AVERROR(EINVAL));
        return NULL;
    }
    if (!(ctx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ctx->pb, filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            fk_set_last_error(ret);
            avformat_free_context(ctx);
            return NULL;
        }
    }
    return ctx;
}

int fk_output_write_header(void *ctx, void *dict) {
    if (!ctx) return AVERROR(EINVAL);
    return avformat_write_header(ctx, fk_dict_options(dict));
}

int fk_output_write_packet(void *ctx, void *packet) {
    if (!ctx) return AVERROR(EINVAL);
    return av_interleaved_write_frame(ctx, packet);
}

int fk_output_write_trailer(void *ctx) {
    return ctx ? av_write_trailer(ctx) : AVERROR(EINVAL);
}

void fk_output_close(void *ctx) {
    AVFormatContext *c = ctx;
    if (!c) return;
    if (c->pb) avio_closep(&c->pb);
    avformat_free_context(c);
}

void *fk_format_new_stream(void *ctx, void *codec) {
    if (!ctx) return NULL;
    return avformat_new_stream(ctx, codec);
}

int fk_stream_get_index(void *stream) {
    return stream ? ((AVStream *)stream)->index : -1;
}

int fk_stream_get_time_base(void *stream, int *num, int *den) {
    AVStream *s = stream;
    if (!s || !num || !den) return AVERROR(EINVAL);
    *num = s->time_base.num;
    *den = s->time_base.den;
    return 0;
}

int fk_stream_set_time_base(void *stream, int num, int den) {
    if (!stream) return AVERROR(EINVAL);
    ((AVStream *)stream)->time_base = (AVRational){num, den};
    return 0;
}

fk_i64 fk_stream_get_duration(void *stream) {
    return stream ? ((AVStream *)stream)->duration : 0;
}

int fk_stream_get_avg_frame_rate(void *stream, int *num, int *den) {
    AVStream *s = stream;
    if (!s || !num || !den) return AVERROR(EINVAL);
    *num = s->avg_frame_rate.num;
    *den = s->avg_frame_rate.den;
    return 0;
}

void *fk_stream_get_codecpar(void *stream) {
    return stream ? ((AVStream *)stream)->codecpar : NULL;
}

int fk_stream_set_codecpar(void *stream, void *par) {
    if (!stream || !par) return AVERROR(EINVAL);
    return avcodec_parameters_copy(((AVStream *)stream)->codecpar, par);
}

/* ==================== libavfilter ==================== */

void *fk_graph_alloc(void) { return avfilter_graph_alloc(); }

void fk_graph_free(void *graph) {
    AVFilterGraph *g = graph;
    if (!g) return;
    avfilter_graph_free(&g);
}

void *fk_graph_create_filter(void *graph, const char *name, const char *instance_name,
                             const char *args) {
    if (!graph || !name) return NULL;
    const AVFilter *filter = avfilter_get_by_name(name);
    if (!filter) return NULL;
    AVFilterContext *ctx = NULL;
    int ret = avfilter_graph_create_filter(&ctx, filter, instance_name, args, NULL, graph);
    if (ret < 0) {
        fk_set_last_error(ret);
        return NULL;
    }
    return ctx;
}

int fk_graph_link(void *src, int src_pad, void *dst, int dst_pad) {
    if (!src || !dst) return AVERROR(EINVAL);
    return avfilter_link(src, (unsigned)src_pad, dst, (unsigned)dst_pad);
}

int fk_graph_config(void *graph) {
    return graph ? avfilter_graph_config(graph, NULL) : AVERROR(EINVAL);
}

int fk_buffersrc_add_frame(void *src, void *frame) {
    if (!src) return AVERROR(EINVAL);
    return av_buffersrc_add_frame_flags(src, frame, AV_BUFFERSRC_FLAG_KEEP_REF);
}

int fk_buffersink_get_frame(void *sink, void *frame) {
    if (!sink || !frame) return AVERROR(EINVAL);
    return av_buffersink_get_frame(sink, frame);
}

/* ==================== libswscale ==================== */

void *fk_sws_get_context(int src_width, int src_height, int src_format,
                         int dst_width, int dst_height, int dst_format, int flags) {
    SwsContext *ctx = sws_getContext(src_width, src_height, (enum AVPixelFormat)src_format,
                                     dst_width, dst_height, (enum AVPixelFormat)dst_format,
                                     flags, NULL, NULL, NULL);
    if (!ctx) fk_set_last_error(AVERROR(EINVAL));
    return ctx;
}

void fk_sws_free_context(void *ctx) {
    if (ctx) sws_freeContext(ctx);
}

int fk_sws_scale_frame(void *ctx, void *dst, void *src) {
    if (!ctx || !dst || !src) return AVERROR(EINVAL);
    return sws_scale_frame(ctx, dst, src);
}

/* ==================== libswresample ==================== */

void *fk_swr_alloc_set_opts(int out_sample_rate, int out_format, int out_channels,
                            int in_sample_rate, int in_format, int in_channels) {
    if (out_channels <= 0 || in_channels <= 0) return NULL;
    AVChannelLayout out_layout;
    AVChannelLayout in_layout;
    av_channel_layout_default(&out_layout, out_channels);
    av_channel_layout_default(&in_layout, in_channels);
    SwrContext *ctx = NULL;
    int ret = swr_alloc_set_opts2(&ctx, &out_layout, (enum AVSampleFormat)out_format,
                                  out_sample_rate, &in_layout,
                                  (enum AVSampleFormat)in_format, in_sample_rate, 0, NULL);
    if (ret < 0) {
        fk_set_last_error(ret);
        return NULL;
    }
    return ctx;
}

int fk_swr_init(void *ctx) {
    return ctx ? swr_init(ctx) : AVERROR(EINVAL);
}

int fk_swr_convert_frame(void *ctx, void *out, void *in) {
    if (!ctx || !out) return AVERROR(EINVAL);
    return swr_convert_frame(ctx, out, in);
}

int fk_swr_get_out_samples(void *ctx, int in_samples) {
    return ctx ? swr_get_out_samples(ctx, in_samples) : AVERROR(EINVAL);
}

void fk_swr_free(void *ctx) {
    SwrContext *c = ctx;
    if (!c) return;
    swr_free(&c);
}

/* ==================== libavdevice ==================== */

void fk_device_register_all(void) { avdevice_register_all(); }
