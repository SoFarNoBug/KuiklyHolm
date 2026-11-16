# ADR 0008: 目标拆包 · holm-core / holm-renderer-gl / holm-renderer-wgpu + Provider

- Status: Accepted（**Android+H5+iOS+Ohos 已对齐** Provider + renderer 目标 + kmp-wgpu Present；Maven 独立坐标 / 拆仓另单）
- Date: 2026-11-14
- Updated: 2026-11-16（包名：`holm-renderer-*`；Android+H5 物理拆；iOS / Ohos 架构对齐）

## Context

审讯锁定（2026-11-14）：Holm 为通用渲染底座（ADR 0007）。业务要**自选依赖**：要小选 GL 族，要冲性能选 wgpu；骨架只提供统一岛 API。曾用短名 `holm-gl` / `holm-wgpu`；落地时改为 **`holm-renderer-*`**，强调岛 Present，避免与底层 `kmp-wgpu` 算力层混淆。

## Decision

1. **三包目标名**（对外坐标）：
   - **`holm-core`**：岛组件、生命周期、喂 shader / 选后端的统一 API + **Provider 槽**；**不含**画像素实现、不含官方特效目录。
   - **`holm-renderer-gl`**：轻量 **GL 族**后端（Android GLES / iOS GLES / H5 WebGL… 同一依赖名，按平台源集实现）。（原草案名 `holm-gl`）
   - **`holm-renderer-wgpu`**：Holm 的 wgpu **Provider / 岛 Present 薄适配**（含 native `.so` / wasm 等按端产物）；通用原语下沉见 **ADR 0013**（`kmp-wgpu`）。（原草案名 `holm-wgpu`）
2. **可共存**：同一 App 可同时依赖 `holm-renderer-gl` 与 `holm-renderer-wgpu`。
3. **Provider 必设**：无论 classpath 上有无后端，宿主都必须**注册 Provider 并 setDefault**；仅有 `holm-core` 或未设 → **明确失败/报错**，禁止静默假画（对齐 ADR 0006 诚实失败）。  
   - **兼容例外**：依赖 umbrella `:KuiklyHolmCore` 时，可由 `HolmUmbrellaBootstrap` 自动 register 两后端并 default=`gl`；**推荐**宿主/Demo 仍显式 `HolmProviders.register` 演示契约。  
   - **禁止**：仅因 classpath 上有 renderer jar、却从未 register / 未 setDefault，就静默默选出画。
4. **选用**：启动时注册默认 Provider；**岛上可覆盖**（保留 DualTrack 式 `executor=lite|runtime`）。不再「只引一个后端 jar 就自动默选」。
5. **岛契约（v1）**：业务用布局决定岛的位置与大小；一段上色代码把该矩形每帧画满（全屏四边形片元语义）。第一期不承诺官方多 Pass 图 / Compute 产品入口。
6. **样本**：产品叙事**废弃五席**；Demo **主推隧道**；五席源码已于 2026-11-14 从仓库移除。
7. **物理拆节奏**：
   - **Android + H5**：三模块 + umbrella 已落地。
   - **iOS（本波）**：`kmp-wgpu` / `kmp-wgpu-surface` / `holm-renderer-*` 加 ios 目标；Runtime Present 经 `HolmFullscreenPass` + `HolmRuntimeIosBridge`；壳 **不再直调** C Session；泵仍 ObjC（ADR 0009）。
   - **Ohos（本波）**：`settings.ohos` 纳入三包 + `kmp-wgpu*`；Runtime Present 经 `HolmFullscreenPass` + `HolmRuntimeOhosBridge`（Demo `@CName` / `libholm_xc` dlsym）；壳 **不再直调** C Session；泵仍 XComponent OnFrame（ADR 0010）；Lite EGL 仍在 `libholm_xc`（ADR 0011）。Maven 独立坐标 / 拆仓另单（O7）。

## Consequences

- `docs/product/00`、CONTEXT、glossary 使用上述包名与 Provider 规则。  
- 旧文出现的 `holm-gl` / `holm-wgpu` 均视为 **`holm-renderer-gl` / `holm-renderer-wgpu`**。  
- 拆包执行单对照本文验收。

## Relates

- ADR 0007（身份）  
- ADR 0013（kmp-wgpu 原语层；`holm-renderer-wgpu` 叠其上）  
- ADR 0005（fragmentShader / Shadertoy 壳）  
- ADR 0006（Android Runtime Session）  
- ADR 0009（iOS 泵与 View；Present 经 kmp-wgpu）
