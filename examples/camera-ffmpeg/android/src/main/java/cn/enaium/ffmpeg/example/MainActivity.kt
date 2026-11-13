/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import org.libsdl.app.SDLActivity
import java.io.File

/**
 * Launcher activity. SDLActivity loads libmain.so and calls its exported
 * SDL_main on a dedicated SDL thread; the camera permission is requested
 * before that thread starts capturing.
 */
class MainActivity : SDLActivity() {

    override fun getLibraries(): Array<String> = arrayOf("main")

    private val cameraPermission: Int = 1

    private companion object {
        const val DPI_SCALE_ENV = "CAMERA_FFMPEG_DPI_SCALE"
        const val RECORD_PATH_ENV = "CAMERA_FFMPEG_RECORD_PATH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // SDL reports the Android window in physical pixels; ImGui needs the
        // display density to lay out text and widgets at a readable size.
        nativeSetenv(DPI_SCALE_ENV, resources.displayMetrics.density.toString())
        // The native record checkbox writes here. App-specific external files
        // need no storage permission.
        val directory = getExternalFilesDir(null) ?: filesDir
        val recording = File(directory, "camera-ffmpeg-${System.currentTimeMillis()}.mp4")
        nativeSetenv(RECORD_PATH_ENV, recording.absolutePath)
        requestCameraPermission()
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        hideSystemBars()
    }

    override fun onConfigurationChanged(newConfiguration: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfiguration)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun setRequestedOrientation(requestedOrientation: Int) {
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraPermission)
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        setWindowStyle(true)
    }
}
