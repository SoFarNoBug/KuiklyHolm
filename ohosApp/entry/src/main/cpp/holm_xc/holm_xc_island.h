/**
 * @file holm_xc_island.h
 * @brief ADR 0010/0011 · XComponent 岛状态（按 XComponent id 索引）
 * @date 2026-11-15
 * @author Auto
 */
#ifndef HOLM_XC_ISLAND_H
#define HOLM_XC_ISLAND_H

#include "holm_xc_lite_gl.h"

#include <napi/native_api.h>
#include <cstdint>
#include <mutex>
#include <string>
#include <unordered_map>

struct OH_NativeXComponent;

struct HolmXcIsland {
    std::string id;
    std::string executor = "renderer-gl";
    std::string fragmentShader;
    std::string scene;
    bool playing = true;
    int32_t maxFps = 60;
    /** 相对 XComponent 物理像素基线；(0,1]，默认 1=不降采样 */
    float renderScale = 1.0f;
    float liquidBoost = 1.35f;
    float liquidBend = 0.05f;

    uint64_t sessionHandle = 0;
    HolmXcLiteGl liteGl;
    void *nativeWindow = nullptr;
    OH_NativeXComponent *component = nullptr;
    bool frameRegistered = false;
    bool readyFired = false;
    bool blockedNotified = false;
    /** Runtime→Lite：拆 session 后推迟若干 OnFrame 再建 EGL，等 wgpu 放窗 */
    int32_t liteStartDeferFrames = 0;

    uint64_t lastFrameTsNs = 0;
    uint64_t lastDrawTsNs = 0;
    uint64_t perfWindowTsNs = 0;
    uint32_t perfFrames = 0;
    double minFrameIntervalSec = 1.0 / 60.0;

    napi_env env = nullptr;
    napi_ref onReadyRef = nullptr;
    napi_ref onErrorRef = nullptr;
    napi_ref onPerfRef = nullptr;
    napi_ref onShaderCompiledRef = nullptr;

    /** 持 HolmXcMutex 时置 true：Fire* 只入队，解锁后再 Flush，避免 JS 回打 NAPI 死锁。 */
    bool deferJs = false;
    bool pendingReady = false;
    std::string pendingReadyBackend;
    bool pendingError = false;
    std::string pendingErrCode;
    std::string pendingErrMsg;
    bool pendingCompiled = false;
    bool pendingCompiledOk = false;
    double pendingWrapMs = 0.0;
    double pendingCompileMs = 0.0;
    std::string pendingCompiledExec;
};

inline std::mutex &HolmXcMutex()
{
    static std::mutex mu;
    return mu;
}

inline std::unordered_map<std::string, HolmXcIsland> &HolmXcIslands()
{
    static std::unordered_map<std::string, HolmXcIsland> map;
    return map;
}

inline HolmXcIsland &HolmXcEnsure(const std::string &id)
{
    auto &map = HolmXcIslands();
    auto it = map.find(id);
    if (it == map.end()) {
        HolmXcIsland island;
        island.id = id;
        it = map.emplace(id, std::move(island)).first;
    }
    return it->second;
}

#endif /* HOLM_XC_ISLAND_H */
