/*
 * Per-OS/arch JNI artifact: windows-x86_64.
 * Ships ffmpeg_jni.dll as a classpath resource at
 * /cn/enaium/ffmpeg/native/windows-x86_64/, which FFmpegJniLoader
 * (in :ffmpeg-kmp:bridge's jvmMain) extracts and System.load()s at runtime.
 */
import java.io.File
import org.gradle.internal.os.OperatingSystem

plugins {
    `java-library`
    alias(libs.plugins.maven.publish)
}

group = rootProject.group
version = rootProject.version

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val jniOs = "windows"
val jniArch = "x86_64"
val classifier = "$jniOs-$jniArch"
val libFile = "ffmpeg_jni.dll"
val resourceDir = "cn/enaium/ffmpeg/native/$classifier"

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

val canBuildHere = OperatingSystem.current().isWindows ||
    (OperatingSystem.current().isLinux && hasExecutable("x86_64-w64-mingw32-gcc"))

val cmakeExecutable: String = rootProject.extra["cmakeExecutable"] as String

val nativeOutputDir = layout.buildDirectory.dir("jni-native/$classifier")
val cmakeBuildDir = layout.buildDirectory.dir("cmake-jni/$classifier")
val ffmpegBuildDir = rootProject.layout.buildDirectory.dir("ffmpeg/mingwX64")
val msysRoot = System.getenv("MSYS2_ROOT")?.takeIf { it.isNotBlank() } ?: "C:/msys64"
val msysMake = File(msysRoot, "usr/bin/make.exe").absolutePath.replace('\\', '/')
val msysGcc = File(msysRoot, "mingw64/bin/gcc.exe").absolutePath.replace('\\', '/')
val msysPath = listOf(
    File(msysRoot, "mingw64/bin").absolutePath,
    File(msysRoot, "usr/bin").absolutePath,
    System.getenv("PATH").orEmpty(),
).joinToString(File.pathSeparator)

val configureJniLibrary = tasks.register<Exec>("configureJniLibrary") {
    group = "build"
    description = "cmake-configures ffmpeg_jni for $classifier."
    onlyIf { canBuildHere }
    dependsOn(":ffmpeg-kmp:bridge:buildFFmpeg_mingwX64")
    val outDir = nativeOutputDir.get().asFile
    val buildDir = cmakeBuildDir.get().asFile
    doFirst {
        outDir.mkdirs()
        buildDir.mkdirs()
    }
    workingDir = buildDir
    if (OperatingSystem.current().isWindows) environment("PATH", msysPath)
    val javaHome = System.getProperty("java.home") ?: System.getenv("JAVA_HOME") ?: ""
    val jniInclude = if (javaHome.isNotEmpty()) "$javaHome/include" else ""
    val platformInclude = if (OperatingSystem.current().isWindows) {
        "$jniInclude/win32"
    } else {
        // Cross-compiling from Linux: the host JDK has no win32/jni_md.h.
        rootProject.file("jni/include/win32").absolutePath
    }
    val hostWindows = OperatingSystem.current().isWindows
    commandLine(
        listOf(
            cmakeExecutable,
            rootProject.file("jni").absolutePath,
            "-DCMAKE_BUILD_TYPE=Release",
            "-DBUILD_JNI=ON",
            "-DFFMPEG_SOURCE_DIR=${rootProject.file("ffmpeg").absolutePath}",
            "-DFFMPEG_BUILD_DIR=${ffmpegBuildDir.get().asFile.absolutePath}",
            "-DJNI_INCLUDE_DIR=$jniInclude",
            "-DJNI_INCLUDE_DIR_PLATFORM=$platformInclude",
            "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${outDir.absolutePath}",
            "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=${outDir.absolutePath}",
            "-G", "MinGW Makefiles",
        ) + if (hostWindows) {
            listOf(
                "-DCMAKE_C_COMPILER=$msysGcc",
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
        },
    )
}

val buildJniLibrary = tasks.register<Exec>("buildJniLibrary") {
    group = "build"
    description = "Builds $libFile for $classifier."
    onlyIf { canBuildHere }
    dependsOn(configureJniLibrary)
    workingDir = cmakeBuildDir.get().asFile
    if (OperatingSystem.current().isWindows) environment("PATH", msysPath)
    commandLine(cmakeExecutable, "--build", ".", "--target", "ffmpeg_jni", "--config", "Release")
    inputs.files(
        rootProject.file("jni/CMakeLists.txt"),
        rootProject.file("jni/ffmpeg_kmp.c"),
        rootProject.file("jni/ffmpeg_jni.c"),
        rootProject.file("jni/include/ffmpeg_kmp.h"),
    )
    inputs.dir(ffmpegBuildDir.map { it.dir("libavcodec") })
    outputs.file(nativeOutputDir.map { it.file(libFile) })
}

tasks.named<Copy>("processResources") {
    dependsOn(buildJniLibrary)
    from(nativeOutputDir.map { it.file(libFile) }) {
        into(resourceDir)
    }
}

val hasSigningKey = rootProject.extra["hasSigningKey"] as Boolean

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (hasSigningKey) signAllPublications()
    coordinates(
        groupId = rootProject.group.toString(),
        artifactId = "ffmpeg-kmp-jni-jvm-$classifier",
        version = rootProject.version.toString(),
    )
    pom {
        name.set("ffmpeg-kmp-jni-jvm-$classifier")
        description.set(
            "Prebuilt JNI shared library for ffmpeg-kmp on $jniOs/$jniArch. " +
                "Loaded automatically by FFmpegJniLoader; not intended to be depended on directly.",
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
