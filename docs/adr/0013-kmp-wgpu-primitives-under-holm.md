# ADR 0013: kmp-wgpu · 通用 wgpu 原语层（Holm 之下）

- Status: Accepted
- Date: 2026-11-16

## Context

审讯锁定（2026-11-16）：其它项目需要跨端 GPU 算力与图像加速，不能被迫依赖 Holm / Kuikly。现有 `holm-runtime` 是 **Holm Session + 各端 Present**（见 `holm-runtime/README.md`），语义偏窄，不宜直接当通用库。ADR 0007 将 Holm 定为渲染底座；ADR 0008 的 `holm-wgpu` 是 Holm **后端包**，不是通用原语产品。

## Decision

1. **新产品身份**：`kmp-wgpu` = 跨端 **wgpu 原语库**（Device / Queue / Buffer / Texture / BindGroup / Render+Compute Pipeline / Encoder / ShaderModule 等）。不绑 UI 框架、不内置业务算法目录。
2. **产品脸**：Kotlin KMP 为对外主 API；底层 Rust + wgpu（自 `holm-runtime` **抽芯**）；热路径粗粒度过桥，避免碎 JNI。
3. **API 贴合度**：与 wgpu **近 1:1 概念映射**（Kotlin 命名风格）；v1/v1.1 **不做**高层 toolkit 包。
4. **模块**：
   - `kmp-wgpu`：core 原语；**默认无**窗口 Present。
   - `kmp-wgpu-surface`：可选 Present / Surface。
   - Native crate 目标名：`kmp_wgpu_native`（由 `holm-runtime` 演进改名）。
5. **与 Holm / ADR 0008**：
   - `kmp-wgpu`（+ 可选 surface）= 与 Holm **无关**的通用层。
   - `holm-wgpu` = Holm Provider / 岛 Present **薄适配**，依赖前者；保留三包 + Provider 必设。
6. **仓**：先在 `kuikly-holm` 内分层模块化；API 与 Holm 迁移稳定后再拆独立仓发版。
7. **里程碑**：
   - **v1**：Holm Runtime 出帧路径迁到 `kmp-wgpu` + `kmp-wgpu-surface`，行为与现 Session **对等**；端范围先 **Android + H5**；iOS / Ohos → v1.x。
   - **v1.1（硬门禁）**：headless **Compute + 回读**示例必须交付（证明 Present 可选不是纸面）。
8. **非目标**（另 ADR 才能推翻）：第二套非 wgpu 图形后端；Kuikly/Compose/任何 UI 桥；官方图像算子/特效目录；场景图/游戏引擎/物理；NPU/OpenCL 等非 wgpu 算力；v1/v1.1 toolkit 包。

## Consequences

- `holm-runtime` 不再是长期对外身份；文档与路线图改称抽芯 → `kmp-wgpu` / `kmp_wgpu_native`。
- 其它业务（粒子、图像算法等）应依赖 `kmp-wgpu`，**不应**为了算力去依赖 `holm-wgpu`。
- 物理 Gradle/Maven 坐标与独立仓仍另单；本 ADR 先锁方向与边界。
- 须更新 `docs/product/00`（D14）、CONTEXT / glossary、AGENTS 指向本文。

## Relates

- ADR 0007（Holm 身份 · 渲染底座）
- ADR 0008（holm-core / holm-gl / holm-wgpu · 不废除，叠在 kmp-wgpu 之上）
