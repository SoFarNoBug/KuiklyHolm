# Holm · Kuikly 局部 GPU 特效增强

> **身份：Kuikly 业务优先**（D12）。Holm 给 Kuikly 页面加高级视觉效果，**不是**第一期通用开源多框架引擎。  
> **锚点：同源 Shader/Pass**；统一 Runtime/RHI 是可选增强（Lite / Standard / Full）。  
> **先过** [`phases/Stage0.md`](./phases/Stage0.md)；特效少时 **Lite/路径 A 往往更优**。  
> 命名锁定：Holm / `HolmSurface` / `KRHolmSurface` / `holm_*`（不改）。

## 品牌与命名

| 用途 | 名称 |
| --- | --- |
| 产品 | **Holm** |
| 文档 | `D:\code_space\kuikly-holm\docs\product\` |
| API | `HolmSurface` |
| viewName | `KRHolmSurface` |
| ABI | `holm_*` |

## 文档地图

| 文档 | 用途 |
| --- | --- |
| [00-context-and-decisions.md](./00-context-and-decisions.md) | 身份、Lite/Standard、何时不选、Stage 0 |
| [07-p0-hard-problems.md](./07-p0-hard-problems.md) | Backdrop 等生死问题（含推荐默认） |
| [phases/Stage0.md](./phases/Stage0.md) | 选型门禁 |
| [01-architecture.md](./01-architecture.md) | 四层栈；Shader 锚点；Kuikly 桥接 |
| [02](./02-kuikly-integration.md)–[06](./06-implementation-roadmap.md) | 集成 / RHI / 场景 / 对拍 / 路线图 |
| [phases/M0.md](./phases/M0.md)… | 仅 Stage 0 选 Standard 后 |
| [references/](./references/) | 证据与术语 |

## Agent 协议

1. 读 `00`（含 D12）+ `07` + `Stage0`。  
2. 未选 Standard 不得全量 M0–M3 Runtime。  
3. 优先让业务用上特效（效果优先），禁止只交空引擎。  
4. 禁止系统 Liquid Glass / WebView+Three 冒充完成。  
5. 命名不改。  

## 架构一句话

```text
Kuikly 壳 + HolmSurface
  └── 同源 Shader/Pass
        ├── Lite：各端轻量执行（可无大 Runtime）
        └── Standard+：可选 Holm Runtime / RHI
```

## 状态

| 项 | 状态 |
| --- | --- |
| 身份 | **Kuikly 业务优先** |
| 命名 | Holm 锁定 |
| Stage 0 | 未做 |
| 主重量档 | 待 Stage 0（Lite vs Standard） |

## 修订

| 日期 | 说明 |
| --- | --- |
| 2026-11-11 | 初版；Holm 命名；Stage 0 / 路径 A；P0 专章 |
| 2026-11-12 | **Kuikly 优先**；Shader 锚点；Lite/Standard/Full；Backdrop 推荐默认；拒绝第一期多框架开源化；全文词汇对齐 |
