package cn.enaium.ffmpeg.example

/** Headless runs block the caller on JVM and native; the browser never uses them. */
internal expect fun runBlockingDemo(block: suspend () -> Unit)
