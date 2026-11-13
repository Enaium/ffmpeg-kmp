import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

/**
 * Camera capture, FFmpeg filter/scale/encode/decode and MP4 recording pipeline
 * shared by every example target. It has no window or renderer dependency so
 * the headless CI run can use it without SDL.
 */
kotlin {
    jvm {
        compilerOptions {
            // camera-kmp's desktop backend uses the JDK 22 FFM API.
            jvmTarget.set(JvmTarget.JVM_22)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            jvmArgs("--enable-native-access=ALL-UNNAMED")
        }
    }

    macosArm64()
    macosX64()
    linuxX64 {
        binaries.all { linkerOpts("-Wl,--allow-multiple-definition") }
    }
    linuxArm64 {
        binaries.all { linkerOpts("-Wl,--allow-multiple-definition") }
    }
    mingwX64()

    androidNativeArm64()
    androidNativeArm32()
    androidNativeX64()
    androidNativeX86()

    sourceSets {
        getByName("commonMain") {
            dependencies {
                api(project(":ffmpeg-kmp"))
                api(libs.camera.kmp)
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

        getByName("jvmTest").dependsOn(getByName("commonTest"))
    }
}
