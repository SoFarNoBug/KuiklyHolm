# ADR 0008: 目标拆包 · holm-core / holm-gl / holm-wgpu + Provider

- Status: Accepted（目标架构；**本单不拆 Gradle**）
- Date: 2026-11-14

## Context

审讯锁定（2026-11-14）：Holm 为通用渲染底座（ADR 0007）。业务要**自选依赖**：要小选 GL 族，要冲性能选 wgpu；骨架只提供统一岛 API。当前仓库仍是胖 `:KuiklyHolmCore` + `holm-runtime`，需先把目标坐标写进 SoT，再另单物理拆包。

## Decision

1. **三包目标名**（对外坐标，暂不要求模块已拆）：
   - **`holm-core`**：岛组件、生命周期、喂 shader / 选后端的统一 API；**不含**画像素实现、不含官方特效目录。
   - **`holm-gl`**：轻量 **GL 族**后端（Android GLES / iOS GLES / H5 WebGL… 同一依赖名，按平台源集实现）。
   - **`holm-wgpu`**：wgpu Runtime 后端（含 native `.so` / wasm 等按端产物）。
2. **可共存**：同一 App 可同时依赖 `holm-gl` 与 `holm-wgpu`。
3. **Provider 必设**：无论 classpath 上有无后端，宿主都必须**显式注册 Provider**；仅有 `holm-core` 或未设 Provider → **明确失败/报错**，禁止静默假画（对齐 ADR 0006 诚实失败）。
4. **选用**：启动时注册默认 Provider；**岛上可覆盖**（保留 DualTrack 式显式切轨）。不再「只引一个后端就自动默选」。
5. **岛契约（v1）**：业务用布局决定岛的位置与大小；一段上色代码把该矩形每帧画满（全屏四边形片元语义）。第一期不承诺官方多 Pass 图 / Compute 产品入口。
6. **样本**：产品叙事**废弃五席**；Demo **主推隧道**；五席源码可暂留并标废弃，删除另单。
7. **本 ADR 不改**：物理 Gradle/Maven 坐标落地、删五席源码、`holm_api` 规范全文（见后单）。

## Consequences

- `docs/product/00`、CONTEXT、glossary 使用上述包名与 Provider 规则。  
- 实现可继续胖 Core，但新设计不得违背「core 无后端必挂、Provider 必设」。  
- 拆包执行单须对照本文验收。

## Relates

- ADR 0007（身份）  
- ADR 0005（fragmentShader / Shadertoy 壳）  
- ADR 0006（Android Runtime Session）
