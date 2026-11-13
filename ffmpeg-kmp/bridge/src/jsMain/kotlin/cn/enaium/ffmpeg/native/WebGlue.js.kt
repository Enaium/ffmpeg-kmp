/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package cn.enaium.ffmpeg.native

import kotlin.io.encoding.Base64
import kotlin.js.js

private val webInitialized: Unit by lazy {
    js(FFMPEG_WEB_GLUE_JS)(
        FFMPEG_WEB_RUNTIME_BASE64,
        FFMPEG_WASM_BASE64,
        js("typeof require !== 'undefined' ? require : null"),
    )
}

internal actual object WebGlue {
    actual fun init() {
        webInitialized
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
    ): Int = js("(n,a,b,c,d,e,f,g) => globalThis.__ffmpegWeb.callInt(n,a,b,c,d,e,f,g)")(
        name, a, b, c, d, e, f, g,
    ) as Int

    actual fun callDouble(
        name: String,
        a: Double,
        b: Double,
        c: Double,
        d: Double,
        e: Double,
        f: Double,
        g: Double,
    ): Double = js("(n,a,b,c,d,e,f,g) => globalThis.__ffmpegWeb.callDouble(n,a,b,c,d,e,f,g)")(
        name, a, b, c, d, e, f, g,
    ) as Double

    actual fun alloc(size: Int): Int = js("(s) => globalThis.__ffmpegWeb.alloc(s)")(size) as Int

    actual fun free(pointer: Int) {
        js("(p) => globalThis.__ffmpegWeb.free(p)")(pointer)
    }

    actual fun writeString(value: String): Int =
        js("(v) => globalThis.__ffmpegWeb.writeString(v)")(value) as Int

    actual fun readString(pointer: Int): String? =
        js("(p) => globalThis.__ffmpegWeb.readString(p)")(pointer) as String?

    actual fun writeBytes(value: ByteArray): Int =
        js("(v) => globalThis.__ffmpegWeb.writeBytes(v)")(Base64.Default.encode(value)) as Int

    actual fun readBytes(pointer: Int, size: Int): ByteArray =
        Base64.Default.decode(
            js("(p,s) => globalThis.__ffmpegWeb.readBytes(p,s)")(pointer, size) as String,
        )

    actual fun readInt(pointer: Int): Int =
        js("(p) => globalThis.__ffmpegWeb.readInt(p)")(pointer) as Int

    actual fun readFile(path: String): ByteArray? {
        val encoded = js("(p) => globalThis.__ffmpegWeb.readFile(p)")(path) as String?
            ?: return null
        return Base64.Default.decode(encoded)
    }

    actual fun writeFile(path: String, value: ByteArray) {
        js("(p,v) => globalThis.__ffmpegWeb.writeFile(p,v)")(
            path,
            Base64.Default.encode(value),
        )
    }

    actual fun deleteFile(path: String): Boolean =
        js("(p) => globalThis.__ffmpegWeb.deleteFile(p)")(path) as Boolean

    actual fun exists(path: String): Boolean =
        js("(p) => globalThis.__ffmpegWeb.exists(p)")(path) as Boolean
}
