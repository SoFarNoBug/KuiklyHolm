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
    // XComponent 独立加载 libholm_xc：libshared 常为 RTLD_LOCAL，RTLD_DEFAULT 看不到 @CName。
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
    // ADR 0015: setScene optional（nullable no-op）
    api.setScene = nullptr;
    (void)Bind(api.setScene, "holm_ohos_bridge_set_scene");
    *outApi = api;
    return true;
}
