/**
 * @file holm_xc_plugin.cpp
 * @brief ADR 0010/0011 · libholm_xc.so：XComponent OnFrame + Runtime/Lite 双轨
 * Present 经 KN holm_ohos_bridge_*（dlsym）；禁止直调 holm_runtime_session_*。
 * @date 2026-11-16
 * @author jlj
 */
#include "holm_xc_island.h"
#include "holm_ohos_bridge.h"

#include <ace/xcomponent/native_interface_xcomponent.h>
#include <hilog/log.h>
#include <napi/native_api.h>
#include <native_window/external_window.h>

#include <algorithm>
#include <cctype>
#include <chrono>
#include <cmath>
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

HolmOhosBridgeApi g_bridgeApi {};
bool g_bridgeResolved = false;
bool g_bridgeResolveAttempted = false;

bool EnsureBridgeResolved()
{
    if (g_bridgeResolved) {
        return true;
    }
    if (!g_bridgeResolveAttempted) {
        g_bridgeResolveAttempted = true;
        g_bridgeResolved = HolmOhosBridgeResolve(&g_bridgeApi);
    }
    return g_bridgeResolved;
}

const HolmOhosBridgeApi &Bridge()
{
    return g_bridgeApi;
}

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
    // 仅低档（Demo 的 30）软件掐帧；≥55 或 0（不限）交给 ExpectedRange + 面板 vsync。
    // 否则 maxFps=60 时 1/60 再掐会在 60Hz 抖动下隔帧丢弃 → 实测 ~30。
    if (island.maxFps > 0 && island.maxFps < 55) {
        island.minFrameIntervalSec = 1.0 / static_cast<double>(island.maxFps);
    } else {
        island.minFrameIntervalSec = 0.0;
    }
}

void ApplyExpectedFps(HolmXcIsland &island)
{
    if (island.component == nullptr) {
        return;
    }
    OH_NativeXComponent_ExpectedRateRange range {};
    if (island.maxFps <= 0) {
        // 对齐 Android unlimited → 峰值档提示（面板若仅 60Hz 仍停在 60）
        range.min = 0;
        range.max = 120;
        range.expected = 120;
    } else if (island.maxFps < 55) {
        // 低档：ExpectedRange 仍要密回调；真正限到 maxFps 靠软件间隔。
        // 若 expected=maxFps=30 再软件 1/30，会再次隔帧 → ~15。
        range.min = 1;
        range.max = 60;
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
    OH_LOG_ERROR(LOG_APP, "FireError %{public}s: %{public}s", code != nullptr ? code : "",
                 message != nullptr ? message : "");
    if (island.deferJs) {
        island.pendingError = true;
        island.pendingErrCode = code != nullptr ? code : "";
        island.pendingErrMsg = message != nullptr ? message : "";
        return;
    }
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
    if (island.deferJs) {
        island.pendingReady = true;
        island.pendingReadyBackend = backend != nullptr ? backend : "wgpu-m0";
        return;
    }
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

void FireShaderCompiled(HolmXcIsland &island, bool ok, double wrapMs, double compileMs,
                        const char *executor)
{
    if (island.deferJs) {
        island.pendingCompiled = true;
        island.pendingCompiledOk = ok;
        island.pendingWrapMs = wrapMs;
        island.pendingCompileMs = compileMs;
        island.pendingCompiledExec = executor != nullptr ? executor : "";
        return;
    }
    if (island.env == nullptr || island.onShaderCompiledRef == nullptr) {
        return;
    }
    napi_value obj = nullptr;
    if (!NapiOk(napi_create_object(island.env, &obj))) {
        return;
    }
    napi_value okV = nullptr;
    napi_value wrapV = nullptr;
    napi_value compileV = nullptr;
    napi_value execV = nullptr;
    napi_get_boolean(island.env, ok, &okV);
    napi_create_double(island.env, wrapMs, &wrapV);
    napi_create_double(island.env, compileMs, &compileV);
    napi_create_string_utf8(island.env, executor != nullptr ? executor : "", NAPI_AUTO_LENGTH, &execV);
    napi_set_named_property(island.env, obj, "ok", okV);
    napi_set_named_property(island.env, obj, "wrapMs", wrapV);
    napi_set_named_property(island.env, obj, "compileMs", compileV);
    napi_set_named_property(island.env, obj, "executor", execV);
    FireJsObject(island.env, island.onShaderCompiledRef, obj);
}

void FlushDeferredJs(HolmXcIsland &island)
{
    const bool ready = island.pendingReady;
    const std::string readyBackend = island.pendingReadyBackend;
    const bool err = island.pendingError;
    const std::string errCode = island.pendingErrCode;
    const std::string errMsg = island.pendingErrMsg;
    const bool compiled = island.pendingCompiled;
    const bool compiledOk = island.pendingCompiledOk;
    const double wrapMs = island.pendingWrapMs;
    const double compileMs = island.pendingCompileMs;
    const std::string compiledExec = island.pendingCompiledExec;
    island.pendingReady = false;
    island.pendingError = false;
    island.pendingCompiled = false;
    island.pendingReadyBackend.clear();
    island.pendingErrCode.clear();
    island.pendingErrMsg.clear();
    island.pendingCompiledExec.clear();
    island.deferJs = false;
    if (ready) {
        FireReady(island, readyBackend.c_str());
    }
    if (err) {
        FireError(island, errCode.c_str(), errMsg.c_str());
    }
    if (compiled) {
        FireShaderCompiled(island, compiledOk, wrapMs, compileMs, compiledExec.c_str());
    }
}

bool IsRuntime(const HolmXcIsland &island)
{
    return island.executor == kExecutorRuntime;
}

void DestroyRuntimeSession(HolmXcIsland &island)
{
    if (island.sessionHandle != 0) {
        if (EnsureBridgeResolved()) {
            Bridge().destroy(static_cast<int64_t>(island.sessionHandle));
        }
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

float NormalizeRenderScale(float value)
{
    if (!(value > 0.0f) || !std::isfinite(value)) {
        return 1.0f;
    }
    if (value > 1.0f) {
        return 1.0f;
    }
    return value;
}

void GetSurfaceSize(HolmXcIsland &island, void *window, uint32_t &outW, uint32_t &outH)
{
    uint64_t width = 1;
    uint64_t height = 1;
    if (island.component != nullptr && window != nullptr) {
        OH_NativeXComponent_GetXComponentSize(island.component, window, &width, &height);
    }
    const uint32_t baseW = static_cast<uint32_t>(std::max<uint64_t>(1, width));
    const uint32_t baseH = static_cast<uint32_t>(std::max<uint64_t>(1, height));
    const float scale = NormalizeRenderScale(island.renderScale);
    outW = static_cast<uint32_t>(std::max(1.0f, std::floor(static_cast<float>(baseW) * scale)));
    outH = static_cast<uint32_t>(std::max(1.0f, std::floor(static_cast<float>(baseH) * scale)));

    // 真降采样：缩 NativeWindow 缓冲；失败仍用逻辑尺寸喂 Lite/Runtime
    if (window != nullptr) {
        auto *nw = reinterpret_cast<OHNativeWindow *>(window);
        const int32_t code = OH_NativeWindow_NativeWindowHandleOpt(
            nw, SET_BUFFER_GEOMETRY, static_cast<int32_t>(outW), static_cast<int32_t>(outH));
        if (code != 0) {
            OH_LOG_WARN(LOG_APP,
                "SET_BUFFER_GEOMETRY %{public}ux%{public}u failed code=%{public}d; use logical size",
                outW, outH, code);
        }
    }
}

void ApplyDrawSizeNow(HolmXcIsland &island)
{
    if (island.nativeWindow == nullptr) {
        return;
    }
    uint32_t w = 1;
    uint32_t h = 1;
    GetSurfaceSize(island, island.nativeWindow, w, h);
    if (IsRuntime(island)) {
        if (island.sessionHandle != 0 && EnsureBridgeResolved()) {
            Bridge().resize(static_cast<int64_t>(island.sessionHandle), static_cast<int32_t>(w),
                static_cast<int32_t>(h));
        }
        return;
    }
    if (!island.liteGl.ready) {
        return;
    }
    std::string errCode;
    std::string errMsg;
    if (!HolmXcLiteGlResize(island.liteGl, w, h, &errCode, &errMsg)) {
        FireError(island, errCode.c_str(), errMsg.c_str());
    }
}

void ApplyRuntimeShaders(HolmXcIsland &island)
{
    if (island.sessionHandle == 0 || !EnsureBridgeResolved()) {
        return;
    }
    const int64_t handle = static_cast<int64_t>(island.sessionHandle);
    std::string trimmed = TrimCopy(island.fragmentShader);
    if (trimmed.empty()) {
        Bridge().setCustomFs(handle, "");
        if (!island.scene.empty()) {
            Bridge().setScene(handle, island.scene.c_str());
        }
        // 空片元 ClearTri：ok=false，对齐 Lite 拒绝语义（审查 P2）
        FireShaderCompiled(island, false, 0.0, 0.0, kExecutorRuntime);
        return;
    }
    if (!LooksLikeWgsl(trimmed)) {
        FireShaderCompiled(island, false, 0.0, 0.0, kExecutorRuntime);
        return;
    }
    using Clock = std::chrono::steady_clock;
    auto t0 = Clock::now();
    int ok = Bridge().setCustomFs(handle, trimmed.c_str());
    double compileMs = std::chrono::duration<double, std::milli>(Clock::now() - t0).count();
    if (ok == 0) {
        char codeBuf[64] = {0};
        char msgBuf[512] = {0};
        Bridge().lastErrorCode(handle, codeBuf, static_cast<int32_t>(sizeof(codeBuf)));
        Bridge().lastErrorMessage(handle, msgBuf, static_cast<int32_t>(sizeof(msgBuf)));
        const char *code = codeBuf[0] ? codeBuf : "SHADER_CUSTOM";
        const char *msg = msgBuf[0] ? msgBuf : "Runtime setCustomFs failed";
        FireError(island, code, msg);
        FireShaderCompiled(island, false, 0.0, compileMs, kExecutorRuntime);
    } else {
        FireShaderCompiled(island, true, 0.0, compileMs, kExecutorRuntime);
    }
}

/**
 * @param reportDialectError true=显式 setFragmentShader 时对 WGSL 报 SHADER_DIALECT；
 *        false=切轨途中暂存 WGSL 不报错（等 Compose 换成 LITE_BODY）。
 */
bool ApplyLiteShaders(HolmXcIsland &island, bool reportDialectError)
{
    if (!island.liteGl.ready) {
        // Surface 未就绪也结束本次尝试，避免业务干等（审查 P2）
        FireShaderCompiled(island, false, 0.0, 0.0, kExecutorLite);
        return false;
    }
    std::string trimmed = TrimCopy(island.fragmentShader);
    if (trimmed.empty()) {
        // 首包未到：不抢报；已 onReady 后被清空才 FRAGMENT_REQUIRED（对齐 iOS）
        if (island.readyFired) {
            FireError(island, "FRAGMENT_REQUIRED", "fragmentShader required (Shadertoy body)");
            FireShaderCompiled(island, false, 0.0, 0.0, kExecutorLite);
        }
        return false;
    }
    if (LooksLikeWgsl(trimmed)) {
        if (reportDialectError) {
            FireError(island, "SHADER_DIALECT",
                      "Lite fragmentShader must be GLSL Shadertoy body (WGSL ignored)");
            FireShaderCompiled(island, false, 0.0, 0.0, kExecutorLite);
        }
        // 切轨暂态仍持 WGSL：不报 compile fail，等 Compose 换成 LITE_BODY
        return false;
    }
    std::string errCode;
    std::string errMsg;
    double wrapMs = 0.0;
    double compileMs = 0.0;
    if (!HolmXcLiteGlSetFragmentBody(island.liteGl, trimmed, &errCode, &errMsg, &wrapMs, &compileMs)) {
        FireError(island, errCode.c_str(), errMsg.c_str());
        FireShaderCompiled(island, false, wrapMs, compileMs, kExecutorLite);
        return false;
    }
    HolmXcLiteGlSetLiquid(island.liteGl, island.liquidBoost, island.liquidBend);
    FireShaderCompiled(island, true, wrapMs, compileMs, kExecutorLite);
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

void StartLiteIfNeeded(HolmXcIsland &island, void *window);

void OnFramePump(OH_NativeXComponent *component, uint64_t timestamp, uint64_t /*targetTimestamp*/)
{
    char idBuf[OH_XCOMPONENT_ID_LEN_MAX + 1] = {0};
    uint64_t idSize = OH_XCOMPONENT_ID_LEN_MAX + 1;
    if (OH_NativeXComponent_GetXComponentId(component, idBuf, &idSize) != OH_NATIVEXCOMPONENT_RESULT_SUCCESS) {
        return;
    }
    std::unique_lock<std::mutex> lock(HolmXcMutex());
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
        // 1ms 容差：避免与 vsync 时间戳贴边时隔帧丢一半
        if (sinceDraw + 0.001 < island.minFrameIntervalSec) {
            return;
        }
    }

    if (IsRuntime(island)) {
        if (island.sessionHandle == 0 || !EnsureBridgeResolved()) {
            return;
        }
        const int64_t handle = static_cast<int64_t>(island.sessionHandle);
        if (Bridge().tick(handle, static_cast<float>(dt)) == 0) {
            if (!island.blockedNotified) {
                island.blockedNotified = true;
                char codeBuf[64] = {0};
                char msgBuf[512] = {0};
                Bridge().lastErrorCode(handle, codeBuf, static_cast<int32_t>(sizeof(codeBuf)));
                Bridge().lastErrorMessage(handle, msgBuf, static_cast<int32_t>(sizeof(msgBuf)));
                const char *code = codeBuf[0] ? codeBuf : "TICK";
                const char *msg = msgBuf[0] ? msgBuf : "holm_runtime tick failed";
                FireError(island, code, msg);
            }
            return;
        }
    } else {
        if (!island.liteGl.ready) {
            if (island.liteStartDeferFrames > 0) {
                island.liteStartDeferFrames -= 1;
                if (island.liteStartDeferFrames == 0 && island.nativeWindow != nullptr) {
                    island.deferJs = true;
                    StartLiteIfNeeded(island, island.nativeWindow);
                    lock.unlock();
                    FlushDeferredJs(island);
                }
            }
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

void StartRuntimeIfNeeded(HolmXcIsland &island, void *window);

void StartLiteIfNeeded(HolmXcIsland &island, void *window)
{
    if (window == nullptr) {
        FireError(island, "BLOCKED", "OHNativeWindow null");
        return;
    }
    if (!EnsureBridgeResolved()) {
        FireError(island, "BLOCKED",
            "holm_ohos_bridge unresolved (libshared not loaded / missing @CName). No silent GLES stand-in.");
        island.blockedNotified = true;
        return;
    }
    {
        char errBuf[512] = {0};
        if (Bridge().requireProvider(kExecutorLite, errBuf, static_cast<int>(sizeof(errBuf))) != 0) {
            const char *msg = errBuf[0] ? errBuf : "HolmProvider missing for lite";
            FireError(island, "BLOCKED", msg);
            island.blockedNotified = true;
            return;
        }
    }
    island.nativeWindow = window;

    // 刚拆 Runtime：先放窗，推迟几帧再建 EGL（避免 MakeCurrent 空壳）
    if (island.sessionHandle != 0) {
        UnregisterFrame(island);
        DestroyRuntimeSession(island);
        {
            EGLDisplay d = eglGetDisplay(EGL_DEFAULT_DISPLAY);
            if (d != EGL_NO_DISPLAY) {
                eglMakeCurrent(d, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            }
            eglReleaseThread();
        }
        island.liteStartDeferFrames = 5;
        EnsureFrameRegistered(island);
        return;
    }
    island.liteStartDeferFrames = 0;

    uint32_t w = 1;
    uint32_t h = 1;
    GetSurfaceSize(island, window, w, h);

    if (island.liteGl.ready && island.liteGl.nativeWindow == window) {
        std::string errCode;
        std::string errMsg;
        HolmXcLiteGlResize(island.liteGl, w, h, &errCode, &errMsg);
        ApplyLiteShaders(island, false);
        if (!LooksLikeWgsl(TrimCopy(island.fragmentShader))) {
            ApplyLiteShaders(island, true);
        }
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
        // 再试一次：部分机型上 wgpu 拆 session 后首帧仍占窗
        eglReleaseThread();
        errCode.clear();
        errMsg.clear();
        if (!HolmXcLiteGlCreate(island.liteGl, window, w, h, &errCode, &errMsg)) {
            FireError(island, errCode.c_str(), errMsg.c_str());
            island.blockedNotified = true;
            return;
        }
    }
    if (!ApplyLiteShaders(island, false)) {
        // 仍可深蓝清屏；不拆 EGL，便于后续补 fragmentShader / 切轨方言暂态
    }
    if (!LooksLikeWgsl(TrimCopy(island.fragmentShader))) {
        ApplyLiteShaders(island, true);
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
    if (!EnsureBridgeResolved()) {
        FireError(island, "BLOCKED",
            "holm_ohos_bridge unresolved (libshared not loaded / missing @CName). No silent GLES stand-in.");
        island.blockedNotified = true;
        return;
    }
    {
        char errBuf[512] = {0};
        if (Bridge().requireProvider(kExecutorRuntime, errBuf, static_cast<int>(sizeof(errBuf))) != 0) {
            const char *msg = errBuf[0] ? errBuf : "HolmProvider missing for runtime";
            FireError(island, "BLOCKED", msg);
            island.blockedNotified = true;
            return;
        }
    }
    if (Bridge().hasWgpu() == 0) {
        FireError(island, "BLOCKED", "holm_runtime has_wgpu=0. No silent GLES stand-in.");
        island.blockedNotified = true;
        return;
    }

    uint32_t w = 1;
    uint32_t h = 1;
    GetSurfaceSize(island, window, w, h);

    int64_t handle = Bridge().create(window, static_cast<int32_t>(w), static_cast<int32_t>(h));
    if (handle == 0) {
        char createErr[512] = {0};
        Bridge().lastCreateError(createErr, static_cast<int32_t>(sizeof(createErr)));
        const char *msg = createErr[0]
            ? createErr
            : "holm_ohos_bridge_create failed. No silent GLES stand-in.";
        OH_LOG_ERROR(LOG_APP, "create_ohos via bridge failed: %{public}s", msg);
        FireError(island, "BLOCKED", msg);
        island.blockedNotified = true;
        return;
    }
    island.sessionHandle = static_cast<uint64_t>(handle);
    island.nativeWindow = window;
    Bridge().setLiquid(handle, island.liquidBoost, island.liquidBend);
    Bridge().setPlaying(handle, island.playing ? 1 : 0);
    ApplyRuntimeShaders(island);

    char backendBuf[64] = {0};
    Bridge().backendId(handle, backendBuf, static_cast<int32_t>(sizeof(backendBuf)));
    const char *backend = backendBuf[0] != '\0' ? backendBuf : "wgpu-m0";

    if (Bridge().tick(handle, 1.f / 60.f) == 0) {
        char codeBuf[64] = {0};
        char msgBuf[512] = {0};
        Bridge().lastErrorCode(handle, codeBuf, static_cast<int32_t>(sizeof(codeBuf)));
        Bridge().lastErrorMessage(handle, msgBuf, static_cast<int32_t>(sizeof(msgBuf)));
        const char *code = codeBuf[0] ? codeBuf : "TICK";
        const char *msg = msgBuf[0]
            ? msgBuf
            : "holm_runtime tick failed after create. No silent GLES stand-in.";
        DestroyRuntimeSession(island);
        FireError(island, code, msg);
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
    std::unique_lock<std::mutex> lock(HolmXcMutex());
    HolmXcIsland &island = HolmXcEnsure(id);
    island.component = component;
    // 先存 window：Runtime create 失败时也要保留，否则切 Lite 会 OHNativeWindow null
    island.nativeWindow = window;
    RecomputeFrameInterval(island);
    island.deferJs = true;
    StartExecutorIfNeeded(island, window);
    lock.unlock();
    FlushDeferredJs(island);
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
        if (EnsureBridgeResolved()) {
            Bridge().resize(static_cast<int64_t>(island.sessionHandle), static_cast<int32_t>(w),
                static_cast<int32_t>(h));
        }
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
    int32_t value = 0;
    if (EnsureBridgeResolved()) {
        value = Bridge().hasWgpu();
    }
    napi_create_int32(env, value, &result);
    return result;
}

napi_value NapiAbiVersion(napi_env env, napi_callback_info /*info*/)
{
    // Present 经 KN；壳不再直读 holm_runtime_abi_version。未知时回 0。
    napi_value result = nullptr;
    napi_create_int32(env, 0, &result);
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
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    std::unique_lock<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 2);
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
    island->deferJs = true;
    // 切轨：先卸当前轨 GPU，再启目标轨（同 window）
    if (IsRuntime(*island)) {
        DestroyLite(*island);
        StartRuntimeIfNeeded(*island, island->nativeWindow);
    } else {
        DestroyRuntimeSession(*island);
        StartLiteIfNeeded(*island, island->nativeWindow);
    }
    lock.unlock();
    FlushDeferredJs(*island);
    return undefined;
}

napi_value NapiSetFragmentShader(napi_env env, napi_callback_info info)
{
    size_t argc = 2;
    napi_value args[2] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    std::unique_lock<std::mutex> lock(HolmXcMutex());
    HolmXcIsland *island = IslandFromArg0(env, info, 2);
    if (island == nullptr) {
        return undefined;
    }
    ReadUtf8Arg(env, args[1], island->fragmentShader);
    island->deferJs = true;
    if (IsRuntime(*island)) {
        if (island->sessionHandle != 0) {
            ApplyRuntimeShaders(*island);
        }
    } else if (island->liteGl.ready) {
        ApplyLiteShaders(*island, true);
    }
    lock.unlock();
    FlushDeferredJs(*island);
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
    if (island->sessionHandle != 0 && EnsureBridgeResolved()) {
        Bridge().setPlaying(static_cast<int64_t>(island->sessionHandle), playing ? 1 : 0);
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

napi_value NapiSetRenderScale(napi_env env, napi_callback_info info)
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
    double raw = 1.0;
    napi_get_value_double(env, args[1], &raw);
    const float next = NormalizeRenderScale(static_cast<float>(raw));
    if (std::fabs(next - island->renderScale) < 1e-6f) {
        return undefined;
    }
    island->renderScale = next;
    ApplyDrawSizeNow(*island);
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
    if (island->sessionHandle != 0 && EnsureBridgeResolved()) {
        Bridge().setScene(static_cast<int64_t>(island->sessionHandle), island->scene.c_str());
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
    if (island->sessionHandle != 0 && EnsureBridgeResolved()) {
        Bridge().setLiquid(static_cast<int64_t>(island->sessionHandle), island->liquidBoost,
            island->liquidBend);
    }
    if (island->liteGl.ready) {
        HolmXcLiteGlSetLiquid(island->liteGl, island->liquidBoost, island->liquidBend);
    }
    return undefined;
}

napi_value NapiSetCallbacks(napi_env env, napi_callback_info info)
{
    size_t argc = 5;
    napi_value args[5] = {nullptr};
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
    replaceRef(island->onShaderCompiledRef, 4);
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
        if (it->second.onShaderCompiledRef) {
            napi_delete_reference(env, it->second.onShaderCompiledRef);
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
    ExportFn(env, exports, "setRenderScale", NapiSetRenderScale);
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
