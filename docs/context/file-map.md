# file-map（Holm）

> 通用渲染底座；Demo 主推示例1（隧道）双轨。官方五席源码已删。

| 路径 | 说明 |
| --- | --- |
| `holm-core/` | 岛 API + `HolmProviders` / `HolmBackendProvider`（common；各端共享） |
| `holm-renderer-gl/` | Lite GL 族：Shadertoy 壳、`HolmGlPumpSource`、GLES 半部；ios/ohos 目标 + `GlBackendProvider` |
| `holm-renderer-wgpu/` | Runtime Present：`HolmFullscreenPass` / `HolmWgpuNative`；Android/H5/iOS/Ohos；依赖 kmp-wgpu* |
| `KuiklyHolmCore/` | Umbrella：`api` 三包；Android/H5 `KRHolmSurfaceView`；iOS `HolmWgpuIosBridge`；Ohos `HolmWgpuOhosBridge` + Bootstrap |
| `~/Studio/kmp/kmp-wgpu/` | **独立仓**（ADR 0015）：core / surface / native + sample；Maven 0.1.0 |
| `~/Studio/kmp/kmp-wgpu/ohosInclude/` | Ohos `includeBuild` 入口（`build.ohos` + KBA；补 `ohosArm64`） |
| `ohosApp/.../holm_xc/` | XComponent 泵 + Lite EGL；Runtime dlsym `holm_ohos_bridge_*` |
| `iosApp/.../KRHolmSurface.*` | 容器岛：Lite GLES + Runtime 经 `HolmDemoHolmWgpuIosBridge`；泵仍 CADisplayLink |
| `holmDemo/` | Demo：`home` + `holm_dualtrack`（双渲染后端）+ `kmp_wgpu_compute`（v1.1） |
| `docs/specs/business-custom-shader/` | 业务片元 changelog |
| `docs/specs/holm-render-substrate/` | 底座身份 / 删五席 changelog |
| `docs/adr/0005-*.md` | 业务片元 + Shadertoy 薄壳 |
| `androidApp/` / `h5App/` | 注册 `KRHolmSurface`；其它端壳仍在 |
| `docs/specs/kuikly-dev-android/runbook.md` | Android 真机装包 / 排障 |
| `docs/specs/kuikly-dev-h5/runbook.md` | H5 起服（可与 CONTRIBUTING 对照） |
| `docs/specs/kuikly-dev-ios/runbook.md` | iOS 起服 / DualTrack 验收 |
| `settings.gradle.kts` | 根 `KuiklyHolmUI`；三包 / Core / Demo / android / h5；local → includeBuild `../kmp-wgpu` |
| `settings.ohos.gradle.kts` | 鸿蒙另册；三包 / Core / holmDemo；local → includeBuild `../kmp-wgpu/ohosInclude` |
| `docs/guide.md` | **接入手册**（API 全表；与 README / CONTRIBUTING 三份对齐 kmp-wgpu） |
| `docs/product/` | 产品 SoT |
| `docs/adr/` | 0007 底座；0008 三包；0013 kmp-wgpu；0014 命名；**0015 独立仓** |
| `docs/skill-references/` | AI 编码规范货架 |
| `.cursor/` | Cursor rules / skills / hooks |
