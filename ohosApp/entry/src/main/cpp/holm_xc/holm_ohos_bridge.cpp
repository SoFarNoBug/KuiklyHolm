/**
 * @file holm_ohos_bridge.cpp
 * @brief Resolve holm_ohos_bridge_* from libshared via RTLD_DEFAULT.
 * @date 2026-11-16
 * @author jlj
 */
#include "holm_ohos_bridge.h"

#include <dlfcn.h>

namespace {

template <typename T>
bool Bind(T &slot, const char *name)
{
    void *sym = dlsym(RTLD_DEFAULT, name);
    if (sym == nullptr) {
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
        !Bind(api.setScene, "holm_ohos_bridge_set_scene") ||
        !Bind(api.setLiquid, "holm_ohos_bridge_set_liquid") ||
        !Bind(api.setPlaying, "holm_ohos_bridge_set_playing") ||
        !Bind(api.backendId, "holm_ohos_bridge_backend_id") ||
        !Bind(api.lastErrorCode, "holm_ohos_bridge_last_error_code") ||
        !Bind(api.lastErrorMessage, "holm_ohos_bridge_last_error_message") ||
        !Bind(api.lastCreateError, "holm_ohos_bridge_last_create_error")) {
        return false;
    }
    *outApi = api;
    return true;
}
