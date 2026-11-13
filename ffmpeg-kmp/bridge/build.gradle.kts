@file:Suppress("DEPRECATION")
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import java.io.File
import java.util.Base64
import java.util.Properties
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.maven.publish)
}

group = rootProject.group
version = rootProject.version

val jniDir = rootProject.file("jni")
val ffmpegDir = rootProject.file("ffmpeg")
val hostOs = OperatingSystem.current()
val cmakeExecutable: String = rootProject.extra["cmakeExecutable"] as String
val androidNdk: File? = (rootProject.extra["androidNdkDir"] as String?)?.let(::File)
val androidNdkHost: String? = rootProject.extra["androidNdkHost"] as String?
val androidApiLevel = 21
val makeJobs = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

fun msys2Root(): String =
    System.getenv("MSYS2_ROOT")?.takeIf { it.isNotBlank() } ?: "C:/msys64"

fun windowsTool(name: String): String = listOf(
    File("C:/mingw64/bin/$name.exe"),
    File(msys2Root(), "mingw64/bin/$name.exe"),
).firstOrNull { it.isFile }
    ?.absolutePath
    ?.replace('\\', '/')
    ?: resolveTool(name)

fun msys2Path(): String = listOf(
    File("C:/mingw64/bin").absolutePath,
    File(msys2Root(), "mingw64/bin").absolutePath,
    File(msys2Root(), "usr/bin").absolutePath,
    System.getenv("PATH").orEmpty(),
).joinToString(File.pathSeparator)

fun windowsBash(): String {
    val msysRoot = msys2Root()
    return listOf(
        File(msysRoot, "usr/bin/bash.exe"),
        File("C:/Program Files/Git/bin/bash.exe"),
        File("C:/Program Files/Git/usr/bin/bash.exe"),
    ).firstOrNull { it.isFile }?.absolutePath ?: "bash"
}

// ==================== Target capabilities ====================

fun hasExecutable(name: String): Boolean =
    System.getenv("PATH")?.split(File.pathSeparator)
        ?.map { File(it, name) }
        ?.any { it.isFile && it.canExecute() } == true

fun resolveTool(name: String): String =
    System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
        .map { File(it, name) }
        .firstOrNull { it.isFile && it.canExecute() }
        ?.absolutePath
        ?: name

fun canBuildNativeTarget(targetName: String): Boolean = when {
    hostOs.isMacOsX && targetName.startsWith("macos") -> true
    hostOs.isLinux && targetName == "linuxX64" ->
        System.getProperty("os.arch").lowercase() in listOf("amd64", "x86_64", "x64") ||
            hasExecutable("x86_64-linux-gnu-gcc")
    hostOs.isLinux && targetName == "linuxArm64" ->
        System.getProperty("os.arch").lowercase() in listOf("aarch64", "arm64") ||
            hasExecutable("aarch64-linux-gnu-gcc")
    hostOs.isLinux && targetName == "mingwX64" ->
        hasExecutable("x86_64-w64-mingw32-gcc")
    hostOs.isWindows && targetName == "mingwX64" -> true
    targetName.startsWith("androidNative") && androidNdk != null && androidNdkHost != null -> true
    else -> false
}

// ==================== FFmpeg targets ====================

class FFmpegTarget(
    val targetName: String,
    val configureArgs: List<String>,
    val cmakeArgs: List<String>,
    val canBuild: Boolean,
)

fun commonConfigureArgs(buildDir: File): List<String> = listOf(
    "--prefix=${buildDir.absolutePath}/prefix",
    "--disable-programs",
    "--disable-doc",
    "--disable-debug",
    "--enable-static",
    "--disable-shared",
    "--enable-pic",
    "--disable-autodetect",
    "--disable-asm",
    "--enable-avdevice",
    "--enable-avfilter",
    "--enable-swscale",
    "--enable-swresample",
)

val androidAbis = listOf(
    Triple("arm64-v8a", "androidNativeArm64", "aarch64-linux-android"),
    Triple("armeabi-v7a", "androidNativeArm32", "armv7a-linux-androideabi"),
    Triple("x86", "androidNativeX86", "i686-linux-android"),
    Triple("x86_64", "androidNativeX64", "x86_64-linux-android"),
)

val ffmpegTargets = buildList {
    // macOS: clang can target both architectures from either host.
    add(
        FFmpegTarget(
            "macosArm64",
            commonConfigureArgs(rootProject.layout.buildDirectory.dir("ffmpeg/macosArm64").get().asFile) +
                listOf(
                    "--target-os=darwin",
                    "--arch=arm64",
                    "--cc=clang",
                    "--cxx=clang++",
                    "--enable-avfoundation",
                ),
            listOf("-DCMAKE_OSX_ARCHITECTURES=arm64"),
            hostOs.isMacOsX,
        ),
    )
    add(
        FFmpegTarget(
            "macosX64",
            commonConfigureArgs(rootProject.layout.buildDirectory.dir("ffmpeg/macosX64").get().asFile) +
                listOf(
                    "--target-os=darwin",
                    "--arch=x86_64",
                    "--cc=clang",
                    "--cxx=clang++",
                    "--host-cc=clang",
                    "--extra-cflags=-arch x86_64",
                    "--extra-ldflags=-arch x86_64",
                    "--enable-avfoundation",
                ),
            listOf("-DCMAKE_OSX_ARCHITECTURES=x86_64"),
            hostOs.isMacOsX,
        ),
    )

    // Linux
    val hostIsX64 = System.getProperty("os.arch").lowercase() in
        listOf("amd64", "x86_64", "x64")
    val linuxX64Args = buildList {
        addAll(
            commonConfigureArgs(
                rootProject.layout.buildDirectory.dir("ffmpeg/linuxX64").get().asFile,
            ),
        )
        add("--target-os=linux")
        add("--arch=x86_64")
        // Kotlin/Native's Linux sysroot has no libmvec; keep gcc from
        // vectorising log/atan2 into calls to it.
        add("--extra-cflags=-fno-tree-vectorize")
        if (hostIsX64) {
            add("--cc=cc")
            add("--cxx=c++")
        } else {
            add("--enable-cross-compile")
            add("--cross-prefix=x86_64-linux-gnu-")
            add("--cc=x86_64-linux-gnu-gcc")
            add("--cxx=x86_64-linux-gnu-g++")
        }
    }
    val linuxX64CmakeArgs = if (hostIsX64) {
        emptyList()
    } else {
        listOf(
            "-DCMAKE_SYSTEM_NAME=Linux",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_C_COMPILER=${resolveTool("x86_64-linux-gnu-gcc")}",
            "-DCMAKE_CXX_COMPILER=${resolveTool("x86_64-linux-gnu-g++")}",
            "-DCMAKE_AR=${resolveTool("x86_64-linux-gnu-ar")}",
            "-DCMAKE_RANLIB=${resolveTool("x86_64-linux-gnu-ranlib")}",
        )
    }
    add(
        FFmpegTarget(
            "linuxX64",
            linuxX64Args,
            linuxX64CmakeArgs,
            hostOs.isLinux &&
                (hostIsX64 || hasExecutable("x86_64-linux-gnu-gcc")),
        ),
    )
    val linuxArm64Args = buildList {
        addAll(
            commonConfigureArgs(
                rootProject.layout.buildDirectory.dir("ffmpeg/linuxArm64").get().asFile,
            ),
        )
        add("--target-os=linux")
        add("--arch=aarch64")
        // See the linuxX64 comment: this also disables outlined atomics, whose
        // helpers are missing from Kotlin/Native's older aarch64 sysroot.
        add("--extra-cflags=-fno-tree-vectorize -mno-outline-atomics")
        if (System.getProperty("os.arch").lowercase() in listOf("aarch64", "arm64")) {
            add("--cc=cc")
            add("--cxx=c++")
        } else {
            add("--enable-cross-compile")
            add("--cross-prefix=aarch64-linux-gnu-")
            add("--cc=aarch64-linux-gnu-gcc")
            add("--cxx=aarch64-linux-gnu-g++")
        }
    }
    val linuxArm64CmakeArgs =
        if (System.getProperty("os.arch").lowercase() in listOf("aarch64", "arm64")) {
            emptyList()
        } else {
            listOf(
                "-DCMAKE_SYSTEM_NAME=Linux",
                "-DCMAKE_SYSTEM_PROCESSOR=aarch64",
                "-DCMAKE_C_COMPILER=${resolveTool("aarch64-linux-gnu-gcc")}",
                "-DCMAKE_CXX_COMPILER=${resolveTool("aarch64-linux-gnu-g++")}",
                "-DCMAKE_AR=${resolveTool("aarch64-linux-gnu-ar")}",
                "-DCMAKE_RANLIB=${resolveTool("aarch64-linux-gnu-ranlib")}",
            )
        }
    add(
        FFmpegTarget(
            "linuxArm64",
            linuxArm64Args,
            linuxArm64CmakeArgs,
            hostOs.isLinux &&
                (System.getProperty("os.arch").lowercase() in listOf("aarch64", "arm64") ||
                    hasExecutable("aarch64-linux-gnu-gcc")),
        ),
    )

    // Windows: MinGW. Cross-build on Linux, native MSYS2 on Windows.
    val mingwArgs = buildList {
        addAll(
            commonConfigureArgs(
                rootProject.layout.buildDirectory.dir("ffmpeg/mingwX64").get().asFile,
            ),
        )
        add("--target-os=mingw32")
        add("--arch=x86_64")
        if (hostOs.isWindows) {
            add("--cc=gcc")
            add("--cxx=g++")
        } else {
            add("--enable-cross-compile")
            add("--cross-prefix=x86_64-w64-mingw32-")
            add("--cc=x86_64-w64-mingw32-gcc")
            add("--cxx=x86_64-w64-mingw32-g++")
        }
        add("--enable-indev=dshow")
    }
    val msysMake = File(msys2Root(), "mingw64/bin/ninja.exe").absolutePath.replace('\\', '/')
    val mingwCmakeArgs = if (hostOs.isWindows) {
        listOf(
            "-G", "Ninja",
            "-DCMAKE_C_COMPILER=${windowsTool("gcc")}",
            "-DCMAKE_AR=${windowsTool("ar")}",
            "-DCMAKE_RANLIB=${windowsTool("ranlib")}",
            "-DCMAKE_MAKE_PROGRAM=$msysMake",
        )
    } else {
        listOf(
            "-DCMAKE_SYSTEM_NAME=Windows",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_C_COMPILER=${resolveTool("x86_64-w64-mingw32-gcc")}",
            "-DCMAKE_AR=${resolveTool("x86_64-w64-mingw32-ar")}",
            "-DCMAKE_RANLIB=${resolveTool("x86_64-w64-mingw32-ranlib")}",
        )
    }
    add(
        FFmpegTarget(
            "mingwX64",
            mingwArgs,
            mingwCmakeArgs,
            hostOs.isWindows ||
                (hostOs.isLinux && hasExecutable("x86_64-w64-mingw32-gcc")),
        ),
    )

    // Android NDK
    if (androidNdk != null && androidNdkHost != null) {
        val toolchainBin = androidNdk.resolve("toolchains/llvm/prebuilt/$androidNdkHost/bin")
        androidAbis.forEach { (abi, targetName, triple) ->
            val arch = when (abi) {
                "arm64-v8a" -> "aarch64"
                "armeabi-v7a" -> "arm"
                "x86" -> "x86"
                else -> "x86_64"
            }
            val cc = toolchainBin.resolve("$triple$androidApiLevel-clang").absolutePath
            val cxx = "$cc++"
            val args = buildList {
                addAll(
                    commonConfigureArgs(
                        rootProject.layout.buildDirectory.dir("ffmpeg/$targetName").get().asFile,
                    ),
                )
                add("--enable-cross-compile")
                add("--target-os=android")
                add("--arch=$arch")
                if (abi == "armeabi-v7a") add("--cpu=armv7-a")
                add("--cc=$cc")
                add("--cxx=$cxx")
                add("--ar=${toolchainBin.resolve("llvm-ar").absolutePath}")
                add("--ranlib=${toolchainBin.resolve("llvm-ranlib").absolutePath}")
                add("--strip=${toolchainBin.resolve("llvm-strip").absolutePath}")
                add("--nm=${toolchainBin.resolve("llvm-nm").absolutePath}")
                // Android ARM: Kotlin/Native's runtime predates the NDK's
                // outlined atomics helpers.
                add(
                    if (arch == "aarch64" || arch == "arm") {
                        "--extra-cflags=-fPIC -mno-outline-atomics"
                    } else {
                        "--extra-cflags=-fPIC"
                    },
                )
            }
            add(
                FFmpegTarget(
                    targetName,
                    args,
                    listOf(
                        "-DCMAKE_TOOLCHAIN_FILE=${androidNdk.resolve("build/cmake/android.toolchain.cmake").absolutePath}",
                        "-DANDROID_ABI=$abi",
                        "-DANDROID_PLATFORM=android-$androidApiLevel",
                        "-DCMAKE_C_FLAGS=-mno-outline-atomics",
                    ),
                    androidNdk.resolve("build/cmake/android.toolchain.cmake").isFile,
                ),
            )
        }
    }
}

val ffmpegTargetsByName = ffmpegTargets.associateBy { it.targetName }

// ==================== FFmpeg configure/make ====================

fun ffmpegBuildDir(targetName: String): File =
    rootProject.layout.buildDirectory.dir("ffmpeg/$targetName").get().asFile

val ffmpegBuildTasks = mutableMapOf<String, TaskProvider<Exec>>()

ffmpegTargets.forEach { target ->
    val buildDir = ffmpegBuildDir(target.targetName)
    val script = buildDir.resolve("build.sh")
    val task = tasks.register<Exec>("buildFFmpeg_${target.targetName}") {
        group = "build"
        description = "Builds FFmpeg static libraries for ${target.targetName}."
        onlyIf { target.canBuild }
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
        doFirst {
            buildDir.mkdirs()
            val configure = ffmpegDir.resolve("configure").absolutePath.replace('\\', '/')
            val workingDir = buildDir.absolutePath.replace('\\', '/')
            val args = target.configureArgs.joinToString(" ") { argument ->
                "\"" + argument.replace("\\", "/").replace("\"", "\\\"") + "\""
            }
            script.writeText(
                """
                #!/bin/sh
                set -e
                cd "$workingDir"
                "$configure" $args
                if command -v cygpath >/dev/null 2>&1; then
                  src_win="$(cygpath -m "$(dirname "$configure")")"
                  src_posix="$(cygpath -u "${'$'}src_win")"
                  if [ ! -f Makefile ]; then echo "configure did not create Makefile"; exit 1; fi
                  if [ ! -f "$(dirname "$configure")/Makefile" ]; then
                    echo "source Makefile is missing: $(dirname "$configure")/Makefile"
                    ls -la "$(dirname "$configure")" | head
                    exit 1
                  fi
                  sed -i "s|${'$'}{src_posix}|${'$'}{src_win}|g" Makefile
                  [ -f ffbuild/config.mak ] && sed -i "s|${'$'}{src_posix}|${'$'}{src_win}|g" ffbuild/config.mak
                  [ -L src ] && ln -sfn "${'$'}src_win" src
                fi
                make -j$makeJobs
                """.trimIndent() + "\n",
            )
            script.setExecutable(true)
        }
        if (hostOs.isWindows) {
            commandLine(windowsBash(), script.absolutePath.replace('\\', '/'))
        } else {
            commandLine("sh", script.absolutePath)
        }
    }
    ffmpegBuildTasks[target.targetName] = task
}

val generatedWebDir = layout.buildDirectory.dir("generated/ffmpegWeb/kotlin")

// ==================== Kotlin Multiplatform ====================

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
        namespace = "cn.enaium.ffmpeg.bridge"
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

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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

    targets.withType<KotlinNativeTarget>().configureEach {
        val targetName = name
        val canBuild = canBuildNativeTarget(targetName)
        compilations.getByName("main") {
            cinterops.create("ffmpeg_kmp") {
                defFile(project.file("src/nativeInterop/cinterop/ffmpeg_kmp.def"))
                includeDirs(
                    project.file("src/nativeInterop/cinterop"),
                    jniDir.resolve("include"),
                )
                if (canBuild) {
                    val outputDir = layout.buildDirectory.dir("native/$targetName").get().asFile
                    extraOpts(
                        "-libraryPath", outputDir.absolutePath,
                        "-staticLibrary", "libffmpeg_kmp.a",
                    )
                }
            }
        }
    }

    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        getByName("jvmMain") {
            dependencies {
                // One JNI library serves every FFmpeg module. Bundle each
                // platform artifact so consumers get the matching binary.
                runtimeOnly(project(":jni-jvm-linux-x86_64"))
                runtimeOnly(project(":jni-jvm-linux-aarch64"))
                runtimeOnly(project(":jni-jvm-darwin-x86_64"))
                runtimeOnly(project(":jni-jvm-darwin-aarch64"))
                runtimeOnly(project(":jni-jvm-windows-x86_64"))
            }
        }

        getByName("jvmTest") {
            dependencies {
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }

        getByName("androidMain") {
            kotlin.srcDir("src/jvmMain/kotlin")
        }

        val commonMain = getByName("commonMain")
        val commonTest = getByName("commonTest")
        val nativeMain = maybeCreate("nativeMain").apply { dependsOn(commonMain) }
        val nativeTest = maybeCreate("nativeTest").apply { dependsOn(commonTest) }
        val webMain = maybeCreate("webMain").apply {
            dependsOn(commonMain)
            kotlin.srcDir(generatedWebDir)
        }
        val webTest = maybeCreate("webTest").apply { dependsOn(commonTest) }

        getByName("jvmTest").dependsOn(commonTest)
        getByName("androidHostTest").dependsOn(commonTest)
        getByName("jsMain").dependsOn(webMain)
        getByName("wasmJsMain").dependsOn(webMain)
        getByName("jsTest").dependsOn(webTest)
        getByName("wasmJsTest").dependsOn(webTest)
    }
}

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    compilations.getByName("main").defaultSourceSet.dependsOn(
        kotlin.sourceSets.getByName("nativeMain"),
    )
    compilations.getByName("test").defaultSourceSet.dependsOn(
        kotlin.sourceSets.getByName("nativeTest"),
    )
}

// ==================== Native archive + cinterop ====================

fun registerNativeArchive(target: FFmpegTarget) {
    val targetName = target.targetName
    val outputDir = layout.buildDirectory.dir("native/$targetName").get().asFile
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-$targetName").get().asFile
    val ffmpegBuildDir = ffmpegBuildDir(targetName)
    val archive = File(outputDir, "libffmpeg_kmp.a")

    val configureTask = tasks.register<Exec>("configureNative_$targetName") {
        onlyIf { target.canBuild }
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
        dependsOn(ffmpegBuildTasks.getValue(targetName))
        workingDir = cmakeBuildDir
        if (hostOs.isWindows) environment("PATH", msys2Path())
        doFirst {
            cmakeBuildDir.mkdirs()
            outputDir.mkdirs()
        }
        commandLine(
            listOf(
                cmakeExecutable,
                jniDir.absolutePath,
                "-DCMAKE_BUILD_TYPE=Release",
                "-DBUILD_JNI=OFF",
                "-DBUILD_ARCHIVE=ON",
                "-DFFMPEG_SOURCE_DIR=${ffmpegDir.absolutePath}",
                "-DFFMPEG_BUILD_DIR=${ffmpegBuildDir.absolutePath}",
                "-DARCHIVE_OUTPUT_DIR=${outputDir.absolutePath}",
            ) + target.cmakeArgs,
        )
    }

    val buildTask = tasks.register<Exec>("buildNativeArchive_$targetName") {
        onlyIf { target.canBuild }
        dependsOn(configureTask)
        workingDir = cmakeBuildDir
        if (hostOs.isWindows) environment("PATH", msys2Path())
        commandLine(cmakeExecutable, "--build", ".", "--target", "ffmpeg_kmp_archive", "--config", "Release")
        inputs.file(jniDir.resolve("ffmpeg_kmp.c"))
        inputs.file(jniDir.resolve("include/ffmpeg_kmp.h"))
        inputs.file(jniDir.resolve("CMakeLists.txt"))
        inputs.file(jniDir.resolve("merge_archives.cmake"))
        outputs.file(archive)
    }

    if (target.canBuild) {
        tasks.matching {
            it.name.startsWith("cinterop") &&
                it.name.lowercase().contains("ffmpeg_kmp") &&
                it.name.endsWith(targetName.replaceFirstChar { c -> c.uppercase() })
        }.configureEach {
            dependsOn(buildTask)
            inputs.file(archive)
        }
    }
}

ffmpegTargets.forEach { registerNativeArchive(it) }

// ==================== WebAssembly ====================

fun resolveEmsdk(): File? {
    System.getenv("EMSDK")?.takeIf { it.isNotBlank() }?.let {
        val dir = File(it)
        if (dir.isDirectory) return dir
    }
    val home = System.getProperty("user.home")
    return listOf("$home/emsdk", "$home/.emsdk").map(::File).firstOrNull { it.isDirectory }
}

fun resolveEmscripten(command: String): String {
    System.getenv("PATH")?.split(File.pathSeparator).orEmpty().forEach { dir ->
        val file = File(dir, command)
        if (file.isFile && file.canExecute()) return file.absolutePath
    }
    val sdk = resolveEmsdk()
    val candidate = sdk?.resolve("upstream/emscripten/$command")
    if (candidate != null && candidate.isFile) return candidate.absolutePath
    return command
}

fun Exec.configureEmscripten() {
    val sdk = resolveEmsdk() ?: return
    environment("EMSDK", sdk.absolutePath)
    sdk.resolve(".emscripten").takeIf { it.isFile }?.let { file ->
        environment("EM_CONFIG", file.absolutePath)
    }
    val emscripten = sdk.resolve("upstream/emscripten")
    val upstreamBin = sdk.resolve("upstream/bin")
    val path = buildString {
        append(upstreamBin.absolutePath)
        append(File.pathSeparator).append(emscripten.absolutePath)
        System.getenv("PATH")?.let { append(File.pathSeparator).append(it) }
    }
    environment("PATH", path)
}

val emccExecutable = resolveEmscripten("emcc")
val emconfigureExecutable = resolveEmscripten("emconfigure")
val emmakeExecutable = resolveEmscripten("emmake")
val canBuildWebWasm = File(emccExecutable).isFile || hasExecutable("emcc")

val ffmpegWasmDir = rootProject.layout.buildDirectory.dir("ffmpeg/wasm").get().asFile
val webOutputDir = layout.buildDirectory.dir("web").get().asFile
val webExportsFile = webOutputDir.resolve("exports.txt")

fun writeWebExports(file: File) {
    val regex = Regex("fk_[a-z0-9_]+\\(")
    val functions = jniDir.resolve("include/ffmpeg_kmp.h").readLines()
        .flatMap { line -> regex.findAll(line).map { match -> "_" + match.value.dropLast(1) } }
        .distinct()
        .sorted()
    file.writeText((functions + listOf("_malloc", "_free")).joinToString("\n") + "\n")
}

val configureWebWasm = tasks.register<Exec>("configureFFmpegWasm") {
    group = "build"
    description = "Configures FFmpeg for WebAssembly with Emscripten."
    onlyIf { canBuildWebWasm }
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    workingDir = ffmpegWasmDir
    configureEmscripten()
    doFirst {
        ffmpegWasmDir.mkdirs()
        webOutputDir.mkdirs()
        writeWebExports(webExportsFile)
    }
    commandLine(
        listOf(
            emconfigureExecutable,
            ffmpegDir.resolve("configure").absolutePath,
            "--target-os=none",
            "--arch=wasm32",
            "--enable-cross-compile",
            "--cc=emcc",
            "--cxx=em++",
            "--ar=emar",
            "--ranlib=emranlib",
            "--nm=llvm-nm",
            "--strip=llvm-strip",
            "--disable-programs",
            "--disable-doc",
            "--disable-debug",
            "--enable-static",
            "--disable-shared",
            "--disable-autodetect",
            "--disable-asm",
            "--disable-network",
            "--enable-avdevice",
            "--enable-avfilter",
            "--enable-swscale",
            "--enable-swresample",
        ),
    )
    inputs.files(
        ffmpegDir.resolve("configure"),
        jniDir.resolve("include/ffmpeg_kmp.h"),
    )
    outputs.file(ffmpegWasmDir.resolve("ffbuild/config.mak"))
    outputs.upToDateWhen { false }
}

val buildWebWasm = tasks.register<Exec>("buildFFmpegWasm") {
    group = "build"
    description = "Builds the FFmpeg static libraries for WebAssembly."
    onlyIf { canBuildWebWasm }
    outputs.cacheIf { false }
    dependsOn(configureWebWasm)
    workingDir = ffmpegWasmDir
    configureEmscripten()
    commandLine(emmakeExecutable, "make", "-j$makeJobs")
    inputs.files(
        ffmpegDir.resolve("configure"),
        jniDir.resolve("include/ffmpeg_kmp.h"),
    )
    outputs.file(ffmpegWasmDir.resolve("libavcodec/libavcodec.a"))
    outputs.upToDateWhen { false }
}

val linkWebWasm = tasks.register<Exec>("linkFFmpegWasm") {
    group = "build"
    description = "Links the Emscripten runtime and the standalone WebAssembly module."
    onlyIf { canBuildWebWasm }
    dependsOn(buildWebWasm)
    val script = webOutputDir.resolve("link-web.sh")
    val jsOutput = webOutputDir.resolve("ffmpeg_kmp.js")
    val wasmOutput = webOutputDir.resolve("ffmpeg_kmp.wasm")
    doFirst {
        webOutputDir.mkdirs()
        writeWebExports(webExportsFile)
        val archives = listOf(
            "libavdevice/libavdevice.a",
            "libavfilter/libavfilter.a",
            "libavformat/libavformat.a",
            "libavcodec/libavcodec.a",
            "libswresample/libswresample.a",
            "libswscale/libswscale.a",
            "libavutil/libavutil.a",
        ).joinToString(" ") { "\"${ffmpegWasmDir.resolve(it).absolutePath.replace('\\', '/')}\"" }
        script.writeText(
            """
            #!/bin/sh
            set -e
            cd "${ffmpegWasmDir.absolutePath.replace('\\', '/')}"
            "${emccExecutable.replace('\\', '/')}" -std=c11 -O2 -fPIC               -I"${ffmpegDir.absolutePath.replace('\\', '/')}"               -I"${ffmpegWasmDir.absolutePath.replace('\\', '/')}"               -I"${jniDir.resolve("include").absolutePath.replace('\\', '/')}"               -c "${jniDir.resolve("ffmpeg_kmp.c").absolutePath.replace('\\', '/')}"               -o "${webOutputDir.resolve("shim.o").absolutePath.replace('\\', '/')}"
            "${emccExecutable.replace('\\', '/')}" -O2               -sMODULARIZE=0 -sWASM_ASYNC_COMPILATION=0 -sSINGLE_FILE=0               -sALLOW_MEMORY_GROWTH=1 -sINITIAL_MEMORY=67108864 -sSTACK_SIZE=8388608               -sFILESYSTEM=1 -sFORCE_FILESYSTEM=1               -sEXPORTED_FUNCTIONS=@"${webExportsFile.absolutePath.replace('\\', '/')}"               -sEXPORTED_RUNTIME_METHODS=HEAPU8,FS               --no-entry               -Wl,--start-group $archives -Wl,--end-group               "${webOutputDir.resolve("shim.o").absolutePath.replace('\\', '/')}"               -o "${jsOutput.absolutePath.replace('\\', '/')}"
            """.trimIndent() + "\n",
        )
        script.setExecutable(true)
    }
    commandLine("sh", script.absolutePath)
    inputs.files(
        jniDir.resolve("ffmpeg_kmp.c"),
        jniDir.resolve("include/ffmpeg_kmp.h"),
        jniDir.resolve("CMakeLists.txt"),
    )
    inputs.files(
        ffmpegWasmDir.resolve("libavdevice/libavdevice.a"),
        ffmpegWasmDir.resolve("libavfilter/libavfilter.a"),
        ffmpegWasmDir.resolve("libavformat/libavformat.a"),
        ffmpegWasmDir.resolve("libavcodec/libavcodec.a"),
        ffmpegWasmDir.resolve("libswresample/libswresample.a"),
        ffmpegWasmDir.resolve("libswscale/libswscale.a"),
        ffmpegWasmDir.resolve("libavutil/libavutil.a"),
    )
    outputs.files(jsOutput, wasmOutput)
}

val generateWebWasmData = tasks.register<DefaultTask>("generateWebWasmData") {
    group = "build"
    description = "Embeds the Emscripten runtime and wasm module into Kotlin sources."
    onlyIf { canBuildWebWasm }
    dependsOn(linkWebWasm)
    val jsFile = webOutputDir.resolve("ffmpeg_kmp.js")
    val wasmFile = webOutputDir.resolve("ffmpeg_kmp.wasm")
    inputs.files(jsFile, wasmFile)
    outputs.dir(generatedWebDir)
    doLast {
        val target = generatedWebDir.get().asFile.resolve("cn/enaium/ffmpeg/native/FFmpegWebData.kt")
        target.parentFile.mkdirs()
        val runtime = Base64.getEncoder().encodeToString(jsFile.readBytes())
        val wasm = Base64.getEncoder().encodeToString(wasmFile.readBytes())
        val output = StringBuilder()
        output.appendLine("// Generated by :ffmpeg-kmp:bridge:generateWebWasmData. Do not edit.")
        output.appendLine("package cn.enaium.ffmpeg.native")
        output.appendLine()
        fun emit(name: String, base64: String) {
            val chunks = base64.chunked(8192)
            output.appendLine("internal const val ${name}_SIZE = ${base64.length}")
            output.appendLine("internal val $name: String by lazy {")
            output.appendLine("    buildString(${name}_SIZE) {")
            chunks.indices.forEach { index ->
                output.appendLine("        append(${name}_CHUNK_$index)")
            }
            output.appendLine("    }")
            output.appendLine("}")
            chunks.forEachIndexed { index, chunk ->
                output.appendLine("private const val ${name}_CHUNK_$index = \"$chunk\"")
            }
        }
        emit("FFMPEG_WEB_RUNTIME_BASE64", runtime)
        emit("FFMPEG_WASM_BASE64", wasm)
        target.writeText(output.toString())
    }
}

tasks.matching {
    it.name.startsWith("compileKotlinJs") ||
        it.name.startsWith("compileKotlinWasmJs") ||
        it.name.startsWith("compileTestKotlinJs") ||
        it.name.startsWith("compileTestKotlinWasmJs") ||
        it.name == "jsSourcesJar" ||
        it.name == "wasmJsSourcesJar" ||
        it.name == "sourcesJar" ||
        it.name.endsWith("SourcesJar") ||
        it.name.contains("WebMain") ||
        it.name == "jsNodeTest" ||
        it.name == "wasmJsNodeTest"
}.configureEach {
    dependsOn(generateWebWasmData)
}

// ==================== Android JNI shared libraries ====================

val androidJniLibsDir = layout.buildDirectory.dir("jniLibs")
val buildAndroidJniLibs = tasks.register<DefaultTask>("buildAndroidJniLibs") {
    group = "build"
    description = "Builds libffmpeg_jni.so for all Android ABIs."
}

androidAbis.forEach { (abi, targetName, _) ->
    val outputDir = layout.buildDirectory.dir("jniLibs/$abi")
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-android-jni-$abi").get().asFile
    val target = ffmpegTargetsByName[targetName]
    if (target == null) return@forEach

    val configureTask = tasks.register<Exec>("configureAndroidJni_$abi") {
        onlyIf { target.canBuild }
        dependsOn(ffmpegBuildTasks.getValue(targetName))
        workingDir = cmakeBuildDir
        doFirst {
            cmakeBuildDir.mkdirs()
            outputDir.get().asFile.mkdirs()
        }
        commandLine(
            listOf(
                cmakeExecutable,
                jniDir.absolutePath,
                "-DCMAKE_BUILD_TYPE=Release",
                "-DBUILD_JNI=ON",
                "-DFFMPEG_SOURCE_DIR=${ffmpegDir.absolutePath}",
                "-DFFMPEG_BUILD_DIR=${ffmpegBuildDir(targetName).absolutePath}",
                "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${outputDir.get().asFile.absolutePath}",
                "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=${outputDir.get().asFile.absolutePath}",
            ) + target.cmakeArgs,
        )
    }

    val buildTask = tasks.register<Exec>("buildAndroidJni_$abi") {
        onlyIf { target.canBuild }
        dependsOn(configureTask)
        workingDir = cmakeBuildDir
        commandLine(cmakeExecutable, "--build", ".", "--target", "ffmpeg_jni", "--config", "Release")
        inputs.file(jniDir.resolve("CMakeLists.txt"))
        inputs.file(jniDir.resolve("ffmpeg_kmp.c"))
        inputs.file(jniDir.resolve("ffmpeg_jni.c"))
        inputs.file(jniDir.resolve("include/ffmpeg_kmp.h"))
        outputs.file(outputDir.map { it.file("libffmpeg_jni.so") })
    }

    buildAndroidJniLibs.configure { dependsOn(buildTask) }
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addStaticSourceDirectory(androidJniLibsDir.get().asFile.absolutePath)
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.contains("JniLibFolders") }
    .configureEach { dependsOn(buildAndroidJniLibs) }

// ==================== Publishing ====================

val hasSigningKey = rootProject.extra["hasSigningKey"] as Boolean

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (hasSigningKey) signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "ffmpeg-kmp-bridge",
        version = null,
    )

    pom {
        name.set("ffmpeg-kmp-bridge")
        description.set(
            "Internal shared JNI, Kotlin/Native and WebAssembly bridge for the " +
                "ffmpeg-kmp library bindings.",
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
