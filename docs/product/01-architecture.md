# 01 · 架构详解（Holm · 通用渲染底座）

> **身份以 [`00`](./00-context-and-decisions.md) + [ADR 0007](../adr/0007-holm-as-generic-render-substrate.md) / [ADR 0008](../adr/0008-holm-core-gl-wgpu-packages.md) 为准。**  
> 决策：`00`（含 D13 三包）。身份：**通用渲染底座**；目标 **holm-core / holm-gl / holm-wgpu**。  
> 硬问题：`07`。下文若仍出现「特效产品 / Lite 五席 KPI」措辞，以 00/0007/0008 覆盖。

## 1. 一句话

**Kuikly（或宿主）画壳；Holm 提供局部 GPU 岛，按传入 shader 出帧。**  
核心锚点是 **shader 契约 / Pass 语义**；统一 Runtime/RHI 是 **可插拔后端档**，不是 Lite 的强制项。

## 2. 四层可插拔栈（对内）

```text
┌─────────────────────────────────────────────┐
│ L4 业务组件（Kuikly DSL / Compose）           │  HolmSurface、开箱特效参数
├─────────────────────────────────────────────┤
│ L3 场景与着色（Scene / Pass / WGSL 源）       │  ★ 单一真相源（所有重量档共享）
├─────────────────────────────────────────────┤
│ L2 执行后端（可插拔）                         │  Lite: 各端轻量后端 / 预编译产物
│                                             │  Standard+: RHI（如 wgpu）可选
├─────────────────────────────────────────────┤
│ L1 Kuikly 桥接                              │  KRHolmSurface、生命周期、事件
└─────────────────────────────────────────────┘
```

### 与「开源通用库」方案的差异（刻意）

| 项 | 本项目 |
| --- | --- |
| 第一宿主 | **仅 Kuikly**（D12） |
| 桥接包 | `kuikly-bridge` / 工程内模块，不优先拆 `holm-uikit` 等 |
| 对外 API | `HolmSurface` + `KRHolmSurface`（D10，不改） |
| 比喻「河心岛」 | 仅品牌/ABOUT；技术文档少用 |

## 3. 三档重量如何落层

| 档 | L3 Shader | L2 | L1 | 业务感知 |
| --- | --- | --- | --- | --- |
| **Lite** | 有；构建期编到各端 | **无**大 Runtime；原生 GLES/Metal/WebGL 等执行器 | KRHolmSurface 或等价薄壳 | 调特效组件即可 |
| **Standard** | 有 | Holm Runtime + RHI | 同左 | 可自定义 Pass |
| **Full** | 有 | Standard + glTF/简单 3D | 同左 | 英雄 3D |

Stage 0 选 A ≈ 先落地 Lite；选 B ≈ 建 Standard（仍建议先有 1～2 个特效能用）。

## 4. Shader 锚点（替代「强制 wgpu」）

1. 手写源：**WGSL**（或团队选定的单一源语言）。  
2. 构建期：用 naga / spirv-cross 等离线生成 MSL / SPIR-V / GLSL 等（工具链可 Stage 0 后渐进，但目标如此）。  
3. 运行时：只加载产物并跑 Pass；**允许** Lite 与 Standard 用不同 L2，只要 Pass 语义一致。  
4. 禁止长期「Android 一份 AGSL 手写算法、iOS 另一份」且无同源源码。

证据：业界一致性来自材质/shader，而非必须同一图形 API（见 evidence-index D/E）。

## 5. Kuikly 桥接（L1）

- 唯一第一期桥接：Kuikly `expand-native-ui`。  
- viewName：`KRHolmSurface`。  
- 职责：Surface、resize、props、destroy、可选手势转发；**不写特效算法**。  
- Compose：`MakeKuiklyComposeNode` 包装（禁 ComposeView 子类）。

远期若要其它宿主：另 ADR，**不阻塞** Kuikly 业务。

## 6. 一帧流（Standard；Lite 类同更简）

```text
Kuikly 布局 → Bridge resize/props
→（可选）宿主提交 backdrop 纹理（07 H1）
→ 执行 PassGraph（按 Quality 剪枝）
→ present 到 Holm Surface
→ 上层 Kuikly 兄弟控件正常点按
```

线程：encode 不堵 Kuikly 布局线程。

## 7. 性能机制（写入实现，不靠口头）

| 机制 | 说明 |
| --- | --- |
| 静态冻结 | `playing=false` 或无动画时只绘一帧并缓存，后续当普通层 |
| 列表 | 默认禁止实时 Holm；若只要外观，用 **预烘焙静态结果**（可同 API `playing=false` + 文档约定），不承诺 List 内实时 |
| 多实例 | 同页 >1 个实时实例：Debug **警告**；可选严格模式拒绝（配置项，默认警告以免静默「坏了」） |
| 半分辨率 backdrop | H1 推荐默认 |

D6 的「少岛」与上述机制一致：克制是产品边界，机制负责兜底。

## 8. 降级（对业务尽量改代码）

```text
无 GPU / 初始化失败 → onError + 业务占位或 Kuikly Blur
WebGPU 不可用 → WebGL2 → 再降须登记
鸿蒙弱 → Caps 减 Pass（D9）
```

## 9. 模块建议（Kuikly 仓内或旁路仓）

```text
kuikly-holm/           # 或业务 monorepo 子模块
  shaders/             # 同源 WGSL ★
  holm-lite-backends/  # 可选：各端轻量执行器
  holm-runtime/        # Standard+ 才需要
  kuikly-bridge/       # KRHolmSurface + KMP API
  demo/                # Kuikly Demo 页
```

## 10. 架构验收

- [ ] 说得出「锚点是 Shader 不是 wgpu」  
- [ ] Lite/Standard/Full 边界清晰  
- [ ] 第一桥接只有 Kuikly  
- [ ] 防膨胀与 H1 默认策略写进实现计划  
- [ ] 业务能先用到特效，而不是先看到空 Runtime  

---

**上一篇**：[00-context-and-decisions.md](./00-context-and-decisions.md)  
**下一篇**：[02-kuikly-integration.md](./02-kuikly-integration.md)
