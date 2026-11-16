/**
 * @file holm_ohos_bridge.h
 * @brief KN @CName Present 桥（dlsym）；壳禁止直调 holm_runtime_session_*。
 * @date 2026-11-16
 * @author jlj
 */
#ifndef HOLM_OHOS_BRIDGE_H
#define HOLM_OHOS_BRIDGE_H

#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif

typedef int (*HolmOhosBridgeRequireProviderFn)(const char *executor, char *errBuf, int errCap);
typedef int (*HolmOhosBridgeHasWgpuFn)(void);
typedef int64_t (*HolmOhosBridgeCreateFn)(void *window, int32_t width, int32_t height);
typedef void (*HolmOhosBridgeDestroyFn)(int64_t handle);
typedef int (*HolmOhosBridgeTickFn)(int64_t handle, float dtSecs);
typedef void (*HolmOhosBridgeResizeFn)(int64_t handle, int32_t width, int32_t height);
typedef int (*HolmOhosBridgeSetCustomFsFn)(int64_t handle, const char *fs);
typedef void (*HolmOhosBridgeSetSceneFn)(int64_t handle, const char *scene);
typedef void (*HolmOhosBridgeSetLiquidFn)(int64_t handle, float boost, float bend);
typedef void (*HolmOhosBridgeSetPlayingFn)(int64_t handle, int playing);
typedef uint32_t (*HolmOhosBridgeBackendIdFn)(int64_t handle, char *buf, int32_t cap);
typedef uint32_t (*HolmOhosBridgeLastErrorCodeFn)(int64_t handle, char *buf, int32_t cap);
typedef uint32_t (*HolmOhosBridgeLastErrorMessageFn)(int64_t handle, char *buf, int32_t cap);
typedef uint32_t (*HolmOhosBridgeLastCreateErrorFn)(char *buf, int32_t cap);

struct HolmOhosBridgeApi {
    HolmOhosBridgeRequireProviderFn requireProvider;
    HolmOhosBridgeHasWgpuFn hasWgpu;
    HolmOhosBridgeCreateFn create;
    HolmOhosBridgeDestroyFn destroy;
    HolmOhosBridgeTickFn tick;
    HolmOhosBridgeResizeFn resize;
    HolmOhosBridgeSetCustomFsFn setCustomFs;
    HolmOhosBridgeSetSceneFn setScene;
    HolmOhosBridgeSetLiquidFn setLiquid;
    HolmOhosBridgeSetPlayingFn setPlaying;
    HolmOhosBridgeBackendIdFn backendId;
    HolmOhosBridgeLastErrorCodeFn lastErrorCode;
    HolmOhosBridgeLastErrorMessageFn lastErrorMessage;
    HolmOhosBridgeLastCreateErrorFn lastCreateError;
};

/**
 * @brief dlsym(RTLD_DEFAULT) 解析 KN bridge；失败返回 false。
 */
bool HolmOhosBridgeResolve(HolmOhosBridgeApi *outApi);

#ifdef __cplusplus
}
#endif

#endif /* HOLM_OHOS_BRIDGE_H */
