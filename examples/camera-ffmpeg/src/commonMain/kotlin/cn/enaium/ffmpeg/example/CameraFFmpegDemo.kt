/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.example

import cn.enaium.camera.CameraLensFacing
import cn.enaium.camera.CameraResolution
import cn.enaium.camera.I420Frame
import cn.enaium.camera.cameraSystem
import cn.enaium.ffmpeg.example.core.CameraSource
import cn.enaium.ffmpeg.example.core.DemoOptions
import cn.enaium.ffmpeg.example.core.DisplayFrame
import cn.enaium.ffmpeg.example.core.FFmpegPipeline
import cn.enaium.ffmpeg.example.core.FilterPreset
import cn.enaium.ffmpeg.example.core.PipelineOutput
import cn.enaium.ffmpeg.example.core.PipelineSettings
import cn.enaium.ffmpeg.example.core.PipelineStats
import cn.enaium.ffmpeg.example.core.RecordingFormat
import cn.enaium.ffmpeg.example.core.ScaleOptions
import cn.enaium.ffmpeg.example.core.SourceKind
import cn.enaium.ffmpeg.example.core.TestPattern
import cn.enaium.imgui.ImFontConfig
import cn.enaium.imgui.ImGui
import cn.enaium.imgui.ImGuiChildFlags
import cn.enaium.imgui.ImGuiCond
import cn.enaium.imgui.ImGuiWindowFlags
import cn.enaium.imgui.ImVec2
import cn.enaium.imgui.ImVec4
import cn.enaium.imgui.backends.sdl.ImGuiSdlBackend
import cn.enaium.imgui.backends.sdl.ImGuiSdlRendererBackend
import cn.enaium.imgui.backends.sdl.toImTextureID
import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.SDLEventType
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLPixelFormat
import cn.enaium.sdl.SDLTexture
import cn.enaium.sdl.SDLTextureAccess
import cn.enaium.sdl.SDLWindow
import cn.enaium.sdl.SDLWindowEventType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.min
import kotlin.time.TimeSource

/**
 * Camera -> FFmpeg example. The window shows the live camera and the same
 * picture after swscale, avfilter, avcodec encode/decode; libavformat records
 * the encoded packets as MP4 when the record checkbox is on.
 *
 * `options.headless` runs the same pipeline without SDL, which is what CI uses.
 */
object CameraFFmpegDemo {

    fun run(options: DemoOptions, uiScale: Float = 1f) {
        if (options.headless) {
            runHeadless(options)
        } else {
            runWindowed(options, uiScale)
        }
    }

    // ==================== headless ====================

    private fun runHeadless(options: DemoOptions) {
        val limit = if (options.frames <= 0) 60 else options.frames
        val pipeline = FFmpegPipeline(options.frameRate, options.bitrateKbps)
        val camera = CameraSource(options.width, options.height, options.frameRate)
        val channel = Channel<I420Frame>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        var input = 0
        var decoded = 0
        var sourceName = "synthetic"
        var lastOutput: PipelineOutput? = null
        var recordingPath: String? = null

        try {
            runBlocking {
                var usingCamera = false
                if (options.source == SourceKind.CAMERA) {
                    usingCamera = camera.start(
                        onFrame = { channel.trySend(it) },
                        onError = { println("camera error: $it") },
                        onFailure = { println("camera unavailable: ${it.message ?: it}") },
                    )
                    if (usingCamera) {
                        val first = withTimeoutOrNull(3_000) { channel.receive() }
                        if (first == null) {
                            println("camera produced no frame; using the test pattern")
                            camera.close()
                            usingCamera = false
                        } else {
                            sourceName = camera.cameraName ?: "camera"
                            input++
                            lastOutput = pipeline.process(first, pipelineSettings(options))
                            recordingPath = lastOutput.recordingPath ?: recordingPath
                            if (lastOutput.filtered != null) decoded++
                        }
                    }
                }

                val pattern = TestPattern(options.width, options.height, options.frameRate)
                while (input < limit) {
                    val frame = if (usingCamera) {
                        withTimeoutOrNull(3_000) { channel.receive() } ?: break
                    } else {
                        pattern.next()
                    }
                    input++
                    val output = pipeline.process(frame, pipelineSettings(options))
                    lastOutput = output
                    recordingPath = output.recordingPath ?: recordingPath
                    if (output.filtered != null) decoded++
                }
            }

            val stats = lastOutput?.stats ?: PipelineStats(width = options.width, height = options.height)
            println("source=$sourceName")
            println("stats=$stats")
            check(input > 0) { "the example produced no input frames" }
            check(decoded > 0) { "the decoder produced no picture" }
            if (options.recording) {
                println("recording=$recordingPath")
            }
        } finally {
            channel.close()
            camera.close()
            pipeline.close()
        }
    }

    private fun pipelineSettings(options: DemoOptions): PipelineSettings = PipelineSettings(
        filter = options.filter,
        scalePercent = options.scalePercent,
        record = options.recording,
        recordingFormat = options.container,
        recordingPath = options.recordingPath,
    )

    // ==================== windowed ====================

    private data class WindowSettings(
        val source: SourceKind,
        val width: Int,
        val height: Int,
        val frameRate: Int,
        val bitrateKbps: Int,
        val filter: FilterPreset,
        val scalePercent: Int,
        val container: RecordingFormat,
    )

    private fun runWindowed(options: DemoOptions, uiScale: Float) {
        SDL.setMainReady()
        if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
            SDL.setHint("SDL_VIDEO_DRIVER", "dummy")
            if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
                error("SDL_Init failed: ${SDL.error()}")
            }
        }

        val resolutions = resolutionChoices(options)
        val initial = WindowSettings(
            options.source,
            options.width,
            options.height,
            options.frameRate,
            options.bitrateKbps,
            options.filter,
            options.scalePercent,
            options.container,
        )
        val window = SDL.createWindow(
            title = "ffmpeg-kmp camera",
            width = 1280,
            height = 800,
            flags = cn.enaium.sdl.SDLWindowFlags.RESIZABLE or
                cn.enaium.sdl.SDLWindowFlags.HIGH_PIXEL_DENSITY,
        )
        // The software renderer keeps present() from blocking on a Metal
        // drawable when the window is occluded; the two video textures are
        // cheap enough for it.
        val renderer = runCatching { SDL.createRenderer(window, name = "software") }
            .getOrElse { SDL.createRenderer(window) }
        println("video driver=${SDL.getCurrentVideoDriver()}, renderer=${renderer.name}")

        val imguiContext = ImGui.createContext()
        val imgui = ImGuiSdlBackend(window)
        val imguiRenderer = ImGuiSdlRendererBackend(renderer)
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, error ->
                println("pipeline coroutine failed: ${error.message}")
                error.printStackTrace()
            },
        )
        val settings = MutableStateFlow(initial)
        val recording = MutableStateFlow(options.recording)
        val container = MutableStateFlow(options.container)
        val direct = MutableStateFlow<DisplayFrame?>(null)
        val processed = MutableStateFlow<DisplayFrame?>(null)
        val stats = MutableStateFlow(
            PipelineStats(width = initial.width, height = initial.height),
        )
        val errorText = MutableStateFlow<String?>(null)
        val recordingPath = MutableStateFlow<String?>(null)
        val ui = UiState(resolutions, initial)
        val windowStarted = TimeSource.Monotonic.markNow()
        var sessionJob: kotlinx.coroutines.Job? = null
        var left: Panel? = null
        var right: Panel? = null
        var running = true
        var rendered = 0
        val limit = if (options.frames <= 0) Int.MAX_VALUE else options.frames
        val interfaceScale = maxOf(uiScale, 0.5f)

        try {
            imgui.init()
            val fontDensity = maxOf(imgui.framebufferScale.x, imgui.framebufferScale.y, 1f)
            val fonts = ImGui.getIO().fonts
            fonts.addFontDefault(
                ImFontConfig(
                    sizePixels = 13f * fontDensity * interfaceScale,
                    rasterizerDensity = fontDensity,
                ),
            )
            check(fonts.build()) { "font atlas build failed" }
            val atlas = fonts.getTexDataAsRGBA32()
            fonts.setTexID(imguiRenderer.uploadFontTexture(atlas.pixels, atlas.width, atlas.height))

            // A settings change cancels the previous camera/pipeline and starts
            // the next session on the same worker. The record checkbox is a
            // separate flow so toggling it does not restart the camera.
            sessionJob = scope.launch {
                settings.collectLatest { current ->
                    direct.value = null
                    processed.value = null
                    errorText.value = null
                    stats.value = PipelineStats(width = current.width, height = current.height)
                    runSession(
                        current,
                        options.recordingPath,
                        recording,
                        container,
                        direct,
                        processed,
                        stats,
                        errorText,
                        recordingPath,
                    )
                }
            }

            while (running && rendered < limit) {
                if (options.quitAfterMillis > 0 &&
                    windowStarted.elapsedNow().inWholeMilliseconds >= options.quitAfterMillis
                ) {
                    running = false
                    continue
                }
                while (true) {
                    val event = SDL.pollEvent() ?: break
                    when (event) {
                        is SDLEvent.Quit -> running = false
                        is SDLEvent.Window -> {
                            if (event.type == SDLWindowEventType.CLOSE_REQUESTED) {
                                running = false
                            } else {
                                imgui.processEvent(event)
                            }
                        }

                        is SDLEvent.TouchFinger -> {
                            val size = window.size
                            val x = event.x * size.x
                            val y = event.y * size.y
                            when (event.type) {
                                SDLEventType.FINGER_DOWN -> {
                                    imgui.processEvent(SDLEvent.MouseMotion(event.timestamp, 0, x, y, 0f, 0f))
                                    imgui.processEvent(SDLEvent.MouseButton(event.timestamp, 0, true, 1, 1, x, y))
                                }

                                SDLEventType.FINGER_MOTION ->
                                    imgui.processEvent(SDLEvent.MouseMotion(event.timestamp, 0, x, y, 0f, 0f))

                                SDLEventType.FINGER_UP -> {
                                    imgui.processEvent(SDLEvent.MouseMotion(event.timestamp, 0, x, y, 0f, 0f))
                                    imgui.processEvent(SDLEvent.MouseButton(event.timestamp, 0, false, 1, 1, x, y))
                                }

                                else -> Unit
                            }
                        }

                        else -> imgui.processEvent(event)
                    }
                }

                direct.value?.let { frame ->
                    val panel = left?.takeIf { it.matches(frame) } ?: Panel(renderer, imguiRenderer, frame).also {
                        left?.close()
                        left = it
                    }
                    panel.update(frame)
                }
                processed.value?.let { frame ->
                    val panel = right?.takeIf { it.matches(frame) } ?: Panel(renderer, imguiRenderer, frame).also {
                        right?.close()
                        right = it
                    }
                    panel.update(frame)
                }

                imgui.newFrame()
                drawUi(
                    ui,
                    settings,
                    recording,
                    container,
                    left,
                    right,
                    stats.value,
                    errorText.value,
                    recordingPath.value,
                    interfaceScale,
                )
                ImGui.render()

                renderer.drawColor = SDLColor(18, 18, 24, 255)
                renderer.clear()
                imguiRenderer.renderDrawData(ImGui.getDrawData())
                renderer.present()

                rendered++
                if (rendered % 120 == 0) {
                    println("rendered $rendered frames, ${stats.value}")
                }
                SDL.delay((1000 / settings.value.frameRate).coerceAtLeast(1))
            }
            println("stopped after $rendered rendered frames, ${stats.value}")
        } finally {
            scope.cancel()
            runBlocking { sessionJob?.join() }
            left?.close()
            right?.close()
            imguiRenderer.close()
            ImGui.destroyContext(imguiContext)
            renderer.close()
            window.close()
            SDL.quit()
        }
    }

    private class UiState(
        val resolutions: List<CameraResolution>,
        initial: WindowSettings,
    ) {
        val resolutionLabels: Array<String> = resolutions.map { "${it.width}x${it.height}" }.toTypedArray()
        val resolutionIndex = IntArray(1) {
            resolutions.indexOfFirst { it.width == initial.width && it.height == initial.height }
                .coerceAtLeast(0)
        }
        val sourceLabels: Array<String> = arrayOf("Camera", "Synthetic test pattern")
        val sourceIndex = IntArray(1) { if (initial.source == SourceKind.CAMERA) 0 else 1 }
        val filterLabels: Array<String> = FilterPreset.entries.map { it.label }.toTypedArray()
        val filterIndex = IntArray(1) {
            FilterPreset.entries.indexOf(initial.filter).coerceAtLeast(0)
        }
        val scaleLabels: Array<String> = ScaleOptions.labels
        val scaleIndex = IntArray(1) { ScaleOptions.indexOf(initial.scalePercent) }
        val containerLabels: Array<String> = RecordingFormat.entries.map { it.label }.toTypedArray()
        val containerIndex = IntArray(1) {
            RecordingFormat.entries.indexOf(initial.container).coerceAtLeast(0)
        }
        val bitrate = IntArray(1) { initial.bitrateKbps }
    }

    private fun drawUi(
        ui: UiState,
        settings: MutableStateFlow<WindowSettings>,
        recording: MutableStateFlow<Boolean>,
        container: MutableStateFlow<RecordingFormat>,
        left: Panel?,
        right: Panel?,
        stats: PipelineStats,
        error: String?,
        recordingPath: String?,
        scale: Float,
    ) {
        ImGui.setNextWindowPos(ImVec2(0f, 0f), ImGuiCond.ALWAYS)
        ImGui.setNextWindowSize(ImGui.getIO().displaySize, ImGuiCond.ALWAYS)
        val flags = ImGuiWindowFlags.NO_TITLE_BAR or
            ImGuiWindowFlags.NO_RESIZE or
            ImGuiWindowFlags.NO_MOVE or
            ImGuiWindowFlags.NO_COLLAPSE or
            ImGuiWindowFlags.NO_SCROLLBAR or
            ImGuiWindowFlags.NO_SAVED_SETTINGS

        val open = ImGui.begin("##ffmpeg-kmp", flags = flags)
        if (open) {
            val available = ImGui.getContentRegionAvail()
            val controlsWidth = min(310f * scale, available.x * 0.45f)
            if (controlsWidth > 1f) {
                if (ImGui.beginChild("##controls", ImVec2(controlsWidth, 0f), ImGuiChildFlags.BORDERS)) {
                    ImGui.text("ffmpeg-kmp camera")
                    ImGui.separatorText("Capture")

                    ImGui.setNextItemWidth(200f * scale)
                    if (ImGui.combo("Resolution", ui.resolutionIndex, ui.resolutionLabels)) {
                        val picked = ui.resolutions.getOrNull(ui.resolutionIndex[0])
                        if (picked != null) {
                            settings.value = settings.value.copy(width = picked.width, height = picked.height)
                        }
                    }

                    ImGui.setNextItemWidth(200f * scale)
                    if (ImGui.combo("Source", ui.sourceIndex, ui.sourceLabels)) {
                        val picked = if (ui.sourceIndex[0] == 0) SourceKind.CAMERA else SourceKind.SYNTHETIC
                        settings.value = settings.value.copy(source = picked)
                    }

                    ImGui.separatorText("Pipeline")
                    ImGui.setNextItemWidth(200f * scale)
                    if (ImGui.combo("Filter", ui.filterIndex, ui.filterLabels)) {
                        val picked = FilterPreset.entries.getOrNull(ui.filterIndex[0]) ?: FilterPreset.NONE
                        settings.value = settings.value.copy(filter = picked)
                    }

                    ImGui.setNextItemWidth(200f * scale)
                    if (ImGui.combo("Scale", ui.scaleIndex, ui.scaleLabels)) {
                        val picked = ScaleOptions.values.getOrNull(ui.scaleIndex[0]) ?: 100
                        settings.value = settings.value.copy(scalePercent = picked)
                    }

                    ImGui.setNextItemWidth(200f * scale)
                    ImGui.sliderInt("Bitrate (kbps)", ui.bitrate, 200, 12_000, "%d kbps")
                    if (ImGui.isItemDeactivatedAfterEdit() &&
                        ui.bitrate[0] != settings.value.bitrateKbps
                    ) {
                        settings.value = settings.value.copy(bitrateKbps = ui.bitrate[0])
                    }
                    ImGui.textDisabled("Apply on release")

                    ImGui.separatorText("Recording")
                    val record = booleanArrayOf(recording.value)
                    if (ImGui.checkbox("Record", record)) {
                        recording.value = record[0]
                    }
                    ImGui.setNextItemWidth(200f * scale)
                    if (ImGui.combo("Container", ui.containerIndex, ui.containerLabels)) {
                        container.value = RecordingFormat.entries.getOrNull(ui.containerIndex[0])
                            ?: RecordingFormat.MP4
                    }
                    if (recording.value) {
                        ImGui.textWrapped(recordingPath ?: "starting...")
                    } else {
                        ImGui.textDisabled("off")
                    }

                    ImGui.separatorText("Statistics")
                    ImGui.text("Input ${stats.inputFrames}   encoded ${stats.encodedFrames}")
                    ImGui.text("Decoded ${stats.decodedFrames}")
                    ImGui.text("${stats.averageFrameBytes} B/frame")
                    ImGui.text("${(stats.measuredFps * 10).toInt() / 10.0} fps")
                    ImGui.textDisabled("${settings.value.filter.label} @ ${settings.value.scalePercent}%")
                    if (!error.isNullOrBlank()) {
                        ImGui.separator()
                        ImGui.textColored(ImVec4(1f, 0.35f, 0.35f, 1f), error)
                    }
                }
                ImGui.endChild()
                ImGui.sameLine()
            }

            if (ImGui.beginChild("##views", ImVec2(0f, 0f))) {
                val views = ImGui.getContentRegionAvail()
                val gap = 8f * scale
                val cellWidth = (views.x - gap) / 2f
                drawTexturePanel("Camera", left, cellWidth, views.y)
                if (cellWidth > 1f) ImGui.sameLine()
                drawTexturePanel("Filtered / decoded", right, cellWidth, views.y)
            }
            ImGui.endChild()
        }
        ImGui.end()
    }

    private fun drawTexturePanel(
        title: String,
        panel: Panel?,
        width: Float,
        height: Float,
    ) {
        if (width <= 1f || height <= 1f) return
        if (ImGui.beginChild("##$title", ImVec2(width, height), ImGuiChildFlags.BORDERS)) {
            ImGui.text(title)
            val available = ImGui.getContentRegionAvail()
            if (panel == null || !panel.ready) {
                ImGui.textDisabled("waiting for frames...")
            } else {
                val imageScale = min(available.x / panel.width, available.y / panel.height)
                val imageWidth = panel.width * imageScale
                val imageHeight = panel.height * imageScale
                if (imageWidth > 1f && imageHeight > 1f) {
                    val cursor = ImGui.getCursorPos()
                    ImGui.setCursorPos(
                        ImVec2(
                            cursor.x + (available.x - imageWidth) / 2f,
                            cursor.y + (available.y - imageHeight) / 2f,
                        ),
                    )
                    ImGui.image(panel.texture.toImTextureID(), ImVec2(imageWidth, imageHeight))
                }
            }
        }
        ImGui.endChild()
    }

    private suspend fun runSession(
        session: WindowSettings,
        recordingPathBase: String,
        recording: MutableStateFlow<Boolean>,
        container: MutableStateFlow<RecordingFormat>,
        direct: MutableStateFlow<DisplayFrame?>,
        processed: MutableStateFlow<DisplayFrame?>,
        stats: MutableStateFlow<PipelineStats>,
        errorText: MutableStateFlow<String?>,
        recordingPath: MutableStateFlow<String?>,
    ) = coroutineScope {
        errorText.value = null
        val channel = Channel<I420Frame>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val pipeline = FFmpegPipeline(session.frameRate, session.bitrateKbps)
        val camera = CameraSource(session.width, session.height, session.frameRate)
        var syntheticStarted = false
        var cameraOpen = false

        fun startSynthetic() {
            if (syntheticStarted) return
            syntheticStarted = true
            println("using the synthetic test pattern")
            launch {
                val pattern = TestPattern(session.width, session.height, session.frameRate)
                while (isActive) {
                    channel.trySend(pattern.next())
                    delay((1000 / session.frameRate).coerceAtLeast(1).toLong())
                }
            }
        }

        try {
            if (session.source == SourceKind.CAMERA) {
                cameraOpen = camera.start(
                    onFrame = { channel.trySend(it) },
                    onError = {
                        println("camera error: $it")
                        errorText.value = it
                    },
                    onFailure = {
                        println("camera unavailable: ${it.message ?: it}")
                        errorText.value = it.message ?: it.toString()
                    },
                )
            }
            if (!cameraOpen) {
                startSynthetic()
            } else {
                launch {
                    delay(2_500)
                    if (direct.value == null) {
                        println("camera produced no frame within 2.5 s; falling back to the test pattern")
                        camera.close()
                        startSynthetic()
                    }
                }
            }

            while (isActive) {
                val frame = withTimeoutOrNull(500) { channel.receive() } ?: continue
                val output = pipeline.process(
                    frame,
                    PipelineSettings(
                        filter = session.filter,
                        scalePercent = session.scalePercent,
                        record = recording.value,
                        recordingFormat = container.value,
                        recordingPath = recordingPathBase,
                    ),
                )
                direct.value = output.camera
                output.filtered?.let { processed.value = it }
                stats.value = output.stats
                recordingPath.value = output.recordingPath
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            println("FFmpeg pipeline failed: ${error.message}")
            error.printStackTrace()
            errorText.value = error.message ?: error.toString()
        } finally {
            channel.close()
            camera.close()
            pipeline.close()
        }
    }

    private fun resolutionChoices(options: DemoOptions): List<CameraResolution> {
        val supported = runCatching {
            cameraSystem().use { system ->
                system.defaultCamera(CameraLensFacing.UNKNOWN)?.resolutions.orEmpty()
            }
        }.getOrDefault(emptyList())
        val common = listOf(
            CameraResolution(320, 240),
            CameraResolution(640, 480),
            CameraResolution(1280, 720),
            CameraResolution(1920, 1080),
        )
        val initial = CameraResolution(options.width, options.height)
        return (listOf(initial) + supported + common)
            .filter { it.width % 2 == 0 && it.height % 2 == 0 }
            .distinct()
            .sortedByDescending { it.pixelCount }
            .take(12)
    }

    private class Panel(
        private val renderer: cn.enaium.sdl.SDLRenderer,
        private val imgui: ImGuiSdlRendererBackend,
        frame: DisplayFrame,
    ) : AutoCloseable {
        var width: Int = frame.width
            private set
        var height: Int = frame.height
            private set
        var texture: SDLTexture = renderer.createTexture(
            SDLPixelFormat.RGBA32,
            SDLTextureAccess.STREAMING,
            width,
            height,
        )
            private set
        var ready: Boolean = false
            private set

        init {
            imgui.registerTexture(texture)
        }

        fun matches(frame: DisplayFrame): Boolean =
            frame.width == width && frame.height == height

        fun update(frame: DisplayFrame) {
            if (!matches(frame)) {
                imgui.unregisterTexture(texture)
                texture.close()
                width = frame.width
                height = frame.height
                texture = renderer.createTexture(
                    SDLPixelFormat.RGBA32,
                    SDLTextureAccess.STREAMING,
                    width,
                    height,
                )
                imgui.registerTexture(texture)
            }
            texture.update(null, frame.rgba, width * 4)
            ready = true
        }

        override fun close() {
            imgui.unregisterTexture(texture)
            texture.close()
        }
    }
}
