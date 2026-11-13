/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.native

import java.io.File
import java.security.MessageDigest

/**
 * Loads `libffmpeg_jni` from the matching `ffmpeg-kmp-jni-jvm-{os}-{arch}`
 * artifact's classpath resources.
 *
 * On Android the AAR ships the .so in jniLibs, so the platform loader is used.
 * On desktop the resource is extracted to a content-addressed temporary
 * directory and passed to `System.load`, which keeps workspaces without a
 * writable `java.library.path` working.
 */
internal object FFmpegJniLoader {
    private const val LIB_NAME = "ffmpeg_jni"
    private const val RESOURCE_BASE = "/cn/enaium/ffmpeg/native"

    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return

        val isAndroid = runCatching {
            Class.forName("android.os.Build")
            true
        }.getOrDefault(false)

        if (isAndroid) {
            System.loadLibrary(LIB_NAME)
            loaded = true
            return
        }

        val classifier = detectClassifier()
        val prefix = if (classifier.startsWith("windows")) "" else "lib"
        val libFile = "$prefix$LIB_NAME.${libExtension()}"
        val resourcePath = "$RESOURCE_BASE/$classifier/$libFile"
        val stream = FFmpegJniLoader::class.java.getResourceAsStream(resourcePath)
            ?: throw UnsatisfiedLinkError(
                "Resource $resourcePath not found. " +
                    "os=${System.getProperty("os.name")}, arch=${System.getProperty("os.arch")}, " +
                    "classifier=$classifier. Check that the matching ffmpeg-kmp-jni-jvm artifact is on the classpath.",
            )
        val bytes = stream.use { it.readBytes() }
        val target = extractToTemp(bytes, libFile)
        try {
            System.load(target.absolutePath)
        } catch (e: UnsatisfiedLinkError) {
            throw UnsatisfiedLinkError(
                "Failed to load $libFile from $resourcePath (os=${System.getProperty("os.name")}, " +
                    "arch=${System.getProperty("os.arch")}): ${e.message}",
            )
        }
        loaded = true
    }

    private fun extractToTemp(bytes: ByteArray, libFile: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hex = digest.joinToString("") { "%02x".format(it) }.take(16)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "ffmpeg-kmp-$hex")
        tempDir.mkdirs()
        val target = File(tempDir, libFile)
        if (!target.isFile || target.length() != bytes.size.toLong()) {
            val tmp = File.createTempFile(libFile, ".part", tempDir)
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(target)) {
                target.delete()
                if (!tmp.renameTo(target)) {
                    tmp.inputStream().use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    tmp.delete()
                }
            }
        }
        return target
    }

    private fun detectClassifier(): String {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        val osArch = System.getProperty("os.arch").orEmpty().lowercase()
        val os = when {
            osName.contains("linux") -> "linux"
            osName.contains("mac") || osName.contains("darwin") || osName.contains("osx") -> "darwin"
            osName.contains("win") -> "windows"
            else -> error("Unsupported OS for ffmpeg-kmp JVM artifact: $osName")
        }
        val arch = when (osArch) {
            "amd64", "x86_64", "x64" -> "x86_64"
            "aarch64", "arm64" -> "aarch64"
            else -> error("Unsupported CPU architecture for ffmpeg-kmp JVM artifact: $osArch")
        }
        return "$os-$arch"
    }

    private fun libExtension(): String {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            osName.contains("win") -> "dll"
            osName.contains("mac") || osName.contains("darwin") || osName.contains("osx") -> "dylib"
            else -> "so"
        }
    }
}
