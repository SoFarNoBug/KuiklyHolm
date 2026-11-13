# 00 · 背景、目标与锁定决策（含证据）

> SoT：凡与本文冲突的口头约定 / 临时想法，以本文为准，先改本文再改代码。  
> 产品名：**Holm**（命名已定，不改）。  
> **产品身份（已定）：Kuikly 业务优先**——Holm 是 Kuikly App 的局部特效增强层，**不是**通用开源多框架引擎；不把「解绑 Kuikly、多宿主齐头并进」当第一目标。  
> **重要**：全量统一 Runtime **不是默认开工选项**。须过 **Stage 0**；且即使上 Holm，也以 **Shader 同源为锚点**，wgpu Runtime 为 **可选增强**。

## 1. 问题陈述

业务 App 大部分页面用 **Kuikly 常规组件**；另有 **少量复杂特效** 与 **简单 3D**，要在 Android / iOS / 鸿蒙 / Web 上 **尽量对齐**，性能可接受。

不要用 iOS 26+ 系统 Liquid Glass 冒充四端方案。

### 证据（问题边界）

| 结论 | 证据 |
| --- | --- |
| Kuikly 主路线原生 View | Compose overview / 渲染博客 |
| 系统 Liquid Glass 仅 Apple 26+ | `ios26-liquid-glass.md` |
| 跨端折射靠同源 shader/分级 | Prismal；Flutter liquid_glass |

## 2. 目标（Goals）

1. **服务 Kuikly 业务页**：DSL + Compose 可调用；桥接走 `expand-native-ui`（`KRHolmSurface`）。  
2. **ROI 优先**：特效少且稳定 → 路径 A（轻量）；仅增殖/强同源需求 → 路径 B。  
3. **锚点是 Shader/Pass 同源**，不是「必须上 wgpu」。  
4. **先交付可给业务用的效果，再加深基建**（效果优先于引擎炫耀）。  
5. 禁止系统 Liquid Glass / WebView+Three 当生产终态。  
6. 不替换 Kuikly 主 UI 树；守住「局部增强」边界（§4.5）。

## 3. 非目标（Non-Goals）

| 非目标 | 理由 |
| --- | --- |
| 第一期做成通用开源多框架库（Compose/UIKit/ArkUI 齐头） | **身份：Kuikly 优先**；其他宿主若需要，另开 ADR，不挡业务 |
| 未过 Stage 0 就全量 Runtime | 过度设计 |
| 整页自绘 / 完整 3D 引擎 / 物理骨骼 | 生态位不是游戏引擎 |
| 列表实时真折射等未承诺能力 | 见 07；白纸黑字 |
| 改名 / 去掉 KR 前缀 | D10 锁定 |

## 4. 路径、重量等级与 Stage 0

### 4.0 身份与价值边界（写给业务）

Holm（及路径 A 的同系 API）对业务的价值是：

> 在 Kuikly 页面里，用尽量少的代码加上高级视觉效果；四端尽量齐；弱机可降级；**不**要求业务理解 RHI。

内部实现可以分三档重量（对业务可透明）：

| 重量 | 含义 | 约等于 | 包体直觉 |
| --- | --- | --- | --- |
| **Lite** | 官方标准特效；各端跑 **预编译 shader 产物** 或原生轻量后端；无通用 Scene 引擎 | 路径 A 加强版（仍尽量同源 shader） | 小 |
| **Standard** | 可选统一 Runtime（如 wgpu）+ 自定义 Pass/Shader | 路径 B | 中（须实测） |
| **Full** | Standard + 简单 3D/glTF | 路径 B + M2 | 更重 |

**业务默认应能只用 Lite 级能力**（玻璃等）；Standard/Full 按 Stage 0 / 需求解锁。

### 4.1 路径对比（诚实）

| 路径 | 做法 | 工期粗估 | 一致性 | 结论 |
| --- | --- | --- | --- | --- |
| **A / Lite** | 各端执行同源 shader 编译产物（或极简后端）+ Kuikly API 壳 | 数周级 | 逻辑同源强于「各写各的」；无大 Runtime | **特效少时默认** |
| **B / Standard+** | Holm Runtime + 可插拔 RHI + Kuikly Bridge | 数月基建 | 同源 + 易扩展 | **增殖时再上** |
| **C** | WebView+Three | 验证快 | 中 | 仅验证 |

> 观感一致的核心是 **Shader/Pass 一致**，不是底层 API 统一。wgpu 是 Standard 档的可选实现，不是宗教。

### 4.2 何时不选 Standard/Full（仍用 Lite/A）

1. 特效 ≤5 且 12 个月不大增殖。  
2. 不要求动态加载任意自定义 Pass。  
3. 包体/图形人力紧。  
4. 仅英雄区、不进列表。  
5. Stage 0：Lite 边际成本更优。

### 4.3 何时上 Standard（原「选 B」）

Stage 0 书面选 B，且需要持续加材质/自定义 Pass，并接受 D6 使用纪律与 D9 能力子集。

### 4.4 Stage 0 门禁

未关闭 Stage 0，禁止 Standard/Full 全量编码。

| 项 | 要求 |
| --- | --- |
| 产出 | `phases/Stage0.md` + 结论 A(Lite) / B(Standard+) / 终止 |
| A 尖刺 | 1 个特效，Android+iOS，**优先共用同一份 WGSL/编译产物**（能则做；否则先分端再收敛） |
| B 尖刺 | 单端最小 Surface + 包体 |
| 硬问题 | `07` H1–H4 认知 |

### 4.5 防膨胀

| 允许 | 禁止（除非 ADR） |
| --- | --- |
| 英雄区特效、简单 3D、程序化/提交纹理背景 | 整页进岛、IME、长列表进 GPU、路由壳 |
| 岛上叠 **Kuikly 兄弟控件** | 把控件树搬进 Scene |

### 4.6 交付顺序（Kuikly 业务：效果优先）

在 **选 Lite 或通过 Stage 0** 后，对业务的可见节奏建议：

1. **先**：1～2 个开箱特效（霜化 / 液态近似）在 Kuikly Demo 可调。  
2. **再**：更多官方特效、质量档、静态缓存。  
3. **然后**（若选 Standard）：自定义 Shader/Pass、完整 Runtime。  
4. **最后**：简单 3D（Full）。  

禁止「半年只有仓和三角、业务零特效」却宣称 Holm 已落地。

## 5. 锁定决策

| ID | 决策 | 状态 |
| --- | --- | --- |
| D0 | Stage 0 门禁 | 锁定 |
| D1 | 局部嵌入，不替换 Kuikly 主渲染 | 锁定 |
| D2 | **锚点 = Shader/Pass 同源**；统一 RHI Runtime 为 **Standard 可选**，非 Lite 强制 | **修订锁定** |
| D3 | 禁系统 Liquid Glass 充四端 | 锁定 |
| D4 | 观感+阈值；不承诺系统气质 | 锁定 |
| D5 | Shader 单一源；弱端 Caps 子集 | 锁定 |
| D6 | 少岛、列表默认禁实时重特效；机制上优先静态缓存（见 01/07） | 锁定 |
| D7 | Stage0 →（Lite 交付 / 或 M0–M3 Standard） | 锁定 |
| D8 | Web：WebGPU→WebGL2→再降级须登记 | 锁定 |
| D9 | 鸿蒙能力子集 + Caps；ABI 形稳、能力不装全 | 锁定 |
| D10 | 命名 Holm / HolmSurface / KRHolmSurface / holm_* | 锁定 |
| D11 | Lite/路径 A 合法默认 | 锁定 |
| D12 | **身份：Kuikly 业务优先**；非第一期通用开源多 bridge | **锁定** |

## 6. 成功标准

### Stage 0

书面选 Lite/A、Standard/B 或终止；H1–H4 有认知。

### 若 Lite/A

Kuikly 上可调用代表特效；Android+iOS（目标四端）可验收；包体可接受。

### 若 Standard/B

`05` 阈值；`HolmSurface` DSL+Compose；销毁无泄漏；业务能用到的特效先于「引擎完整」。

## 7. 依赖

- Kuikly `expand-native-ui` / Compose 扩展  
- `07` / `05` / `01` / evidence-index  

## 8. 开放问题

| ID | 问题 | 关闭 |
| --- | --- | --- |
| O0 | Stage 0 选 Lite 还是 Standard | **已关闭 → Lite(A)**（2026-11-12；见 `phases/Stage0.md`、ADR 0002）；DualTrack ADR 0004 **Keep Lite，未改选** |
| O1 | Standard 的 Runtime：wgpu vs 其它 | **尖刺默认 wgpu**（DualTrack B / ADR 0004）；正式全量选型仍须通面后另决 |
| O2 | Backdrop：07 推荐默认是否采纳 | M1 前书面 |
| O3 | 3D / Filament | Full 前 |
| O4 | 与 GearUI 边界 | 定仓后 |
| O5 | 是否远期开源/多框架 | **非当前**；另 ADR |

---

**下一篇**：[07-p0-hard-problems.md](./07-p0-hard-problems.md) · [01-architecture.md](./01-architecture.md)
