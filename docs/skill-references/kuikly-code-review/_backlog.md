# 待沉淀池（审查 skill 自有）

> 候选只进**本文件**。不得自动写入码农的 `_backlog`（ADR 0036）。
> `project.yaml` 的 `peer_pools` 仅提示可人工抄录的路径。
> 候选只保留与本仓无关业务细节的通用判据；晋升或作废走人工评审。

**条目格式（候选）**：`- [ ] {规则一句话} ｜{日期} ｜自评：{已达标 / 未达标（缺什么）}`

## 候选

- [ ] Kotlin/JS 禁止 `js("ident").asDynamic()` 赋浏览器全局（会编成 `ident.asDynamic()` 运行时炸）；改用 `js("(function(fn){ self.x = fn; })")(fn)` 或等价 IIFE ｜2026-11-12｜自评：已达标（h5-worker-gl-pump 已规避）
- [ ] 原生桥 `onError`/`onReady`：若 Attr 侧可能在回调注册前 fire，须像 onReady 一样支持「已发生则重放」，避免 Compose Attr→Event 顺序丢错 ｜2026-11-12｜自评：未达标（dualtrack P1-concurrency）

## 已晋升


（暂无）
