#
# Builds the stable C shim, the JNI shared library and the merged static
# archive consumed by Kotlin/Native cinterop.
#
# FFmpeg itself is built by Gradle (it ships a configure/make build system, not
# CMake). This project only compiles the shim against the resulting tree:
#
#   -DFFMPEG_SOURCE_DIR=<ffmpeg checkout>
#   -DFFMPEG_BUILD_DIR=<out-of-tree FFmpeg build>
#   -DBUILD_ARCHIVE=ON      -> libffmpeg_kmp.a (FFmpeg objects + shim)
#   -DBUILD_JNI=ON          -> libffmpeg_jni.{so,dylib,dll}
#
cmake_minimum_required(VERSION 3.20)

project(ffmpeg_kmp C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

option(BUILD_JNI "Build the JNI shared library" OFF)
option(BUILD_ARCHIVE "Merge the FFmpeg archives and the shim into one static library" OFF)

set(FFMPEG_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../ffmpeg" CACHE PATH "FFmpeg source tree")
set(FFMPEG_BUILD_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../build/ffmpeg/host" CACHE PATH "Out-of-tree FFmpeg build")
set(ARCHIVE_OUTPUT_DIR "${CMAKE_CURRENT_BINARY_DIR}" CACHE PATH "Directory for libffmpeg_kmp.a")

if(NOT EXISTS "${FFMPEG_SOURCE_DIR}/libavcodec/avcodec.h")
  message(FATAL_ERROR "FFmpeg sources are missing at ${FFMPEG_SOURCE_DIR}")
endif()
if(NOT EXISTS "${FFMPEG_BUILD_DIR}/libavcodec/libavcodec.a")
  message(FATAL_ERROR "FFmpeg is not built at ${FFMPEG_BUILD_DIR}; run the buildFFmpeg_* task first")
endif()

set(FFMPEG_ARCHIVES
  "${FFMPEG_BUILD_DIR}/libavdevice/libavdevice.a"
  "${FFMPEG_BUILD_DIR}/libavfilter/libavfilter.a"
  "${FFMPEG_BUILD_DIR}/libavformat/libavformat.a"
  "${FFMPEG_BUILD_DIR}/libavcodec/libavcodec.a"
  "${FFMPEG_BUILD_DIR}/libswresample/libswresample.a"
  "${FFMPEG_BUILD_DIR}/libswscale/libswscale.a"
  "${FFMPEG_BUILD_DIR}/libavutil/libavutil.a"
)
foreach(archive IN LISTS FFMPEG_ARCHIVES)
  if(NOT EXISTS "${archive}")
    message(FATAL_ERROR "Missing FFmpeg archive ${archive}")
  endif()
endforeach()

include_directories(
  "${FFMPEG_SOURCE_DIR}"
  "${FFMPEG_BUILD_DIR}"
  "${CMAKE_CURRENT_SOURCE_DIR}/include"
)

add_library(ffmpeg_shim STATIC "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg_kmp.c")
target_include_directories(ffmpeg_shim PUBLIC
  "${FFMPEG_SOURCE_DIR}"
  "${FFMPEG_BUILD_DIR}"
  "${CMAKE_CURRENT_SOURCE_DIR}/include"
)

if(APPLE)
  set(FFMPEG_PLATFORM_LIBS
    "-lm"
    "-pthread"
    "-framework CoreFoundation"
    "-framework CoreVideo"
    "-framework CoreMedia"
    "-framework AVFoundation"
    "-framework CoreGraphics"
    "-framework ApplicationServices"
    "-framework Foundation"
  )
elseif(WIN32)
  set(FFMPEG_PLATFORM_LIBS
    "-lm"
    "-lws2_32"
    "-lbcrypt"
    "-lsecur32"
    "-lncrypt"
    "-lcrypt32"
    "-lole32"
    "-loleaut32"
    "-lstrmiids"
    "-luuid"
    "-lshlwapi"
    "-lpsapi"
    "-luser32"
    "-lgdi32"
  )
elseif(ANDROID)
  set(FFMPEG_PLATFORM_LIBS "-lm" "-llog")
else()
  find_package(Threads REQUIRED)
  set(FFMPEG_PLATFORM_LIBS "${CMAKE_DL_LIBS}" "m" Threads::Threads)
endif()

if(BUILD_JNI)
  add_library(ffmpeg_jni SHARED
    "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg_kmp.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg_jni.c"
  )
  target_include_directories(ffmpeg_jni PRIVATE
    "${FFMPEG_SOURCE_DIR}"
    "${FFMPEG_BUILD_DIR}"
    "${CMAKE_CURRENT_SOURCE_DIR}/include"
  )
  target_link_libraries(ffmpeg_jni PRIVATE ${FFMPEG_ARCHIVES} ${FFMPEG_PLATFORM_LIBS})
  set_target_properties(ffmpeg_jni PROPERTIES OUTPUT_NAME ffmpeg_jni)

  if(WIN32)
    # MinGW prefixes DLLs with "lib"; the Kotlin loader expects ffmpeg_jni.dll.
    set_target_properties(ffmpeg_jni PROPERTIES PREFIX "")
    target_link_options(ffmpeg_jni PRIVATE -static -static-libgcc -s)
  endif()

  if(ANDROID)
    # 16 KB page-size devices (Android 15+).
    target_link_options(ffmpeg_jni PRIVATE
      "-Wl,-z,max-page-size=16384"
      "-Wl,-z,common-page-size=16384"
      "-s"
    )
  else()
    if(NOT JNI_INCLUDE_DIR AND DEFINED ENV{JAVA_HOME})
      set(JNI_INCLUDE_DIR "$ENV{JAVA_HOME}/include")
      if(WIN32)
        set(JNI_INCLUDE_DIR_PLATFORM "$ENV{JAVA_HOME}/include/win32")
      elseif(APPLE)
        set(JNI_INCLUDE_DIR_PLATFORM "$ENV{JAVA_HOME}/include/darwin")
      else()
        set(JNI_INCLUDE_DIR_PLATFORM "$ENV{JAVA_HOME}/include/linux")
      endif()
    endif()
    if(NOT JNI_INCLUDE_DIR)
      find_package(JNI REQUIRED)
    endif()
    if(JNI_INCLUDE_DIR)
      target_include_directories(ffmpeg_jni PRIVATE "${JNI_INCLUDE_DIR}")
    endif()
    if(JNI_INCLUDE_DIR_PLATFORM)
      target_include_directories(ffmpeg_jni PRIVATE "${JNI_INCLUDE_DIR_PLATFORM}")
    endif()
    if(JNI_INCLUDE_DIRS)
      target_include_directories(ffmpeg_jni PRIVATE ${JNI_INCLUDE_DIRS})
    endif()
  endif()
endif()

if(BUILD_ARCHIVE)
  set(FFMPEG_KMP_ARCHIVE "${ARCHIVE_OUTPUT_DIR}/libffmpeg_kmp.a")
  # Custom-command arguments are split on semicolons, so pass the input list
  # with "|" as separator and restore it in the script.
  string(REPLACE ";" "|" FFMPEG_ARCHIVE_INPUTS "${FFMPEG_ARCHIVES}")
  add_custom_command(
    OUTPUT "${FFMPEG_KMP_ARCHIVE}"
    COMMAND "${CMAKE_COMMAND}"
      "-DOUTPUT=${FFMPEG_KMP_ARCHIVE}"
      "-DWORK_DIR=${CMAKE_CURRENT_BINARY_DIR}/archive-work"
      "-DAR=${CMAKE_AR}"
      "-DIS_APPLE=${APPLE}"
      "-DINPUTS=${FFMPEG_ARCHIVE_INPUTS}|$<TARGET_FILE:ffmpeg_shim>"
      -P "${CMAKE_CURRENT_SOURCE_DIR}/merge_archives.cmake"
    DEPENDS ffmpeg_shim ${FFMPEG_ARCHIVES} "${CMAKE_CURRENT_SOURCE_DIR}/merge_archives.cmake"
    COMMENT "Merging FFmpeg archives into libffmpeg_kmp.a"
    VERBATIM
  )
  add_custom_target(ffmpeg_kmp_archive ALL DEPENDS "${FFMPEG_KMP_ARCHIVE}")
endif()
