# file-map（Holm）

> Stage 0 Lite 尖刺已落地；Runtime / 策略 B 后置。

| 路径 | 说明 |
| --- | --- |
| `KuiklyHolmCore/` | 库：`HolmSurface` + `HolmSceneShaders` + frost/glow/liquid_approx 源码 + Android/JS `KRHolmSurfaceView` |
| `KuiklyHolmCore/.../HolmGlPumpSource.kt` | H5 纯 JS WebGL 泵（Blob Worker / 主线程回退） |
| `shaders/frost.*.glsl` / `glow.frag.glsl` / `liquid_approx.frag.glsl` | 同源 GLSL ES（与 `*ShaderSource` 对齐） |
| `holmDemo/` | Demo：`home` + `holm_spike` |
| `androidApp/` / `h5App/` | 注册 `KRHolmSurface`；其它端壳仍在 |
| `docs/specs/kuikly-dev-android/runbook.md` | Android 真机装包 / 排障 |
| `docs/specs/kuikly-dev-h5/runbook.md` | H5 起服（可与 CONTRIBUTING 对照） |
| `settings.gradle.kts` | 根 `KuiklyHolmUI`；include Core / Demo / android / h5 |
| `settings.ohos.gradle.kts` | 鸿蒙另册；Core + holmDemo |
| `docs/product/` | 产品 SoT |
| `docs/adr/` | 0001 胖 Core；0002 Stage0 Lite |
| `docs/skill-references/` | AI 编码规范货架 |
| `.cursor/` | Cursor rules / skills / hooks |
