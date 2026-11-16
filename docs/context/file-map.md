# file-map（Holm）

> 通用渲染底座；Demo 主推示例1（隧道）双轨。官方五席源码已删。

| 路径 | 说明 |
| --- | --- |
| `holm-core/` | 岛 API + `HolmProviders` / `HolmBackendProvider`（Android+H5；ios 目标共享 common） |
| `holm-renderer-gl/` | Lite GL 族：Shadertoy 壳、`HolmGlPumpSource`、GLES 半部 |
| `holm-renderer-wgpu/` | Runtime Present：`HolmFullscreenPass` / `HolmRuntimeNative` / Runtime 泵；依赖 kmp-wgpu* |
| `KuiklyHolmCore/` | Umbrella：`api` 三包；Android/H5 `KRHolmSurfaceView`；iOS/Ohos 源暂留 |
| `kmp-wgpu/` | ADR 0013 原语层（Kotlin 类型壳 + 粗 FFI；方案 B）；v1.1 headless Compute+回读 |
| `kmp-wgpu-surface/` | 可选 Present（`AndroidSurfaces` / `JsSurfaces`） |
| `holm-runtime/` | `kmp_wgpu_native`：Rust+wgpu；`holm_fullscreen` + `compute_readback`；产物名 `holm_runtime`；`build-*.ps1/sh` |
| `iosApp/.../KRHolmSurface.*` | Track A：iOS GLES2 Lite 薄壳（五席已删；无宿主片元 → 清屏+error） |
| `holmDemo/` | Demo：`home` + `holm_runtime_m0`（双轨）+ `kmp_wgpu_compute`（v1.1 无 Surface 冒烟） |
| `docs/specs/business-custom-shader/` | 业务片元 changelog |
| `docs/specs/holm-render-substrate/` | 底座身份 / 删五席 changelog |
| `docs/adr/0005-*.md` | 业务片元 + Shadertoy 薄壳 |
| `androidApp/` / `h5App/` | 注册 `KRHolmSurface`；其它端壳仍在 |
| `docs/specs/kuikly-dev-android/runbook.md` | Android 真机装包 / 排障 |
| `docs/specs/kuikly-dev-h5/runbook.md` | H5 起服（可与 CONTRIBUTING 对照） |
| `settings.gradle.kts` | 根 `KuiklyHolmUI`；include 三包 / kmp-wgpu* / Core / Demo / android / h5 |
| `settings.ohos.gradle.kts` | 鸿蒙另册；Core + holmDemo（本波未拆） |
| `docs/product/` | 产品 SoT |
| `docs/adr/` | 0007 底座；0008 三包+Provider；0013 kmp-wgpu |
| `docs/skill-references/` | AI 编码规范货架 |
| `.cursor/` | Cursor rules / skills / hooks |
