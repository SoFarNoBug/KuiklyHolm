#ifndef HOLM_RUNTIME_H
#define HOLM_RUNTIME_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/** ABI version; bump when C Session layout/semantics break. */
uint32_t holm_runtime_abi_version(void);

/** 1 if wgpu backend compiled in; 0 otherwise. */
uint32_t holm_runtime_has_wgpu(void);

/**
 * Create a Runtime Session bound to a CAMetalLayer (iOS).
 * @param metal_layer  CAMetalLayer* (not retained by Rust; must outlive the session)
 * @param width        pixel width (>=1)
 * @param height       pixel height (>=1)
 * @return opaque handle, or 0 on failure
 */
uint64_t holm_runtime_session_create_metal(void *metal_layer, uint32_t width, uint32_t height);

/**
 * Create a Runtime Session bound to an OHNativeWindow (HarmonyOS / ADR 0010).
 * @param native_window  OHNativeWindow* from XComponent OnSurfaceCreated (must outlive session)
 * @param width          pixel width (>=1)
 * @param height         pixel height (>=1)
 * @return opaque handle, or 0 on failure
 */
uint64_t holm_runtime_session_create_ohos(void *native_window, uint32_t width, uint32_t height);

void holm_runtime_session_destroy(uint64_t handle);

void holm_runtime_session_resize(uint64_t handle, uint32_t width, uint32_t height);

/** scene name UTF-8 (e.g. "custom"); NULL ignored. */
void holm_runtime_session_set_scene(uint64_t handle, const char *scene);

/**
 * Set custom WGSL fragment module body (full module with vs_main/fs_main via shell).
 * Empty / NULL clears custom pipe.
 * @return 1 on success, 0 on failure
 */
uint32_t holm_runtime_session_set_custom_fs(uint64_t handle, const char *fs);

void holm_runtime_session_set_liquid(uint64_t handle, float boost, float bend);

void holm_runtime_session_set_playing(uint64_t handle, uint32_t playing);

/**
 * Advance one frame.
 * @return 1 on success, 0 on failure / invalid handle
 */
uint32_t holm_runtime_session_tick(uint64_t handle, float dt_secs);

/**
 * Copy backend id into out_buf (NUL-terminated). Truncates if needed.
 * @return bytes written excluding NUL, or 0
 */
uint32_t holm_runtime_session_backend_id(uint64_t handle, char *out_buf, uint32_t out_len);

#ifdef __cplusplus
}
#endif

#endif /* HOLM_RUNTIME_H */
