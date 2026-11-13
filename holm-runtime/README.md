# holm-runtime · DualTrack B（Standard / Lite3 近似）

> **状态（2026-11-13）**：H5 wasm **Session 泵帧** + 三席 `frost` / `glow` / `liquid_approx`（WGSL 观感近似）已通。Android `.so` 仍未接。  
> 尖刺默认：**Rust + wgpu**（禁止静默换非 wgpu 栈）。

## 范围

- H5：`HolmRuntimeSession` + `onReady.backend=wgpu-m0`
- 席位：前三席观感近似 Lite；`clear_tri` 作 debug fallback
- **不做（本波）**：逐字 GLSL≡WGSL、真 Backdrop、gradient/warp、改 O0

## H5 解锁

```powershell
cd holm-runtime
.\build-wasm.ps1
# → h5App/src/jsMain/resources/holm-runtime/
# 再编 h5App + holmDemo pack，起 8080/8083，进 Runtime 旁路页切三席
```

`wasm-bindgen-cli` 须与 crate 锁定的 `wasm-bindgen` **精确一致**（当前 0.2.130）。

## 验收

- [x] 工具链 + wasm-bindgen
- [x] H5 Session 泵帧
- [x] H5 frost / glow / liquid_approx（观感近似）
- [ ] Android 清屏/三席
- [ ] resize / destroy 专项压测
- [x] H5 wasm 包体 ≈ 3.84 MiB（写入 DualTrack changelog）
