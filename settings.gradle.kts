pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ffmpeg-kmp"

include(":libavutil-kmp")
include(":libavcodec-kmp")
include(":libavformat-kmp")
include(":libavfilter-kmp")
include(":libswscale-kmp")
include(":libswresample-kmp")
include(":libavdevice-kmp")
include(":ffmpeg-kmp")
include(":ffmpeg-kmp:bridge")

include(":examples:ffmpeg-core")
include(":examples:camera-ffmpeg")
include(":examples:camera-ffmpeg:android")

// Per-OS/arch JNI artifacts that ship the prebuilt libffmpeg_jni shared library
// as a classpath resource. FFmpegJniLoader extracts the matching one at runtime.
listOf(
    "linux-x86_64",
    "linux-aarch64",
    "darwin-x86_64",
    "darwin-aarch64",
    "windows-x86_64",
).forEach { classifier ->
    val name = ":jni-jvm-$classifier"
    include(name)
    project(name).projectDir = file("jni/jvm/$classifier")
}
