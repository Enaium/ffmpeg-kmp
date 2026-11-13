import java.io.File
import java.util.Properties
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.maven.publish) apply false
}

// ==================== Toolchain resolution ====================
// IDEs start the Gradle daemon with the PATH inherited from the desktop
// session, which on macOS and Linux often lacks Homebrew, MacPorts and
// /usr/local/bin. Every module that drives CMake reads these absolute paths
// instead of relying on a bare `cmake` lookup.

/** Android SDK root from the environment or `local.properties`, or `null`. */
fun androidSdkDir(): File? {
    listOf("ANDROID_HOME", "ANDROID_SDK_ROOT").forEach { key ->
        System.getenv(key)?.takeIf { it.isNotBlank() }?.let {
            val dir = File(it)
            if (dir.isDirectory) return dir
        }
    }
    val localProperties = file("local.properties")
    if (localProperties.isFile) {
        val properties = Properties().apply { localProperties.inputStream().use { load(it) } }
        properties.getProperty("sdk.dir")?.takeIf { it.isNotBlank() }?.let {
            val dir = File(it)
            if (dir.isDirectory) return dir
        }
    }
    return null
}

fun resolveCmakeExecutable(): String {
    val executable = if (OperatingSystem.current().isWindows) "cmake.exe" else "cmake"
    val candidates = buildList {
        System.getenv("PATH")?.split(File.pathSeparator).orEmpty().forEach { add(File(it, executable)) }
        listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin", "/opt/local/bin").forEach {
            add(File(it, executable))
        }
        androidSdkDir()?.resolve("cmake")
            ?.listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }
            ?.let { add(it.resolve("bin/$executable")) }
    }
    return candidates.firstOrNull { it.isFile && it.canExecute() }?.absolutePath ?: executable
}

/** Android NDK prebuilt host tag: the NDK names its directory after the build host it ships. */
fun androidNdkHostTag(): String? = when {
    OperatingSystem.current().isMacOsX -> "darwin-x86_64"
    OperatingSystem.current().isLinux -> "linux-x86_64"
    OperatingSystem.current().isWindows -> "windows-x86_64"
    else -> null
}

val PINNED_ANDROID_NDK_VERSION = "27.0.12077973"

/** Android NDK root: the pinned version when present, the newest one otherwise. */
fun androidNdkDir(): File? {
    val sdk = androidSdkDir() ?: return null
    val ndkParent = sdk.resolve("ndk")
    if (!ndkParent.isDirectory) return null
    val pinned = ndkParent.resolve(PINNED_ANDROID_NDK_VERSION)
    if (pinned.isDirectory) return pinned
    return ndkParent.listFiles()?.filter { it.isDirectory }?.maxByOrNull { it.name }
}

val androidNdk = androidNdkDir()
val androidNdkHost = androidNdkHostTag()

val hasSigningKey = providers.gradleProperty("signing.keyId").isPresent ||
    providers.gradleProperty("signing.secretKeyRingFile").isPresent

allprojects {
    group = "cn.enaium.ffmpeg"
    version = "9.0.2.1"

    extra["hasSigningKey"] = hasSigningKey
    extra["cmakeExecutable"] = resolveCmakeExecutable()
    extra["androidSdkDir"] = androidSdkDir()?.absolutePath
    extra["androidNdkDir"] = androidNdk?.absolutePath
    extra["androidNdkHost"] = androidNdkHost
}

// Android host tests (unit tests on the JVM) load the desktop JNI library by
// name. Every KMP library module with an Android target needs the host build
// on java.library.path, not just the module that owns the JNI artifact.
val hostJniClassifier = run {
    val arch = System.getProperty("os.arch").lowercase()
    val archClassifier = when (arch) {
        "amd64", "x86_64", "x64" -> "x86_64"
        "aarch64", "arm64" -> "aarch64"
        else -> null
    }
    val osClassifier = when {
        OperatingSystem.current().isLinux -> "linux"
        OperatingSystem.current().isMacOsX -> "darwin"
        OperatingSystem.current().isWindows -> "windows"
        else -> null
    }
    if (osClassifier != null && archClassifier != null) "$osClassifier-$archClassifier" else null
}

if (hostJniClassifier != null) {
    subprojects {
        plugins.withId("com.android.kotlin.multiplatform.library") {
            tasks.withType<Test>().configureEach {
                if (name.contains("AndroidHostTest", ignoreCase = true)) {
                    dependsOn(":jni-jvm-$hostJniClassifier:buildJniLibrary")
                    val nativeDir = rootProject.project(":jni-jvm-$hostJniClassifier")
                        .layout.buildDirectory
                        .dir("jni-native/$hostJniClassifier")
                    systemProperty("java.library.path", nativeDir.get().asFile.absolutePath)
                }
            }
        }
    }
}
