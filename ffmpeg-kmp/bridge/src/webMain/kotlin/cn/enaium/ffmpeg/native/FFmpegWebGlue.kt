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
 * Low-level JavaScript bridge shared by the JS and Wasm targets.
 *
 * The Emscripten runtime is evaluated synchronously with an embedded
 * `wasmBinary`, so the rest of the binding can stay blocking. Pointers are
 * JavaScript numbers (WASM32); strings and byte arrays are copied through the
 * module's heap.
 */
internal expect object WebGlue {
    fun init()

    fun callInt(name: String, a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int): Int
    fun callDouble(name: String, a: Double, b: Double, c: Double, d: Double, e: Double, f: Double, g: Double): Double

    fun alloc(size: Int): Int
    fun free(pointer: Int)

    fun writeString(value: String): Int
    fun readString(pointer: Int): String?

    fun writeBytes(value: ByteArray): Int
    fun readBytes(pointer: Int, size: Int): ByteArray
    fun readInt(pointer: Int): Int

    fun readFile(path: String): ByteArray?
    fun writeFile(path: String, value: ByteArray)
    fun deleteFile(path: String): Boolean
    fun exists(path: String): Boolean
}

/**
 * Access to the Emscripten in-memory filesystem used by `libavformat`.
 *
 * WebAssembly has no native file system. Write input media with [writeFile]
 * before calling `AvFormatContext.openInput`, and read a finished recording
 * with [readFile] after the muxer was closed.
 */
public object FFmpegWeb {
    public fun readFile(path: String): ByteArray? {
        WebGlue.init()
        return WebGlue.readFile(path)
    }

    public fun writeFile(path: String, value: ByteArray) {
        WebGlue.init()
        WebGlue.writeFile(path, value)
    }

    public fun deleteFile(path: String): Boolean {
        WebGlue.init()
        return WebGlue.deleteFile(path)
    }

    public fun exists(path: String): Boolean {
        WebGlue.init()
        return WebGlue.exists(path)
    }
}

/**
 * JavaScript glue. It decodes the embedded Emscripten runtime and wasm bytes,
 * installs the Node globals the runtime expects, instantiates it synchronously
 * and exposes the marshaling helpers on `globalThis.__ffmpegWeb`.
 */
internal const val FFMPEG_WEB_GLUE_JS: String = """
(runtimeBase64, wasmBase64, _nodeRequire) => {
  if (globalThis.__ffmpegWeb) return;

  const decodeBase64 = (text) => {
    const raw = atob(text);
    const out = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
    return out;
  };
  const encodeBase64 = (data) => {
    let text = '';
    const chunk = 0x8000;
    for (let i = 0; i < data.length; i += chunk) {
      text += String.fromCharCode.apply(null, data.subarray(i, i + chunk));
    }
    return btoa(text);
  };

  const runtime = new TextDecoder().decode(decodeBase64(runtimeBase64));
  const wasm = decodeBase64(wasmBase64);

  if (typeof process !== 'undefined' && process.versions && process.versions.node) {
    globalThis.__dirname = globalThis.__dirname || process.cwd();
    globalThis.__filename = globalThis.__filename || (process.cwd() + '/ffmpeg_kmp_web.js');
    // Kotlin/Wasm installs a proxy named `require`; assigning that proxy back
    // to globalThis.require recurses through its getter. The runtime only needs
    // fs (unused because wasmBinary is supplied) and crypto for randomness.
    globalThis.require = (name) => {
      if (name === 'node:fs' || name === 'fs') {
        return {
          readFileSync() {
            throw new Error('wasmBinary is embedded; fs.readFileSync is unavailable');
          },
        };
      }
      if (name === 'node:crypto' || name === 'crypto') {
        const fill = (array) => {
          if (globalThis.crypto && globalThis.crypto.getRandomValues) {
            globalThis.crypto.getRandomValues(array);
          } else {
            for (let i = 0; i < array.length; i++) array[i] = (Math.random() * 256) | 0;
          }
          return array;
        };
        return {
          randomFillSync: fill,
          randomBytes(size) {
            return fill(new Uint8Array(size));
          },
        };
      }
      throw new Error('Unsupported module in the FFmpeg wasm runtime: ' + name);
    };
  }

  globalThis.Module = { wasmBinary: wasm };
  // Calling eval through a variable keeps it indirect, so the runtime's
  // `var Module` resolves the global object we just initialized.
  const globalEval = globalThis.eval;
  globalEval(runtime);
  const M = globalThis.Module;
  const heap = () => M.HEAPU8;
  const textEncoder = new TextEncoder();
  const textDecoder = new TextDecoder();

  const callInt = (name, a, b, c, d, e, f, g) =>
    M['_' + name](a, b, c, d, e, f, g) | 0;
  const callDouble = (name, a, b, c, d, e, f, g) => {
    const value = M['_' + name](a, b, c, d, e, f, g);
    return value === undefined ? 0 : value;
  };

  const writeString = (value) => {
    const data = textEncoder.encode(value);
    const pointer = M._malloc(data.length + 1);
    heap().set(data, pointer);
    heap()[pointer + data.length] = 0;
    return pointer;
  };
  const readString = (pointer) => {
    if (!pointer) return null;
    const memory = heap();
    let end = pointer;
    while (memory[end] !== 0) end++;
    return textDecoder.decode(memory.subarray(pointer, end));
  };
  const writeBytes = (base64) => {
    const data = decodeBase64(base64);
    const pointer = M._malloc(data.length || 1);
    if (data.length) heap().set(data, pointer);
    return pointer;
  };
  const readBytes = (pointer, size) => {
    if (!pointer || size <= 0) return '';
    return encodeBase64(heap().subarray(pointer, pointer + size));
  };
  const readInt = (pointer) => new DataView(M.HEAPU8.buffer).getInt32(pointer, true);

  const readFile = (path) => {
    try {
      return encodeBase64(M.FS.readFile(path));
    } catch (error) {
      return null;
    }
  };
  const writeFile = (path, base64) => {
    M.FS.writeFile(path, decodeBase64(base64));
  };
  const deleteFile = (path) => {
    try {
      M.FS.unlink(path);
      return true;
    } catch (error) {
      return false;
    }
  };
  const exists = (path) => {
    try {
      return M.FS.analyzePath(path).exists;
    } catch (error) {
      return false;
    }
  };

  globalThis.__ffmpegWeb = {
    callInt,
    callDouble,
    alloc: (size) => M._malloc(size),
    free: (pointer) => M._free(pointer),
    writeString,
    readString,
    writeBytes,
    readBytes,
    readInt,
    readFile,
    writeFile,
    deleteFile,
    exists,
  };
}
"""
