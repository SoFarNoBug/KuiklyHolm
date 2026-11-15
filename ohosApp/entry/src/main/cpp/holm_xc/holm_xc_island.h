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
    std::string executor = "lite";
    std::string fragmentShader;
    std::string scene;
    bool playing = true;
    int32_t maxFps = 60;
    float liquidBoost = 1.35f;
    float liquidBend = 0.05f;

    uint64_t sessionHandle = 0;
    HolmXcLiteGl liteGl;
    void *nativeWindow = nullptr;
    OH_NativeXComponent *component = nullptr;
    bool frameRegistered = false;
    bool readyFired = false;
    bool blockedNotified = false;

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
