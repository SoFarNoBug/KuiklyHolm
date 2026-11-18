# ADR 0007: Holm · 通用渲染底座（shader-in / surface-out）

- Status: Accepted
- Date: 2026-11-14

## Context

此前产品身份（`docs/product/00` D12、Stage 0 / ADR 0002–0004）将 Holm 定义为 **Kuikly 局部特效增强**：官方五席（霜化等）为交付中心，「效果优先于引擎」。

实际工程已具备：局部 GPU 岛（`HolmSurface` / `KRHolmSurface`）、宿主可传入片元（ADR 0005）、Lite GLES 与 Runtime wgpu 双后端（DualTrack / ADR 0006）。触发动机之一是上层库（如粒子）在 Compose `Canvas`+DrawScope 上遇到帧率墙，需要可替换的 GPU 呈现路径——但 **Holm 不应因此绑定某一业务**。

需要推翻的是**产品身份句子**，不是立刻拆掉 Demo 样本或改 ABI 名。

## Decision

1. **身份**：Holm 是 **通用渲染底座**——宿主传入 shader（及约定壳 / knobs / 生命周期），库负责岛表面、泵帧、跨端后端呈现（**shader-in / surface-out**）。
2. **不绑定业务**：SoT **禁止**写「专为粒子 / 专为玻璃 / 专为某 App」；任意 Kuikly（及后续另开 ADR 的宿主）可作消费者。粒子等库仅可写在「示例消费者 / 触发动机」。
3. **旧特效产品线降级**：官方五席与 frost 业务叙事降为 **Demo 样本与通面/回归夹具**，不再是产品完成态标准。
4. **后端档**：Lite（各端轻量 GLES/WebGL 等）与 Standard Runtime（如 wgpu）是 **底座实现档**，不是「玻璃产品 vs 引擎产品」对立。
5. **命名保留**：`Holm` / `HolmSurface` / `KRHolmSurface` / `holm_*`（D10 仍锁定）。
6. **仍成立的工程约束**：局部岛、不替换 Kuikly 主 UI 树；桥接优先 `expand-native-ui`；禁系统 Liquid Glass / WebView+Three 冒充底座完成态。
7. **显式废止（身份段）**：旧 D12「Kuikly 业务优先 = 特效增强层」产品句；Stage 0「选 Lite 做玻璃特效交付」作为**产品主目标**的结论（技术尖刺史实保留在 ADR 0002–0004，见文首 superseded）。

## Non-Goals（本决策）

- 粒子模拟 / 物理 / 完整游戏引擎  
- 端侧 AI / NPU 推理产品  
- 本 ADR 不改业务源码；编码规范 `holm_api` 全文另单对齐（**已于 2026-11-18 完成**）

## Consequences

- `docs/product/00`、CONTEXT、AGENTS、glossary 以本文 + 重写后的 00 为身份 SoT。  
- Demo 五席/隧道可留作夹具；路线图以「稳定 shader 契约 + 后端档」为准，不以「再交几个玻璃特效」为准。  
- 消费者（含 ParticleEngine）自行决定如何喂 shader / 是否迁渲染；Holm 不内置其领域模型。

## Supersedes (in part)

- ADR 0002 / 0003 / 0004 的**产品身份与「特效交付中心」目标**（工程与尖刺记录仍有效）  
- `00` 旧 D12 / 「效果优先于基建」作为身份锚的表述（由重写后的 00 取代）
