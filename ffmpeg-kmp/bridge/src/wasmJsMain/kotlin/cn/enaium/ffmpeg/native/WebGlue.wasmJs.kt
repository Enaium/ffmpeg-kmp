/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

@file:OptIn(
    kotlin.js.ExperimentalWasmJsInterop::class,
    kotlin.io.encoding.ExperimentalEncodingApi::class,
)

package cn.enaium.ffmpeg.native

import kotlin.JsFun
import kotlin.io.encoding.Base64

private var webInitialized = false

internal actual object WebGlue {
    actual fun init() {
        if (!webInitialized) {
            initGlue(
                FFMPEG_WEB_RUNTIME_BASE64,
                FFMPEG_WASM_BASE64,
                nodeRequire(),
            )
            webInitialized = true
        }
    }

    actual fun callInt(
        name: String,
        a: Int,
        b: Int,
        c: Int,
        d: Int,
        e: Int,
        f: Int,
        g: Int,
    ): Int = callIntExternal(name, a, b, c, d, e, f, g)

    actual fun callDouble(
        name: String,
        a: Double,
        b: Double,
        c: Double,
        d: Double,
        e: Double,
        f: Double,
        g: Double,
    ): Double = callDoubleExternal(name, a, b, c, d, e, f, g)

    actual fun alloc(size: Int): Int = allocExternal(size)
    actual fun free(pointer: Int): Unit = freeExternal(pointer)
    actual fun writeString(value: String): Int = writeStringExternal(value)
    actual fun readString(pointer: Int): String? = readStringExternal(pointer)

    actual fun writeBytes(value: ByteArray): Int =
        writeBytesExternal(Base64.Default.encode(value))

    actual fun readBytes(pointer: Int, size: Int): ByteArray =
        Base64.Default.decode(readBytesExternal(pointer, size))

    actual fun readInt(pointer: Int): Int = readIntExternal(pointer)

    actual fun readFile(path: String): ByteArray? {
        val encoded = readFileExternal(path) ?: return null
        return Base64.Default.decode(encoded)
    }

    actual fun writeFile(path: String, value: ByteArray): Unit =
        writeFileExternal(path, Base64.Default.encode(value))

    actual fun deleteFile(path: String): Boolean = deleteFileExternal(path)
    actual fun exists(path: String): Boolean = existsExternal(path)
}

@JsFun(FFMPEG_WEB_GLUE_JS)
private external fun initGlue(runtimeBase64: String, wasmBase64: String, nodeRequire: JsAny?)

@JsFun("() => typeof require !== 'undefined' ? require : null")
private external fun nodeRequire(): JsAny?

@JsFun("(n,a,b,c,d,e,f,g) => globalThis.__ffmpegWeb.callInt(n,a,b,c,d,e,f,g)")
private external fun callIntExternal(
    name: String,
    a: Int,
    b: Int,
    c: Int,
    d: Int,
    e: Int,
    f: Int,
    g: Int,
): Int

@JsFun("(n,a,b,c,d,e,f,g) => globalThis.__ffmpegWeb.callDouble(n,a,b,c,d,e,f,g)")
private external fun callDoubleExternal(
    name: String,
    a: Double,
    b: Double,
    c: Double,
    d: Double,
    e: Double,
    f: Double,
    g: Double,
): Double

@JsFun("(s) => globalThis.__ffmpegWeb.alloc(s)")
private external fun allocExternal(size: Int): Int

@JsFun("(p) => globalThis.__ffmpegWeb.free(p)")
private external fun freeExternal(pointer: Int)

@JsFun("(v) => globalThis.__ffmpegWeb.writeString(v)")
private external fun writeStringExternal(value: String): Int

@JsFun("(p) => globalThis.__ffmpegWeb.readString(p)")
private external fun readStringExternal(pointer: Int): String?

@JsFun("(v) => globalThis.__ffmpegWeb.writeBytes(v)")
private external fun writeBytesExternal(value: String): Int

@JsFun("(p,s) => globalThis.__ffmpegWeb.readBytes(p,s)")
private external fun readBytesExternal(pointer: Int, size: Int): String

@JsFun("(p) => globalThis.__ffmpegWeb.readInt(p)")
private external fun readIntExternal(pointer: Int): Int

@JsFun("(p) => globalThis.__ffmpegWeb.readFile(p)")
private external fun readFileExternal(path: String): String?

@JsFun("(p,v) => globalThis.__ffmpegWeb.writeFile(p,v)")
private external fun writeFileExternal(path: String, value: String)

@JsFun("(p) => globalThis.__ffmpegWeb.deleteFile(p)")
private external fun deleteFileExternal(path: String): Boolean

@JsFun("(p) => globalThis.__ffmpegWeb.exists(p)")
private external fun existsExternal(path: String): Boolean
