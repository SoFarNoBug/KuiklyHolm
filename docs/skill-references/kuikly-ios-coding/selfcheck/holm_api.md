# 自检清单 · holm_api（L3）

> 全文 SoT：`docs/skill-references/kuikly-android-coding/project/holm_api.md`。

| ID | 条款 | 自检要点 |
|---|---|---|
| H1 | 身份 D12 | 未做成多框架开源第一目标 |
| H2 | 命名 D10 | HolmSurface / KRHolmSurface / holm_* |
| H3 | Stage 0 | 未选 Standard 则无全量 Runtime |
| H4 | Shader 锚点 D2 | 同源 Pass；不强制 wgpu（Lite） |
| H5 | 效果优先 | 有可用特效或本单明确非特效基建且已声明 |
| H6 | 禁冒充 | 无系统玻璃 / WebView+Three 充数 |
| H7 | 局部岛 D6 | 少岛；列表默认禁实时重特效 |
| H8 | Backdrop | 符合 07 H1；未承诺列表实时真折射 |
| H9 | 桥接 | expand-native-ui；无 ComposeView 子类扩展 |
| H10 | Caps/D9 | 弱端子集诚实 |

命中条件：改 Holm 公开 API / Bridge / Scene / Shader 契约 / Runtime 边界。纯文档或未改契约 → 全表 `N/A` +「未改库契约」。
