# Stage 0 · 选型尖刺（Kuikly 优先）

> 结论三选一：**Lite(A)** / **Standard(B)** / **终止**。  
> 未完成禁止 Standard 全量 M0–M3。  
> 阅读：`../00` §4、`../07`。

## DoD

1. Lite 尖刺：至少一端可跑代表特效；**本单端范围 = Android + H5**（相对原「目标 Android+iOS」有意偏离：iOS 记缺口，不挡关单）；**尽量**同一份 shader 源。  
2. Standard 尖刺（可选但建议）：**本单不做**（S0.3 跳过）。  
3. 对比表 + H1–H4 认知。  
4. 书面结论回写 `00` O0。  
5. 若选 Lite：下一步是 **交业务可用特效**，不是建大 Runtime。

## 任务

- [x] S0.1 冻结 12 个月特效数量与是否进 List  
  - **冻结**：≤5 个稳定特效；**不进 List 实时**（仅英雄区 / 固定岛）  
  - **尖刺端**：Android + H5；iOS / 鸿蒙本单不跑  
- [x] S0.2 Lite：Kuikly API 壳 + Android/H5 霜化尖刺；记人日与包体  
  - `HolmSurface` / `KRHolmSurface` 薄壳；同源 GLSL ES 霜化（Android GLES2 + H5 WebGL1）  
  - 编译：`:androidApp:compileDebugKotlin`、`:KuiklyHolmCore:compileKotlinJs` / `:holmDemo:compileKotlinJs` / `:h5App:compileKotlinJs` 通过  
- [x] S0.3（建议）Standard：清屏/三角 + 包体 — **本单跳过**  
- [x] S0.4 填写 07 模板（H1 写明 A/B；滚动真采默认否）  
- [x] S0.5 评审结论 → **Lite(A)**（见下）

## 对比表

| 项 | Lite(A) | Standard(B) |
| --- | --- | --- |
| 人日 | ~1～2（薄壳 + 双端霜化尖刺；本机粗估） | （本单未做尖刺） |
| 包体 | 编译绿；未测正式 APK/bundle 增量（尖刺期备注） | — |
| 第 2 特效边际 | 低：同 `KRHolmSurface` + 换 scene/shader | 高：需 Runtime/Pass |
| 是否需要自定义 Pass | 否（官方/固定霜化即可） | 是（若增殖） |
| 与 Kuikly 集成成本 | 低：expand-native-ui + Compose 包装 | 高：另加 Runtime 桥 |
| iOS | **未跑**（缺口） | — |

## H1–H4 认知（Stage 0）

| ID | 认知 |
| --- | --- |
| H1 | **策略 A**（程序化/静态输入）；**滚动不真采**；策略 B 为选 Lite 后增强 |
| H2 | Holm 仅英雄区/固定岛；不承诺 List 内实时 + 真 backdrop |
| H3 | 尖刺为纯展示；手势默认少消费；按钮用 Kuikly 兄弟叠层 |
| H4 | detach 释放 GL；**默认禁止** List 内实时 Holm |

## 结论

```text
日期：2026-11-12
选择：Lite(A)
理由：≤5 特效且不进 List；Android+H5 经 Kuikly 壳可跑同源霜化；无需统一 Runtime；S0.3 未做亦不阻断。iOS 记缺口，后续补跑。
回写 O0：是
```

---

选 Lite → 交付特效，不进入 [M0.md](./M0.md) 全量 Runtime。  
选 Standard → [M0.md](./M0.md)（仍应尽快接上官方特效）。

## Lite 特效扩容（关单后）

- [x] 第 2 scene=`glow`：柔光光晕；Android/H5 `scene` 热换；Demo 可切换（仍策略 A）
- [x] 第 3 scene=`liquid_approx`：单 Pass UV 扭曲+边缘色散/高光；Attr `ior`/`warp`；Demo 步进（仍策略 A）
