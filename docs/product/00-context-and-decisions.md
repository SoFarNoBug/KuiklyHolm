# 00 · 背景、目标与锁定决策（含证据）

> SoT：凡与本文冲突的口头约定 / 临时想法，以本文为准，先改本文再改代码。  
> 产品名：**Holm**（命名已定，不改）。  
> **产品身份（已定 · ADR 0007）**：**通用渲染底座**（shader-in / surface-out）——业务定岛区域，传入上色 shader，库出呈现面。  
> **目标拆包（已定 · ADR 0008）**：`holm-core` / `holm-gl` / `holm-wgpu`；**Provider 必设**；可共存、显式选用。  
> **不是**「Kuikly 局部特效产品」；**不是**绑定某一业务。  
> 第一宿主桥接仍走 Kuikly `expand-native-ui`；物理拆包另单。

## 1. 问题陈述

跨端 Kuikly（及后续宿主）需要一块**可嵌入的局部 GPU 表面**：传入着色器与少量旋钮即可出帧；后端可换（轻量 GLES / 可选 wgpu Runtime），契约稳定。

历史动机曾落在「玻璃类特效」与「上层 Canvas 粒子掉帧」；二者只说明**为何需要底座**，不定义 Holm 是什么。

不要用 iOS 系统 Liquid Glass / WebView+Three 冒充跨端渲染底座完成态。

### 证据（问题边界）

| 结论 | 证据 |
| --- | --- |
| Kuikly 主路线原生 View；扩展走 expand-native-ui | Compose overview / 渲染博客 |
| 岛上可喂业务片元 | ADR 0005；Demo `fragmentShader` |
| 双后端通面（Lite GLES + Runtime wgpu） | DualTrack；ADR 0006 / 0009 / 0010 |
| 旧「特效产品」身份已推翻 | ADR 0007 |
| 三包 + Provider 目标架构 | ADR 0008 |
| 鸿蒙 Runtime：XComponent + 外泵 + 双 so | ADR 0010（Lite 后置） |

## 2. 目标（Goals）

1. **通用 shader 契约**：宿主可传入片元（及约定顶点/壳）；库负责上色与 present。  
2. **局部 GPU 岛**：布局决定位置与大小；不替换 Kuikly 主 UI 树。  
3. **可插拔后端（目标三包）**：`holm-gl`（小）/ `holm-wgpu`（冲性能）；`holm-core` 只提供统一 API；**须显式 Provider**。  
4. **Kuikly 可调用**：DSL + Compose；viewName `KRHolmSurface`。  
5. **诚实失败**：无 Provider / 缺后端 → 明确报错，禁止静默假画。  
6. **少岛纪律**：列表默认禁重实时岛；Backdrop 见 `07`。

## 3. 非目标（Non-Goals）

| 非目标 | 理由 |
| --- | --- |
| 以官方玻璃五席为产品完成态 | ADR 0007/0008：叙事废弃；Demo 主推隧道 |
| 绑定 ParticleEngine 或任一业务域名 | 底座不内置领域模型 |
| 仅 core、不设 Provider 还能出画 | ADR 0008 |

| 粒子模拟 / 物理 / 完整游戏引擎 | 生态位是呈现底座 |
| 端侧 AI / NPU 推理产品 | 另产品线 |
| 第一期通用多框架齐头（Compose/UIKit/ArkUI 多 bridge） | 桥接另 ADR；不挡 Kuikly 底座 |
| 改名 / 去掉 KR 前缀 | D10 锁定 |
| 系统 Liquid Glass / WebView+Three 冒充完成 | D3 |

## 4. 后端档与 Stage 0（历史）

### 4.0 价值边界（写给宿主）

> 给你一块局部 GPU 岛：传入 shader（+ knobs / playing / maxFps…），稳定出帧；弱端可走轻量后端或 Caps 降级。

| 目标包 | 含义 | 包体直觉 |
| --- | --- | --- |
| **holm-core** | 岛 API + 生命周期 + Provider 槽 | 极小 |
| **holm-gl** | GL 族后端（含 iOS GLES / H5 WebGL） | 小 |
| **holm-wgpu** | wgpu Runtime 后端 | 中（须实测） |

历史称 Lite/Standard = 上述后端档口语；**对外目标名以 ADR 0008 为准**。  
五席叙事**废弃**；官方五席源码**已删**（2026-11-14）；Demo **主推隧道**示例1。

### 4.1 路径对比（诚实）

| 路径 | 做法 | 结论 |
| --- | --- | --- |
| **Lite** | 各端轻量后端执行约定 shader | 默认轻量档 |
| **Standard+** | Holm Runtime + 桥接 | 需要统一 Runtime / 热换 WGSL 时用 |
| **C** | WebView+Three | 仅验证，非生产底座 |

### 4.2 Stage 0（历史门禁 · 产品结论已取代）

Stage 0 曾在 2026-11-12 选 **Lite(A)**（ADR 0002），目标是「特效尖刺」。  
**产品身份**已由 ADR 0007 取代：「选 Lite 做玻璃交付」不再是身份锚。  
**工程含义仍有效**：全量 Standard Runtime（M0–M3）须有书面后端选型；尖刺史实见 `phases/Stage0.md`、ADR 0002–0004。

### 4.3 防膨胀

| 允许 | 禁止（除非 ADR） |
| --- | --- |
| 局部岛、宿主 shader、简单程序化/提交纹理输入 | 整页进岛、IME、长列表默认实时重岛、路由壳 |
| 岛上叠 **Kuikly 兄弟控件** | 把控件树搬进 Scene |
| Demo 样本特效作通面夹具 | 把样本目录当成唯一产品 KPI |

### 4.4 交付顺序（底座）

1. **先**：稳定岛生命周期 + shader 喂入契约 + 至少一档后端通面。  
2. **再**：双后端对照、Caps、包体/符号瘦身。  
3. **然后**：按宿主需求加深 Runtime / 自定义 Pass。  
4. **最后**（若需要）：简单 3D（Full）。  

禁止用「再堆几个官方特效席」代替底座契约完成度。

## 5. 锁定决策

| ID | 决策 | 状态 |
| --- | --- | --- |
| D0 | 全量 Standard Runtime 须书面后端选型（历史称 Stage 0） | 锁定（语义修订） |
| D1 | 局部嵌入，不替换 Kuikly 主渲染 | 锁定 |
| D2 | **锚点 = shader 契约 / Pass 语义**；统一 RHI Runtime 为 **Standard 可选** | **修订锁定** |
| D3 | 禁系统 Liquid Glass 充四端底座 | 锁定 |
| D4 | 观感+可测；不承诺系统气质 | 锁定 |
| D5 | 弱端 Caps 子集；能力可不装全 | 锁定 |
| D6 | 少岛；列表默认禁实时重岛；Backdrop 见 01/07 | 锁定 |
| D7 | 后端档演进：Lite 通面 →（按需）Standard / Full | **修订锁定** |
| D8 | Web：WebGPU→WebGL 降级须登记 | 锁定 |
| D9 | 鸿蒙能力子集 + Caps；ABI 形稳；Runtime 形态见 ADR 0010 | 锁定 |
| D10 | 命名 Holm / HolmSurface / KRHolmSurface / holm_* | 锁定 |
| D11 | 目标默认后端叙事对齐 ADR 0008（Provider 必设；显式选 gl/wgpu） | **修订锁定** |
| D12 | **身份：通用渲染底座**（ADR 0007）；不绑定单一业务 | **修订锁定** |
| D13 | **目标拆包** holm-core/gl/wgpu + Provider（ADR 0008）；物理拆包另单 | **锁定** |

## 6. 成功标准

- 宿主能稳定：建岛 → 喂 shader → 出帧 → 销毁无泄漏。  
- Lite 与（若启用）Runtime 失败路径诚实（BLOCKED / onError），无静默假栈。  
- 文档身份与 ADR 0007 一致；样本五席不充当产品 KPI。  
- 包体与性能以底座通面实测为准（另表）。

## 7. 依赖

- Kuikly `expand-native-ui` / Compose 扩展  
- `07` / `05` / `01` / ADR 0005–0007  

## 8. 开放问题

| ID | 问题 | 关闭 |
| --- | --- | --- |
| O0 | 历史 Stage 0 选 Lite 还是 Standard | **历史关闭 → Lite(A)**；**身份已由 ADR 0007 取代** |
| O1 | Standard Runtime：wgpu vs 其它 | 尖刺默认 wgpu；正式全量另决 |
| O2 | Backdrop：07 推荐默认是否采纳 | 书面前慎承诺 |
| O3 | 3D / Filament | Full 前 |
| O4 | 与 GearUI 边界 | 定仓后 |
| O5 | 是否远期开源/多框架 | **非当前**；另 ADR |
| O6 | 与 ParticleEngine 等消费者的接入契约（仅示例，非身份） | 另单；本 00 不绑定 |

---

**下一篇**：[07-p0-hard-problems.md](./07-p0-hard-problems.md) · [01-architecture.md](./01-architecture.md) · [ADR 0007](../adr/0007-holm-as-generic-render-substrate.md)
