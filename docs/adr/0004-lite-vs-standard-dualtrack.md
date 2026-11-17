# ADR 0004: DualTrack 收口 · Keep Lite

> **Superseded in part by [ADR 0007](./0007-holm-as-generic-render-substrate.md)**：「Keep Lite 官方五席为业务默认」的**产品身份**已废止；Lite/Runtime 作**后端档**对照的技术结论与旁路工程仍有效。

## Status

Accepted（2026-11-12）；产品身份段部分 superseded（2026-11-14）

## Context

Stage 0 已选 Lite(A)（ADR 0002），官方五席锁定（ADR 0003）。业务侧仍关心：

1. **多端对齐**是否已有证据（尤其 iOS）；  
2. **真模型英雄区**是否值得立刻改选 Standard。

限时双轨尖刺（[`DualTrack-Lite-vs-Standard.md`](../product/phases/DualTrack-Lite-vs-Standard.md)）：

- **Track A**：iOS `KRHolmSurface` + `frost`/`liquid_approx`（同源 GLSL ES）。  
- **Track B**：`kmp-wgpu-native` Rust+wgpu M0 清屏/三角 + Android/H5 `executor=renderer-wgpu` 旁路；**禁止**无工具链时静默换栈。

对比表结论摘要：A 代码与工程已落、本机 Windows 无 Mac 目视 → PARTIAL；B 本机无 `rustc`/`cargo` → **BLOCKED**，包体与通面人日无法实测为正增量账单。

三选一选项：Keep Lite / Switch Standard / Lite 业务 + Standard 仅 3D。

## Decision

1. **Keep Lite** — 继续 Lite 官方五席为业务默认；**不**把 O0 改成 Standard。  
2. **O0 回写**：**否**（仍为 Lite(A)，与 ADR 0002 一致）。  
3. **O1（尖刺默认）**：Standard 尖刺若再开，默认 **Rust + wgpu**（本波 B 已锁定；未通面不算选型完成）。  
4. **不选长期双栈产品化**（选项 3）——表未证明必要；Runtime 页仅作旁路 Demo（`holm_dualtrack`），默认入口仍 `holm_spike`。  
5. 真模型 / M2：**暂缓**，直至 B 通面解除 BLOCKED 并重填对比表后另开 ADR。

## Consequences

- iOS Lite 对齐继续补 Mac 验收与（可选）余下三席；不挡业务用 Android/H5 五席。  
- `kmp-wgpu-native/` 骨架保留；装 Rust 后可续 M0，**不得**用 GLES 冒充 wgpu 通面。  
- 产品 `00` O0 不变；Stage0 结论栏仍 Lite(A)。  
- Demo：`executor` 默认 `lite`；Runtime 仅第二页。
