# Review · holm-content-gltf

- 结论：有条件通过
- 依据：Plan「HolmContent + 最小 glTF」；ADR 0016；changelog `docs/specs/holm-content-gltf/changelog.md`
- 日期：2026-11-18
- diff：`holm-core` Content/Registry/Surface；四端 View；`:holm-gltf`；Android Model Present；Demo DualTrack/Model；guide/`holm_api`

## P0

无。

## P1

### P1-device-verify Android DualTrack Content 迁移动 + Model 真画
- 证据：Plan 验收要求四端可编、Android DualTrack 观感与改前一致、Android GL/wgpu 均可加载 hero.glb 见整模可拖转；代码审无法代替真机出画/手势/热换
- 影响：放行条件——契约接线已齐，运行时未闭环
- 处置：必修 → 可执行档 [`device-check-list/holm-content-gltf.md`](device-check-list/holm-content-gltf.md)
- 代码审不了：隧道/芯片切换观感；Model 拖转；缺 assets 回落/报错；切 Shader↔Model 是否粘层

### P1-plan-soft-drift wgpu Model Pass 未独立落地
- 证据：Plan M1「wgpu：新建 Model Pass（不塞进 FullscreenPass）」；实现为双 executor 共用 GLES 网格层（`enterModelPresentMode` + changelog 已知遗留）
- 影响：验收「看见整模」可达，但与 Plan 字面「wgpu Model Pass」软偏离；guide 已写明
- 处置：建议（跟进独立 Pass 或改 Plan/ADR 接受 GLES 共用）

### P1-doc-drift Model 模式 onReady.backend 仍可报 wgpu
- 证据：[`KRHolmSurfaceView.kt`](../../KuiklyHolmCore/src/androidMain/kotlin/com/jlj/kuiklybase/holm/KRHolmSurfaceView.kt) `fireReady`：`_modelMode` 时若 `executor=wgpu` 仍走 `_runtimeBackendId`/`wgpu-m0`，实际出画为 GLES 网格
- 影响：业务若按 backend 判断「真在跑 wgpu」会误判；与 guide「Model 双轨 GLES」口径不一致
- 处置：建议（model 模式固定 `gles2-model` 或等价诚实 id）

### P1-concurrency Model upload 与 GLSurface 创建竞态
- 证据：`HolmAndroidModelPresenter` IO 完成后 `queueEvent { upload }`；极快路径（bytes/内存）可能早于 `_modelGlView` `onSurfaceCreated`
- 影响：偶发首帧无 mesh / GL 调用无上下文（未证必崩）
- 处置：建议（surface ready 后再 upload，或 upload 失败重试）

## P2

### P2-h5-model-double-error
- 证据：JS `PROP_CONTENT_ID` 对 `HolmModelContent` 已 `fireError(MODEL_UNSUPPORTED)`；随后 `PROP_BRIDGE_CONTENT_KIND=model` 再 fire 一次
- 影响：H5 挂 Model 可能双红字
- 处置：建议（contentId 已处理则 contentKind 跳过）

### P2-content-registry-leak-on-detach
- 证据：Compose 每轮 `register`；View 仅在 contentId 变更时 `unregister` 旧 id；卸岛/销毁未强制清当前 id
- 影响：长会话 Registry map 缓增
- 处置：可选（`onDestroy`/detach unregister）

### P2-model-color-heuristic
- 证据：`HolmGlesModelRenderer.upload` 用 `0.85/0.55` 判断是否覆盖 baseColorFactor
- 影响：业务故意同色因子时静默被 glTF 因子覆盖
- 处置：可选（显式 flag）

## 规范候选观察

- Content Registry 过 NVI：View 销毁/卸岛必须 unregister 当前 contentId，避免 map 泄漏｜可抄码农池

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/holm-content-gltf.md`](device-check-list/holm-content-gltf.md)。

## changelog

请确认 `docs/specs/holm-content-gltf/changelog.md` 已含本单条目（审核员不代写）。整改后请 append 审查回执行。

## 待补桶

无。
