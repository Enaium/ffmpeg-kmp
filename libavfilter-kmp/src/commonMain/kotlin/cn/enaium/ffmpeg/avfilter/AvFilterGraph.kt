/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avfilter

import cn.enaium.ffmpeg.avutil.AvError
import cn.enaium.ffmpeg.avutil.AvException
import cn.enaium.ffmpeg.avutil.AvFrame
import cn.enaium.ffmpeg.avutil.AvVersion
import cn.enaium.ffmpeg.native.FFmpegNative

/** libavfilter version. */
public object AvFilter {
    public val version: Int get() = FFmpegNative.avfilterVersion()
    public val versionString: String get() = AvVersion.toString(version)
}

/**
 * A filter instance inside an [AvFilterGraph].
 *
 * Filters are owned by the graph; do not close them individually.
 */
public class AvFilterContext internal constructor(
    public val handle: Long,
    public val graph: AvFilterGraph,
) {
    public val instanceName: String get() = graph.instanceNameOf(this)
}

/**
 * `AVFilterGraph`.
 *
 * Typical source/sink graph:
 * ```
 * AvFilterGraph.create().use { graph ->
 *     val src = graph.createFilter("color", "src", "c=red:s=32x32:r=1")
 *     val sink = graph.createFilter("buffersink", "sink")
 *     graph.link(src, 0, sink, 0)
 *     graph.configure()
 *     val frame = graph.getFrame(sink)
 * }
 * ```
 * For pushed frames, create a `buffer` source, call [addFrame], and drain with
 * [getFrame] on the sink.
 */
public class AvFilterGraph private constructor(handle: Long) : AutoCloseable {
    public var handle: Long = handle
        private set

    private val names = mutableMapOf<Long, String>()

    public fun createFilter(
        name: String,
        instanceName: String,
        args: String? = null,
    ): AvFilterContext {
        val filter = FFmpegNative.graphCreateFilter(handle, name, instanceName, args)
        if (filter == 0L) {
            throw AvException(
                "avfilter_graph_create_filter('$name' as '$instanceName') failed",
                FFmpegNative.lastError(),
            )
        }
        names[filter] = instanceName
        return AvFilterContext(filter, this)
    }

    public fun link(src: AvFilterContext, srcPad: Int, dst: AvFilterContext, dstPad: Int) {
        AvError.check(
            FFmpegNative.graphLink(src.handle, srcPad, dst.handle, dstPad),
            "avfilter_link",
        )
    }

    public fun configure() {
        AvError.check(FFmpegNative.graphConfig(handle), "avfilter_graph_config")
    }

    /** Pushes a frame into a `buffer` source; the frame stays owned by the caller. */
    public fun addFrame(src: AvFilterContext, frame: AvFrame) {
        AvError.check(
            FFmpegNative.buffersrcAddFrame(src.handle, frame.handle),
            "av_buffersrc_add_frame_flags",
        )
    }

    /** Pulls one frame from a `buffersink`; returns `null` on `EAGAIN`/`EOF`. */
    public fun getFrame(sink: AvFilterContext): AvFrame? {
        val frame = AvFrame.create()
        val result = FFmpegNative.buffersinkGetFrame(sink.handle, frame.handle)
        if (result == AvError.EAGAIN || result == AvError.EOF) {
            frame.close()
            return null
        }
        if (result < 0) {
            frame.close()
            AvError.check(result, "av_buffersink_get_frame")
        }
        return frame
    }

    internal fun instanceNameOf(context: AvFilterContext): String =
        names[context.handle] ?: ""

    override fun close() {
        if (handle != 0L) {
            FFmpegNative.graphFree(handle)
            handle = 0L
            names.clear()
        }
    }

    public companion object {
        public fun create(): AvFilterGraph {
            val handle = FFmpegNative.graphAlloc()
            if (handle == 0L) throw AvException("avfilter_graph_alloc failed")
            return AvFilterGraph(handle)
        }
    }
}
