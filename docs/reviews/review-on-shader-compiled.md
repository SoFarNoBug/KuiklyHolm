# Review · on-shader-compiled

- 结论：有条件通过
- 依据：Plan「onShaderCompiled 双耗时回调」；ADR 0012；changelog `docs/specs/business-custom-shader/changelog.md`
- 日期：2026-11-16
- diff：common `HolmSurface` 事件；Android/JS View+pump；iOS GlesHost/Surface；Ohos `holm_xc_*`+ETS；Demo 状态行；ADR/glossary

## P0

无。

## P1

### P1-device-verify 四端装载回调与 Demo 耗时展示
- 证据：Plan 验收要求 Lite/Runtime 切样本见非零 `compileMs`、失败先红字再 `ok=false`、空/错方言零耗时；代码审无法代替端上墙钟与回调时序；本会话仅门禁 `:androidApp:compileDebugKotlin` / JS compile，未跑 iOS/Ohos 编包与真机
- 影响：放行条件——契约接线代码侧已齐，运行时语义未闭环
- 处置：必修 → 可执行档 [`device-check-list/on-shader-compiled.md`](device-check-list/on-shader-compiled.md)
- 代码审不了：各端真实 `wrapMs`/`compileMs` 数量级；失败双发顺序观感；Surface 恢复重编是否再回调；鸿蒙/iOS 出画后状态行是否更新

## P2

### P2-h5-runtime-fail-compileMs-zero
- 证据：[`HolmRuntimePumpSource.kt`](../../KuiklyHolmCore/src/jsMain/kotlin/com/jlj/kuiklybase/holm/HolmRuntimePumpSource.kt) Worker `catch` 路径 `emitShaderCompiled(false, 0)`，未保留 `setCustomFs` 失败前的墙钟
- 影响：H5 Runtime 失败样本 `compileMs` 恒 0，对比 Lite 失败耗时不对称
- 处置：建议（catch 外包 `t0`）

### P2-ohos-lite-not-ready-silent
- 证据：[`holm_xc_plugin.cpp`](../../ohosApp/entry/src/main/cpp/holm_xc/holm_xc_plugin.cpp) `ApplyLiteShaders`：`!island.liteGl.ready` 直接 `return false`，不 `FireShaderCompiled`
- 影响：Surface 未就绪时改 `fragmentShader`，业务等不到装载结束事件（与「每次尝试都回调」略偏）
- 处置：建议（ready 后补发，或未就绪也发 `ok=false` 零耗时）

### P2-runtime-empty-ok-true
- 证据：Android/H5/Ohos Runtime 空片元走 ClearTri/`setScene` 后 `ok=true, wrap=0, compile=0`；Lite 空片元多为 `FRAGMENT_REQUIRED`/`SHADER_WRAP` + `ok=false`
- 影响：双轨对「空串」成败语义不一致；业务若统一断言 `ok` 易踩坑
- 处置：可选（文档标明或 Runtime 空串也 `ok=false`）

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/on-shader-compiled.md`](device-check-list/on-shader-compiled.md)。

## changelog

请确认 `docs/specs/business-custom-shader/changelog.md`（及 dualtrack / 各端）已含本单 `onShaderCompiled` 条目（审核员不代写）。整改后请 append 审查回执行。

## 待补桶

无。
