# Holm · 通用渲染底座

> **身份以 [`00`](./00-context-and-decisions.md) + [ADR 0007](../adr/0007-holm-as-generic-render-substrate.md) / [ADR 0008](../adr/0008-holm-core-gl-wgpu-packages.md) 为准。**  
> Holm = **shader-in / surface-out**；目标包 **`holm-core` / `holm-renderer-gl` / `holm-renderer-wgpu`**；**Provider 必设**。  
> Android+H5 已物理拆；`:KuiklyHolmCore` 为 umbrella（可自动注册，宿主仍可显式 `HolmProviders.register`）。  
> Demo **主推隧道**；五席叙事废弃。  
> 命名锁定：Holm / `HolmSurface` / `KRHolmSurface` / `holm_*`。

## 品牌与命名

| 用途 | 名称 |
| --- | --- |
| 产品 | **Holm** |
| 文档 | `docs/product/` |
| API | `HolmSurface` |
| viewName | `KRHolmSurface` |
| ABI | `holm_*` |

## 文档地图

| 文档 | 用途 |
| --- | --- |
| [00-context-and-decisions.md](./00-context-and-decisions.md) | 身份、后端档、决策表 |
| [ADR 0007](../adr/0007-holm-as-generic-render-substrate.md) | 身份：通用渲染底座 |
| [ADR 0008](../adr/0008-holm-core-gl-wgpu-packages.md) | 目标拆包 + Provider |
| [07-p0-hard-problems.md](./07-p0-hard-problems.md) | Backdrop 等生死问题 |
| [phases/Stage0.md](./phases/Stage0.md) | 历史选型门禁（身份段见 0007） |
| [01-architecture.md](./01-architecture.md) | 四层栈；shader 契约；Kuikly 桥接 |
| [02](./02-kuikly-integration.md)–[06](./06-implementation-roadmap.md) | 集成 / RHI / 场景 / 对拍 / 路线图（若与 00 冲突以 00/0007 为准） |
| [references/](./references/) | 证据与术语 |

## Agent 协议

1. 读 `00` + **ADR 0007/0008**。  
2. 禁止写成「专为粒子 / 专为玻璃」；五席不主推。  
3. 目标：三包 + Provider 必设（Android+H5 已拆；iOS/Ohos 另单）。  
4. 禁止系统 Liquid Glass / WebView+Three 冒充底座完成。  
5. 命名不改。  

## 架构一句话

```text
Host / Kuikly 壳 + HolmSurface
  └── shader 契约（宿主片元或样本）
        ├── renderer-gl：各端轻量 GL
        └── renderer-wgpu：可选 kmp-wgpu Present
```

## 状态

| 项 | 状态 |
| --- | --- |
| 身份 | **通用渲染底座**（ADR 0007） |
| 目标拆包 | ADR 0008：Android+H5 已物理拆（`holm-core` / `holm-renderer-*`）；iOS/Ohos 另单 |
| Demo | 主推隧道；五席废弃叙事 |

## 修订

| 日期 | 说明 |
| --- | --- |
| 2026-11-11 | 初版；Holm 命名；Stage 0 / 路径 A；P0 专章 |
| 2026-11-12 | Kuikly 优先特效叙事；Lite/Standard/Full |
| 2026-11-14 | **推翻身份** → 通用渲染底座（ADR 0007） |
| 2026-11-14 | 目标三包 + Provider（ADR 0008）；Demo 隧道优先 |
| 2026-11-16 | 状态表对齐：Android+H5 三包已拆（关审查 r2 `P1-doc-drift`） |
