# 执行计划 · 删除五席代码

> 用户选 2：先计划后编码。确认本计划后再动手。  
> 对齐审讯：底座只留「喂 shader」路径；Demo 主推隧道；五席叙事已废弃。

## 目标

从 **Core / holm-runtime / iOS 壳 / Demo / 根目录 shaders** 去掉官方五席实现（`frost` / `glow` / `liquid_approx` / `gradient_bg` / `fullscreen_warp`），只保留：

- 宿主 **`fragmentShader`（+ 壳）** 出画（隧道等）
- Runtime **custom WGSL** / 清屏占位
- 不再提供「选 scene=frost 就有官方效果」

## 非目标

- 不物理拆 `holm-core` / `holm-gl` / `holm-wgpu`（ADR 0008 后单）
- 不改 ParticleEngine
- 不删隧道（`NeonTunnelShaders`）
- 不重写整份历史 ADR 正文（可文首注明五席源码已删）
- 不强制本单改 `holm_api` 规范全文（changelog 记一行即可）

## 删除后默认行为（锁定）

| 情况 | 行为 |
|---|---|
| Lite：`fragmentShader` 非空且 wrap 成功 | 画宿主片元 |
| Lite：`fragmentShader` 空或 wrap 失败 | **不**回退 frost；清屏/深蓝 + `onError`（与「无官方席」一致） |
| Runtime：custom FS 非空且成功 | 画 custom |
| Runtime：无 custom / 清空 custom | 走 **ClearTri / 清屏管线**（现有 `pipe_clear`），**不**再编 frost/glow/liquid pipeline |
| 未知 `scene` 字符串 | 忽略或等同无官方席；不再映射到 frost |

`liquidBoost` / `liquidBend` / `warpAmount` / `scrollSpeed`：**本单保留 Attr API**（避免无谓破宿主编译），无官方席后仅作预留 uniform；Demo 可去掉液态旋钮 UI。

## 改动面清单

### A. Kotlin Core（必删/必改）

- 删除五席源：`*ShaderSource.kt`（Frost/Glow/LiquidApprox/GradientBg/FullscreenWarp）及 [`HolmSceneShaders.kt`](KuiklyHolmCore/src/commonMain/kotlin/com/jlj/kuiklybase/holm/HolmSceneShaders.kt) 中五席路由  
  - 替换为极薄解析：仅内置全屏 vert +「无 body 则失败」；或 `HolmBuiltinShaders` 只留 vert
- [`HolmShaderResolve.kt`](KuiklyHolmCore/src/commonMain/kotlin/com/jlj/kuiklybase/holm/HolmShaderResolve.kt)：去掉 `gles2Fragment(scene)` 五席分支
- Android/JS [`KRHolmSurfaceView`](KuiklyHolmCore/src/androidMain/kotlin/com/jlj/kuiklybase/holm/KRHolmSurfaceView.kt)：默认 `_scene` 不再依赖 `SCENE_FROST`；空 fragment → error/clear
- [`HolmGlPumpSource.kt`](KuiklyHolmCore/src/jsMain/kotlin/com/jlj/kuiklybase/holm/HolmGlPumpSource.kt)：默认 `scene=frost` 及内置五席字符串去掉

### B. holm-runtime（必删/必改）

- [`shaders.rs`](holm-runtime/src/shaders.rs)：删 `FROST_FS` / `GLOW_FS` / `LIQUID_FS`；保留 `FULLSCREEN_VS` + clear/custom 所需
- [`session_core.rs`](holm-runtime/src/session_core.rs)：`SceneKind` 去掉 Frost/Glow/Liquid；创建时不建三套 pipe；`set_scene` / 清 custom 回 **Clear** 而非 Frost
- 重建 Android `.so`（`build-android.ps1`）并更新 `jniLibs`（若本机可编）

### C. iOS 壳（必改）

- [`KRHolmSurface.m/.h`](iosApp/iosApp/KuiklyExpand/Views/KRHolmSurface.m)：去掉内嵌 frost/liquid GLSL；无业务片元则清屏 + error；有片元则走同一套（若 iOS 暂不支持宿主任意 GLSL，本单至少删五席字符串并诚实失败）

### D. Demo（必删/必改）

- 删除 [`HolmSpikePager.kt`](holmDemo/src/commonMain/kotlin/com/jlj/kuiklyholmui/HolmSpikePager.kt) 与 Home「五席」入口
- [`HolmRuntimeM0Pager.kt`](holmDemo/src/commonMain/kotlin/com/jlj/kuiklyholmui/HolmRuntimeM0Pager.kt)：去掉霜化/柔光/液态 chip 与液态旋钮；只留 **「示例1」** chip（原 custom/隧道按钮文案；仍绑定 `NeonTunnelShaders`）+ 双轨 + maxFps/采集
- 默认场景仍为隧道样本（`SCENE_CUSTOM`）；按钮对用户显示 **示例1**（便于以后加示例2…）
- Home 主按钮文案可与「示例1 / 双轨」对齐（避免再写「custom」）

### E. 根目录 shaders/（必删）

- 删除 `shaders/frost.*` / `glow.*` / `liquid_approx.*` / `gradient_bg.*` / `fullscreen_warp.*`  
- 若 vert 仍被 Kotlin 内联引用，以代码内 vert 为准，文件可删或留一份 `fullscreen.vert.glsl` 文档用（本单倾向删五席相关文件）

### F. 文档（轻量）

- append [`docs/specs/holm-render-substrate/changelog.md`](docs/specs/holm-render-substrate/changelog.md)
- CONTEXT / glossary：五席改为「已删源码」
- ADR 0003 文首补「源码已移除」；runbook / device-check-list 步骤去掉 frost 通面为主路径（改隧道）
- **不**大改历史 changelog 条目

## 验证

1. `:androidApp:compileDebugKotlin`  
2. （若编了 so）installDebug → Home 仅隧道入口 → Lite/Runtime 隧道可画  
3. 空 `fragmentShader` 不出现霜化画面  
4. 无 `holm_spike` 页或入口

## 风险

- iOS 若仍只硬编码 frost/liquid，删后需明确 BLOCKED/清屏，避免半残编译  
- Runtime 包体可能因少 3 个 pipeline **略降**（非本单 KPI）  
- 真机表/审查文档会过时 → 本单改 checklist 主路径为隧道

## 验收口径

- 仓库内无五席 GLSL/WGSL 官方实现可被 `scene=` 选中出画  
- Demo 只有「示例1」（隧道）主路径；无 custom/五席按钮文案  
- 编译过 Android Debug  

---

确认后回复：**执行** / **按计划执行** / **1**，再编码。
