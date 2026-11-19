/**
 * @file holm_ohos_bridge.cpp
 * @brief Resolve holm_ohos_bridge_* from libshared.so（显式 dlopen，勿仅靠 RTLD_DEFAULT）。
 * @date 2026-11-16
 * @author jlj
 */
#include "holm_ohos_bridge.h"

#include <dlfcn.h>
#include <hilog/log.h>

#undef LOG_DOMAIN
#undef LOG_TAG
#define LOG_DOMAIN 0x3200
#define LOG_TAG "HolmXc"

namespace {

void *g_sharedHandle = nullptr;

void *SharedHandle()
{
    if (g_sharedHandle != nullptr) {
        return g_sharedHandle;
    }
    g_sharedHandle = dlopen("libshared.so", RTLD_NOW | RTLD_NOLOAD);
    if (g_sharedHandle == nullptr) {
        g_sharedHandle = dlopen("libshared.so", RTLD_NOW | RTLD_GLOBAL);
    }
    if (g_sharedHandle == nullptr) {
        OH_LOG_ERROR(LOG_APP, "dlopen libshared.so failed: %{public}s", dlerror());
    }
    return g_sharedHandle;
}

template <typename T>
bool Bind(T &slot, const char *name)
{
    void *handle = SharedHandle();
    void *sym = nullptr;
    if (handle != nullptr) {
        sym = dlsym(handle, name);
    }
    if (sym == nullptr) {
        sym = dlsym(RTLD_DEFAULT, name);
    }
    if (sym == nullptr) {
        OH_LOG_ERROR(LOG_APP, "dlsym %{public}s failed: %{public}s", name, dlerror());
        return false;
    }
    slot = reinterpret_cast<T>(sym);
    return true;
}

template <typename T>
void BindOptional(T &slot, const char *name)
{
    slot = nullptr;
    (void)Bind(slot, name);
}

} // namespace

bool HolmOhosBridgeResolve(HolmOhosBridgeApi *outApi)
{
    if (outApi == nullptr) {
        return false;
    }
    HolmOhosBridgeApi api {};
    if (!Bind(api.requireProvider, "holm_ohos_bridge_require_provider") ||
        !Bind(api.hasWgpu, "holm_ohos_bridge_has_wgpu") ||
        !Bind(api.create, "holm_ohos_bridge_create") ||
        !Bind(api.destroy, "holm_ohos_bridge_destroy") ||
        !Bind(api.tick, "holm_ohos_bridge_tick") ||
        !Bind(api.resize, "holm_ohos_bridge_resize") ||
        !Bind(api.setCustomFs, "holm_ohos_bridge_set_custom_fs") ||
        !Bind(api.setPlaying, "holm_ohos_bridge_set_playing") ||
        !Bind(api.backendId, "holm_ohos_bridge_backend_id") ||
        !Bind(api.lastErrorCode, "holm_ohos_bridge_last_error_code") ||
        !Bind(api.lastErrorMessage, "holm_ohos_bridge_last_error_message") ||
        !Bind(api.lastCreateError, "holm_ohos_bridge_last_create_error")) {
        return false;
    }
    api.setScene = nullptr;
    BindOptional(api.setScene, "holm_ohos_bridge_set_scene");

    // ADR 0019：Model 符号可选；缺则 Shader 双轨仍可用
    BindOptional(api.createModel, "holm_ohos_bridge_create_model");
    BindOptional(api.destroyModel, "holm_ohos_bridge_destroy_model");
    BindOptional(api.resizeModel, "holm_ohos_bridge_resize_model");
    BindOptional(api.tickModelFrame, "holm_ohos_bridge_tick_model_frame");
    BindOptional(api.uploadModelMeshes, "holm_ohos_bridge_upload_model_meshes");
    BindOptional(api.backendIdModel, "holm_ohos_bridge_backend_id_model");
    BindOptional(api.lastErrorCodeModel, "holm_ohos_bridge_last_error_code_model");
    BindOptional(api.lastErrorMessageModel, "holm_ohos_bridge_last_error_message_model");

    BindOptional(api.modelSessionCreate, "holm_ohos_model_session_create");
    BindOptional(api.modelSessionDestroy, "holm_ohos_model_session_destroy");
    BindOptional(api.modelAttach, "holm_ohos_model_attach");
    BindOptional(api.modelSetExecutor, "holm_ohos_model_set_executor");
    BindOptional(api.modelNudge, "holm_ohos_model_nudge");
    BindOptional(api.modelIsReady, "holm_ohos_model_is_ready");
    BindOptional(api.modelClearError, "holm_ohos_model_clear_error");
    BindOptional(api.modelLastErrorCode, "holm_ohos_model_last_error_code");
    BindOptional(api.modelLastErrorMessage, "holm_ohos_model_last_error_message");
    BindOptional(api.modelPeekMesh, "holm_ohos_model_peek_mesh");
    BindOptional(api.modelCopyMesh, "holm_ohos_model_copy_mesh");
    BindOptional(api.modelFillLight, "holm_ohos_model_fill_light");

    *outApi = api;
    return true;
}
