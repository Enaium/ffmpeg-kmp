@file:Suppress("DEPRECATION")
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.maven.publish)
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    android {
        namespace = "cn.enaium.ffmpeg"
        compileSdk = 37
        minSdk = 21

        withHostTest {}

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }
        }
    }

    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
    mingwX64()

    androidNativeArm64()
    androidNativeArm32()
    androidNativeX64()
    androidNativeX86()

    js(IR) {
        nodejs()
    }
    wasmJs {
        nodejs()
    }

    sourceSets {
        getByName("commonMain") {
            dependencies {
                api(project(":ffmpeg-kmp:bridge"))
                api(project(":libavutil-kmp"))
                api(project(":libavcodec-kmp"))
                api(project(":libavformat-kmp"))
                api(project(":libavfilter-kmp"))
                api(project(":libswscale-kmp"))
                api(project(":libswresample-kmp"))
                api(project(":libavdevice-kmp"))
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        getByName("jvmTest") {
            dependencies {
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}


// ==================== npm package ====================
// The public @enaium/ffmpeg-kmp package ships the standalone Emscripten
// runtime, the wasm module and a small loader. Kotlin/JS and Kotlin/Wasm users
// keep the embedded klib path; plain JavaScript consumers instantiate the same
// runtime through initFFmpegKmp().
//
// The release scheme (for example 1.0.1-fixed.9.0.2) is already valid npm
// semver: three core components followed by a prerelease identifier.
val npmPackageVersion: String by lazy { rootProject.version.toString() }

data class NpmLibrary(
    val artifact: String,
    val library: String,
    val initFunction: String,
    val factoryName: String,
    val globalKey: String,
    val description: String,
)

val npmLibraries = listOf(
    NpmLibrary(
        "libavutil-kmp", "libavutil", "initLibavutilKmp", "createFFmpegModuleLibavutil",
        "__ffmpegWebLibavutil",
        "Emscripten-compiled FFmpeg libavutil module and JS glue for ffmpeg-kmp",
    ),
    NpmLibrary(
        "libavcodec-kmp", "libavcodec", "initLibavcodecKmp", "createFFmpegModuleLibavcodec",
        "__ffmpegWebLibavcodec",
        "Emscripten-compiled FFmpeg libavcodec module and JS glue for ffmpeg-kmp",
    ),
    NpmLibrary(
        "libavformat-kmp", "libavformat", "initLibavformatKmp", "createFFmpegModuleLibavformat",
        "__ffmpegWebLibavformat",
        "Emscripten-compiled FFmpeg libavformat module and JS glue for ffmpeg-kmp",
    ),
    NpmLibrary(
        "libavfilter-kmp", "libavfilter", "initLibavfilterKmp", "createFFmpegModuleLibavfilter",
        "__ffmpegWebLibavfilter",
        "Emscripten-compiled FFmpeg libavfilter module and JS glue for ffmpeg-kmp",
    ),
    NpmLibrary(
        "libswscale-kmp", "libswscale", "initLibswscaleKmp", "createFFmpegModuleLibswscale",
        "__ffmpegWebLibswscale",
        "Emscripten-compiled FFmpeg libswscale module and JS glue for ffmpeg-kmp",
    ),
    NpmLibrary(
        "libswresample-kmp", "libswresample", "initLibswresampleKmp", "createFFmpegModuleLibswresample",
        "__ffmpegWebLibswresample",
        "Emscripten-compiled FFmpeg libswresample module and JS glue for ffmpeg-kmp",
    ),
    NpmLibrary(
        "libavdevice-kmp", "libavdevice", "initLibavdeviceKmp", "createFFmpegModuleLibavdevice",
        "__ffmpegWebLibavdevice",
        "Emscripten-compiled FFmpeg libavdevice module and JS glue for ffmpeg-kmp",
    ),
    NpmLibrary(
        "ffmpeg-kmp", "ffmpeg", "initFFmpegKmp", "createFFmpegModule",
        "__ffmpegWeb",
        "Emscripten-compiled complete FFmpeg module and JS glue for ffmpeg-kmp",
    ),
)

val bridgeProject = rootProject.project(":ffmpeg-kmp:bridge")
val bridgeWebDir = bridgeProject.layout.buildDirectory.dir("web-async").get().asFile
val bridgeWebLibrariesDir = bridgeProject.layout.buildDirectory.dir("web-libs").get().asFile
val webGlueSource = rootProject.file(
    "ffmpeg-kmp/bridge/src/webMain/kotlin/cn/enaium/ffmpeg/native/FFmpegWebGlue.kt",
)
val wasmNpmDir = layout.buildDirectory.dir("npm").get().asFile

val webGlueText = webGlueSource.readText()
val glueMarker = "internal const val FFMPEG_WEB_GLUE_JS: String = \"\"\""
val glueStart = webGlueText.indexOf(glueMarker)
require(glueStart >= 0) { "FFMPEG_WEB_GLUE_JS is missing from $webGlueSource" }
val glueBodyStart = glueStart + glueMarker.length
val glueBodyEnd = webGlueText.indexOf("\n\"\"\"", glueBodyStart)
require(glueBodyEnd > glueBodyStart) { "FFMPEG_WEB_GLUE_JS is not closed in $webGlueSource" }
val webGlueInitializer = webGlueText.substring(glueBodyStart, glueBodyEnd).trim()
val moduleMarker = "const M = globalThis.Module;"
val moduleStart = webGlueInitializer.indexOf(moduleMarker)
require(moduleStart >= 0) { "FFMPEG_WEB_GLUE_JS has no Module hand-off" }
val webGlueHelperTail = webGlueInitializer.substring(
    moduleStart + moduleMarker.length,
    webGlueInitializer.lastIndexOf("}"),
)

fun librarySourceDir(library: NpmLibrary): File =
    if (library.library == "ffmpeg") bridgeWebDir
    else bridgeWebLibrariesDir.resolve(library.library)

fun libraryLinkTask(library: NpmLibrary): String =
    if (library.library == "ffmpeg") ":ffmpeg-kmp:bridge:linkFFmpegWasmAsync"
    else ":ffmpeg-kmp:bridge:linkFFmpegWasm${library.library.replaceFirstChar { it.uppercase() }}"

fun npmGlue(library: NpmLibrary): String {
    val helperTail = webGlueHelperTail.replace("__ffmpegWeb", library.globalKey)
    return """
        // Generated by :ffmpeg-kmp:assembleWasmNpm from FFmpegWebGlue.kt. Do not edit.
        // Emscripten module loader for @enaium/${library.artifact}.

        const decodeBase64 = (text) => {
          const raw = atob(text);
          const out = new Uint8Array(raw.length);
          for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
          return out;
        };
        const encodeBase64 = (data) => {
          let text = '';
          const chunk = 0x8000;
          for (let i = 0; i < data.length; i += chunk) {
            text += String.fromCharCode.apply(null, data.subarray(i, i + chunk));
          }
          return btoa(text);
        };

        const installFFmpeg = (M) => {
        $helperTail
        };

        const isNode = typeof process !== 'undefined' &&
          process.versions != null && process.versions.node != null;

        const loadBytes = async (source) => {
          if (source instanceof Uint8Array) return source;
          if (source instanceof ArrayBuffer) return new Uint8Array(source);
          if (isNode) {
            const { readFile } = await import('node:fs/promises');
            return new Uint8Array(await readFile(source));
          }
          const response = await fetch(source);
          if (!response.ok) {
            throw new Error('failed to load FFmpeg wasm asset: ' + response.status);
          }
          return new Uint8Array(await response.arrayBuffer());
        };

        const installRuntime = async (source) => {
          if (!isNode) {
            globalThis.eval(source);
            return;
          }
          // Emscripten's runtime is a classic script containing Node require()
          // calls. Eval inside an ES module is treated as module code by newer
          // Node versions, so run it as a CommonJS-style script instead.
          const { createRequire } = await import('node:module');
          const { runInThisContext } = await import('node:vm');
          const { fileURLToPath } = await import('node:url');
          globalThis.require = createRequire(import.meta.url);
          globalThis.__filename = fileURLToPath(import.meta.url);
          globalThis.__dirname = fileURLToPath(new URL('.', import.meta.url));
          runInThisContext(source, { filename: 'ffmpeg_kmp.js' });
        };

        /**
         * Instantiates the ${library.artifact} Emscripten module asynchronously.
         *
         * @param {object|string|URL|Uint8Array|ArrayBuffer} [sources] optional
         *   `{ runtime, wasm }` locations or bytes. The default uses the
         *   ffmpeg_kmp.js and ffmpeg_kmp.wasm files shipped next to this module.
         * @returns {Promise<object>} the call helper table.
         */
        export async function ${library.initFunction}(sources = {}) {
          if (globalThis.${library.globalKey}) return globalThis.${library.globalKey};
          if (sources instanceof Uint8Array || sources instanceof ArrayBuffer ||
              typeof sources === 'string' || sources instanceof URL) {
            sources = { wasm: sources };
          }
          const runtimeSource = sources.runtime === undefined
            ? new URL('./ffmpeg_kmp.js', import.meta.url)
            : sources.runtime;
          const wasmSource = sources.wasm === undefined
            ? new URL('./ffmpeg_kmp.wasm', import.meta.url)
            : sources.wasm;
          const runtime = new TextDecoder().decode(await loadBytes(runtimeSource));
          const wasm = await loadBytes(wasmSource);
          await installRuntime(runtime);
          const factory = globalThis.${library.factoryName};
          if (typeof factory !== 'function') {
            throw new Error('ffmpeg_kmp.js did not export ${library.factoryName}');
          }
          installFFmpeg(await factory({ wasmBinary: wasm }));
          return globalThis.${library.globalKey};
        }

        export default ${library.initFunction};
    """.trimIndent() + "\n"
}

fun npmPackageJson(library: NpmLibrary): String = """
    {
      "name": "@enaium/${library.artifact}",
      "version": "$npmPackageVersion",
      "description": "${library.description}",
      "license": "LGPL-2.1-or-later",
      "type": "module",
      "exports": {
        ".": "./ffmpeg_kmp_glue.js",
        "./ffmpeg_kmp.js": "./ffmpeg_kmp.js",
        "./ffmpeg_kmp.wasm": "./ffmpeg_kmp.wasm",
        "./package.json": "./package.json"
      },
      "files": ["ffmpeg_kmp_glue.js", "ffmpeg_kmp.js", "ffmpeg_kmp.wasm", "LICENSE"],
      "homepage": "https://github.com/Enaium/ffmpeg-kmp",
      "repository": {
        "type": "git",
        "url": "git+https://github.com/Enaium/ffmpeg-kmp.git"
      },
      "bugs": {
        "url": "https://github.com/Enaium/ffmpeg-kmp/issues"
      },
      "keywords": ["ffmpeg", "wasm", "webassembly", "video", "audio", "${library.library}"]
    }
""".trimIndent() + "\n"

tasks.register("assembleWasmNpm") {
    group = "build"
    description = "Stages the per-library and aggregate FFmpeg npm packages in build/npm."
    dependsOn(":ffmpeg-kmp:bridge:linkFFmpegWasmAsync")
    npmLibraries.forEach { dependsOn(libraryLinkTask(it)) }
    inputs.file(webGlueSource)
    npmLibraries.forEach { library ->
        inputs.files(
            librarySourceDir(library).resolve("ffmpeg_kmp.js"),
            librarySourceDir(library).resolve("ffmpeg_kmp.wasm"),
        )
    }
    inputs.property("npmPackageVersion", npmPackageVersion)
    outputs.dir(wasmNpmDir)
    doLast {
        wasmNpmDir.deleteRecursively()
        wasmNpmDir.mkdirs()
        npmLibraries.forEach { library ->
            val packageDir = wasmNpmDir.resolve(library.artifact)
            val sourceDir = librarySourceDir(library)
            packageDir.mkdirs()
            sourceDir.resolve("ffmpeg_kmp.js").copyTo(packageDir.resolve("ffmpeg_kmp.js"), overwrite = true)
            sourceDir.resolve("ffmpeg_kmp.wasm").copyTo(packageDir.resolve("ffmpeg_kmp.wasm"), overwrite = true)
            rootProject.file("LICENSE").copyTo(packageDir.resolve("LICENSE"), overwrite = true)
            packageDir.resolve("ffmpeg_kmp_glue.js").writeText(npmGlue(library))
            packageDir.resolve("package.json").writeText(npmPackageJson(library))
        }
    }
}

fun registerNpmPublish(library: NpmLibrary) =
    tasks.register<Exec>("publishWasmNpm${library.library.replaceFirstChar { it.uppercase() }}") {
        group = "publishing"
        description = "Publishes @enaium/${library.artifact} to npm (token: npm.publish.key)."
        dependsOn("assembleWasmNpm")
        workingDir = wasmNpmDir.resolve(library.artifact)
        val npmKey = providers.gradleProperty("npm.publish.key")
        doFirst {
            val key = npmKey.orNull?.takeIf { it.isNotBlank() }
                ?: throw GradleException(
                    "npm.publish.key is not set: add it to ~/.gradle/gradle.properties " +
                        "or pass -Pnpm.publish.key=<token>",
                )
            environment("NODE_AUTH_TOKEN", key)
            workingDir.resolve(".npmrc").writeText(
                "//registry.npmjs.org/:_authToken=\${NODE_AUTH_TOKEN}\n",
            )
        }
        commandLine("npm", "publish", "--access", "public", "--tag", "latest")
    }

val npmPublishTasks = npmLibraries.map(::registerNpmPublish)

tasks.register("publishWasmNpm") {
    group = "publishing"
    description = "Publishes every FFmpeg npm package to npm (token: npm.publish.key)."
    dependsOn(npmPublishTasks)
}

// ==================== Publishing ====================

val hasSigningKey = rootProject.extra["hasSigningKey"] as Boolean

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (hasSigningKey) signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "ffmpeg-kmp",
        version = null,
    )

    pom {
        name.set("ffmpeg-kmp")
        description.set(
            "Kotlin Multiplatform aggregate for the FFmpeg bindings: libavutil, " +
                "libavcodec, libavformat, libavfilter, libswscale, libswresample " +
                "and libavdevice, with the shared native bridge as a dependency.",
        )
        url.set("https://github.com/Enaium/ffmpeg-kmp")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("LGPL-2.1-or-later")
                url.set("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("Enaium")
            }
        }

        scm {
            url.set("https://github.com/Enaium/ffmpeg-kmp")
            connection.set("scm:git:git@github.com:Enaium/ffmpeg-kmp.git")
            developerConnection.set("scm:git:git@github.com:Enaium/ffmpeg-kmp.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/Enaium/ffmpeg-kmp/issues")
        }
    }
}
