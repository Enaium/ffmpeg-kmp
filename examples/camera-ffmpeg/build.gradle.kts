@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import java.io.File
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

/**
 * Kotlin/Native's Android toolchain sysroot provides the NDK stub libraries
 * (libEGL, libGLESv2, libandroid, ...) that SDL3 and camera-kmp reference at
 * link time, without requiring a full NDK install.
 */
fun konanAndroidLibDir(abi: String): String? {
    val konanData = System.getenv("KONAN_DATA_DIR")
        ?: providers.gradleProperty("konan.data.dir").getOrElse("${System.getProperty("user.home")}/.konan")
    val toolchain = File(konanData, "dependencies").listFiles()
        ?.firstOrNull { it.isDirectory && it.name.matches(Regex("target-toolchain-.*-android_ndk")) }
        ?: return null
    val triple = when (abi) {
        "arm64-v8a" -> "aarch64-linux-android"
        "armeabi-v7a" -> "arm-linux-androideabi"
        "x86_64" -> "x86_64-linux-android"
        "x86" -> "i686-linux-android"
        else -> return null
    }
    return "$toolchain/sysroot/usr/lib/$triple/26"
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_22)
        }
        mainRun {
            mainClass = "cn.enaium.ffmpeg.example.Main_jvmKt"
        }
    }

    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName = "camera-ffmpeg.js"
            }
        }
    }

    macosArm64 { binaries.executable() }
    macosX64 { binaries.executable() }

    linuxX64 {
        binaries.executable {
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }
    linuxArm64 {
        binaries.executable {
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }

    mingwX64 { binaries.executable() }

    androidNativeArm64 {
        binaries.sharedLib("main") {
            konanAndroidLibDir("arm64-v8a")?.let { linkerOpts("-L$it") }
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }
    androidNativeArm32 {
        binaries.sharedLib("main") {
            konanAndroidLibDir("armeabi-v7a")?.let { linkerOpts("-L$it") }
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }
    androidNativeX64 {
        binaries.sharedLib("main") {
            konanAndroidLibDir("x86_64")?.let { linkerOpts("-L$it") }
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }
    androidNativeX86 {
        binaries.sharedLib("main") {
            konanAndroidLibDir("x86")?.let { linkerOpts("-L$it") }
            linkerOpts("-Wl,--allow-multiple-definition")
        }
    }

    // The root build disables the automatic hierarchy template; add the shared
    // entry-point directories to each native compilation by hand.
    targets.withType<KotlinNativeTarget> {
        val sources = compilations.getByName("main").defaultSourceSet.kotlin
        sources.srcDir("src/nativeMain/kotlin")
        if (name.startsWith("androidNative")) sources.srcDir("src/androidNativeMain/kotlin")
    }

    sourceSets {
        getByName("commonMain") {
            dependencies {
                api(project(":examples:ffmpeg-core"))
                implementation(libs.sdl.kmp)
                implementation(libs.imgui.kmp)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        getByName("wasmJsMain") {
            dependencies {
                implementation(libs.kotlinx.browser)
                implementation(npm("@enaium/sdl-kmp", "3.4.18-2"))
                implementation(npm("@enaium/imgui-kmp", "1.93.0-2"))
                // getUserMedia glue for camera-kmp's browser backend; the page
                // loads it before the Kotlin module (see index.html).
                implementation(npm("@enaium/camera-kmp", libs.versions.camera.get()))
            }
        }
    }
}


// The Emscripten SDL/ImGui runtimes and camera-kmp's getUserMedia glue are
// shipped as npm packages. Copy their files next to the Kotlin/Wasm output so
// the static page can initialize SDL/ImGui and install the camera globals
// before importing the camera bundle.
val copiedWasmAssets = layout.buildDirectory.dir("generated/wasmAssets")
val wasmNodeModules = rootProject.layout.buildDirectory.dir("wasm/node_modules")

val copyWasmAssets = tasks.register<Copy>("copyWasmAssets") {
    dependsOn(":kotlinWasmNpmInstall")
    dependsOn(":ffmpeg-kmp:assembleWasmNpm")
    from(wasmNodeModules.map { it.dir("@enaium/sdl-kmp") }) {
        include("sdl_kmp_glue.js", "sdl_wasm.js", "sdl_wasm.wasm")
    }
    from(wasmNodeModules.map { it.dir("@enaium/imgui-kmp") }) {
        include("imgui_kmp_glue.js", "imgui_kmp_exports.js", "imgui_wasm.js", "imgui_wasm.wasm")
    }
    from(rootProject.file("ffmpeg-kmp/build/npm")) {
        include("ffmpeg_kmp_glue.js", "ffmpeg_kmp.js", "ffmpeg_kmp.wasm")
    }
    from(wasmNodeModules.map { it.dir("@enaium/camera-kmp") }) {
        include("camera-kmp-web.js")
    }
    into(copiedWasmAssets)
}

tasks.named<Copy>("wasmJsProcessResources") {
    dependsOn(copyWasmAssets)
    from(copiedWasmAssets)
}

tasks.withType<JavaExec>().configureEach {
    if (name == "jvmRun") {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
            jvmArgs("-XstartOnFirstThread")
        }
    }
}
