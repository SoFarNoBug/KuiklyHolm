# runbook · kuikly-dev-android

> 真机装包 / 起 Demo / 排障。维护者入口亦见根 [CONTRIBUTING.md](../../../CONTRIBUTING.md)。

## 前置

| 项 | 本机约定 |
|---|---|
| JDK | **17**（例：`D:\tool\jdk-17`；勿用 25） |
| SDK | `local.properties` → `sdk.dir`（例：`D:\AndroidSdk`） |
| adb | `%sdk.dir%\platform-tools\adb.exe` |
| 真机 | USB 调试开；`adb devices` 为 `device`（非 `unauthorized`） |

PowerShell 示例：

```powershell
$env:JAVA_HOME = "D:\tool\jdk-17"
$env:ANDROID_HOME = "D:\AndroidSdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;" + $env:Path
cd D:\code_space\kuikly-holm
```

## 装包（最短）

```bash
./gradlew :androidApp:installDebug
```

拉起：

```bash
adb shell am start -n com.jlj.kuiklyholmui/.KuiklyRenderActivity
```

| 项 | 值 |
|---|---|
| applicationId | `com.jlj.kuiklyholmui` |
| Activity | `.KuiklyRenderActivity` |
| 入口页 | Home → **Frost 尖刺** / `holm_spike` |

## 仅编译（不装机）

```bash
./gradlew :androidApp:compileDebugKotlin
```

## 装包失败：用户拒权限

若日志含 `INSTALL_FAILED_ABORTED: User rejected permissions`：

1. **看手机屏幕**：点允许「USB 安装 / 安装未知应用」（拒一次 adb 就会失败；卸载成功也不等于装得上）  
2. 再执行：

```bash
./gradlew :androidApp:assembleDebug
adb uninstall com.jlj.kuiklyholmui
adb install -r -g androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

vivo 等机型偶发要在「设置 → 更多设置 → 开发者选项」里打开「USB 安装」/关闭「禁止权限监控」。

## 构建产物锁文件

若 `bundleLibCompileToJarDebug` 报 classes.jar「正被另一进程使用」：

1. `adb shell am force-stop com.jlj.kuiklyholmui`
2. `./gradlew --stop`
3. 关闭占用该 jar 的 IDE 索引/其它 Gradle；再装包

## Holm 尖刺目视（holm_spike）

- scene：霜化 / 柔光 / 液态  
- `maxFps`：`0 / 30 / 60 / 120`  
- **Android 限帧语义**（勿当 bug）：`onPerf` = **真实 GL 提交率**；`maxFps` 经 `Surface.setFrameRate` 只是系统提示。`RENDERMODE_CONTINUOUSLY` 下设 30 仍可能报 ~60。跳画会闪，当前不做；真贴档需 WHEN_DIRTY + `requestRender`（未做）。见 `KRHolmSurfaceView.applySurfaceFrameRate` KDoc。  
- backend：`onReady` 报 `gles2`（Lite 刻意 GLES2，与 H5 WebGL1 同源；非探测失败）

## 与 H5 对照

| | Android | H5 |
|---|---|---|
| 命令 | `:androidApp:installDebug` | 见 [kuikly-dev-h5/runbook.md](../kuikly-dev-h5/runbook.md) |
| 后端标签 | `gles2` | `webgl` / `webgl-worker` |
| maxFps | setFrameRate 提示 | rAF + nextDue 跳提交 |

## 相关路径

| 路径 | 说明 |
|---|---|
| `androidApp/` | 壳；注册 `KRHolmSurface` |
| `KuiklyHolmCore/src/androidMain/.../KRHolmSurfaceView.kt` | GLES2 执行器 |
| `holmDemo/.../HolmSpikePager.kt` | `holm_spike` |
| `docs/specs/stage0-lite-spike/changelog.md` | 尖刺业务 changelog |
