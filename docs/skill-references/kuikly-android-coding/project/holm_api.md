doc_version: 20261118_1
layer: L3

# Holm API 准则（L3 · 本仓）

> **分层**：本仓库契约全文真源。编码会话红线摘要见 `.cursor/rules/kuikly-dev-*.mdc`。  
> **依据**：根目录 `CONTEXT.md`、`AGENTS.md`、[`docs/product/00-context-and-decisions.md`](../../../../product/00-context-and-decisions.md)、[`docs/product/07-p0-hard-problems.md`](../../../../product/07-p0-hard-problems.md)、ADR 0007 / 0008 / 0013。  
> **对齐说明（2026-11-18）**：推翻旧「特效增强 / 五席交付中心 / 效果优先于基建」身份句；与 ADR 0007 通用底座 + ADR 0008 三包 Provider 一致。

**级别**：核心规范（强制）。违反 = 该次编码不合格。

## 规则

1. **身份（D12 · ADR 0007）**：Holm 是 **通用渲染底座**（shader-in / surface-out）——宿主定岛、传入 Content，库出呈现面；**禁止**写成「专为粒子 / 专为玻璃 / 专为某 App」；**禁止**第一期做成通用多框架开源引擎或并行多 bridge。
2. **命名（D10）**：对外固定 `HolmSurface` / viewName `KRHolmSurface` / ABI `holm_*`；**禁止**擅自改名或去掉 `KR` 前缀。
3. **三包 + Provider（D11 / D13 · ADR 0008）**：目标包 `holm-core` / `holm-renderer-gl` / `holm-renderer-wgpu`；宿主须 **register Provider 并 setDefault**；仅 core 或未设 → **明确失败**，禁止 classpath 静默默选。umbrella `KuiklyHolmCore` 可自动注册为例外，Demo/宿主仍宜显式演示契约。对外 Attr：`executor=renderer-gl` / `renderer-wgpu`（废 `lite` / `runtime` 用词）。
4. **全量 Runtime 门禁（D0）**：全量 Standard / M0–M3 级 Runtime 加深须有**书面后端选型**；不得把「再堆官方特效席」当成底座完成态。Stage 0「选 Lite 做玻璃交付」仅历史尖刺，**不是**现行身份锚。
5. **锚点（D2）**：观感一致靠 **同源 Shader/Pass 契约**；`renderer-wgpu` 可选，非 `renderer-gl` 强制。
6. **样本 ≠ 产品 KPI**：Demo 隧道等为通面/回归夹具；官方五席叙事废弃且源码已删；**禁止**把「官方特效目录齐全」当产品完成标准，也**禁止**把空引擎清屏三角当长期唯一交付。
7. **禁冒充（D3）**：禁止用系统 Liquid Glass / `UIGlassEffect` / WebView+Three 冒充 Holm 完成态或四端一致方案。
8. **局部岛（D1 / D6）**：不替换 Kuikly 主 UI 树；一页少岛；列表默认禁重实时岛；控件用 Kuikly **兄弟叠层**，不进 GPU 树。
9. **Backdrop（见 07 H1）**：真霜化推荐宿主提交局部纹理 + 半分辨率 + 静态冻结；**明确不支持**列表滚动实时真折射；禁止文档吹嘘「全场景实时完美采样」。
10. **桥接**：第一期唯一宿主桥接走 Kuikly `expand-native-ui`；Compose 用 `MakeKuiklyComposeNode`，**禁止** `ComposeView` 子类扩展。
11. **能力子集（D9）**：鸿蒙等弱端经 Caps 降级；ABI 形稳、能力可不装全；不得假装 Full / 简单 3D 已四端齐。
12. **算力边界（D14 · ADR 0013）**：通用 GPU 算力 / Compute / 无窗口加速走 **`kmp-wgpu`**；**禁止**把 Holm 扩成算力 SDK 或把原语 API 塞进 `holm-renderer-wgpu`。
13. **Content 契约（ADR 0016）**：`HolmSurface` **只挂** `HolmContent`（`HolmShaderContent` / `HolmModelContent`）；Attr 过 NVI 用 `contentId` + Registry；**禁止**对外再暴露 `fragmentShader` / `vertexShader` 业务 Attr。`HolmModelContent` 真画本波 **仅 Android**；其它端须 `MODEL_UNSUPPORTED` + 清屏。glTF 解析在可选 `:holm-gltf`，**禁止**塞进 kmp-wgpu。

## 判据速查

| 场景 | 结论 |
|---|---|
| 写成「特效增强产品 / 五席完成态 / 专为某业务」 | **FAIL** |
| 未 register Provider / 仅靠 classpath 静默出画 | **FAIL** |
| 改名去掉 KRHolmSurface / holm_* | **FAIL** |
| 无书面选型就全量上 M0–M3 Runtime 产品化 | **FAIL** |
| 系统玻璃或 WebView+Three 当验收完成 | **FAIL** |
| 整页自绘进岛 / 列表实时真折射当默认能力 | **FAIL** |
| 把通用算力 API 塞进 Holm 公开面 | **FAIL** → 走 kmp-wgpu |
| 长期只交空清屏、无任何可喂 shader 通面且未声明基建单 | **FAIL** |
| 业务公开 API 仍并列 `fragmentShader` Attr | **FAIL** → Content（ADR 0016） |
| 非 Android 假装 Model 已通 / 静默空画不报错 | **FAIL** → `MODEL_UNSUPPORTED` |
| 仅改文档 / 未改公开契约 | 相关条可 `N/A` |
