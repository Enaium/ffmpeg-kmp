# ffmpeg-kmp

Kotlin Multiplatform bindings for [FFmpeg](https://ffmpeg.org/) 9.0.2 on the
JVM, Android, Kotlin/Native and Android Native.

FFmpeg is included as a Git submodule pinned to the `n9.0.2` commit. It is
compiled from source for every target (static libraries, programs and docs
disabled, portable C kernels) and driven through one stable C ABI
([`jni/include/ffmpeg_kmp.h`](jni/include/ffmpeg_kmp.h)). The same ABI backs the
JNI bridge and the Kotlin/Native cinterop, so both platforms share handle
management, error codes and data copying.

Binding version: **9.0.2.1** — FFmpeg **9.0.2**, binding revision **1**.

> FFmpeg 9.0 removed `libpostproc` upstream, so no `libpostproc-kmp` artifact
> exists. The seven libraries shipped by FFmpeg 9.0.2 are bound.

## Artifacts

| Artifact | Package | FFmpeg library |
|---|---|---|
| `ffmpeg-kmp` | `cn.enaium.ffmpeg` | aggregate: all seven bindings |
| `libavutil-kmp` | `cn.enaium.ffmpeg.avutil` | `libavutil` |
| `libavcodec-kmp` | `cn.enaium.ffmpeg.avcodec` | `libavcodec` |
| `libavformat-kmp` | `cn.enaium.ffmpeg.avformat` | `libavformat` |
| `libavfilter-kmp` | `cn.enaium.ffmpeg.avfilter` | `libavfilter` |
| `libswscale-kmp` | `cn.enaium.ffmpeg.swscale` | `libswscale` |
| `libswresample-kmp` | `cn.enaium.ffmpeg.swresample` | `libswresample` |
| `libavdevice-kmp` | `cn.enaium.ffmpeg.avdevice` | `libavdevice` |

`ffmpeg-kmp` is the aggregate: it re-exports the seven library bindings and the
root-package type aliases. The shared JNI, Kotlin/Native and WebAssembly runtime
lives in the internal `ffmpeg-kmp-bridge` artifact. Each `lib*-kmp` artifact
contains its own binding API and depends on `ffmpeg-kmp-bridge` plus the
libraries it builds on, so it stays independently addressable. The bridge is
published as `cn.enaium.ffmpeg:ffmpeg-kmp-bridge:9.0.2.1` and is normally
consumed transitively.

## Supported targets

The aggregate and the seven library artifacts depend on the internal
`ffmpeg-kmp-bridge` coordinate; applications normally add only `ffmpeg-kmp` or
the specific `lib*-kmp` they use.

| Platform | Kotlin target | Native artifact |
|---|---|---|
| JVM | `jvm` | `ffmpeg-kmp-jni-jvm-{linux,darwin}-{x86_64,aarch64}`, `ffmpeg-kmp-jni-jvm-windows-x86_64` |
| Android | `android` | AAR with `libffmpeg_jni.so` for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` |
| Android Native | `androidNativeArm64`, `androidNativeArm32`, `androidNativeX64`, `androidNativeX86` | merged static archive in the cinterop klib |
| Linux | `linuxX64`, `linuxArm64` | merged static archive in the cinterop klib |
| Windows | `mingwX64` | merged static archive in the cinterop klib |
| macOS | `macosX64`, `macosArm64` | merged static archive in the cinterop klib |
| Kotlin/JS | `js` | embedded Emscripten WebAssembly module |
| Kotlin/Wasm | `wasmJs` | embedded Emscripten WebAssembly module |

JVM consumers receive all five per-OS/arch JNI artifacts; `FFmpegJniLoader`
extracts the matching `.so`/`.dylib`/`.dll` from the classpath. Android loads
the AAR's jniLibs by name.

Encoders/decoders, muxers/demuxers, filters and protocols are the full sets
built into FFmpeg with `--disable-autodetect`; no external libraries (x264,
OpenSSL, zlib, …) are linked. macOS enables `avfoundation` and Windows enables
`dshow`, both built from FFmpeg itself.

### Web targets

`js` and `wasmJs` embed an Emscripten build of FFmpeg (no network, portable C)
in the klib. The runtime and wasm binary are instantiated synchronously on the
first API call, so the public API is blocking on Node and in the browser just
like on the other targets. FFmpeg has no native file system in WebAssembly;
`libavformat` uses Emscripten's MEMFS, exposed through `FFmpegWeb`:

```kotlin
import cn.enaium.ffmpeg.*
import kotlin.random.Random

val input = "input-" + Random.nextInt() + ".ts"
FFmpegWeb.writeFile(input, mediaBytes)

AvFormatContext.openInput(input, "mpegts").use { context ->
    context.findStreamInfo()
    // ... decode ...
}

// Recording lands in the same in-memory file system.
val recording = "record-" + Random.nextInt() + ".mp4"
AvFormatContext.createOutput("mp4", recording).use { /* mux packets */ }
val mp4 = FFmpegWeb.readFile(recording)
```

Emscripten and Node are only needed to build or test the web artifacts. The
embedded module makes the published klib self-contained at runtime; no extra
`fetch`, script tag or asynchronous initialization is required.

### npm package

Plain JavaScript and TypeScript consumers can use the same Emscripten build
from npm:

```bash
npm install @enaium/ffmpeg-kmp
```

```javascript
import { initFFmpegKmp } from "@enaium/ffmpeg-kmp";

const api = await initFFmpegKmp();
const version = api.readString(
  api.callInt("fk_version_info", 0, 0, 0, 0, 0, 0, 0),
);
console.log(version);
```

The package ships `ffmpeg_kmp.js`, `ffmpeg_kmp.wasm` and a loader. The default
`initFFmpegKmp()` reads both files relative to the installed module; pass
`initFFmpegKmp({ runtime, wasm })` to override either with a URL, path, `URL`,
`Uint8Array` or `ArrayBuffer`. The returned table exposes the `fk_*` C ABI
through `callInt`, `callDouble`, the string/byte helpers and the MEMFS helpers
(`readFile`, `writeFile`, `deleteFile`, `exists`).

Staging and publishing:

```bash
./gradlew :ffmpeg-kmp:assembleWasmNpm    # stages ffmpeg-kmp/build/npm
./gradlew :ffmpeg-kmp:publishWasmNpm     # npm publish --access public --tag latest
```

`publishWasmNpm` reads the token from `npm.publish.key` in
`~/.gradle/gradle.properties` (or `-Pnpm.publish.key=<token>`); CI uses the
`NPM_PUBLISH_KEY` secret. npm requires three-component semver, so binding
version `9.0.2.1` is published as `9.0.2-1` with the `latest` tag.

## Using the library

```kotlin
dependencies {
    implementation("cn.enaium.ffmpeg:ffmpeg-kmp:9.0.2.1")
    // or only what you need:
    implementation("cn.enaium.ffmpeg:libavcodec-kmp:9.0.2.1")
    implementation("cn.enaium.ffmpeg:libavformat-kmp:9.0.2.1")
}
```

### Decoding

```kotlin
import cn.enaium.ffmpeg.*

AvFormatContext.openInput("input.ts", "mpegts").use { input ->
    input.findStreamInfo()
    val stream = input.streams.first { it.codecParameters.codecType == AvMediaType.VIDEO }
    val decoder = AvCodec.decoder(stream.codecParameters.codecId)

    AvCodecContext.create(decoder).use { context ->
        context.applyParameters(stream.codecParameters)
        context.open()

        val packet = AvPacket.create()
        while (input.readFrame(packet)) {
            if (packet.streamIndex == stream.index) {
                context.sendPacket(packet)
                while (true) {
                    val frame = context.receiveFrame() ?: break
                    println("decoded ${frame.width}x${frame.height} ${AvPixelFormat.name(frame.format)}")
                    frame.close()
                }
            }
            packet.unref()
        }
        packet.close()
    }
}
```

Content probing misdetects short MPEG-TS files as MPEG-PS (FFmpeg scores both
formats), so pass the known demuxer name to `openInput(url, formatName)` when
the container is known.

### Encoding and muxing

```kotlin
import cn.enaium.ffmpeg.*

AvCodecContext.create(AvCodec.encoder("mpeg4")).use { encoder ->
    encoder.width = 1280
    encoder.height = 720
    encoder.pixelFormat = AvPixelFormat.YUV420P
    encoder.timeBase = AvRational(1, 25)
    encoder.bitRate = 4_000_000
    encoder.gopSize = 50
    encoder.open()

    AvFormatContext.createOutput("mpegts", "output.ts").use { output ->
        val stream = output.newStream(encoder)
        stream.codecParameters.copyFrom(encoder)
        stream.timeBase = encoder.timeBase
        output.writeHeader()

        AvFrame.video(1280, 720, AvPixelFormat.YUV420P).use { frame ->
            // write the Y/U/V planes into frame.writePlane(0..2)
            frame.pts = 0
            encoder.sendFrame(frame)
        }
        encoder.sendFrame(null)
        while (true) {
            val packet = encoder.receivePacket() ?: break
            packet.use {
                it.streamIndex = stream.index
                it.rescaleTs(encoder.timeBase, stream.timeBase)
                output.writePacket(it)
            }
        }
        output.writeTrailer()
    }
}
```

### Filters, scaling and resampling

```kotlin
AvFilterGraph.create().use { graph ->
    val source = graph.createFilter("color", "src", "c=red:s=320x240:r=25")
    val sink = graph.createFilter("buffersink", "sink")
    graph.link(source, 0, sink, 0)
    graph.configure()
    graph.getFrame(sink)?.use { frame ->
        println("filter produced ${frame.width}x${frame.height}")
    }
}

SwsContext.create(320, 240, AvPixelFormat.YUV420P, 160, 120, AvPixelFormat.RGBA)
    .use { scaler -> scaler.scale(destination, source) }

SwrContext.create(
    inputSampleRate = 44_100,
    inputFormat = AvSampleFormat.S16,
    inputChannels = 2,
    outputSampleRate = 48_000,
    outputFormat = AvSampleFormat.FLTP,
    outputChannels = 2,
).use { resampler -> resampler.convert(outputFrame, inputFrame) }
```

All native objects implement `AutoCloseable` and are released by `use { }` /
`close()`. Packets written with `AvFormatContext.writePacket` are consumed by
the muxer; every other frame/packet stays owned by the caller.

### JDK native access

JDK 24+ prints a warning on `System.load`. Add
`--enable-native-access=ALL-UNNAMED` to the application's JVM arguments to
silence it (and to stay compatible with future JDKs that block restricted
methods by default).

## Examples

The example is a Kotlin Multiplatform camera application, structured like
[camera-kmp](https://github.com/Enaium/camera-kmp):

| Module | What it does |
|---|---|
| `examples:ffmpeg-core` | camera-kmp capture, test pattern, `swscale` scale + RGBA conversion, `avfilter` graph, `avcodec` encode/decode, `avformat` MP4 recording |
| `examples:camera-ffmpeg` | SDL3 + ImGui window for JVM, Kotlin/Native desktop and Android Native, with live camera/filtered views and controls |
| `examples:camera-ffmpeg:android` | Android `SDLActivity` app that packages `libmain.so` for all four ABIs |

The runtime pipeline is:

```
camera-kmp I420 frame
  -> swscale (scale percentage)
  -> avfilter (none / flip / negate / gaussian blur / hue / edge detect)
  -> avcodec mpeg4 encoder
  -> avformat muxer (optional recording: MP4/Matroska/MOV/MPEG-TS/MPEG-PS/AVI/3GP)
  -> avcodec mpeg4 decoder
  -> swscale RGBA
  -> SDL streaming texture -> ImGui
```

The UI exposes resolution, camera/test-pattern source, filter, scale, bitrate,
a record checkbox and a container/muxer selector. The example only offers filters that exist in an LGPL
build (`hflip`, `vflip`, `negate`, `gblur`, `hue`, `edgedetect`); GPL-only
filters such as `boxblur` and `eq` are not compiled because FFmpeg is built
without `--enable-gpl`. Recording files are written under the process working
directory on desktop and under the app-specific external files directory on
Android.

```bash
# Windowed desktop app
./gradlew :examples:camera-ffmpeg:jvmRun
./gradlew :examples:camera-ffmpeg:runDebugExecutableMacosArm64

# Headless pipeline (CI): 30 synthetic frames, negate + 50% scale + MP4
./gradlew :examples:camera-ffmpeg:jvmRun \
  --args="--headless --frames=30 --source=synthetic --filter=negate --scale=50 --record --container=matroska"

# Android APK (SDLActivity + native libmain.so)
./gradlew :examples:camera-ffmpeg:android:assembleDebug
```

The JVM target requires JDK 22 or newer because `camera-kmp`'s desktop backend uses the
Foreign Function & Memory API. The native entry point reads
`CAMERA_FFMPEG_*` environment variables; the JVM entry point accepts the same
options as `--frames`, `--source`, `--filter`, `--scale`, `--record`,
`--record-path`, `--container`, etc.

## Building from source

```bash
git submodule update --init --recursive
./gradlew jvmTest macosArm64Test
./gradlew jsNodeTest wasmJsNodeTest   # needs Emscripten
./gradlew :ffmpeg-kmp:assembleWasmNpm # stages the npm package
```

The FFmpeg build runs `configure` and `make` for the requested target under
`build/ffmpeg/<target>`; the C shim and JNI library are built by CMake under the
JNI artifact's build directory. Flags used for every target:

```
--disable-programs --disable-doc --disable-debug
--enable-static --disable-shared --enable-pic
--disable-autodetect --disable-asm
--enable-avdevice --enable-avfilter --enable-swscale --enable-swresample
```

`--disable-asm` keeps one portable C code path and removes NASM from the build
requirements. Cross builds use the host's `aarch64-linux-gnu-gcc`,
`x86_64-w64-mingw32-gcc` or the Android NDK clang wrappers. Windows hosts run
the configure script through MSYS2/Git Bash.

## CI

- [`.github/workflows/test.yml`](.github/workflows/test.yml) runs JVM, native,
  JS/Wasm and Android host tests plus the examples on macOS, Linux and Windows.
- [`.github/workflows/publish.yml`](.github/workflows/publish.yml) publishes
  every artifact to Maven Central from the runner that can build it, and the
  npm package from the macOS runner.

Both workflows are manual (`workflow_dispatch`). Publishing requires
`MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`,
`SIGNING_KEY_ID`, `SIGNING_PASSWORD` and `NPM_PUBLISH_KEY` repository secrets.

## License

The bindings are licensed under **LGPL-2.1-or-later**, matching an FFmpeg build
without GPL-only components. FFmpeg itself is licensed under the LGPL 2.1+; see
the submodule's `COPYING.LGPLv2.1` and `LICENSE.md`.
