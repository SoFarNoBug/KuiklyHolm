# Review · android-display-handle

- 结论：通过
- 依据：速速改（Runtime 真机 `MissingDisplayHandle`）；changelog `docs/specs/dualtrack-lite-vs-standard/changelog.md`（2026-11-15 Android Runtime · DisplayHandle）
- 日期：2026-11-15
- diff：`holm-runtime/src/android_jni.rs`（`AndroidDisplayOwner` + `from_display_and_window`）；入仓 `libholm_runtime.so`

## P0

无。

## P1

无。

## P2

### P2-ohos-from-window-display
- 证据：`ohos_c_api.rs` 仍 `new_without_display_handle` + `SurfaceTargetUnsafe::from_window`（约 `:72-77`）；与 Android 升前同构，wgpu-core 30 在 `(None, None)` 会 `MissingDisplayHandle`
- 影响：本机无 Ohos 工具链未编；有工具链重编后可能复现 Android 刚关的创建卡点（iOS 走 `CoreAnimationLayer`，路径不同）
- 处置：建议 — Ohos 有工具链时对齐 Android：`Instance.display` + `from_display_and_window`（或该端等价 DisplayHandle）

## 规范候选观察

无。

## 真机

无。

（本单验收点「create 非 BLOCKED / `backend=wgpu-m0`」已由同会话 UI dump 闭环：`executor=runtime | … | onReady backend=wgpu-m0 | fps≈66–77`。更广通面/白雾仍见既有 [`device-check-list/unify-wgpu-30.md`](device-check-list/unify-wgpu-30.md) #4，不属本桶。）

## changelog

已确认含「Android Runtime · DisplayHandle」置顶条目（审核员不代写）。

## 待补桶

无。

## 发现面（路由）

- 卡 B（可达性）：`nativeCreate` 主路径已接线 DisplayHandle；失败仍 `LAST_CREATE_ERROR` + `Log.e`，无静默 GLES
- 卡 A/C/D/E：未命中
