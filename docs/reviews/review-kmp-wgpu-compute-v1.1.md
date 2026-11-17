# Review · kmp-wgpu-compute-v1.1

- 结论：有条件通过
- 依据：Plan「ADR 0013 v1.1 · Headless Compute + 回读（方案 B · Android+H5）」；changelog「kmp-wgpu v1.1 · Headless Compute + 回读」
- 日期：2026-11-16
- 范围：`holm-runtime` `compute_readback` + Android JNI / wasm `HeadlessComputeSession`；`:kmp-wgpu` Compute API；Demo `kmp_wgpu_compute`；`index.html` `createHeadless`。不含 iOS/Ohos 实现、不含 Holm 三包拆分。

## P0

无。

## P1

### P1-main-thread-io（未关）
- 证据：`android_jni.rs` `nativeCreateHeadless` / `nativeComputeReadback` 内 `pollster::block_on`；Demo `ComputeReadbackPager` 在 `clickable` 回调同步走 `ComputeReadbackSample.run` → Android 同步粗块
- 影响：样本 8×f32 可接受；公开 API 若喂大 buffer / 冷启动 adapter，主线程/Kuikly 线程可能卡顿或 ANR
- 处置：建议 — 文档标「短同步」；大负载改后台/`computeReadbackAsync` 真异步；Demo 保持小样本

### P1-device-verify（未关）
- 缺口：代码审无法替代 GPU 回读正确性与 Present 回归；本波对话已跑通 H5/Vivo Compute + H5 Present，**Android Present 回归本波未在 vivo 复勾**
- 可执行档：[`device-check-list/kmp-wgpu-compute-v1.1.md`](device-check-list/kmp-wgpu-compute-v1.1.md)
- 处置：必修（放行条件）— 勾完表（至少 Android Present #4）或书面接受遗留

## P2

### P2-wgsl-dual-copy（未关）
- 证据：`compute_readback.rs` `SAMPLE_DOUBLE_WGSL` 与 `ComputeReadback.kt` `COMPUTE_SAMPLE_DOUBLE_WGSL` 双份；Demo 用 Kotlin 侧
- 处置：可选 — 单源或 changelog 同步义务

### P2-destroy-not-idempotent（未关）
- 证据：`HeadlessDevice.destroy` → Android `Box::from_raw`；二次 `destroy` 可 UAF
- 处置：建议 — 置零句柄或文档「只 destroy 一次」

### P2-worktree-noise（未关）
- 证据：同树另有 `iosApp`/`ohosApp`/`static_server` 等与本单无关改动
- 处置：可选 — 提交前拆开

## 规范候选观察

- [ ] Headless / 同步粗块 JNI（`block_on` map+dispatch）默认不得走 UI/Kuikly 主路径；须 Async 或文档硬标「仅短样本」｜2026-11-16｜自评：未达标（本单 P1-main-thread-io）

## 真机

挂 `P1-device-verify`。可执行档：[`device-check-list/kmp-wgpu-compute-v1.1.md`](device-check-list/kmp-wgpu-compute-v1.1.md)。

## Plan 对齐

符合：方案 B 粗块、无 Surface、Android+H5、Demo 非 HolmSurface、与 Present 分表；未越界拆 Holm 包 / 未做 iOS·Ohos Compute。

## changelog

已见 `docs/specs/dualtrack-lite-vs-standard/changelog.md`「kmp-wgpu v1.1 · Headless Compute + 回读」；请码农确认仍在（审核员不代写）。

## 待补桶

无。
