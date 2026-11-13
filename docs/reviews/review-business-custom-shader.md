# Review · business-custom-shader

- 结论：打回
- 依据：Plan「业务自定义 Shader + 双轨性能采集」；changelog `docs/specs/business-custom-shader/changelog.md`
- 日期：2026-11-13
- diff：HolmSurface Attr / Shadertoy 壳 / Android+H5 Lite / holm-runtime `setCustomFs` / Demo `holm_runtime_m0` / wasm / ADR 0005

## P0

### P0-silent-fail H5 Lite：`init` 编译失败后，`scene` 热换成功也不 `start` / 不补发 `ready`
- 证据：
  - [`HolmGlPumpSource.kt`](../../KuiklyHolmCore/src/jsMain/kotlin/com/jlj/kuiklybase/holm/HolmGlPumpSource.kt) `init`：`rebuild` 失败则 `return`（约 289–291），**未** `start()`、**未** `ready`
  - 同文件 `scene`：仅 `rebuild()`（约 337–343），成功也不 `start()`、不发 `ready`
  - 触发链：Compose `viewUpdate` 先 `executor` 后 `fragmentShader`（[`HolmSurface.kt`](../../KuiklyHolmCore/src/commonMain/kotlin/com/jlj/kuiklybase/holm/HolmSurface.kt)）；Runtime→Lite 热切时 `startPump`/`buildInitPayload` 仍可能带上一轨的 WGSL 正文 → 首编失败；随后 `PROP_FRAGMENT_SHADER` 打入正确 GLSL → `scene` 编过但泵不转、状态行可停在 `waiting onReady…`
- 影响：双轨对比主路径上 Lite 岛可无动画 / 无 onReady；与 Plan「失败不静默、custom+切轨可验收」冲突
- 处置：必修 — `scene` 在首次成功 `rebuild` 且尚未 ready 时补发 `ready` 并 `start()`；或 Attr 保证先写 `fragmentShader` 再切 `executor`（仍建议泵侧兜底）

## P1

### P1-concurrency executor / fragmentShader 属性顺序
- 证据：`HolmSurface` `viewUpdate` 先 `executor` 再 `fragmentShader`；`KRHolmSurfaceView`（JS）`PROP_EXECUTOR` 立即 `applyExecutorMode`/`startPump`
- 影响：加剧上述 P0；Runtime↔Lite + custom 时首包方言错误
- 处置：必修（可与 P0 同改：泵兜底 +/或调换 Attr 应用顺序）

### P1-device-verify H5 custom 双轨 / 采集 / 失败 UX
- 证据：changelog「本单无真机验证」；代码审无法代替 8080 勾表
- 影响：放行条件
- 处置：必修 → 可执行档 [`device-check-list/business-custom-shader.md`](device-check-list/business-custom-shader.md)
- 代码审不了：隧道是否真画出来；Lite/Runtime fps JSON 是否可信；切轨后是否卡死；故意坏串是否深蓝+红字

## P2

### P2-sticky-error-after-shader-recovery
- 证据：Demo `errorText` 主要在 `onReady` 清空（[`HolmRuntimeM0Pager.kt`](../../holmDemo/src/commonMain/kotlin/com/jlj/kuiklyholmui/HolmRuntimeM0Pager.kt)）；若 init 失败后 scene 成功但不发 ready，红字可残留
- 影响：对比页误报失败
- 处置：建议（成功绘制/成功 rebuild 后清 error；或依赖 P0 补 ready）

### P2-perf-ts-is-sample-index
- 证据：采集 JSON `ts` = `sampleIndex`；changelog 已知遗留
- 影响：AI 分析无墙钟间隔
- 处置：可选

### P2-dead-frost-fallback-on-wrap
- 证据：`resolveLiteFragmentOrFrost` catch 后回退 frost 片元（[`KRHolmSurfaceView.kt` JS](../../KuiklyHolmCore/src/jsMain/kotlin/com/jlj/kuiklybase/holm/KRHolmSurfaceView.kt)）；与 ADR 0005「失败不静默回 frost」措辞摩擦；当前 `wrap` 对非空正文几乎不抛，实达性低
- 影响：若将来 wrap 抛错，会短暂喂 frost
- 处置：建议（失败时勿改 fragSrc / 保持空程序 + 深蓝）

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/business-custom-shader.md`](device-check-list/business-custom-shader.md)。

## changelog

请确认 `docs/specs/business-custom-shader/changelog.md` 已含本单条目（审核员不代写）。整改后请 append 审查回执行。

## 待补桶

无。
