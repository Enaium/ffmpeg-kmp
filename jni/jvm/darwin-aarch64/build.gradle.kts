/*
 * Per-OS/arch JNI artifact: darwin-aarch64.
 * Ships libffmpeg_jni.dylib as a classpath resource at
 * /cn/enaium/ffmpeg/native/darwin-aarch64/, which FFmpegJniLoader
 * (in :ffmpeg-kmp:bridge's jvmMain) extracts and System.load()s at runtime.
 */
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

val jniOs = "darwin"
val jniArch = "aarch64"
val classifier = "$jniOs-$jniArch"
val libFile = "libffmpeg_jni.dylib"
val resourceDir = "cn/enaium/ffmpeg/native/$classifier"

val canBuildHere = OperatingSystem.current().isMacOsX

val cmakeExecutable: String = rootProject.extra["cmakeExecutable"] as String

val nativeOutputDir = layout.buildDirectory.dir("jni-native/$classifier")
val cmakeBuildDir = layout.buildDirectory.dir("cmake-jni/$classifier")
val ffmpegBuildDir = rootProject.layout.buildDirectory.dir("ffmpeg/macosArm64")

val configureJniLibrary = tasks.register<Exec>("configureJniLibrary") {
    group = "build"
    description = "cmake-configures ffmpeg_jni for $classifier."
    onlyIf { canBuildHere }
    dependsOn(":ffmpeg-kmp:bridge:buildFFmpeg_macosArm64")
    val outDir = nativeOutputDir.get().asFile
    val buildDir = cmakeBuildDir.get().asFile
    doFirst {
        outDir.mkdirs()
        buildDir.mkdirs()
    }
    workingDir = buildDir
    val javaHome = System.getProperty("java.home") ?: System.getenv("JAVA_HOME") ?: ""
    val jniInclude = if (javaHome.isNotEmpty()) "$javaHome/include" else ""
    val platformInclude = when {
        OperatingSystem.current().isMacOsX -> "darwin"
        OperatingSystem.current().isWindows -> "win32"
        else -> "linux"
    }
    commandLine(
        cmakeExecutable,
        rootProject.file("jni").absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DBUILD_JNI=ON",
        "-DFFMPEG_SOURCE_DIR=${rootProject.file("ffmpeg").absolutePath}",
        "-DFFMPEG_BUILD_DIR=${ffmpegBuildDir.get().asFile.absolutePath}",
        "-DJNI_INCLUDE_DIR=$jniInclude",
        "-DJNI_INCLUDE_DIR_PLATFORM=$jniInclude/$platformInclude",
        "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${outDir.absolutePath}",
        "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=${outDir.absolutePath}",
        "-DCMAKE_OSX_ARCHITECTURES=arm64",
    )
}

val buildJniLibrary = tasks.register<Exec>("buildJniLibrary") {
    group = "build"
    description = "Builds $libFile for $classifier."
    onlyIf { canBuildHere }
    dependsOn(configureJniLibrary)
    workingDir = cmakeBuildDir.get().asFile
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
