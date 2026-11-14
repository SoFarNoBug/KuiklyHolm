# ADR 0003: Lite 官方五席锁定 + Attr 可读改名

> **Superseded in part by [ADR 0007](./0007-holm-as-generic-render-substrate.md) / [ADR 0008](./0008-holm-core-gl-wgpu-packages.md)**：五席**产品叙事废弃**；Demo 主推隧道；源码可暂留标废弃（删除另单）。Attr/scene 键工程约定仍有效。

## Status

Accepted（2026-11-12）；产品目标段部分 superseded（2026-11-14）

## Context

Stage 0 选 Lite(A) 后已交付 `frost` / `glow` / `liquid_approx`。S0.1 冻结「≤5 个稳定特效」。审讯锁定本单补齐 `gradient_bg` + `fullscreen_warp`，并**打满五席**。同时公开 Attr `ior`/`warp` 语义晦涩且易被误以为可跨 scene 共用。

## Decision

1. **12 个月官方五席**（换席须先改本文 + Stage0 / glossary）：
   - `frost` / `glow` / `liquid_approx` / `gradient_bg` / `fullscreen_warp`
2. **视觉分工**：
   - `gradient_bg`：斜向多色条带平移；`scrollSpeed` 控速率；`playing=false` 冻结
   - `fullscreen_warp`：全屏程序化场扭曲（热浪/水波）；**无**玻璃/色散/rim；与 liquid 面板正交
3. **Attr 解耦、硬删旧键**（无别名期）：

| scene | Attr | 含义 |
| --- | --- | --- |
| `liquid_approx` | `liquidBoost` / `liquidBend` | 原 `ior` / `warp` |
| `fullscreen_warp` | `warpAmount` | 场扭曲幅度 |
| `gradient_bg` | `scrollSpeed` | 条带平移速率 |
| `frost` / `glow` | （无强度旋钮） | 仅 scene / playing / maxFps |

4. **平台**：本单仍 Android + H5；iOS / 鸿蒙记缺口。
5. **不做**：独立 Runtime、Backdrop B、未知 scene→onError（仍回退 frost）。

## Consequences

- Lite 官方特效名额已满；新增官方 scene 须先修订本 ADR。
- 消费方须改用新 Attr 键；旧 `ior`/`warp` 字符串不再被识别。
- Demo `holm_spike` 按 scene 条件展示旋钮，强化「不耦合」。
