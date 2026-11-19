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

# ADR 0010/0011 · libholm_xc.so（独立于 libentry）
# Runtime Present 经 KN holm_ohos_bridge_*（dlsym）；不再链 libkmp_wgpu_native（由 libshared 持有）。
# Lite EGL/GLES 仍在本 so。
add_library(holm_xc SHARED
        holm_xc/holm_xc_plugin.cpp
        holm_xc/holm_ohos_bridge.cpp
        holm_xc/holm_xc_lite_gl.cpp
        holm_xc/holm_xc_model_gl.cpp)
target_include_directories(holm_xc PRIVATE
        ${NATIVERENDER_ROOT_PATH}/holm_xc)
target_link_libraries(holm_xc PUBLIC
        libace_napi.z.so
        libace_ndk.z.so
        libhilog_ndk.z.so
        libnative_window.so
        libEGL.so
        libGLESv3.so
        dl)
set_target_properties(holm_xc PROPERTIES
        BUILD_RPATH "\$ORIGIN"
        INSTALL_RPATH "\$ORIGIN"
        BUILD_WITH_INSTALL_RPATH TRUE)
