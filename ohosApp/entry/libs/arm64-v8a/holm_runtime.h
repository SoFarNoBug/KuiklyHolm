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

/**
 * Last create_metal / create_ohos / create_android failure reason (NUL-terminated UTF-8).
 * Empty if the last create succeeded. Independent of session handle.
 */
uint32_t holm_runtime_last_create_error(char *out_buf, uint32_t out_len);

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

/**
 * Last error code / message after failed set_custom_fs / tick / uncaptured wgpu.
 * Copies NUL-terminated UTF-8; truncates if needed.
 * @return bytes written excluding NUL, or 0
 */
uint32_t holm_runtime_session_last_error_code(uint64_t handle, char *out_buf, uint32_t out_len);
uint32_t holm_runtime_session_last_error_message(uint64_t handle, char *out_buf, uint32_t out_len);

/**
 * Headless Compute（ADR 0013 v1.1）：无 Surface；与 Present Session 分表。
 * @return opaque handle, or 0 on failure（文案见 holm_runtime_last_create_error）
 */
uint64_t holm_runtime_headless_create(void);

void holm_runtime_headless_destroy(uint64_t handle);

uint32_t holm_runtime_headless_backend_id(uint64_t handle, char *out_buf, uint32_t out_len);

/**
 * WGSL compute + f32 回读。
 * @param out_floats 调用方缓冲；out_cap 为可写 float 个数（通常 = input_len）
 * @return 写入的 float 个数；0 = 失败（文案见 holm_runtime_last_create_error）
 */
uint32_t holm_runtime_headless_compute_readback(
    uint64_t handle,
    const char *wgsl,
    const float *input,
    uint32_t input_len,
    uint32_t workgroup_count_x,
    float *out_floats,
    uint32_t out_cap);

#ifdef __cplusplus
}
#endif

#endif /* HOLM_RUNTIME_H */
