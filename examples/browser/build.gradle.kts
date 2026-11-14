plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName = "browser-demo.js"
            }
        }
    }

    sourceSets {
        getByName("wasmJsMain") {
            dependencies {
                implementation(project(":examples:camera-ffmpeg"))
                implementation(libs.kotlinx.browser)
                implementation(npm("@enaium/sdl-kmp", "3.4.18-2"))
                implementation(npm("@enaium/imgui-kmp", "1.93.0-2"))
            }
        }
    }
}

// The Emscripten SDL/ImGui runtimes are shipped as npm packages. Copy their
// glue, JS and wasm files next to the Kotlin/Wasm output so the static page can
// initialize both modules before importing browser-demo.js.
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
    into(copiedWasmAssets)
}

tasks.named<Copy>("wasmJsProcessResources") {
    dependsOn(copyWasmAssets)
    from(copiedWasmAssets)
}
