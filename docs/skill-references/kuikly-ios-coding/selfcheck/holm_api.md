# 自检清单 · holm_api（L3）

> 全文 SoT：`docs/skill-references/kuikly-android-coding/project/holm_api.md`。

| ID | 条款 | 自检要点 |
|---|---|---|
| H1 | 身份 D12 | 通用底座（shader-in/surface-out）；未绑单一业务；未做成多框架开源第一目标 |
| H2 | 命名 D10 | HolmSurface / KRHolmSurface / holm_* |
| H3 | 三包 + Provider D11/D13 | register + setDefault；无 classpath 静默默选；executor=`renderer-gl`/`renderer-wgpu` |
| H4 | 全量 Runtime 门禁 D0 | 无书面选型则无全量 M0–M3 产品化；不以官方特效席充完成态 |
| H5 | Shader 锚点 D2 | 同源 Pass；wgpu 可选非 GL 强制 |
| H6 | 样本 ≠ KPI | 未把五席/官方特效目录当产品完成；非空基建单已声明 |
| H7 | 禁冒充 D3 | 无系统玻璃 / WebView+Three 充数 |
| H8 | 局部岛 D1/D6 | 少岛；列表默认禁重实时岛 |
| H9 | Backdrop | 符合 07 H1；未承诺列表实时真折射 |
| H10 | 桥接 | expand-native-ui；无 ComposeView 子类扩展 |
| H11 | Caps/D9 | 弱端子集诚实 |
| H12 | 算力边界 D14 | 算力走 kmp-wgpu；未把原语塞进 Holm 公开面 |
| H13 | Content ADR 0016/0018 | 只挂 HolmContent；无公开 fragmentShader Attr；Ohos/H5 Model→MODEL_UNSUPPORTED；Android+iOS 真画；gltf 可选模块 |

命中条件：改 Holm 公开 API / Bridge / Shader 契约 / Provider / 后端边界。纯文档或未改契约 → 全表 `N/A` +「未改库契约」。
