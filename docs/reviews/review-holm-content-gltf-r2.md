# Review · holm-content-gltf-r2

- 结论：打回
- 依据：Plan「真 wgpu Model Pass」`.cursor/plans/2026-11-18-wgpu-model-pass.md`；上轮 [`review-holm-content-gltf.md`](review-holm-content-gltf.md)；ADR 0016
- 日期：2026-11-18
- diff：`kmp-wgpu` ModelPass（Rust/JNI/Kotlin）；Holm `HolmModelPass` + `KRHolmSurfaceView` Model+wgpu 接线；guide/ADR/changelog/真机表 #8

## 对照上轮

| 上轮项 | 本轮 |
|--------|------|
| P1-plan-soft-drift wgpu Model Pass 未独立 | **关** — `model_pass.rs` + `ModelPass` + View 走 `_modelPass`，非 GLES 冒充 |
| P1-doc-drift Model 仍可报 wgpu 冒充 | **关** — `fireReady`：Model+wgpu → `_runtimeBackendId`/`wgpu-m0*`；Model+GL → `gles2-model` |
| P1-concurrency Model upload vs surface | **仍建议** — GLES 路径未改；wgpu 另见本轮 P0 |
| P1-device-verify | **仍开放** — 真机表已改 #8 真 wgpu 断言，结果未回填 |
| P2-h5 / registry / color | 未改，仍建议 |

## P0

### P0-silent-fail Model+wgpu 在 eagerDestroy 后可能永不建 ModelPass
- 证据：[`KRHolmSurfaceView.kt`](../../KuiklyHolmCore/src/androidMain/kotlin/com/jlj/kuiklybase/holm/KRHolmSurfaceView.kt) `enterModelPresentMode`（wgpu 分支）先 `eagerDestroyRuntimeSession`，其内 `_runtimeSurfaceValid.set(false)`；随后仅当 `surface.isValid && _runtimeSurfaceValid.get()` 才 `postCreateModelSession`。`eagerDestroy` **不**销毁 Android Surface，故 `_runtimeView` 已 VISIBLE 时 **不会**再走 `surfaceCreated` 把 valid 置回 true → create 永久跳过，泵无 pass、不出画。可达：同岛 `holm_model` 先 wgpu 出画 → 切 gl → 再切回 wgpu；或已持 Runtime Surface 后再进 Model+wgpu。
- 影响：Plan M1 验收「切 renderer-wgpu 见模」在二次切入失败；核心 Present 静默失效（非仅文案）
- 处置：必修 — destroy 后若 Surface 仍有效应恢复 valid 并 `postCreateModelSession`，或 eagerDestroy 在「仅换 Pass、Surface 仍活」时不要把 valid 钉死为 false

## P1

### P1-device-verify ModelPass 真机出画 / 二次切轨
- 证据：Plan M1/M2 与真机表要求见模、backend、`#8`；代码审无法代替出画与二次切 executor
- 影响：放行条件；且本轮 P0 修后必须用二次切入覆盖
- 处置：必修 → 可执行档 [`device-check-list/holm-content-gltf.md`](device-check-list/holm-content-gltf.md)（已补 #8b）
- 代码审不了：首进 wgpu 出画；gl↔wgpu 往返；orbit；缺 assets；与 DualTrack 进出粘层

### P1-concurrency GLES Model upload vs surface（上轮遗留）
- 证据：同 r1 — `queueEvent { upload }` 可能早于 `_modelGlView` `onSurfaceCreated`
- 影响：偶发首帧无 mesh（未证必崩）
- 处置：建议

## P2

### P2-h5-model-double-error / P2-content-registry-leak-on-detach / P2-model-color-heuristic
- 证据：同 r1，本波未动
- 处置：建议 / 可选

### P2-maven-modelpass-local-only
- 证据：Plan 允本地联调；`kmp.wgpu.local=true` + 自建 `.so`；未发 Maven 含 ModelPass 的版本
- 影响：他机未 local 联调会缺符号/旧 so
- 处置：可选（发版或文档钉死 local 前置）

## 规范候选观察

- Runtime Surface「仅换 Pass」与 `eagerDestroy` 的 `_runtimeSurfaceValid` 语义：Surface 仍活时应允许立即 recreate｜可抄码农池

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/holm-content-gltf.md`](device-check-list/holm-content-gltf.md)。

## changelog

请确认 `docs/specs/holm-content-gltf/changelog.md` 已含 ModelPass 条目；整改后请 append 审查回执行（审核员不代写）。

## 待补桶

无。
