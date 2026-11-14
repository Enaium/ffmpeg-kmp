package cn.enaium.ffmpeg.example

internal actual fun runBlockingDemo(block: suspend () -> Unit) {
    error("headless mode is not available in the browser")
}
