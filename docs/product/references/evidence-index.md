# 证据索引（结论 ↔ 来源）· Holm

> 质疑结论时先查本表；新增结论须增行。  
> 本地 Kuikly：`D:\code_space\Kuikly\...`  
> 文档包：`D:\code_space\kuikly-holm\docs\product\`  
> 调研基准：2026-03

---

## A. Kuikly 渲染与扩展

| ID | 结论 | 证据 |
| --- | --- | --- |
| A1 | 主路径原生控件，非整页 Skia | Kuikly 博客 / `docs/Blog/architecture/kuikly-rendering.md` |
| A2 | Compose 高阶 + Kuikly 原生渲染 | `docs/Compose/concepts.md`；`overview.md` |
| A3 | 自定义原生 View 可四端扩展 | `docs/DevGuide/expand-native-ui.md` |
| A4 | Compose 包装用 `MakeKuiklyComposeNode`；禁 ComposeView 子类 factory | `docs/Compose/extend-kuikly-dsl-ui.md` |
| A5 | 扩展组件范例 TabBar | `ios26-liquid-glass.md` TabbarIOS；`iOSTabbar.kt` |

→ 决策 D1（Holm 嵌入岛）

## B. 系统 Liquid Glass（反例）

| ID | 结论 | 证据 |
| --- | --- | --- |
| B1 | Kuikly 已有 iOS/macOS 26+ 玻璃 API | `ios26-liquid-glass.md`；`liquid-glass.md`；Compose `liquidGlass` |
| B2 | `isLiquidGlassSupported()` 非 Apple 26+ 为 false | `platform-utils.md` |
| B3 | 原生 UIGlassEffect / NSGlassEffectView | `KRView+LiquidGlass.m` |
| B4 | 内容层不宜用玻璃；Scroller 不支持 | `ios26-liquid-glass.md` |
| B5 | 自绘仿系统玻璃成本高 | 液态玻璃博客 |
| B6 | Apple WWDC25 UIKit/SwiftUI Liquid Glass | Sessions 284 / 323 |

→ 决策 D3

## C. Kuikly Blur（降级对照）

| ID | 结论 | 证据 |
| --- | --- | --- |
| C1 | Blur 半径上限约 12.5 | `docs/API/components/blur.md` |
| C2 | Android RenderEffect / RenderScript | `core-render-android/.../blur/` |
| C3 | iOS UIVisualEffectView | `KRBlurView` |
| C4 | 鸿蒙 backdropBlur | `KRBlurView.ets` |

## D. 业界自研玻璃 / 毛玻璃

| ID | 结论 | 证据 |
| --- | --- | --- |
| D-a | Haze：Compose backdrop blur | chrisbanes.github.io/haze |
| D-b | PrismalAGSL：三档 + AGSL 折射 | github.com/styropyr0/PrismalAGSL |
| D-c | AndroidLiquidGlass | github.com/Kyant0/AndroidLiquidGlass |
| D-d | Imla：实验非生产 | github.com/desugar-64/imla |
| D-e | Flutter liquid_glass_widgets | pub.dev/packages/liquid_glass_widgets |
| D-f | 鸿蒙 backdropBlur / backgroundEffect | 华为官方模糊文档 |
| D-g | Web backdrop-filter；SVG 折射 Chromium 偏多 | 多篇 liquid glass CSS 文 |

→ D4/D5/分档策略

## E. RHI / GPU

| ID | 结论 | 证据 |
| --- | --- | --- |
| E1 | wgpu 多后端含 wasm | docs.rs/wgpu |
| E2 | 可嵌入 iOS/Android App | github.com/jinleili/wgpu-in-app |
| E3 | OpenHarmony wgpu 实验 | wgpu-in-app PR #17 |
| E4 | 鸿蒙 XComponent 自定义绘制 | 华为 XComponent 指南 |
| E5 | Impeller / Dawn 包体权衡 | Flutter Impeller FAQ |
| E6 | 整页 Skia 与 Kuikly 路线冲突 | Kuikly Compose 博客 |
| E7 | RN WebGPU 统一后端方向 | Shopify RN graphics 文 |

→ D2 / D8 / D9

## F. Three / WebGL（非终态）

| ID | 结论 | 证据 |
| --- | --- | --- |
| F1 | Three 主场浏览器 | three.js 定位 |
| F2 | 原生跑 Three 多靠 WebView | 工业常见做法 |
| F3 | WebGL 非逐像素 | Web 液体玻璃兼容表 |
| F4 | WebView 性能/层级劣于原生 Surface | 与混排代价论述同构 |

## G. GearUI

| ID | 结论 | 证据 |
| --- | --- | --- |
| G1 | KMP 组件库，依赖 Kuikly | github.com/gearui/gearui-kit |
| G2 | Components → Kuikly Compose | ARCHITECTURE.md |
| G3 | Blur 默认关 | README Limited support |

## H. 性能

| ID | 结论 | 证据 |
| --- | --- | --- |
| H1 | 实时全屏模糊贵 | RenderEffect 文；鸿蒙高效模糊 |
| H2 | 多 backdrop 应合并 | Flutter BackdropGroup |
| H3 | Kuikly Blur 建议 target refs | blur.md |

→ D6

## I. 本包状态

| ID | 事实 | 证据 |
| --- | --- | --- |
| I1 | 产品名 **Holm**；文档目录 `kuikly-holm` | README；D10 |
| I2 | 旧名 `kuikly-rhi-draw-island` 已废止 | 修订记录 |
| I3 | Stage 0；Lite 合法；P0 专章 | `00`；`07`；`Stage0.md` |
| I4 | 评审：ROI/硬问题——已吸收 | 会话 |
| I5 | 开源向优化案：Shader 锚点、可插拔、效果优先——**在 Kuikly 优先前提下吸收**；拒第一期多框架开源化 | 会话优化案；D12 |
| I6 | Backdrop 宿主纹理+半分辨+冻结+不支持列表实时 | `07` H1 |

## J. 决策 ← 证据

| 决策 | 证据 ID |
| --- | --- |
| D0 Stage 0 | I3 I4 |
| D1 Holm 嵌入（条件） | A1–A3 + Stage 0 |
| D2 Shader 锚点；Runtime 可选 | E1 E2 + I5 |
| D3 禁系统玻璃四端 | B1–B3 B5 |
| D4 观感一致、不承诺系统气质 | D-g F3 I4 |
| D5 单源（子集除外） | D-b D-e E1 |
| D6 少岛=克制 | H1–H3 I4 |
| D8 Web 降级 | E1 D-g |
| D9 鸿蒙子集 | E3 E4 I4 |
| D10 命名 Holm | 产品决定（不动） |
| D11 Lite/路径 A 合法默认 | I3 I4 |
| D12 Kuikly 业务优先 | I5 |

## K. 待补缺口

| 缺口 | 何时 |
| --- | --- |
| 包体实测 | M0 |
| 鸿蒙 wgpu/GLES 结论 | M0 |
| SSIM 阈值校准 | M1–M3 |
| 中端 FPS | M1/M3 |
| Filament 四端（若考虑） | M2 O3 |
