# 审查发现面（review_heuristics.md）

> **SoT**：本文件。Skill **不复述**卡片全文。  
> **决策**：[ADR 0037](../../adr/0037-review-heuristics-and-device-checklist.md)  
> **定级**：写结论前仍须 Read [`severity.md`](severity.md)（[ADR 0024](../../adr/0024-review-closed-severity.md)）。  
> **码农反例**：链到 [`../kuikly-android-coding/misuse.md`](../kuikly-android-coding/misuse.md) 错误号，勿双写正文。

## 读码顺序

1. **范围**：对齐 tasks / Plan（强制检查表第 1 条）
2. **按 diff 开卡**：用下方路由表打开 1～3 张卡（深度发现）
3. **规范对照**：本仓已挂 norms（`lang_kotlin` / `platform_*` / L3）+ `misuse`（强制检查表第 3 条）
4. **meta 最后**：changelog、写权限、规范候选、真机缺口/可执行档

禁止颠倒成「先填报告格式、再扫一眼 diff」。

## diff 路由

| diff 信号（路径 / 符号） | 打开卡 |
|--------------------------|--------|
| 扣费 / 门闸 / 双端尺寸 / 两路数据源 / 会员态 | 卡 A 契约双源 |
| `OnClick` / `setOnClickListener` / `observe` / 回调接线 / flag 宣称可达 | 卡 B 可达性 |
| `Printer` / `CURRENT_MODEL` / `isSeal` / `View.post` 读尺寸 / insets / `fitsSystemWindows` | 卡 C 机型·生命周期 |
| 长度 / 里程 / pad / `+=` 跨模块常量 | 卡 D 量纲 |
| `WebView` / `addJavascriptInterface` / deeplink / Intent extra / 日志打 token | 卡 E 安全 |
| `dto` / `bean` / Retrofit 模型包路径 | 本仓无网络模型 keep 门禁；勿据此开卡 |
| 仅 `strings.xml` / 文案 Key | `P1-i18n` 扫一眼即可，可跳过 A～D 深潜 |
| `executor` / DualTrack / `KRHolmSurface` / Worker / Offscreen / `holm_xc` / `catch_unwind` / `renderScale` / Shadertoy wrap | 卡 F DualTrack/Holm 桥 |
| `ohosApp/**/types/lib*/index.d.ts` / NAPI `ExportFn` | 卡 F（dts 同步条） |

无路由命中时仍做卡 B 轻扫（主路径是否接线）。

---

## 卡 A · 契约双源

**何时打开**：同一业务存在两路数据源、门闸、扣费、尺寸或会员态。

**必问**：

1. 两路来源是否同一真值？不一致时以哪路为准是否写进代码？
2. 漏扣费 / 漏门闸是否已论证可达？
3. 文档/注释口径与实现是否双真源？（仅文档 → 勿升 P0）

**勿报**：纯文案不一致且不影响行为。

**默认定级**：已论证行为错 → `P0-api-contract`；仅口径漂移 → `P1-doc-drift`。

**样例**：

- **正**：编辑页取价一路、打印出口又一路且漏门闸 → `P0-api-contract`
- **反**：KDoc 写「以 SP 为准」代码已是 SP，注释旧句未删 → `P1-doc-drift`，不是 P0

---

## 卡 B · 可达性

**何时打开**：改了点击、观察者、子类覆写、任务宣称「已覆盖」。

**必问**：

1. 用户主路径点击后，回调/导航是否接到有效实现？
2. 子类是否吞掉父类关键路径且无替代？
3. Plan/tasks 宣称可达，代码是否真有入口？

**勿报**：次要入口体验差且主路径完好（可 P2）。

**默认定级**：核心功能整条不生效 → `P0-silent-fail`；宣称覆盖但不可达 → `P0-plan-hard-drift`；入口遗漏未到硬漂移 → `P1-plan-soft-drift`。

**样例**：

- **正**：`setOnClickListener` 空实现或早退导致功能永不进 → `P0-silent-fail`
- **反**：「可能忘记 refresh」无可达论证 → 不得 `P0-crash` / `P0-silent-fail`；写 `P1-unlisted` 或建议自测

---

## 卡 C · 机型·生命周期

**何时打开**：机型判据、测量时机、insets、切机型后 UI 显隐。

**必问**（细节见 misuse，勿复述）：

1. 静态留存 UI 是否误用 `getPrinter()` 派生判据？（→ misuse **错误 3**）
2. `onCreate` 同步 `initView` 里 `View.post` 读兄弟尺寸？（→ misuse **错误 2**）
3. 注册 insets 后是否仍指望 `fitsSystemWindows` 自动生效？（→ misuse **错误 5**）
4. 代码审不了的定标/视觉 → 缺口档 `P1-device-verify`，勿升 P0

**勿报**：纯 UE 微调且无功能失效证据却打 P0。

**默认定级**：已论证静默失效 → `P0-silent-fail`；竞态未证必错 → `P1-concurrency`；视觉定标 → `P1-layout-visual` / `P1-device-verify`（缺口）。

**样例**：

- **正**：切机型后入口永不恢复且可归因判据选边 → `P0-silent-fail`（对齐 misuse 错误 3）
- **反**：间距「看起来差一点」无 UE/截图 → `P1-device-verify` 缺口或 `P1-layout-visual`，不是 P0

---

## 卡 D · 量纲

**何时打开**：跨模块长度/补边/里程相加，或点数与毫米混用。

**必问**（→ misuse **错误 6**）：

1. `+=` 两侧单位是否相同？方法名/KDoc 是否标明单位？
2. 换算是否只在一处发生？
3. 是否静默错账（编译运行都正常）？

**勿报**：同单位常量重命名且无数值变化。

**默认定级**：已论证静默错账/污染统计 → `P0-silent-fail`；可疑未证 → `P1-unlisted`（待补 `P1-unit-mismatch`）+ 待补桶。

**样例**：

- **正**：点数当毫米累加里程 → `P0-silent-fail`
- **反**：仅怀疑单位、无对照基准 → `P1-unlisted`，不要硬塞 P0

---

## 卡 E · 安全

**何时打开**：WebView、JS 桥、deeplink、Intent 扩展、日志/存储敏感信息。

**必问**：

1. WebView 是否暴露不当 JS 接口或加载不可信 URL？
2. deeplink / Intent extra 是否未校验即驱动敏感操作？
3. 日志或本地存储是否落 token/密码明文？

**勿报**：纯内部 debug 开关且无敏感数据（可 `P1-debug-guard`）。

**默认定级**：**本轮一律** `P1-unlisted` + 待补桶建议 `P1-security` / `P0-security`（**禁止**自创 P0，[ADR 0037](../../adr/0037-review-heuristics-and-device-checklist.md)）。

**样例**：

- **正**：日志打印 refresh token → `P1-unlisted`，待补桶写建议 `P1-security`
- **反**：把安全问题塞进 `P0-silent-fail` 却无「功能不生效」论证 → 定级漂移，禁止

---

## 卡 F · DualTrack / Holm 桥

**何时打开**：改 `executor` / Lite↔Runtime、Worker/Offscreen、`holm_xc` NAPI、Shadertoy 双源 wrap、`renderScale`/分辨率策略、跨 FFI Session。

**必问**：

1. **对称 teardown**：切轨 / 卸岛是否成对拆 Session + 泵（含 Worker/Offscreen）？禁止只清半套标志。
2. **Worker 失败契约**：Offscreen/Worker 异步 init 失败是否与主线程同契约（`BLOCKED` + ready / 可回退或看门狗）？
3. **方言与 FFI**：Compose 先写 `fragmentShader` 再写 `executor` 时，仍持 Runtime 是否拒绝非本轨方言？跨 FFI native 入口是否 `catch_unwind`（或等价）防 panic abort？
4. **性能对比口径**：报告/changelog/对比是否标注端与分辨率策略（含 iOS Lite 点像素基线、`renderScale`、H5 CSS 基线）？禁止用模拟器 GLES raymarch fps 当真机结论。
5. **Shadertoy 双源**：C++ 内嵌 wrap/vert 与 Kotlin SoT 是否同单同步，且 changelog 留同步义务？
6. **鸿蒙 NAPI dts**：新增/改名 `ExportFn` 是否同步 `entry/src/main/cpp/types/lib*/index.d.ts`？（漏同步 → 编不过，对齐 `P0-build-break`）
7. **H5 `js` 赋值**：禁止 `js("ident").asDynamic()` 赋浏览器全局（会编成非法 `ident.asDynamic()`）；须 IIFE/`js("(function(fn){…})")(fn)` 等安全形态。

**勿报**：仅改官方席文案、未碰双轨/桥/NAPI。

**默认定级**：对称 teardown / Worker 契约缺失导致整条轨不可用 → 论证后 `P0-silent-fail`；dts 漏同步编不过 → `P0-build-break`；口径未标分辨率 → `P1-doc-drift` 或对比场景 `P1-device-verify`；双源不同步 → `P1-doc-drift` / `P2`（视是否已留同步义务）。

**样例**：

- **正**：runtime→lite 不 `destroyPump` 致 Lite 永不画 → `P0-silent-fail`
- **正**：NAPI 已 `setRenderScale` 但 `.d.ts` 无声明 → ArkTS 失败 → `P0-build-break`
- **反**：仅怀疑 Worker「可能慢」无契约缺口 → 不得硬塞 P0

（晋升自审查 `_backlog` 2026-11-12～16 已达标项；未达标项仍留池。）

---

## 与真机两档

代码审不闭环时挂 `P1-device-verify`：**同轮必须**产出可执行档 [`docs/reviews/device-check-list/{task-id}.md`](../../reviews/device-check-list/)（本桶一挂、或 Plan 含真机验证/真机自测 ⇒ 默认产出；仅用户明确说「不用表」才省）⇒ 报告「真机」节只留**链接** + 代码审不了什么（缺口要点），**不得**只写缺口档就收尾、也**不得**无文件却宣称已可执行交接。
