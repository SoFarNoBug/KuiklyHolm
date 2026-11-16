# file-map（Holm）

> 通用渲染底座；Demo 主推示例1（隧道）双轨。官方五席源码已删。

| 路径 | 说明 |
| --- | --- |
| `KuiklyHolmCore/` | 库：`HolmSurface` + View；Android/H5 Runtime 经 `HolmFullscreenPass` → kmp-wgpu |
| `kmp-wgpu/` | ADR 0013 原语层（Kotlin 类型壳 + 粗 FFI；方案 B） |
| `kmp-wgpu-surface/` | 可选 Present（`AndroidSurfaces` / `JsSurfaces`） |
| `holm-runtime/` | `kmp_wgpu_native`：Rust+wgpu；`holm_fullscreen`；产物名 `holm_runtime`；`build-*.ps1/sh` |
| `iosApp/.../KRHolmSurface.*` | Track A：iOS GLES2 Lite 薄壳（五席已删；无宿主片元 → 清屏+error） |
| `KuiklyHolmCore/.../HolmGlPumpSource.kt` | H5 纯 JS WebGL 泵（Blob Worker / 主线程回退） |
| `holmDemo/` | Demo：`home` + `holm_runtime_m0`（示例1 / 双轨 / 采集） |
| `docs/specs/business-custom-shader/` | 业务片元 changelog |
| `docs/specs/holm-render-substrate/` | 底座身份 / 删五席 changelog |
| `docs/adr/0005-*.md` | 业务片元 + Shadertoy 薄壳 |
| `androidApp/` / `h5App/` | 注册 `KRHolmSurface`；其它端壳仍在 |
| `docs/specs/kuikly-dev-android/runbook.md` | Android 真机装包 / 排障 |
| `docs/specs/kuikly-dev-h5/runbook.md` | H5 起服（可与 CONTRIBUTING 对照） |
| `settings.gradle.kts` | 根 `KuiklyHolmUI`；include kmp-wgpu / kmp-wgpu-surface / Core / Demo / android / h5 |
| `settings.ohos.gradle.kts` | 鸿蒙另册；Core + holmDemo |
| `docs/product/` | 产品 SoT |
| `docs/adr/` | 0007 底座；0008 三包+Provider；0013 kmp-wgpu |
| `docs/skill-references/` | AI 编码规范货架 |
| `.cursor/` | Cursor rules / skills / hooks |
