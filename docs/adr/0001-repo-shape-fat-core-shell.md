# ADR 0001: 仓库形态 · 胖 Core 同构空壳

## Status

Accepted（2026-11-12）

## Context

Holm 需要可编译的 Kuikly 多端工程，以便后续挂 Lite 特效与（可选）Runtime。产品文档早期画过「旁路 `holm-runtime` + `kuikly-bridge` + `demo/`」树，容易让人以为必须先建独立 Runtime 目录。审讯（Q1–Q7）锁定：先落地多端同构壳并剥业务，独立 Runtime **后置**。

## Decision

| ID | 决定 |
| --- | --- |
| Q1 | 多端同构壳；独立 `holm-runtime` **后置** |
| Q2 | **胖 Core**：API / 声明 / Lite 挂载点将来进 `:KuiklyHolmCore`；本单 Core 可几乎空 |
| Q3 | 根工程 **`KuiklyHolmUI`**；库 **`:KuiklyHolmCore`**；Demo **`:holmDemo`**（不用 Engine 后缀） |
| Q4 | 四端壳：`androidApp` + `iosApp` + `h5App` + ohos 另册；**mini 先不 include** |
| Q5 | 从既有 Kuikly 多端壳剥业务落地（保留 Gradle / `buildSrc` / ohos 链路） |
| Q6 | 本单 **仅可编译空壳**；**不做** `HolmSurface` / 真特效 / wgpu |
| Q7 | 同单回写 `docs/product/06`、`CONTEXT`、`file-map` + 本 ADR |

包名：`com.jlj.kuiklybase.holm`（Core）、`com.jlj.kuiklyholmui`（App/Demo）。

## Consequences

- `docs/product/06` 仓库图改为胖 Core 同构壳；**禁止**再暗示「必须先建旁路 holm-runtime 目录」。
- Stage 0 / 特效交付仍按产品路线；空壳编译绿（至少 `:androidApp:compileDebugKotlin`）是工程门禁，不是 Stage 0 尖刺验收。
- 若日后拆出 `:holm-runtime`，须新 ADR；默认仍优先把 Lite 挂载点留在 Core。
