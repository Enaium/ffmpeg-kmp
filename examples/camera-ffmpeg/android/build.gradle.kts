import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Links one libmain.so per Android ABI (with an exported SDL_main entry point)
// and stages them plus the matching libc++_shared.so for the APK's jniLibs.
val pinnedNdkVersion = "27.0.12077973"

val androidAbis = mapOf(
    "androidNativeArm64" to "arm64-v8a",
    "androidNativeArm32" to "armeabi-v7a",
    "androidNativeX64" to "x86_64",
    "androidNativeX86" to "x86",
)

val cxxSharedTriple = mapOf(
    "androidNativeArm64" to "aarch64-linux-android",
    "androidNativeArm32" to "arm-linux-androideabi",
    "androidNativeX64" to "x86_64-linux-android",
    "androidNativeX86" to "i686-linux-android",
)

val androidSdkDir: String? = System.getenv("ANDROID_HOME")
    ?: System.getenv("ANDROID_SDK_ROOT")
    ?: rootProject.file("local.properties").takeIf { it.isFile }?.let { file ->
        Properties().apply { file.inputStream().use { load(it) } }.getProperty("sdk.dir")
    }

fun ndkCxxShared(target: String): File? {
    val sdkDir = androidSdkDir ?: return null
    val ndkRoot = File(sdkDir, "ndk")
    val ndk = ndkRoot.resolve(pinnedNdkVersion).takeIf { it.isDirectory }
        ?: ndkRoot.listFiles()
            ?.filter { it.isDirectory && it.name.matches(Regex("\\d+(\\.\\d+)+")) }
            ?.maxByOrNull { it.name }
        ?: return null
    val prebuiltDir = File(ndk, "toolchains/llvm/prebuilt")
    // The NDK host directory is named after the build host, not the JVM
    // architecture: Apple Silicon still uses darwin-x86_64.
    val hostPrefix = when {
        System.getProperty("os.name").lowercase().contains("mac") -> "darwin-"
        System.getProperty("os.name").lowercase().contains("windows") -> "windows-"
        else -> "linux-"
    }
    val host = prebuiltDir.listFiles()
        ?.firstOrNull { it.isDirectory && it.name.startsWith(hostPrefix) }
        ?: return null
    val triple = cxxSharedTriple[target] ?: return null
    return File(host, "sysroot/usr/lib/$triple/libc++_shared.so").takeIf { it.exists() }
}

fun registerPrepareJniLibs(buildType: String): TaskProvider<Sync> {
    val capitalized = buildType.replaceFirstChar { it.uppercase() }
    val outputDir = layout.buildDirectory.dir("generated/jniLibs/$buildType")
    val task = tasks.register<Sync>("prepareJniLibs$capitalized") {
        group = "build"
        description = "Stages libmain.so and libc++_shared.so for the $buildType APK."
        into(outputDir)
        androidAbis.forEach { (target, abi) ->
            val linkTask = project(":examples:camera-ffmpeg").tasks.named(
                "linkMain${capitalized}Shared${target.replaceFirstChar { it.uppercase() }}",
            )
            dependsOn(linkTask)
            from(linkTask.flatMap { (it as KotlinNativeLink).outputFile }) {
                into(abi)
                rename { "libmain.so" }
            }
            ndkCxxShared(target)?.let { file ->
                from(file) { into(abi) }
            }
        }
        doFirst {
            androidAbis.keys.forEach { target ->
                check(ndkCxxShared(target) != null) {
                    "No libc++_shared.so for $target. Install Android NDK $pinnedNdkVersion " +
                        "(sdkmanager \"ndk;$pinnedNdkVersion\") and point ANDROID_HOME or " +
                        "local.properties sdk.dir at that SDK."
                }
            }
        }
    }
    return task
}

val prepareDebugJniLibs = registerPrepareJniLibs("debug")
val prepareReleaseJniLibs = registerPrepareJniLibs("release")

android {
    namespace = "cn.enaium.ffmpeg.example"
    compileSdk = 37
    ndkVersion = pinnedNdkVersion

    defaultConfig {
        applicationId = "cn.enaium.ffmpeg.example"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += androidAbis.values
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            // SDL3 ships a JVM JNI library; this app links SDL into libmain.so.
            excludes += "**/libsdl_jni.so"
        }
    }
}

androidComponents {
    onVariants { variant ->
        val directory = layout.buildDirectory.dir("generated/jniLibs/${variant.buildType}")
        variant.sources.jniLibs?.addStaticSourceDirectory(directory.get().asFile.absolutePath)
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.contains("JniLibFolders") }
    .configureEach {
        when {
            name.contains("Debug") -> dependsOn(prepareDebugJniLibs)
            name.contains("Release") -> dependsOn(prepareReleaseJniLibs)
        }
    }

dependencies {
    implementation(libs.sdl.kmp)
}
