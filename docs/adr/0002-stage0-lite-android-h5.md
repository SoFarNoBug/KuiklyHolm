# ADR 0002: Stage 0 选 Lite(A) · Android+H5 霜化尖刺

## Status

Accepted（2026-11-12）

## Context

空壳（ADR 0001）落地后须关闭 Stage 0（`00` O0）。审讯锁定：≤5 特效、不进 List、本单跑 **Android + H5**（偏离原「目标 Android+iOS」）、霜化策略 A、真名薄壳 `HolmSurface`/`KRHolmSurface`、不做 S0.3 Standard 尖刺。Kotlin/JS 标准库无 `WebGL2RenderingContext`，H5 尖刺改用 **WebGL1 + 同源 GLES2 shader**（与 Android 同源源码）。

## Decision

1. **O0 = Lite(A)**：12 个月 ≤5 稳定特效、不进 List 实时；效果优先于 Runtime。  
2. **尖刺端**：Android GLES2 + H5 WebGL1；iOS / 鸿蒙本单记缺口。  
3. **API**：最小 Attr（`scene`/`playing`）+ `onReady`/`onError`；声明与执行器在 `:KuiklyHolmCore`；App 只注册。  
4. **H1**：第一期策略 A；滚动不真采；策略 B 后置。  
5. **不做**：S0.3、wgpu、独立 `:holm-runtime`、真折射。

## Consequences

- 下一步是业务可用特效扩容，**不是**全量 M0–M3 Runtime。  
- iOS 补跑与策略 B 宿主纹理为后续里程碑。  
- H5 若日后升 WebGL2，可启用 `FrostShaderSource.webgl2*`，须保持同源数学。  
- 尖刺观感已升级为**岛内模拟毛玻璃**（程序化底图 + 邻域模糊 + 白雾）；**H1 仍为策略 A**，不采真页面像素。  
- 已验证第 2 scene=`glow`（柔光）可经 `HolmSurface.scene` 热换同源 shader；仍 Lite(A)，非 Runtime。  
- 已验证第 3 scene=`liquid_approx`（液态近似单 Pass）+ Attr `ior`/`warp` 仅更新 uniform；仍策略 A，非 Backdrop B / 非 Runtime。
