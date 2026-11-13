/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example.core

import cn.enaium.camera.CameraConfig
import cn.enaium.camera.CameraFormat
import cn.enaium.camera.CameraFrame
import cn.enaium.camera.CameraLensFacing
import cn.enaium.camera.CameraSession
import cn.enaium.camera.CameraSystem
import cn.enaium.camera.I420Frame
import cn.enaium.camera.PixelFormat
import cn.enaium.camera.cameraSystem

/**
 * Opens the platform camera and converts its frames to I420.
 *
 * The request asks for I420 directly; camera-kmp still converts from whatever
 * the backend delivers natively (NV12 on Apple, YUV420_888 on Android, YUYV on
 * V4L2, ...), so the FFmpeg pipeline only sees one layout.
 */
class CameraSource(
    private val width: Int,
    private val height: Int,
    private val frameRate: Int,
) : AutoCloseable {
    private var system: CameraSystem? = null
    private var session: CameraSession? = null

    var cameraName: String? = null
        private set

    var actualFormat: CameraFormat? = null
        private set

    /**
     * Starts streaming. [onFrame] runs on the backend's capture thread.
     * Returns `false` when no backend or camera is available.
     */
    fun start(
        onFrame: (I420Frame) -> Unit,
        onError: (String) -> Unit,
        onFailure: (Throwable) -> Unit = {},
    ): Boolean {
        return try {
            val system = cameraSystem()
            val session = system.open(
                CameraConfig(
                    format = CameraFormat(width, height, PixelFormat.I420, frameRate),
                    lensFacing = CameraLensFacing.UNKNOWN,
                    bufferCount = 4,
                ),
            )
            this.system = system
            this.session = session
            this.cameraName = session.camera.name
            session.start({ frame: CameraFrame ->
                if (actualFormat == null) actualFormat = frame.format
                onFrame(frame.toI420())
            }, { error ->
                onError(error.message ?: error.toString())
            })
            true
        } catch (error: Throwable) {
            onFailure(error)
            close()
            false
        }
    }

    override fun close() {
        runCatching { session?.close() }
        runCatching { system?.close() }
        session = null
        system = null
    }
}
