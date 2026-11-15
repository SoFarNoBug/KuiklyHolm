# runbook · kuikly-dev-ohos（ADR 0010 Runtime）

## 前置

- DevEco Studio + OpenHarmony native SDK（本机常见：`~/Library/OpenHarmony/Sdk/15/native`）
- rustup target：`aarch64-unknown-linux-ohos`
- 已编过 KN `libshared.so` 落入 `ohosApp/entry/libs/${OHOS_ARCH}/`（既有流水线）

## Runtime 解锁（改 Rust 必跑）

```bash
cd holm-runtime
./build-ohos.sh
# → ohosApp/entry/libs/arm64-v8a/libholm_runtime.so + holm_runtime.h
```

Hvigor **不**自动编 Rust。缺 `.so` → Demo `onError(BLOCKED)`，禁止 GLES 冒充。

## App 编装

```bash
# 仓库根
./ohos_build.sh                 # KN libshared.so → entry/libs/arm64-v8a
./holm-runtime/build-ohos.sh    # libholm_runtime.so（改 Rust 时）
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

## 验收

1. 打开 Demo「示例1 / 双轨」(`holm_runtime_m0`)。
2. **Runtime**：`executor=runtime` → 隧道出画；`onReady.backend` 为 `wgpu-m0`（或 `backend_id`）。
3. **Lite**：`executor=lite` → 霓虹隧道出画；`onReady.backend=gles2-ohos`；**无** `LITE_PENDING`。
4. Runtime↔Lite 互切多次：backend 不粘；无崩。
5. 切轨 / 离页：急拆 Session **与** Lite EGL（`UnregisterOnFrame` + destroy），无泄漏。
6. `maxFps`：`SetExpectedFrameRateRange` + OnFrame 跳帧可感知。
7. 负向：空 `fragmentShader` → `FRAGMENT_REQUIRED`；Lite 喂 WGSL → `SHADER_DIALECT`。

## 架构备忘

| 产物 | 职责 |
| --- | --- |
| `libholm_runtime.so` | wgpu Session C ABI（`create_ohos` / tick / …） |
| `libholm_xc.so` | XComponent + OnFrame + NAPI；Runtime 泵 + **Lite EGL/GLES**（ADR 0011） |
| `libentry.so` | KN init / dirs（**不含** Runtime/Lite GL） |
| `KRHolmSurface.ets` | expand-native-ui 注册；内嵌 `XComponent(libraryname='holm_xc')` |
