# ADR 0015: kmp-wgpu 独立仓与独立发布

- Status: Accepted
- Date: 2026-11-17

## Context

ADR 0013 锁定 `kmp-wgpu` 为与 Holm 无关的原语层，O7 将独立仓时机留作开放项。仓内分层（v1 Present + v1.1 Compute）与 ADR 0014 命名消灭已完成；其它项目（如算力消费方）不应被迫依赖 Holm。审讯（2026-11-17）锁定：通用全屏 Present 留在 `kmp-wgpu-surface`；liquid/scene/ClearTri 删除不迁；Holm 不另养 Rust；路径 `~/Studio/kmp/kmp-wgpu`；Maven + `includeBuild` 开关；四端 native 随依赖分发。

## Decision

1. **独立仓**：`/Users/aimo/Studio/kmp/kmp-wgpu`（模块 `kmp-wgpu` / `kmp-wgpu-surface` / `kmp-wgpu-native` + 无 Holm sample）。
2. **产品面**：core = 原语 + headless Compute；surface = 窗口 + **FullscreenPass**（`setFragmentShader` / tick / playing / resize / error）；无片元 = 只清屏。
3. **删除遗留**：`setLiquid` / `liquid_*` / `setScene` / ClearTri / `holm_fullscreen` 命名；故意 ABI break，无 compat shim。
4. **坐标（首发 0.1.0）**：Maven `io.github.sofarnobug:kmp-wgpu` / `:kmp-wgpu-surface`；Pod `KmpWgpuNative`；ohpm `@jlj/kmp-wgpu-native`；npm `@sofarnobug/kmp-wgpu-native`（npm org 与 ohpm scope 不同）。
5. **Holm 消费**：默认 Maven；`local.properties` / `-Pkmp.wgpu.local=true` → `includeBuild` 覆盖坐标。
   - 主 settings → `../kmp-wgpu`
   - `settings.ohos` → `../kmp-wgpu/ohosInclude`（KBA + `build.ohos`，补 `ohosArm64`；主 settings 无该变体）
6. **执行序**：先在 Holm 仓内清边界并四端绿，再剪仓发版（关闭 **O7**）。

## Consequences

- Holm 仅 Kotlin 薄适配叠在已发布的 kmp-wgpu* 上；运行时一份 `kmp_wgpu_native`。
- 旧 `set_custom_fs` / `set_scene` / `set_liquid` 符号失效；四端须重编 native。
- `docs/product/00` O7 → 关闭，指向本文。

## Relates

- ADR 0013（原语层）
- ADR 0014（命名消灭）
- ADR 0008（Holm 三包叠其上）
