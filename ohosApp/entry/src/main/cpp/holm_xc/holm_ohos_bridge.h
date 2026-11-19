/**
 * @file holm_ohos_bridge.h
 * @brief KN @CName Present 桥（dlsym）；壳禁止直调 kmp_wgpu_*。
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
typedef void (*HolmOhosBridgeSetPlayingFn)(int64_t handle, int playing);
typedef uint32_t (*HolmOhosBridgeBackendIdFn)(int64_t handle, char *buf, int32_t cap);
typedef uint32_t (*HolmOhosBridgeLastErrorCodeFn)(int64_t handle, char *buf, int32_t cap);
typedef uint32_t (*HolmOhosBridgeLastErrorMessageFn)(int64_t handle, char *buf, int32_t cap);
typedef uint32_t (*HolmOhosBridgeLastCreateErrorFn)(char *buf, int32_t cap);

/** ADR 0019 ModelPass */
typedef int64_t (*HolmOhosBridgeCreateModelFn)(void *window, int32_t width, int32_t height);
typedef void (*HolmOhosBridgeDestroyModelFn)(int64_t handle);
typedef void (*HolmOhosBridgeResizeModelFn)(int64_t handle, int32_t width, int32_t height);
typedef int (*HolmOhosBridgeTickModelFrameFn)(int64_t handle, int64_t sessionId, float aspect, float dt,
                                             int playing);
typedef int (*HolmOhosBridgeUploadModelMeshesFn)(int64_t handle, int64_t sessionId);
typedef uint32_t (*HolmOhosBridgeBackendIdModelFn)(int64_t handle, char *buf, int32_t cap);
typedef uint32_t (*HolmOhosBridgeLastErrorCodeModelFn)(int64_t handle, char *buf, int32_t cap);
typedef uint32_t (*HolmOhosBridgeLastErrorMessageModelFn)(int64_t handle, char *buf, int32_t cap);

/** ADR 0019 Model session / mesh pull */
typedef int64_t (*HolmOhosModelSessionCreateFn)(void);
typedef void (*HolmOhosModelSessionDestroyFn)(int64_t sessionId);
typedef void (*HolmOhosModelAttachFn)(int64_t sessionId, const char *contentId, const char *executor);
typedef void (*HolmOhosModelSetExecutorFn)(int64_t sessionId, const char *executor);
typedef void (*HolmOhosModelNudgeFn)(int64_t sessionId, float dx, float dy);
typedef int (*HolmOhosModelIsReadyFn)(int64_t sessionId);
typedef void (*HolmOhosModelClearErrorFn)(int64_t sessionId);
typedef uint32_t (*HolmOhosModelLastErrorCodeFn)(int64_t sessionId, char *buf, int32_t cap);
typedef uint32_t (*HolmOhosModelLastErrorMessageFn)(int64_t sessionId, char *buf, int32_t cap);
typedef void (*HolmOhosModelPeekMeshFn)(int64_t sessionId, int32_t *vertFloats, int32_t *indexCount);
typedef int (*HolmOhosModelCopyMeshFn)(int64_t sessionId, float *verts, int32_t vertCap, int32_t *indices,
                                      int32_t idxCap, int32_t *outVertFloats, int32_t *outIndexCount,
                                      float *center3, float *radius, float *color4, float *yaw, float *pitch);
typedef void (*HolmOhosModelFillLightFn)(int64_t sessionId, float *out5);

struct HolmOhosBridgeApi {
    HolmOhosBridgeRequireProviderFn requireProvider;
    HolmOhosBridgeHasWgpuFn hasWgpu;
    HolmOhosBridgeCreateFn create;
    HolmOhosBridgeDestroyFn destroy;
    HolmOhosBridgeTickFn tick;
    HolmOhosBridgeResizeFn resize;
    HolmOhosBridgeSetCustomFsFn setCustomFs;
    HolmOhosBridgeSetSceneFn setScene;
    HolmOhosBridgeSetPlayingFn setPlaying;
    HolmOhosBridgeBackendIdFn backendId;
    HolmOhosBridgeLastErrorCodeFn lastErrorCode;
    HolmOhosBridgeLastErrorMessageFn lastErrorMessage;
    HolmOhosBridgeLastCreateErrorFn lastCreateError;

    HolmOhosBridgeCreateModelFn createModel;
    HolmOhosBridgeDestroyModelFn destroyModel;
    HolmOhosBridgeResizeModelFn resizeModel;
    HolmOhosBridgeTickModelFrameFn tickModelFrame;
    HolmOhosBridgeUploadModelMeshesFn uploadModelMeshes;
    HolmOhosBridgeBackendIdModelFn backendIdModel;
    HolmOhosBridgeLastErrorCodeModelFn lastErrorCodeModel;
    HolmOhosBridgeLastErrorMessageModelFn lastErrorMessageModel;

    HolmOhosModelSessionCreateFn modelSessionCreate;
    HolmOhosModelSessionDestroyFn modelSessionDestroy;
    HolmOhosModelAttachFn modelAttach;
    HolmOhosModelSetExecutorFn modelSetExecutor;
    HolmOhosModelNudgeFn modelNudge;
    HolmOhosModelIsReadyFn modelIsReady;
    HolmOhosModelClearErrorFn modelClearError;
    HolmOhosModelLastErrorCodeFn modelLastErrorCode;
    HolmOhosModelLastErrorMessageFn modelLastErrorMessage;
    HolmOhosModelPeekMeshFn modelPeekMesh;
    HolmOhosModelCopyMeshFn modelCopyMesh;
    HolmOhosModelFillLightFn modelFillLight;
};

/**
 * @brief dlopen(libshared.so) + dlsym 解析 KN bridge；失败返回 false（可重试）。
 */
bool HolmOhosBridgeResolve(HolmOhosBridgeApi *outApi);

#ifdef __cplusplus
}
#endif

#endif /* HOLM_OHOS_BRIDGE_H */
