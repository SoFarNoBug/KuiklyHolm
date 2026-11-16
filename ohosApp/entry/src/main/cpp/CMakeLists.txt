# the minimum version of CMake.
cmake_minimum_required(VERSION 3.4.1)
project(HarmonyDemo)

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

if (DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif ()

include_directories(${NATIVERENDER_ROOT_PATH}
        ${NATIVERENDER_ROOT_PATH}/include)

add_library(kuikly_render SHARED IMPORTED)
set_target_properties(kuikly_render
        PROPERTIES
        IMPORTED_LOCATION ${NATIVERENDER_ROOT_PATH}/../../../oh_modules/@kuikly-open/render/libs/${OHOS_ARCH}/libkuikly.so)

add_library(kuikly_shared SHARED IMPORTED)
set_target_properties(kuikly_shared
        PROPERTIES
        IMPORTED_LOCATION ${NATIVERENDER_ROOT_PATH}/../../../libs/${OHOS_ARCH}/libshared.so)

add_library(entry SHARED napi_init.cpp)
target_link_libraries(entry PUBLIC libace_napi.z.so libhilog_ndk.z.so kuikly_shared kuikly_render)

# ADR 0010 · libholm_xc.so（独立于 libentry）+ 脚本产物 libholm_runtime.so
# 必须设 IMPORTED_SONAME，否则 lld 会把本机绝对路径写进 NEEDED，真机 dlopen 失败。
set(HOLM_RUNTIME_LIB_DIR ${NATIVERENDER_ROOT_PATH}/../../../libs/${OHOS_ARCH})
add_library(holm_runtime SHARED IMPORTED)
set_target_properties(holm_runtime
        PROPERTIES
        IMPORTED_LOCATION ${HOLM_RUNTIME_LIB_DIR}/libholm_runtime.so
        IMPORTED_SONAME libholm_runtime.so)

# ADR 0010 Runtime + ADR 0011 Lite EGL/GLES
add_library(holm_xc SHARED
        holm_xc/holm_xc_plugin.cpp
        holm_xc/holm_xc_lite_gl.cpp)
target_include_directories(holm_xc PRIVATE
        ${NATIVERENDER_ROOT_PATH}/holm_xc
        ${HOLM_RUNTIME_LIB_DIR})
target_link_libraries(holm_xc PUBLIC
        libace_napi.z.so
        libace_ndk.z.so
        libhilog_ndk.z.so
        libnative_window.so
        libEGL.so
        libGLESv3.so
        holm_runtime)
# 同目录装机解析；禁止把宿主绝对路径写进 RUNPATH
set_target_properties(holm_xc PROPERTIES
        BUILD_RPATH "\$ORIGIN"
        INSTALL_RPATH "\$ORIGIN"
        BUILD_WITH_INSTALL_RPATH TRUE)
