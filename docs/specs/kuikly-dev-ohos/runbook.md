# runbook · kuikly-dev-ohos（ADR 0010 Runtime）

## 前置

- DevEco Studio + OpenHarmony native SDK（本机常见：`~/Library/OpenHarmony/Sdk/15/native`）
- rustup target：`aarch64-unknown-linux-ohos`
- 已编过 KN `libshared.so` 落入 `ohosApp/entry/libs/${OHOS_ARCH}/`（既有流水线）

## Runtime 解锁（改 Rust 必跑）

```bash
cd kmp-wgpu-native
./build-ohos.sh
# → ohosApp/entry/libs/arm64-v8a/libkmp_wgpu_native.so + kmp_wgpu_native.h
```

Hvigor **不**自动编 Rust。缺 `.so` → Demo `onError(BLOCKED)`，禁止 GLES 冒充。

## App 编装

```bash
# 仓库根
./ohos_build.sh                 # KN libshared.so → entry/libs/arm64-v8a
./kmp-wgpu-native/build-ohos.sh    # libkmp_wgpu_native.so（改 Rust 时）
./ohosApp/runOhosApp.sh         # ohpm + hvigor assembleHap + hdc install/start
```

首次还需 **DevEco 自动签名**（本机尚无 `signingConfigs` 时 `runOhosApp.sh` 会停在「先配置签名」）：

1. DevEco 打开 `ohosApp/` → File → Project Structure → Signing Configs → 勾选 Automatically generate signature。
2. 保存后 `ohosApp/build-profile.json5` 会出现 `signingConfigs`（含本机密文口令，**勿提交远程**）。
3. 再跑 `./ohosApp/runOhosApp.sh`。

装真机后：Vulkan / `VK_OHOS_surface` 依赖设备；失败诚实 BLOCKED，产品允许 PARTIAL。

鸿蒙 `bundleName`：`com.jlj.kuiklyholm.demo`。

### 坑：`app.json5` UTF-8 BOM → 自动签名假报包名非法

**现象**：Signing Configs 勾选 Automatically generate signature 后，红字  
`The bundle name contains 7 to 128 characters...`（或中文等价文案），Apply 灰色；改 `bundleName` 无效。

**根因**：`ohosApp/AppScope/app.json5` 文件头带 **UTF-8 BOM**（`EF BB BF`）。DevEco 解析失败，读不到合法 `bundleName`，校验当成空/非法。能签的对照工程（如 PerspectiveCrop）均为无 BOM、直接以 `{` 开头。

**排查**：

```bash
xxd ohosApp/AppScope/app.json5 | head -1
# 坏：efbb bf7b ...
# 好：7b0a 2020 ...（以 `{` 开头）
```

**修复**：去掉 BOM 后 **Sync and Refresh Project**（或重开工程），再进 Signing Configs 自动签名。

```bash
python3 -c "from pathlib import Path; p=Path('ohosApp/AppScope/app.json5'); b=p.read_bytes(); p.write_bytes(b[3:] if b.startswith(b'\xef\xbb\xbf') else b)"
```

**预防**：编辑/生成 `*.json5` 时用 UTF-8 **无 BOM**；合并冲突或某些编辑器「带签名保存」易重新引入。`oh-package.json5` 等同理（Sync 也会读）。

### 坑：`libholm_xc` NEEDED 写成宿主绝对路径 → 双轨黑屏

**现象**：HAP 里有 `libkmp_wgpu_native.so`，真机 hilog：
`Error loading shared library /Users/.../libs/arm64-v8a/libkmp_wgpu_native.so: (needed by .../libholm_xc.so)`；renderer-gl / renderer-wgpu 黑屏。

**根因**：IMPORTED `.so` 无 SONAME 时，lld 把**本机绝对路径**写进 `DT_NEEDED` / `RUNPATH`；设备上该路径不存在。

**排查**：

```bash
llvm-readelf -d ohosApp/entry/build/.../libholm_xc.so | rg 'NEEDED|RUNPATH'
# 坏：NEEDED [.../Users/.../libkmp_wgpu_native.so]
# 好：NEEDED [libkmp_wgpu_native.so]；RUNPATH [$ORIGIN]
```

**修复**：`build-ohos.sh` 带 `-Wl,-soname,libkmp_wgpu_native.so`；CMake `IMPORTED_SONAME` + `BUILD_RPATH=$ORIGIN`。改完须重编 Runtime 与 HAP。

## 验收

1. 打开 Demo「示例1 / 双轨」(`holm_dualtrack`)。
2. **Runtime**：`executor=renderer-wgpu` → 隧道出画；`onReady.backend` 为 `wgpu-m0*`。
3. **Lite**：`executor=renderer-gl` → 霓虹隧道出画；`onReady.backend=gles2-ohos`；**无** `LITE_PENDING`。冷启须已加载 `libshared`（Lite 亦经 bridge `requireProvider`，无 shared 不静默 GLES）。
4. Runtime↔Lite 互切多次：backend 不粘；无崩。
5. 切轨 / 离页：急拆 Session **与** Lite EGL（`UnregisterOnFrame` + destroy），无泄漏。
6. `maxFps`：`SetExpectedFrameRateRange` 调度 OnFrame；**仅** `0 < maxFps < 55`（如 30）软件跳帧，且 ExpectedRange 仍按 60Hz 密回调（防 expected=30 再软件掐 → ~15）；`≥55` / `0` 不软件掐（防 60Hz 隔帧掉到 ~30）。
7. `renderScale`（默认 `1`）：相对 XComponent 物理像素基线；`<1` 时 `SET_BUFFER_GEOMETRY` 缩缓冲（糊一点换帧率）；库不强制 1×。
8. 负向：Lite **已就绪后**清空 `fragmentShader` → `FRAGMENT_REQUIRED`（冷启首包 defer）；Lite 喂 WGSL → `SHADER_DIALECT`。
9. 负向（**可选 · 特制未 register 宿主**）：切轨/start → `BLOCKED` 含 HolmProvider；**默认 Demo**（`configure`/umbrella 已 register）可跳过。
10. 壳 `holm_xc` / ets：**无** Present 直调 `kmp_wgpu_session_*`（ripreg）。

## 架构备忘

| 产物 | 职责 |
| --- | --- |
| `libkmp_wgpu_native.so` | wgpu Session C ABI；由 **libshared**（kmp-wgpu cinterop）持有 |
| `libshared.so` | KN + `@CName holm_ohos_bridge_*` Present 门面 |
| `libholm_xc.so` | XComponent + OnFrame 泵 + Lite EGL；Runtime **dlsym** bridge（不直调 C Session） |
| `libentry.so` | KN init / dirs NAPI |
| `KRHolmSurface.ets` | expand-native-ui；`XComponent(libraryname='holm_xc')` |
