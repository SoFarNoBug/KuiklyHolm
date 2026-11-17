# Review · kmp-wgpu-independent-repo

- 结论：打回
- 依据：Plan「kmp-wgpu 独立仓与独立发布」；ADR 0015；changelog「ADR 0015 · kmp-wgpu 独立仓」
- 日期：2026-11-17
- 范围：独立仓 `~/Studio/kmp/kmp-wgpu`；Holm `includeBuild`/Maven 接线；native `fullscreen_pass` + ABI `set_fragment_shader`；删 liquid/scene/ClearTri；`FullscreenPass`；四端壳清理。不含 Central 正式发版、不含方案 C FFI。

## P0

### P0-silent-fail H5 wasm glue 未跟 ABI：Kotlin 调 `setFragmentShader`，产物仍只有 `setCustomFs`
- 证据：
  - Rust [`session.rs`](../../../kmp-wgpu/kmp-wgpu-native/src/session.rs)（独立仓）已 `js_name = setFragmentShader`
  - 泵源 [`HolmWgpuPumpSource.kt`](../../holm-renderer-wgpu/src/jsMain/kotlin/com/jlj/kuiklybase/holm/HolmWgpuPumpSource.kt) 调 `session.setFragmentShader`
  - 入库 glue 仍为旧 API：[`h5App/.../kmp_wgpu_native.js`](../../h5App/src/jsMain/resources/kmp-wgpu-native/kmp_wgpu_native.js) 仅 `setCustomFs` / `setLiquid` / `setScene`；独立仓 [`kmp-wgpu-native/npm/kmp_wgpu_native.js`](../../../kmp-wgpu/kmp-wgpu-native/npm/kmp_wgpu_native.js) 同
  - 泵源在 `typeof setFragmentShader !== "function"` 时走「无片元 → emitShaderCompiled(false)」清屏分支，**不**装隧道片元
- 影响：H5 Runtime 主路径（喂片元出画）可达地静默退化成只清屏 / 编译失败回调，与「四端 Present 对等」冲突
- 处置：必修 — `build-wasm` 重编并同步 `h5App` + 独立仓 `npm/`（及 `kmp-wgpu` JS resources）；确认 glue 暴露 `setFragmentShader` 且无旧 `setCustomFs` 硬依赖

## P1

### P1-device-verify（未关）独立仓 + Holm 四端通面 / Maven·local 切换
- 证据：Plan Phase 5 与 ADR 0015 要求四端 sample/双轨 + Maven/local 切换；代码审无法证真机/浏览器
- 影响：放行条件未闭环（且本轮因 P0 先打回）
- 处置：必修 → 可执行档 [`device-check-list/kmp-wgpu-independent-repo.md`](device-check-list/kmp-wgpu-independent-repo.md)
- 代码审不了：H5 重编 wasm 后隧道；Android 已含 `nativeSetFragmentShader` 的 so 真机出画；iOS xcframework / Ohos so；关 local 走 `0.1.0` 坐标

### P1-plan-soft-drift（未关）core 仍留 `Device.sessionTick*`；GLES liquid Attr 仍泵
- 证据：
  - Plan：Present 能力迁 `kmp-wgpu-surface`；core 无 tick；liquid 删除含 GLES
  - 独立仓仍有 [`DeviceSession.kt`](../../../kmp-wgpu/kmp-wgpu/src/commonMain/kotlin/com/jlj/kmp/wgpu/DeviceSession.kt) `sessionTick` 等
  - Android Lite 仍 [`KRHolmSurfaceView`](../../KuiklyHolmCore/src/androidMain/kotlin/com/jlj/kuiklybase/holm/KRHolmSurfaceView.kt) `_renderer.setLiquidBoost` / `setLiquidBend`；iOS GLES `holm_setLiquidBoost` 仍在
- 影响：分层/删除叙事未收干净；未证 wgpu 主路径因此不可达
- 处置：建议 — 复审前迁走或内联 DeviceSession；GLES liquid 删泵或 Attr 变 no-op 与 ADR 对齐

## P2

### P2-orphan-holm-tree
- 证据：Holm 仍保留 `kmp-wgpu*` 整树 + `MOVED.md`；settings 已不 include，真源在 `~/Studio/kmp/kmp-wgpu`
- 影响：双树易漂移；审查/改码易找错仓
- 处置：可选 — 删旧树或缩成指针 README

### P2-comment-noise-ios
- 证据：[`KRHolmSurface.m`](../../iosApp/iosApp/KuiklyExpand/Views/KRHolmSurface.m) 出现「ADR 0015 removed: // ADR 0015 removed:」叠注释
- 影响：可读性；无行为
- 处置：可选 — 收成单行或删死代码块

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/kmp-wgpu-independent-repo.md`](device-check-list/kmp-wgpu-independent-repo.md)。

## Plan 对齐

- 独立仓路径 / Maven 0.1.0 / includeBuild 开关 — 已覆盖（Android compile + `publishToMavenLocal` 狗粮）
- FullscreenPass + 删 liquid/scene/ClearTri + ABI rename — 源码侧基本覆盖；**H5 产物未跟**（打回点）
- 四端 native 分发脚手架（jniLibs / podspec / ohpm / npm）— 有；npm/h5App glue 内容过期
- DeviceSession 迁出 / GLES liquid 删尽 — 软漂移
- changelog：已有「ADR 0015 · kmp-wgpu 独立仓」条目（审核员不代写）

## changelog

请码农确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md` 含 ADR 0015 条目；修 P0 后宜补一行「H5/wasm 重编 setFragmentShader」。

## 待补桶

无。
