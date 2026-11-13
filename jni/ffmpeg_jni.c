/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * JNI bridge for cn.enaium.ffmpeg.native.FFmpegJni. Every function forwards
 * to the C shim in ffmpeg_kmp.c; handles are jlong-cast pointers.
 */

#include <jni.h>
#include <stdint.h>

#include "ffmpeg_kmp.h"

#define JNI_FN(name) Java_cn_enaium_ffmpeg_native_FFmpegJni_##name

static void *from_handle(jlong handle) { return (void *)(intptr_t)handle; }
static jlong to_handle(const void *pointer) { return (jlong)(intptr_t)pointer; }

static const char *get_utf(JNIEnv *env, jstring value) {
    return value ? (*env)->GetStringUTFChars(env, value, NULL) : NULL;
}

static void release_utf(JNIEnv *env, jstring value, const char *chars) {
    if (value && chars) (*env)->ReleaseStringUTFChars(env, value, chars);
}

static jstring new_string(JNIEnv *env, const char *value) {
    return value ? (*env)->NewStringUTF(env, value) : NULL;
}

/* ==================== Versions ==================== */

JNIEXPORT jstring JNICALL JNI_FN(versionInfo)(JNIEnv *env, jobject self) {
    (void)self;
    return new_string(env, fk_version_info());
}

JNIEXPORT jstring JNICALL JNI_FN(configuration)(JNIEnv *env, jobject self) {
    (void)self;
    return new_string(env, fk_configuration());
}

JNIEXPORT jstring JNICALL JNI_FN(license)(JNIEnv *env, jobject self) {
    (void)self;
    return new_string(env, fk_license());
}

JNIEXPORT jint JNICALL JNI_FN(lastError)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return fk_last_error();
}

JNIEXPORT jint JNICALL JNI_FN(avutilVersion)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return (jint)fk_avutil_version();
}

JNIEXPORT jint JNICALL JNI_FN(avcodecVersion)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return (jint)fk_avcodec_version();
}

JNIEXPORT jint JNICALL JNI_FN(avformatVersion)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return (jint)fk_avformat_version();
}

JNIEXPORT jint JNICALL JNI_FN(avfilterVersion)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return (jint)fk_avfilter_version();
}

JNIEXPORT jint JNICALL JNI_FN(swscaleVersion)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return (jint)fk_swscale_version();
}

JNIEXPORT jint JNICALL JNI_FN(swresampleVersion)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return (jint)fk_swresample_version();
}

JNIEXPORT jint JNICALL JNI_FN(avdeviceVersion)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return (jint)fk_avdevice_version();
}

/* ==================== libavutil ==================== */

JNIEXPORT jint JNICALL JNI_FN(logLevel)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return fk_log_level();
}

JNIEXPORT void JNICALL JNI_FN(setLogLevel)(JNIEnv *env, jobject self, jint level) {
    (void)env;
    (void)self;
    fk_log_set_level(level);
}

JNIEXPORT void JNICALL JNI_FN(logMessage)(JNIEnv *env, jobject self, jint level, jstring message) {
    (void)self;
    const char *chars = get_utf(env, message);
    fk_log_message(level, chars);
    release_utf(env, message, chars);
}

JNIEXPORT jint JNICALL JNI_FN(errorEagain)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return fk_error_eagain();
}

JNIEXPORT jint JNICALL JNI_FN(errorEof)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return fk_error_eof();
}

JNIEXPORT jstring JNICALL JNI_FN(errorString)(JNIEnv *env, jobject self, jint error) {
    (void)self;
    return new_string(env, fk_error_string(error));
}

JNIEXPORT jstring JNICALL JNI_FN(pixFmtName)(JNIEnv *env, jobject self, jint format) {
    (void)self;
    return new_string(env, fk_pix_fmt_name(format));
}

JNIEXPORT jint JNICALL JNI_FN(pixFmtGet)(JNIEnv *env, jobject self, jstring name) {
    (void)self;
    const char *chars = get_utf(env, name);
    int result = fk_pix_fmt_get(chars);
    release_utf(env, name, chars);
    return result;
}

JNIEXPORT jstring JNICALL JNI_FN(sampleFmtName)(JNIEnv *env, jobject self, jint format) {
    (void)self;
    return new_string(env, fk_sample_fmt_name(format));
}

JNIEXPORT jint JNICALL JNI_FN(sampleFmtGet)(JNIEnv *env, jobject self, jstring name) {
    (void)self;
    const char *chars = get_utf(env, name);
    int result = fk_sample_fmt_get(chars);
    release_utf(env, name, chars);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(sampleFmtBytes)(JNIEnv *env, jobject self, jint format) {
    (void)env;
    (void)self;
    return fk_sample_fmt_bytes(format);
}

JNIEXPORT jlong JNICALL JNI_FN(dictCreate)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return to_handle(fk_dict_create());
}

JNIEXPORT void JNICALL JNI_FN(dictFree)(JNIEnv *env, jobject self, jlong dict) {
    (void)env;
    (void)self;
    fk_dict_free(from_handle(dict));
}

JNIEXPORT jint JNICALL JNI_FN(dictSet)(JNIEnv *env, jobject self, jlong dict, jstring key,
                                         jstring value) {
    (void)self;
    const char *key_chars = get_utf(env, key);
    const char *value_chars = get_utf(env, value);
    int result = fk_dict_set(from_handle(dict), key_chars, value_chars);
    release_utf(env, value, value_chars);
    release_utf(env, key, key_chars);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(dictSetInt)(JNIEnv *env, jobject self, jlong dict, jstring key,
                                           jlong value) {
    (void)self;
    const char *key_chars = get_utf(env, key);
    int result = fk_dict_set_int(from_handle(dict), key_chars, value);
    release_utf(env, key, key_chars);
    return result;
}

JNIEXPORT jstring JNICALL JNI_FN(dictGet)(JNIEnv *env, jobject self, jlong dict, jstring key) {
    (void)self;
    const char *key_chars = get_utf(env, key);
    jstring result = new_string(env, fk_dict_get(from_handle(dict), key_chars));
    release_utf(env, key, key_chars);
    return result;
}

JNIEXPORT jlong JNICALL JNI_FN(dictGetInt)(JNIEnv *env, jobject self, jlong dict, jstring key,
                                            jlong fallback) {
    (void)self;
    const char *key_chars = get_utf(env, key);
    jlong result = (jlong)fk_dict_get_int(from_handle(dict), key_chars, fallback);
    release_utf(env, key, key_chars);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(dictCount)(JNIEnv *env, jobject self, jlong dict) {
    (void)env;
    (void)self;
    return fk_dict_count(from_handle(dict));
}

JNIEXPORT jlong JNICALL JNI_FN(frameAlloc)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return to_handle(fk_frame_alloc());
}

JNIEXPORT void JNICALL JNI_FN(frameFree)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    fk_frame_free(from_handle(frame));
}

JNIEXPORT void JNICALL JNI_FN(frameUnref)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    fk_frame_unref(from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(frameRef)(JNIEnv *env, jobject self, jlong dst, jlong src) {
    (void)env;
    (void)self;
    return fk_frame_ref(from_handle(dst), from_handle(src));
}

JNIEXPORT jint JNICALL JNI_FN(frameGetBuffer)(JNIEnv *env, jobject self, jlong frame, jint align) {
    (void)env;
    (void)self;
    return fk_frame_get_buffer(from_handle(frame), align);
}

JNIEXPORT jint JNICALL JNI_FN(frameMakeWritable)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_make_writable(from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(frameSetVideo)(JNIEnv *env, jobject self, jlong frame, jint width,
                                              jint height, jint format) {
    (void)env;
    (void)self;
    return fk_frame_set_video(from_handle(frame), width, height, format);
}

JNIEXPORT jint JNICALL JNI_FN(frameSetAudio)(JNIEnv *env, jobject self, jlong frame,
                                              jint sampleRate, jint format, jint nbSamples,
                                              jint channels) {
    (void)env;
    (void)self;
    return fk_frame_set_audio(from_handle(frame), sampleRate, format, nbSamples, channels);
}

JNIEXPORT jint JNICALL JNI_FN(frameWidth)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_get_width(from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(frameHeight)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_get_height(from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(frameFormat)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_get_format(from_handle(frame));
}

JNIEXPORT jlong JNICALL JNI_FN(framePts)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return (jlong)fk_frame_get_pts(from_handle(frame));
}

JNIEXPORT void JNICALL JNI_FN(setFramePts)(JNIEnv *env, jobject self, jlong frame, jlong pts) {
    (void)env;
    (void)self;
    fk_frame_set_pts(from_handle(frame), pts);
}

JNIEXPORT jlong JNICALL JNI_FN(framePktDts)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return (jlong)fk_frame_get_pkt_dts(from_handle(frame));
}

JNIEXPORT jlong JNICALL JNI_FN(frameDuration)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return (jlong)fk_frame_get_duration(from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(frameNbSamples)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_get_nb_samples(from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(frameSampleRate)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_get_sample_rate(from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(frameChannels)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_get_channels(from_handle(frame));
}

JNIEXPORT jboolean JNICALL JNI_FN(frameIsKey)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_is_key(from_handle(frame)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL JNI_FN(frameSampleAspectRatioNum)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_get_sample_aspect_ratio_num(from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(frameSampleAspectRatioDen)(JNIEnv *env, jobject self, jlong frame) {
    (void)env;
    (void)self;
    return fk_frame_get_sample_aspect_ratio_den(from_handle(frame));
}

JNIEXPORT void JNICALL JNI_FN(setFrameSampleAspectRatio)(JNIEnv *env, jobject self, jlong frame,
                                                          jint num, jint den) {
    (void)env;
    (void)self;
    fk_frame_set_sample_aspect_ratio(from_handle(frame), num, den);
}

JNIEXPORT jint JNICALL JNI_FN(frameLinesize)(JNIEnv *env, jobject self, jlong frame, jint plane) {
    (void)env;
    (void)self;
    return fk_frame_get_linesize(from_handle(frame), plane);
}

JNIEXPORT jint JNICALL JNI_FN(framePlaneSize)(JNIEnv *env, jobject self, jlong frame, jint plane) {
    (void)env;
    (void)self;
    return fk_frame_get_plane_size(from_handle(frame), plane);
}

JNIEXPORT jint JNICALL JNI_FN(frameCopyPlane)(JNIEnv *env, jobject self, jlong frame, jint plane,
                                               jbyteArray out) {
    (void)self;
    if (!out) return fk_error_eagain();
    jsize size = (*env)->GetArrayLength(env, out);
    jbyte *bytes = (*env)->GetByteArrayElements(env, out, NULL);
    if (!bytes && size > 0) return fk_error_eagain();
    int result = fk_frame_copy_plane(from_handle(frame), plane, (uint8_t *)bytes, size);
    (*env)->ReleaseByteArrayElements(env, out, bytes, 0);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(frameWritePlane)(JNIEnv *env, jobject self, jlong frame, jint plane,
                                                jbyteArray data) {
    (void)self;
    if (!data) return fk_error_eagain();
    jsize size = (*env)->GetArrayLength(env, data);
    jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
    if (!bytes && size > 0) return fk_error_eagain();
    int result = fk_frame_write_plane(from_handle(frame), plane, (const uint8_t *)bytes, size);
    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
    return result;
}

/* ==================== libavcodec ==================== */

JNIEXPORT jint JNICALL JNI_FN(avctxGetInt)(JNIEnv *env, jobject self, jlong ctx, jint field) {
    (void)env;
    (void)self;
    return fk_avctx_get_int(from_handle(ctx), field);
}

JNIEXPORT jint JNICALL JNI_FN(avctxSetInt)(JNIEnv *env, jobject self, jlong ctx, jint field,
                                            jint value) {
    (void)env;
    (void)self;
    return fk_avctx_set_int(from_handle(ctx), field, value);
}

JNIEXPORT jlong JNICALL JNI_FN(avctxGetInt64)(JNIEnv *env, jobject self, jlong ctx, jint field) {
    (void)env;
    (void)self;
    return (jlong)fk_avctx_get_int64(from_handle(ctx), field);
}

JNIEXPORT jint JNICALL JNI_FN(avctxSetInt64)(JNIEnv *env, jobject self, jlong ctx, jint field,
                                              jlong value) {
    (void)env;
    (void)self;
    return fk_avctx_set_int64(from_handle(ctx), field, value);
}

JNIEXPORT jlong JNICALL JNI_FN(avctxGetRational)(JNIEnv *env, jobject self, jlong ctx,
                                                  jint field) {
    (void)env;
    (void)self;
    int num = 0;
    int den = 0;
    if (fk_avctx_get_rational(from_handle(ctx), field, &num, &den) < 0) return 0;
    return ((jlong)num << 32) | (jlong)(uint32_t)den;
}

JNIEXPORT jint JNICALL JNI_FN(avctxSetRational)(JNIEnv *env, jobject self, jlong ctx, jint field,
                                                 jint num, jint den) {
    (void)env;
    (void)self;
    return fk_avctx_set_rational(from_handle(ctx), field, num, den);
}

JNIEXPORT jint JNICALL JNI_FN(avctxSetExtradata)(JNIEnv *env, jobject self, jlong ctx,
                                                  jbyteArray data) {
    (void)self;
    if (!data) return fk_avctx_set_extradata(from_handle(ctx), NULL, 0);
    jsize size = (*env)->GetArrayLength(env, data);
    jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
    if (!bytes && size > 0) return fk_error_eagain();
    int result = fk_avctx_set_extradata(from_handle(ctx), (const uint8_t *)bytes, size);
    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(avctxExtradataSize)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    return fk_avctx_get_extradata_size(from_handle(ctx));
}

JNIEXPORT jint JNICALL JNI_FN(avctxCopyExtradata)(JNIEnv *env, jobject self, jlong ctx,
                                                   jbyteArray out) {
    (void)self;
    if (!out) return fk_error_eagain();
    jsize size = (*env)->GetArrayLength(env, out);
    jbyte *bytes = (*env)->GetByteArrayElements(env, out, NULL);
    if (!bytes && size > 0) return fk_error_eagain();
    int result = fk_avctx_copy_extradata(from_handle(ctx), (uint8_t *)bytes, size);
    (*env)->ReleaseByteArrayElements(env, out, bytes, 0);
    return result;
}

JNIEXPORT jlong JNICALL JNI_FN(findDecoderByName)(JNIEnv *env, jobject self, jstring name) {
    (void)self;
    const char *chars = get_utf(env, name);
    jlong result = to_handle(fk_find_decoder_by_name(chars));
    release_utf(env, name, chars);
    return result;
}

JNIEXPORT jlong JNICALL JNI_FN(findEncoderByName)(JNIEnv *env, jobject self, jstring name) {
    (void)self;
    const char *chars = get_utf(env, name);
    jlong result = to_handle(fk_find_encoder_by_name(chars));
    release_utf(env, name, chars);
    return result;
}

JNIEXPORT jlong JNICALL JNI_FN(findDecoder)(JNIEnv *env, jobject self, jint id) {
    (void)env;
    (void)self;
    return to_handle(fk_find_decoder(id));
}

JNIEXPORT jlong JNICALL JNI_FN(findEncoder)(JNIEnv *env, jobject self, jint id) {
    (void)env;
    (void)self;
    return to_handle(fk_find_encoder(id));
}

JNIEXPORT jstring JNICALL JNI_FN(codecName)(JNIEnv *env, jobject self, jlong codec) {
    (void)self;
    return new_string(env, fk_codec_get_name(from_handle(codec)));
}

JNIEXPORT jint JNICALL JNI_FN(codecId)(JNIEnv *env, jobject self, jlong codec) {
    (void)env;
    (void)self;
    return fk_codec_get_id(from_handle(codec));
}

JNIEXPORT jint JNICALL JNI_FN(codecType)(JNIEnv *env, jobject self, jlong codec) {
    (void)env;
    (void)self;
    return fk_codec_get_type(from_handle(codec));
}

JNIEXPORT jboolean JNICALL JNI_FN(codecIsDecoder)(JNIEnv *env, jobject self, jlong codec) {
    (void)env;
    (void)self;
    return fk_codec_is_decoder(from_handle(codec)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL JNI_FN(codecIsEncoder)(JNIEnv *env, jobject self, jlong codec) {
    (void)env;
    (void)self;
    return fk_codec_is_encoder(from_handle(codec)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL JNI_FN(codecIdFromName)(JNIEnv *env, jobject self, jstring name) {
    (void)self;
    const char *chars = get_utf(env, name);
    int result = fk_codec_id_from_name(chars);
    release_utf(env, name, chars);
    return result;
}

JNIEXPORT jlong JNICALL JNI_FN(avctxAlloc)(JNIEnv *env, jobject self, jlong codec) {
    (void)env;
    (void)self;
    return to_handle(fk_avctx_alloc(from_handle(codec)));
}

JNIEXPORT void JNICALL JNI_FN(avctxFree)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    fk_avctx_free(from_handle(ctx));
}

JNIEXPORT jint JNICALL JNI_FN(avctxOpen2)(JNIEnv *env, jobject self, jlong ctx, jlong codec,
                                           jlong dict) {
    (void)env;
    (void)self;
    return fk_avctx_open2(from_handle(ctx), from_handle(codec), from_handle(dict));
}

JNIEXPORT jint JNICALL JNI_FN(avctxSendPacket)(JNIEnv *env, jobject self, jlong ctx,
                                                jlong packet) {
    (void)env;
    (void)self;
    return fk_avctx_send_packet(from_handle(ctx), from_handle(packet));
}

JNIEXPORT jint JNICALL JNI_FN(avctxReceiveFrame)(JNIEnv *env, jobject self, jlong ctx,
                                                  jlong frame) {
    (void)env;
    (void)self;
    return fk_avctx_receive_frame(from_handle(ctx), from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(avctxSendFrame)(JNIEnv *env, jobject self, jlong ctx, jlong frame) {
    (void)env;
    (void)self;
    return fk_avctx_send_frame(from_handle(ctx), from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(avctxReceivePacket)(JNIEnv *env, jobject self, jlong ctx,
                                                   jlong packet) {
    (void)env;
    (void)self;
    return fk_avctx_receive_packet(from_handle(ctx), from_handle(packet));
}

JNIEXPORT void JNICALL JNI_FN(avctxFlush)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    fk_avctx_flush(from_handle(ctx));
}

JNIEXPORT jlong JNICALL JNI_FN(packetAlloc)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return to_handle(fk_packet_alloc());
}

JNIEXPORT void JNICALL JNI_FN(packetFree)(JNIEnv *env, jobject self, jlong packet) {
    (void)env;
    (void)self;
    fk_packet_free(from_handle(packet));
}

JNIEXPORT void JNICALL JNI_FN(packetUnref)(JNIEnv *env, jobject self, jlong packet) {
    (void)env;
    (void)self;
    fk_packet_unref(from_handle(packet));
}

JNIEXPORT jint JNICALL JNI_FN(packetAllocData)(JNIEnv *env, jobject self, jlong packet,
                                                jint size) {
    (void)env;
    (void)self;
    return fk_packet_alloc_data(from_handle(packet), size);
}

JNIEXPORT jint JNICALL JNI_FN(packetSetData)(JNIEnv *env, jobject self, jlong packet,
                                              jbyteArray data) {
    (void)self;
    if (!data) return fk_packet_set_data(from_handle(packet), NULL, 0);
    jsize size = (*env)->GetArrayLength(env, data);
    jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
    if (!bytes && size > 0) return fk_error_eagain();
    int result = fk_packet_set_data(from_handle(packet), (const uint8_t *)bytes, size);
    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(packetSize)(JNIEnv *env, jobject self, jlong packet) {
    (void)env;
    (void)self;
    return fk_packet_get_size(from_handle(packet));
}

JNIEXPORT jint JNICALL JNI_FN(packetCopyData)(JNIEnv *env, jobject self, jlong packet,
                                               jbyteArray out) {
    (void)self;
    if (!out) return fk_error_eagain();
    jsize size = (*env)->GetArrayLength(env, out);
    jbyte *bytes = (*env)->GetByteArrayElements(env, out, NULL);
    if (!bytes && size > 0) return fk_error_eagain();
    int result = fk_packet_copy_data(from_handle(packet), (uint8_t *)bytes, size);
    (*env)->ReleaseByteArrayElements(env, out, bytes, 0);
    return result;
}

JNIEXPORT jlong JNICALL JNI_FN(packetGetInt64)(JNIEnv *env, jobject self, jlong packet,
                                                jint field) {
    (void)env;
    (void)self;
    return (jlong)fk_packet_get_int64(from_handle(packet), field);
}

JNIEXPORT void JNICALL JNI_FN(packetSetInt64)(JNIEnv *env, jobject self, jlong packet, jint field,
                                               jlong value) {
    (void)env;
    (void)self;
    fk_packet_set_int64(from_handle(packet), field, value);
}

JNIEXPORT void JNICALL JNI_FN(packetRescaleTs)(JNIEnv *env, jobject self, jlong packet,
                                                jint srcNum, jint srcDen, jint dstNum,
                                                jint dstDen) {
    (void)env;
    (void)self;
    fk_packet_rescale_ts(from_handle(packet), srcNum, srcDen, dstNum, dstDen);
}

JNIEXPORT jlong JNICALL JNI_FN(codecparAlloc)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return to_handle(fk_codecpar_alloc());
}

JNIEXPORT void JNICALL JNI_FN(codecparFree)(JNIEnv *env, jobject self, jlong par) {
    (void)env;
    (void)self;
    fk_codecpar_free(from_handle(par));
}

JNIEXPORT jint JNICALL JNI_FN(codecparFromContext)(JNIEnv *env, jobject self, jlong par,
                                                    jlong ctx) {
    (void)env;
    (void)self;
    return fk_codecpar_from_context(from_handle(par), from_handle(ctx));
}

JNIEXPORT jint JNICALL JNI_FN(codecparToContext)(JNIEnv *env, jobject self, jlong ctx,
                                                  jlong par) {
    (void)env;
    (void)self;
    return fk_codecpar_to_context(from_handle(ctx), from_handle(par));
}

JNIEXPORT jint JNICALL JNI_FN(codecparCopy)(JNIEnv *env, jobject self, jlong dst, jlong src) {
    (void)env;
    (void)self;
    return fk_codecpar_copy(from_handle(dst), from_handle(src));
}

JNIEXPORT jint JNICALL JNI_FN(codecparGetInt)(JNIEnv *env, jobject self, jlong par, jint field) {
    (void)env;
    (void)self;
    return fk_codecpar_get_int(from_handle(par), field);
}

JNIEXPORT jint JNICALL JNI_FN(codecparSetInt)(JNIEnv *env, jobject self, jlong par, jint field,
                                               jint value) {
    (void)env;
    (void)self;
    return fk_codecpar_set_int(from_handle(par), field, value);
}

JNIEXPORT jlong JNICALL JNI_FN(codecparGetInt64)(JNIEnv *env, jobject self, jlong par,
                                                  jint field) {
    (void)env;
    (void)self;
    return (jlong)fk_codecpar_get_int64(from_handle(par), field);
}

JNIEXPORT jint JNICALL JNI_FN(codecparSetInt64)(JNIEnv *env, jobject self, jlong par, jint field,
                                                 jlong value) {
    (void)env;
    (void)self;
    return fk_codecpar_set_int64(from_handle(par), field, value);
}

JNIEXPORT jlong JNICALL JNI_FN(codecparGetRational)(JNIEnv *env, jobject self, jlong par,
                                                     jint field) {
    (void)env;
    (void)self;
    int num = 0;
    int den = 0;
    if (fk_codecpar_get_rational(from_handle(par), field, &num, &den) < 0) return 0;
    return ((jlong)num << 32) | (jlong)(uint32_t)den;
}

JNIEXPORT jint JNICALL JNI_FN(codecparSetRational)(JNIEnv *env, jobject self, jlong par,
                                                    jint field, jint num, jint den) {
    (void)env;
    (void)self;
    return fk_codecpar_set_rational(from_handle(par), field, num, den);
}

JNIEXPORT jint JNICALL JNI_FN(codecparSetExtradata)(JNIEnv *env, jobject self, jlong par,
                                                     jbyteArray data) {
    (void)self;
    if (!data) return fk_codecpar_set_extradata(from_handle(par), NULL, 0);
    jsize size = (*env)->GetArrayLength(env, data);
    jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
    if (!bytes && size > 0) return fk_error_eagain();
    int result = fk_codecpar_set_extradata(from_handle(par), (const uint8_t *)bytes, size);
    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(codecparExtradataSize)(JNIEnv *env, jobject self, jlong par) {
    (void)env;
    (void)self;
    return fk_codecpar_get_extradata_size(from_handle(par));
}

JNIEXPORT jint JNICALL JNI_FN(codecparCopyExtradata)(JNIEnv *env, jobject self, jlong par,
                                                      jbyteArray out) {
    (void)self;
    if (!out) return fk_error_eagain();
    jsize size = (*env)->GetArrayLength(env, out);
    jbyte *bytes = (*env)->GetByteArrayElements(env, out, NULL);
    if (!bytes && size > 0) return fk_error_eagain();
    int result = fk_codecpar_copy_extradata(from_handle(par), (uint8_t *)bytes, size);
    (*env)->ReleaseByteArrayElements(env, out, bytes, 0);
    return result;
}

/* ==================== libavformat ==================== */

JNIEXPORT jlong JNICALL JNI_FN(formatFindInputFormat)(JNIEnv *env, jobject self, jstring name) {
    (void)self;
    const char *chars = get_utf(env, name);
    jlong result = to_handle(fk_format_find_input_format(chars));
    release_utf(env, name, chars);
    return result;
}

JNIEXPORT jlong JNICALL JNI_FN(formatFindOutputFormat)(JNIEnv *env, jobject self, jstring name) {
    (void)self;
    const char *chars = get_utf(env, name);
    jlong result = to_handle(fk_format_find_output_format(chars));
    release_utf(env, name, chars);
    return result;
}

JNIEXPORT jstring JNICALL JNI_FN(inputFormatName)(JNIEnv *env, jobject self, jlong format) {
    (void)self;
    return new_string(env, fk_input_format_get_name(from_handle(format)));
}

JNIEXPORT jstring JNICALL JNI_FN(outputFormatName)(JNIEnv *env, jobject self, jlong format) {
    (void)self;
    return new_string(env, fk_output_format_get_name(from_handle(format)));
}

JNIEXPORT jlong JNICALL JNI_FN(inputOpen)(JNIEnv *env, jobject self, jstring url, jlong dict) {
    (void)self;
    const char *chars = get_utf(env, url);
    jlong result = to_handle(fk_input_open(chars, from_handle(dict)));
    release_utf(env, url, chars);
    return result;
}

JNIEXPORT jlong JNICALL JNI_FN(inputOpenFormat)(JNIEnv *env, jobject self, jstring url,
                                                 jstring formatName, jlong dict) {
    (void)self;
    const char *url_chars = get_utf(env, url);
    const char *format_chars = get_utf(env, formatName);
    jlong result = to_handle(fk_input_open_format(url_chars, format_chars, from_handle(dict)));
    release_utf(env, formatName, format_chars);
    release_utf(env, url, url_chars);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(inputFindStreamInfo)(JNIEnv *env, jobject self, jlong ctx,
                                                    jlong dict) {
    (void)env;
    (void)self;
    return fk_input_find_stream_info(from_handle(ctx), from_handle(dict));
}

JNIEXPORT void JNICALL JNI_FN(inputClose)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    fk_input_close(from_handle(ctx));
}

JNIEXPORT jint JNICALL JNI_FN(inputReadFrame)(JNIEnv *env, jobject self, jlong ctx, jlong packet) {
    (void)env;
    (void)self;
    return fk_input_read_frame(from_handle(ctx), from_handle(packet));
}

JNIEXPORT jint JNICALL JNI_FN(inputSeek)(JNIEnv *env, jobject self, jlong ctx, jint streamIndex,
                                          jlong timestamp, jint flags) {
    (void)env;
    (void)self;
    return fk_input_seek(from_handle(ctx), streamIndex, timestamp, flags);
}

JNIEXPORT jint JNICALL JNI_FN(formatNbStreams)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    return fk_format_get_nb_streams(from_handle(ctx));
}

JNIEXPORT jlong JNICALL JNI_FN(formatGetStream)(JNIEnv *env, jobject self, jlong ctx, jint index) {
    (void)env;
    (void)self;
    return to_handle(fk_format_get_stream(from_handle(ctx), index));
}

JNIEXPORT jlong JNICALL JNI_FN(formatStartTime)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    return (jlong)fk_format_get_start_time(from_handle(ctx));
}

JNIEXPORT jlong JNICALL JNI_FN(formatDuration)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    return (jlong)fk_format_get_duration(from_handle(ctx));
}

JNIEXPORT jstring JNICALL JNI_FN(formatName)(JNIEnv *env, jobject self, jlong ctx) {
    (void)self;
    return new_string(env, fk_format_get_format_name(from_handle(ctx)));
}

JNIEXPORT jlong JNICALL JNI_FN(outputOpen)(JNIEnv *env, jobject self, jstring formatName,
                                            jstring filename) {
    (void)self;
    const char *format_chars = get_utf(env, formatName);
    const char *file_chars = get_utf(env, filename);
    jlong result = to_handle(fk_output_open(format_chars, file_chars));
    release_utf(env, filename, file_chars);
    release_utf(env, formatName, format_chars);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(outputWriteHeader)(JNIEnv *env, jobject self, jlong ctx, jlong dict) {
    (void)env;
    (void)self;
    return fk_output_write_header(from_handle(ctx), from_handle(dict));
}

JNIEXPORT jint JNICALL JNI_FN(outputWritePacket)(JNIEnv *env, jobject self, jlong ctx,
                                                  jlong packet) {
    (void)env;
    (void)self;
    return fk_output_write_packet(from_handle(ctx), from_handle(packet));
}

JNIEXPORT jint JNICALL JNI_FN(outputWriteTrailer)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    return fk_output_write_trailer(from_handle(ctx));
}

JNIEXPORT void JNICALL JNI_FN(outputClose)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    fk_output_close(from_handle(ctx));
}

JNIEXPORT jlong JNICALL JNI_FN(formatNewStream)(JNIEnv *env, jobject self, jlong ctx, jlong codec) {
    (void)env;
    (void)self;
    return to_handle(fk_format_new_stream(from_handle(ctx), from_handle(codec)));
}

JNIEXPORT jint JNICALL JNI_FN(streamIndex)(JNIEnv *env, jobject self, jlong stream) {
    (void)env;
    (void)self;
    return fk_stream_get_index(from_handle(stream));
}

JNIEXPORT jlong JNICALL JNI_FN(streamTimeBase)(JNIEnv *env, jobject self, jlong stream) {
    (void)env;
    (void)self;
    int num = 0;
    int den = 0;
    if (fk_stream_get_time_base(from_handle(stream), &num, &den) < 0) return 0;
    return ((jlong)num << 32) | (jlong)(uint32_t)den;
}

JNIEXPORT jint JNICALL JNI_FN(streamSetTimeBase)(JNIEnv *env, jobject self, jlong stream, jint num,
                                                  jint den) {
    (void)env;
    (void)self;
    return fk_stream_set_time_base(from_handle(stream), num, den);
}

JNIEXPORT jlong JNICALL JNI_FN(streamDuration)(JNIEnv *env, jobject self, jlong stream) {
    (void)env;
    (void)self;
    return (jlong)fk_stream_get_duration(from_handle(stream));
}

JNIEXPORT jlong JNICALL JNI_FN(streamAvgFrameRate)(JNIEnv *env, jobject self, jlong stream) {
    (void)env;
    (void)self;
    int num = 0;
    int den = 0;
    if (fk_stream_get_avg_frame_rate(from_handle(stream), &num, &den) < 0) return 0;
    return ((jlong)num << 32) | (jlong)(uint32_t)den;
}

JNIEXPORT jlong JNICALL JNI_FN(streamCodecpar)(JNIEnv *env, jobject self, jlong stream) {
    (void)env;
    (void)self;
    return to_handle(fk_stream_get_codecpar(from_handle(stream)));
}

JNIEXPORT jint JNICALL JNI_FN(streamSetCodecpar)(JNIEnv *env, jobject self, jlong stream,
                                                  jlong par) {
    (void)env;
    (void)self;
    return fk_stream_set_codecpar(from_handle(stream), from_handle(par));
}

/* ==================== libavfilter ==================== */

JNIEXPORT jlong JNICALL JNI_FN(graphAlloc)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return to_handle(fk_graph_alloc());
}

JNIEXPORT void JNICALL JNI_FN(graphFree)(JNIEnv *env, jobject self, jlong graph) {
    (void)env;
    (void)self;
    fk_graph_free(from_handle(graph));
}

JNIEXPORT jlong JNICALL JNI_FN(graphCreateFilter)(JNIEnv *env, jobject self, jlong graph,
                                                   jstring name, jstring instanceName,
                                                   jstring args) {
    (void)self;
    const char *name_chars = get_utf(env, name);
    const char *instance_chars = get_utf(env, instanceName);
    const char *args_chars = get_utf(env, args);
    jlong result = to_handle(fk_graph_create_filter(from_handle(graph), name_chars,
                                                    instance_chars, args_chars));
    release_utf(env, args, args_chars);
    release_utf(env, instanceName, instance_chars);
    release_utf(env, name, name_chars);
    return result;
}

JNIEXPORT jint JNICALL JNI_FN(graphLink)(JNIEnv *env, jobject self, jlong src, jint srcPad,
                                          jlong dst, jint dstPad) {
    (void)env;
    (void)self;
    return fk_graph_link(from_handle(src), srcPad, from_handle(dst), dstPad);
}

JNIEXPORT jint JNICALL JNI_FN(graphConfig)(JNIEnv *env, jobject self, jlong graph) {
    (void)env;
    (void)self;
    return fk_graph_config(from_handle(graph));
}

JNIEXPORT jint JNICALL JNI_FN(buffersrcAddFrame)(JNIEnv *env, jobject self, jlong src,
                                                  jlong frame) {
    (void)env;
    (void)self;
    return fk_buffersrc_add_frame(from_handle(src), from_handle(frame));
}

JNIEXPORT jint JNICALL JNI_FN(buffersinkGetFrame)(JNIEnv *env, jobject self, jlong sink,
                                                   jlong frame) {
    (void)env;
    (void)self;
    return fk_buffersink_get_frame(from_handle(sink), from_handle(frame));
}

/* ==================== libswscale ==================== */

JNIEXPORT jlong JNICALL JNI_FN(swsGetContext)(JNIEnv *env, jobject self, jint srcWidth,
                                               jint srcHeight, jint srcFormat, jint dstWidth,
                                               jint dstHeight, jint dstFormat, jint flags) {
    (void)env;
    (void)self;
    return to_handle(fk_sws_get_context(srcWidth, srcHeight, srcFormat, dstWidth, dstHeight,
                                        dstFormat, flags));
}

JNIEXPORT void JNICALL JNI_FN(swsFreeContext)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    fk_sws_free_context(from_handle(ctx));
}

JNIEXPORT jint JNICALL JNI_FN(swsScaleFrame)(JNIEnv *env, jobject self, jlong ctx, jlong dst,
                                              jlong src) {
    (void)env;
    (void)self;
    return fk_sws_scale_frame(from_handle(ctx), from_handle(dst), from_handle(src));
}

/* ==================== libswresample ==================== */

JNIEXPORT jlong JNICALL JNI_FN(swrAllocSetOpts)(JNIEnv *env, jobject self, jint outSampleRate,
                                                 jint outFormat, jint outChannels,
                                                 jint inSampleRate, jint inFormat,
                                                 jint inChannels) {
    (void)env;
    (void)self;
    return to_handle(fk_swr_alloc_set_opts(outSampleRate, outFormat, outChannels, inSampleRate,
                                           inFormat, inChannels));
}

JNIEXPORT jint JNICALL JNI_FN(swrInit)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    return fk_swr_init(from_handle(ctx));
}

JNIEXPORT jint JNICALL JNI_FN(swrConvertFrame)(JNIEnv *env, jobject self, jlong ctx, jlong out,
                                                jlong in) {
    (void)env;
    (void)self;
    return fk_swr_convert_frame(from_handle(ctx), from_handle(out), from_handle(in));
}

JNIEXPORT jint JNICALL JNI_FN(swrOutSamples)(JNIEnv *env, jobject self, jlong ctx,
                                              jint inSamples) {
    (void)env;
    (void)self;
    return fk_swr_get_out_samples(from_handle(ctx), inSamples);
}

JNIEXPORT void JNICALL JNI_FN(swrFree)(JNIEnv *env, jobject self, jlong ctx) {
    (void)env;
    (void)self;
    fk_swr_free(from_handle(ctx));
}

/* ==================== libavdevice ==================== */

JNIEXPORT void JNICALL JNI_FN(deviceRegisterAll)(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    fk_device_register_all();
}
