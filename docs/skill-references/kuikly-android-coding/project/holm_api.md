doc_version: 20261112_1
layer: L3

# Holm API 准则（L3 · 本仓）

> **分层**：本仓库契约全文真源。编码会话红线摘要见 `.cursor/rules/kuikly-dev-*.mdc`。  
> **依据**：根目录 `CONTEXT.md`、`AGENTS.md`、[`docs/product/00-context-and-decisions.md`](../../../../product/00-context-and-decisions.md)、[`docs/product/07-p0-hard-problems.md`](../../../../product/07-p0-hard-problems.md)。

**级别**：核心规范（强制）。违反 = 该次编码不合格。

## 规则

1. **身份（D12）**：Holm 是 **Kuikly 业务优先** 的局部 GPU 特效增强；**禁止**第一期做成通用多框架开源引擎或并行多 bridge。
2. **命名（D10）**：对外固定 `HolmSurface` / viewName `KRHolmSurface` / ABI `holm_*`；**禁止**擅自改名或去掉 `KR` 前缀。
3. **Stage 0（D0）**：未书面选 Lite(A) / Standard(B) / 终止前，**禁止**全量 Standard Runtime（M0–M3）开工。
4. **锚点（D2）**：观感一致靠 **同源 Shader/Pass**；统一 RHI/wgpu Runtime 为 Standard **可选**，非 Lite 强制。
5. **效果优先**：禁止长期只交空引擎（清屏三角）而无 Kuikly 可用业务特效。
6. **禁冒充**：禁止用系统 Liquid Glass / `UIGlassEffect` / WebView+Three 冒充 Holm 完成态或四端一致方案。
7. **局部岛（D1/D6）**：不替换 Kuikly 主 UI 树；一页少岛；列表默认禁实时重特效；控件用 Kuikly **兄弟叠层**，不进 GPU 树。
8. **Backdrop（见 07 H1）**：真霜化推荐宿主提交局部纹理 + 半分辨率 + 静态冻结；**明确不支持**列表滚动实时真折射；禁止文档吹嘘「全场景实时完美采样」。
9. **桥接**：第一期唯一宿主桥接走 Kuikly `expand-native-ui`；Compose 用 `MakeKuiklyComposeNode`，**禁止** `ComposeView` 子类扩展。
10. **能力子集（D9）**：鸿蒙等弱端经 Caps 降级；ABI 形稳、能力可不装全；不得假装 Full 已四端齐。

## 判据速查

| 场景 | 结论 |
|---|---|
| 未过 Stage 0 就全量上 wgpu Runtime | **FAIL** |
| 改名去掉 KRHolmSurface / holm_* | **FAIL** |
| 系统玻璃或 WebView+Three 当验收完成 | **FAIL** |
| 整页自绘进岛 / 列表实时真折射当默认能力 | **FAIL** |
| Lite 交付无任何业务可调特效、只有基建 | **FAIL** → 效果优先 |
| 仅改文档 / 未改公开契约 | 相关条可 `N/A` |
