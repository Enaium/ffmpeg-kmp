package cn.enaium.ffmpeg.example

import kotlinx.coroutines.runBlocking

internal actual fun runBlockingDemo(block: suspend () -> Unit) {
    runBlocking { block() }
}
