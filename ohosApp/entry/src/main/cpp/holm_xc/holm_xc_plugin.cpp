/**
 * @file holm_xc_plugin.cpp
 * @brief ADR 0010/0011 · libholm_xc.so：XComponent OnFrame + Runtime/Lite 双轨
 * @date 2026-11-15
 * @author Auto
 */
#include "holm_xc_island.h"
#include "holm_runtime.h"

#include <ace/xcomponent/native_interface_xcomponent.h>
#include <hilog/log.h>
#include <napi/native_api.h>

#include <algorithm>
#include <cctype>
#include <cstring>
#include <string>
#include <vector>

#undef LOG_DOMAIN
#undef LOG_TAG
#define LOG_DOMAIN 0x3200
#define LOG_TAG "HolmXc"

namespace {

constexpr const char *kExecutorRuntime = "runtime";
constexpr const char *kExecutorLite = "lite";
constexpr const char *kBackendGles2Ohos = "gles2-ohos";
constexpr double kNsPerSec = 1e9;

bool NapiOk(napi_status status)
{
    return status == napi_ok;
}

bool ReadUtf8Arg(napi_env env, napi_value arg, std::string &out)
{
    out.clear();
    if (arg == nullptr) {
        return false;
    }
    napi_valuetype t = napi_undefined;
    if (!NapiOk(napi_typeof(env, arg, &t)) || t != napi_string) {
        return false;
    }
    size_t length = 0;
    if (!NapiOk(napi_get_value_string_utf8(env, arg, nullptr, 0, &length))) {
        return false;
    }
    std::vector<char> buf(length + 1, '\0');
    size_t written = 0;
    if (!NapiOk(napi_get_value_string_utf8(env, arg, buf.data(), buf.size(), &written))) {
        return false;
    }
    out.assign(buf.data(), written);
    return true;
}

bool LooksLikeWgsl(const std::string &src)
{
    return src.find("@fragment") != std::string::npos || src.find("fn fs_main") != std::string::npos;
}

std::string TrimCopy(const std::string &src)
{
    size_t start = 0;
    while (start < src.size() &&
           (src[start] == ' ' || src[start] == '\n' || src[start] == '\r' || src[start] == '\t')) {
        ++start;
    }
    size_t end = src.size();
    while (end > start &&
           (src[end - 1] == ' ' || src[end - 1] == '\n' || src[end - 1] == '\r' || src[end - 1] == '\t')) {
        --end;
    }
    return src.substr(start, end - start);
}

void RecomputeFrameInterval(HolmXcIsland &island)
{
    if (island.maxFps <= 0) {
        island.minFrameIntervalSec = 0.0;
    } else {
        island.minFrameIntervalSec = 1.0 / static_cast<double>(island.maxFps);
    }
}

void ApplyExpectedFps(HolmXcIsland &island)
{
    if (island.component == nullptr) {
        return;
    }
    OH_NativeXComponent_ExpectedRateRange range {};
    if (island.maxFps <= 0) {
        range.min = 0;
        range.max = 120;
        range.expected = 60;
    } else {
        range.min = 1;
        range.max = island.maxFps;
        range.expected = island.maxFps;
    }
    OH_NativeXComponent_SetExpectedFrameRateRange(island.component, &range);
}

void FireJsObject(napi_env env, napi_ref ref, napi_value payload)
{
    if (env == nullptr || ref == nullptr || payload == nullptr) {
        return;
    }
    napi_value cb = nullptr;
    if (!NapiOk(napi_get_reference_value(env, ref, &cb)) || cb == nullptr) {
        return;
    }
    napi_valuetype t = napi_undefined;
    if (!NapiOk(napi_typeof(env, cb, &t)) || t != napi_function) {
        return;
    }
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    napi_value result = nullptr;
    napi_call_function(env, undefined, cb, 1, &payload, &result);
}

void FireError(HolmXcIsland &island, const char *code, const char *message)
{
    if (island.env == nullptr || island.onErrorRef == nullptr) {
        return;
    }
    napi_value obj = nullptr;
    if (!NapiOk(napi_create_object(island.env, &obj))) {
        return;
    }
    napi_value codeV = nullptr;
    napi_value msgV = nullptr;
    napi_create_string_utf8(island.env, code != nullptr ? code : "", NAPI_AUTO_LENGTH, &codeV);
    napi_create_string_utf8(island.env, message != nullptr ? message : "", NAPI_AUTO_LENGTH, &msgV);
    napi_set_named_property(island.env, obj, "code", codeV);
    napi_set_named_property(island.env, obj, "message", msgV);
    FireJsObject(island.env, island.onErrorRef, obj);
}

void FireReady(HolmXcIsland &island, const char *backend)
{
    if (island.env == nullptr || island.onReadyRef == nullptr) {
        return;
    }
    napi_value obj = nullptr;
    if (!NapiOk(napi_create_object(island.env, &obj))) {
        return;
    }
    napi_value backendV = nullptr;
    napi_value qualityV = nullptr;
    napi_create_string_utf8(island.env, backend != nullptr ? backend : "wgpu-m0", NAPI_AUTO_LENGTH, &backendV);
    napi_create_string_utf8(island.env, "auto", NAPI_AUTO_LENGTH, &qualityV);
    napi_set_named_property(island.env, obj, "backend", backendV);
    napi_set_named_property(island.env, obj, "quality", qualityV);
    FireJsObject(island.env, island.onReadyRef, obj);
}

void FirePerf(HolmXcIsland &island, double fps)
{
    if (island.env == nullptr || island.onPerfRef == nullptr) {
        return;
    }
    napi_value obj = nullptr;
    if (!NapiOk(napi_create_object(island.env, &obj))) {
        return;
    }
    napi_value fpsV = nullptr;
    napi_create_double(island.env, fps, &fpsV);
    napi_set_named_property(island.env, obj, "fps", fpsV);
    FireJsObject(island.env, island.onPerfRef, obj);
}

bool IsRuntime(const HolmXcIsland &island)
{
    return island.executor == kExecutorRuntime;
}

void DestroyRuntimeSession(HolmXcIsland &island)
{
    if (island.sessionHandle != 0) {
        holm_runtime_session_destroy(island.sessionHandle);
        island.sessionHandle = 0;
    }
}

void DestroyLite(HolmXcIsland &island)
{
    HolmXcLiteGlDestroy(island.liteGl);
}

void UnregisterFrame(HolmXcIsland &island)
{
    if (island.component != nullptr && island.frameRegistered) {
        OH_NativeXComponent_UnregisterOnFrameCallback(island.component);
        island.frameRegistered = false;
    }
    island.lastFrameTsNs = 0;
    island.lastDrawTsNs = 0;
    island.perfWindowTsNs = 0;
    island.perfFrames = 0;
}

void TeardownAllGpu(HolmXcIsland &island)
{
    UnregisterFrame(island);
    DestroyRuntimeSession(island);
    DestroyLite(island);
    island.nativeWindow = nullptr;
}

void GetSurfaceSize(HolmXcIsland &island, void *window, uint32_t &outW, uint32_t &outH)
{
    uint64_t width = 1;
    uint64_t height = 1;
    if (island.component != nullptr && window != nullptr) {
        OH_NativeXComponent_GetXComponentSize(island.component, window, &width, &height);
    }
    outW = static_cast<uint32_t>(std::max<uint64_t>(1, width));
    outH = static_cast<uint32_t>(std::max<uint64_t>(1, height));
}

void ApplyRuntimeShaders(HolmXcIsland &island)
{
    if (island.sessionHandle == 0) {
        return;
    }
    std::string trimmed = TrimCopy(island.fragmentShader);
    if (trimmed.empty()) {
        holm_runtime_session_set_custom_fs(island.sessionHandle, "");
        if (!island.scene.empty()) {
            holm_runtime_session_set_scene(island.sessionHandle, island.scene.c_str());
        }
        return;
    }
    if (!LooksLikeWgsl(trimmed)) {
        return;
    }
    if (holm_runtime_session_set_custom_fs(island.sessionHandle, trimmed.c_str()) == 0) {
        FireError(island, "SHADER_CUSTOM", "Runtime setCustomFs failed");
    }
}

/**
 * @param reportDialectError true=显式 setFragmentShader 时对 WGSL 报 SHADER_DIALECT；
 *        false=切轨途中暂存 WGSL 不报错（等 Compose 换成 LITE_BODY）。
 */
bool ApplyLiteShaders(HolmXcIsland &island, bool reportDialectError)
{
    if (!island.liteGl.ready) {
        return false;
    }
    std::string trimmed = TrimCopy(island.fragmentShader);
    if (trimmed.empty()) {
        FireError(island, "FRAGMENT_REQUIRED", "fragmentShader required (Shadertoy body)");
        return false;
    }
    if (LooksLikeWgsl(trimmed)) {
        if (reportDialectError) {
            FireError(island, "SHADER_DIALECT",
                      "Lite fragmentShader must be GLSL Shadertoy body (WGSL ignored)");
        }
        return false;
    }
    std::string errCode;
    std::string errMsg;
    if (!HolmXcLiteGlSetFragmentBody(island.liteGl, trimmed, &errCode, &errMsg)) {
        FireError(island, errCode.c_str(), errMsg.c_str());
        return false;
    }
    HolmXcLiteGlSetLiquid(island.liteGl, island.liquidBoost, island.liquidBend);
    return true;
}

void NotePerf(HolmXcIsland &island, uint64_t timestamp)
{
    island.perfFrames += 1;
    if (island.perfWindowTsNs == 0) {
        island.perfWindowTsNs = timestamp;
        return;
    }
    double windowSec = static_cast<double>(timestamp - island.perfWindowTsNs) / kNsPerSec;
    if (windowSec >= 1.0) {
        double fps = static_cast<double>(island.perfFrames) / windowSec;
        FirePerf(island, fps);
        island.perfFrames = 0;
        island.perfWindowTsNs = timestamp;
    }
}

void OnFramePump(OH_NativeXComponent *component, uint64_t timestamp, uint64_t /*targetTimestamp*/)
{
    char idBuf[OH_XCOMPONENT_ID_LEN_MAX + 1] = {0};
    uint64_t idSize = OH_XCOMPONENT_ID_LEN_MAX + 1;
    if (OH_NativeXComponent_GetXComponentId(component, idBuf, &idSize) != OH_NATIVEXCOMPONENT_RESULT_SUCCESS) {
        return;
    }
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    auto &map = HolmXcIslands();
    auto it = map.find(idBuf);
    if (it == map.end()) {
        return;
    }
    HolmXcIsland &island = it->second;
    if (!island.playing) {
        return;
    }

    double dt = 1.0 / 60.0;
    if (island.lastFrameTsNs != 0 && timestamp > island.lastFrameTsNs) {
        dt = static_cast<double>(timestamp - island.lastFrameTsNs) / kNsPerSec;
    }
    island.lastFrameTsNs = timestamp;
    if (island.minFrameIntervalSec > 0.0 && island.lastDrawTsNs != 0) {
        double sinceDraw = static_cast<double>(timestamp - island.lastDrawTsNs) / kNsPerSec;
        if (sinceDraw + 1e-9 < island.minFrameIntervalSec) {
            return;
        }
    }

    if (IsRuntime(island)) {
        if (island.sessionHandle == 0) {
            return;
        }
        if (!holm_runtime_session_tick(island.sessionHandle, static_cast<float>(dt))) {
            if (!island.blockedNotified) {
                island.blockedNotified = true;
                FireError(island, "BLOCKED", "holm_runtime tick failed");
            }
            return;
        }
    } else {
        if (!island.liteGl.ready) {
            return;
        }
        HolmXcLiteGlDrawFrame(island.liteGl, static_cast<float>(dt));
    }

    island.lastDrawTsNs = timestamp;
    NotePerf(island, timestamp);
}

void EnsureFrameRegistered(HolmXcIsland &island)
{
    if (island.component == nullptr || island.frameRegistered) {
        return;
    }
    OH_NativeXComponent_RegisterOnFrameCallback(island.component, OnFramePump);
    island.frameRegistered = true;
    ApplyExpectedFps(island);
}

void StartLiteIfNeeded(HolmXcIsland &island, void *window);
void StartRuntimeIfNeeded(HolmXcIsland &island, void *window);

void StartLiteIfNeeded(HolmXcIsland &island, void *window)
{
    DestroyRuntimeSession(island);
    if (window == nullptr) {
        FireError(island, "BLOCKED", "OHNativeWindow null");
        return;
    }
    island.nativeWindow = window;

    uint32_t w = 1;
    uint32_t h = 1;
    GetSurfaceSize(island, window, w, h);

    if (island.liteGl.ready && island.liteGl.nativeWindow == window) {
        std::string errCode;
        std::string errMsg;
        HolmXcLiteGlResize(island.liteGl, w, h, &errCode, &errMsg);
        ApplyLiteShaders(island, false);
        EnsureFrameRegistered(island);
        if (!island.readyFired) {
            island.readyFired = true;
            island.blockedNotified = false;
            FireReady(island, kBackendGles2Ohos);
        }
        return;
    }

    DestroyLite(island);
    std::string errCode;
    std::string errMsg;
    if (!HolmXcLiteGlCreate(island.liteGl, window, w, h, &errCode, &errMsg)) {
        FireError(island, errCode.c_str(), errMsg.c_str());
        island.blockedNotified = true;
        return;
    }
    if (!ApplyLiteShaders(island, false)) {
        // 仍可深蓝清屏；不拆 EGL，便于后续补 fragmentShader / 切轨方言暂态
    }
    EnsureFrameRegistered(island);
    if (!island.readyFired) {
        island.readyFired = true;
        island.blockedNotified = false;
        FireReady(island, kBackendGles2Ohos);
    }
}

void StartRuntimeIfNeeded(HolmXcIsland &island, void *window)
{
    DestroyLite(island);
    if (window == nullptr) {
        FireError(island, "BLOCKED", "OHNativeWindow null");
        return;
    }
    if (island.sessionHandle != 0) {
        return;
    }
    if (holm_runtime_has_wgpu() == 0) {
        FireError(island, "BLOCKED", "holm_runtime has_wgpu=0. No silent GLES stand-in.");
        island.blockedNotified = true;
        return;
    }

    uint32_t w = 1;
    uint32_t h = 1;
    GetSurfaceSize(island, window, w, h);

    uint64_t handle = holm_runtime_session_create_ohos(window, w, h);
    if (handle == 0) {
        FireError(island, "BLOCKED", "holm_runtime_session_create_ohos failed. No silent GLES stand-in.");
        island.blockedNotified = true;
        return;
    }
    island.sessionHandle = handle;
    island.nativeWindow = window;
    holm_runtime_session_set_liquid(island.sessionHandle, island.liquidBoost, island.liquidBend);
    holm_runtime_session_set_playing(island.sessionHandle, island.playing ? 1u : 0u);
    ApplyRuntimeShaders(island);

    char backendBuf[64] = {0};
    holm_runtime_session_backend_id(island.sessionHandle, backendBuf, sizeof(backendBuf));
    const char *backend = backendBuf[0] != '\0' ? backendBuf : "wgpu-m0";

    if (!holm_runtime_session_tick(island.sessionHandle, 1.f / 60.f)) {
        DestroyRuntimeSession(island);
        FireError(island, "BLOCKED", "holm_runtime tick failed after create. No silent GLES stand-in.");
        island.blockedNotified = true;
        return;
    }

    EnsureFrameRegistered(island);
    if (!island.readyFired) {
        island.readyFired = true;
        island.blockedNotified = false;
        FireReady(island, backend);
    }
}

void StartExecutorIfNeeded(HolmXcIsland &island, void *window)
{
    if (IsRuntime(island)) {
        StartRuntimeIfNeeded(island, window);
    } else {
        StartLiteIfNeeded(island, window);
    }
}

std::string ReadXcId(OH_NativeXComponent *component)
{
    char idBuf[OH_XCOMPONENT_ID_LEN_MAX + 1] = {0};
    uint64_t idSize = OH_XCOMPONENT_ID_LEN_MAX + 1;
    if (OH_NativeXComponent_GetXComponentId(component, idBuf, &idSize) != OH_NATIVEXCOMPONENT_RESULT_SUCCESS) {
        return {};
    }
    return std::string(idBuf);
}

void OnSurfaceCreated(OH_NativeXComponent *component, void *window)
{
    std::string id = ReadXcId(component);
    if (id.empty()) {
        OH_LOG_ERROR(LOG_APP, "OnSurfaceCreated: empty id");
        return;
    }
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    HolmXcIsland &island = HolmXcEnsure(id);
    island.component = component;
    RecomputeFrameInterval(island);
    StartExecutorIfNeeded(island, window);
}

void OnSurfaceChanged(OH_NativeXComponent *component, void *window)
{
    std::string id = ReadXcId(component);
    if (id.empty()) {
        return;
    }
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    auto it = HolmXcIslands().find(id);
    if (it == HolmXcIslands().end()) {
        return;
    }
    HolmXcIsland &island = it->second;
    island.component = component;
    island.nativeWindow = window;

    uint32_t w = 1;
    uint32_t h = 1;
    GetSurfaceSize(island, window, w, h);

    if (IsRuntime(island)) {
        if (island.sessionHandle == 0) {
            StartRuntimeIfNeeded(island, window);
            return;
        }
        holm_runtime_session_resize(island.sessionHandle, w, h);
        return;
    }

    if (!island.liteGl.ready) {
        StartLiteIfNeeded(island, window);
        return;
    }
    std::string errCode;
    std::string errMsg;
    if (!HolmXcLiteGlResize(island.liteGl, w, h, &errCode, &errMsg)) {
        FireError(island, errCode.c_str(), errMsg.c_str());
    }
}

void OnSurfaceDestroyed(OH_NativeXComponent *component, void * /*window*/)
{
    std::string id = ReadXcId(component);
    if (id.empty()) {
        return;
    }
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    auto it = HolmXcIslands().find(id);
    if (it == HolmXcIslands().end()) {
        return;
    }
    TeardownAllGpu(it->second);
    it->second.component = nullptr;
    it->second.readyFired = false;
}

void DispatchTouchEvent(OH_NativeXComponent * /*component*/, void * /*window*/) {}

OH_NativeXComponent_Callback gXcCallback = {
    .OnSurfaceCreated = OnSurfaceCreated,
    .OnSurfaceChanged = OnSurfaceChanged,
    .OnSurfaceDestroyed = OnSurfaceDestroyed,
    .DispatchTouchEvent = DispatchTouchEvent,
};

HolmXcIsland *IslandFromArg0(napi_env env, napi_callback_info info, size_t minArgs)
{
    size_t argc = 8;
    napi_value args[8] = {nullptr};
    if (!NapiOk(napi_get_cb_info(env, info, &argc, args, nullptr, nullptr)) || argc < minArgs) {
        return nullptr;
    }
    std::string id;
    if (!ReadUtf8Arg(env, args[0], id) || id.empty()) {
        return nullptr;
    }
    HolmXcIsland &island = HolmXcEnsure(id);
    island.env = env;
    return &island;
}

napi_value NapiHasWgpu(napi_env env, napi_callback_info /*info*/)
{
    napi_value result = nullptr;
    napi_create_int32(env, static_cast<int32_t>(holm_runtime_has_wgpu()), &result);
    return result;
}

napi_value NapiAbiVersion(napi_env env, napi_callback_info /*info*/)
{
    napi_value result = nullptr;
    napi_create_int32(env, static_cast<int32_t>(holm_runtime_abi_version()), &result);
    return result;
}

napi_value NapiEnsureIsland(napi_env env, napi_callback_info info)
{
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 1);
    napi_value result = nullptr;
    napi_get_boolean(env, island != nullptr, &result);
    return result;
}

napi_value NapiSetExecutor(napi_env env, napi_callback_info info)
{
    size_t argc = 2;
    napi_value args[2] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 2);
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    if (island == nullptr) {
        return undefined;
    }
    std::string exec;
    ReadUtf8Arg(env, args[1], exec);
    for (auto &c : exec) {
        c = static_cast<char>(tolower(c));
    }
    if (exec != kExecutorRuntime) {
        exec = kExecutorLite;
    }
    if (exec == island->executor) {
        return undefined;
    }
    island->executor = exec;
    island->readyFired = false;
    island->blockedNotified = false;
    // 切轨：先卸当前轨 GPU，再启目标轨（同 window）
    if (IsRuntime(*island)) {
        DestroyLite(*island);
        StartRuntimeIfNeeded(*island, island->nativeWindow);
    } else {
        DestroyRuntimeSession(*island);
        StartLiteIfNeeded(*island, island->nativeWindow);
    }
    return undefined;
}

napi_value NapiSetFragmentShader(napi_env env, napi_callback_info info)
{
    size_t argc = 2;
    napi_value args[2] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 2);
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    if (island == nullptr) {
        return undefined;
    }
    ReadUtf8Arg(env, args[1], island->fragmentShader);
    if (IsRuntime(*island)) {
        if (island->sessionHandle != 0) {
            ApplyRuntimeShaders(*island);
        }
    } else if (island->liteGl.ready) {
        ApplyLiteShaders(*island, true);
    }
    return undefined;
}

napi_value NapiSetPlaying(napi_env env, napi_callback_info info)
{
    size_t argc = 2;
    napi_value args[2] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 2);
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    if (island == nullptr) {
        return undefined;
    }
    bool playing = true;
    napi_get_value_bool(env, args[1], &playing);
    island->playing = playing;
    if (island->sessionHandle != 0) {
        holm_runtime_session_set_playing(island->sessionHandle, playing ? 1u : 0u);
    }
    return undefined;
}

napi_value NapiSetMaxFps(napi_env env, napi_callback_info info)
{
    size_t argc = 2;
    napi_value args[2] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 2);
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    if (island == nullptr) {
        return undefined;
    }
    int32_t fps = 60;
    napi_get_value_int32(env, args[1], &fps);
    if (fps < 0) {
        fps = 0;
    }
    island->maxFps = fps;
    RecomputeFrameInterval(*island);
    ApplyExpectedFps(*island);
    return undefined;
}

napi_value NapiSetScene(napi_env env, napi_callback_info info)
{
    size_t argc = 2;
    napi_value args[2] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 2);
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    if (island == nullptr) {
        return undefined;
    }
    ReadUtf8Arg(env, args[1], island->scene);
    if (island->sessionHandle != 0) {
        holm_runtime_session_set_scene(island->sessionHandle, island->scene.c_str());
    }
    return undefined;
}

napi_value NapiSetLiquid(napi_env env, napi_callback_info info)
{
    size_t argc = 3;
    napi_value args[3] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 3);
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    if (island == nullptr) {
        return undefined;
    }
    double boost = island->liquidBoost;
    double bend = island->liquidBend;
    napi_get_value_double(env, args[1], &boost);
    napi_get_value_double(env, args[2], &bend);
    island->liquidBoost = static_cast<float>(boost);
    island->liquidBend = static_cast<float>(bend);
    if (island->sessionHandle != 0) {
        holm_runtime_session_set_liquid(island->sessionHandle, island->liquidBoost, island->liquidBend);
    }
    if (island->liteGl.ready) {
        HolmXcLiteGlSetLiquid(island->liteGl, island->liquidBoost, island->liquidBend);
    }
    return undefined;
}

napi_value NapiSetCallbacks(napi_env env, napi_callback_info info)
{
    size_t argc = 4;
    napi_value args[4] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 1);
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    if (island == nullptr) {
        return undefined;
    }
    auto replaceRef = [&](napi_ref &slot, size_t idx) {
        if (slot != nullptr) {
            napi_delete_reference(env, slot);
            slot = nullptr;
        }
        if (idx >= argc || args[idx] == nullptr) {
            return;
        }
        napi_valuetype t = napi_undefined;
        if (!NapiOk(napi_typeof(env, args[idx], &t)) || t != napi_function) {
            return;
        }
        napi_create_reference(env, args[idx], 1, &slot);
    };
    replaceRef(island->onReadyRef, 1);
    replaceRef(island->onErrorRef, 2);
    replaceRef(island->onPerfRef, 3);
    island->env = env;
    return undefined;
}

napi_value NapiDestroyIsland(napi_env env, napi_callback_info info)
{
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    std::string id;
    ReadUtf8Arg(env, args[0], id);
    std::lock_guard<std::mutex> lock(HolmXcMutex());
    auto it = HolmXcIslands().find(id);
    if (it != HolmXcIslands().end()) {
        TeardownAllGpu(it->second);
        if (it->second.onReadyRef) {
            napi_delete_reference(env, it->second.onReadyRef);
        }
        if (it->second.onErrorRef) {
            napi_delete_reference(env, it->second.onErrorRef);
        }
        if (it->second.onPerfRef) {
            napi_delete_reference(env, it->second.onPerfRef);
        }
        HolmXcIslands().erase(it);
    }
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    return undefined;
}

void ExportFn(napi_env env, napi_value exports, const char *name, napi_callback cb)
{
    napi_value fn = nullptr;
    napi_create_function(env, name, NAPI_AUTO_LENGTH, cb, nullptr, &fn);
    napi_set_named_property(env, exports, name, fn);
}

napi_value Init(napi_env env, napi_value exports)
{
    napi_value exportInstance = nullptr;
    if (NapiOk(napi_get_named_property(env, exports, OH_NATIVE_XCOMPONENT_OBJ, &exportInstance)) &&
        exportInstance != nullptr) {
        OH_NativeXComponent *nativeXComponent = nullptr;
        if (NapiOk(napi_unwrap(env, exportInstance, reinterpret_cast<void **>(&nativeXComponent))) &&
            nativeXComponent != nullptr) {
            OH_NativeXComponent_RegisterCallback(nativeXComponent, &gXcCallback);
        }
    }

    ExportFn(env, exports, "hasWgpu", NapiHasWgpu);
    ExportFn(env, exports, "abiVersion", NapiAbiVersion);
    ExportFn(env, exports, "ensureIsland", NapiEnsureIsland);
    ExportFn(env, exports, "setExecutor", NapiSetExecutor);
    ExportFn(env, exports, "setFragmentShader", NapiSetFragmentShader);
    ExportFn(env, exports, "setPlaying", NapiSetPlaying);
    ExportFn(env, exports, "setMaxFps", NapiSetMaxFps);
    ExportFn(env, exports, "setScene", NapiSetScene);
    ExportFn(env, exports, "setLiquid", NapiSetLiquid);
    ExportFn(env, exports, "setCallbacks", NapiSetCallbacks);
    ExportFn(env, exports, "destroyIsland", NapiDestroyIsland);
    return exports;
}

static napi_module gHolmXcModule = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "holm_xc",
    .nm_priv = nullptr,
    .reserved = {0},
};

} // namespace

extern "C" __attribute__((constructor)) void RegisterHolmXcModule(void)
{
    napi_module_register(&gHolmXcModule);
}
