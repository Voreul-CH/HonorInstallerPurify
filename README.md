# HonorInstallerPurify（荣耀安装器净化）

> ## ⚠️ 本模块由 AI 生成 / This module is AI-generated
> 本项目由大语言模型（AI）在人类指导下生成并迭代维护，包括全部逆向分析、Hook 代码、
> 构建流水线与文档。代码未经人工长期审计，请自行评估风险后使用；欢迎人工审查与 PR。
>
> This project was generated and iterated by a large language model (AI) under human
> direction, including all reverse engineering, hook code, the build pipeline and these docs.
> The code has not been long-term audited by humans — evaluate the risk yourself;
> human review and PRs are welcome.

面向荣耀 MagicOS 系统「软件包安装程序」`com.android.packageinstaller` 的 LSPosed 模块：
**跳过安装第三方 APK 时的联网安全检测与安装前指纹验证**，点开即装。

An LSPosed module targeting Honor MagicOS's stock PackageInstaller
(`com.android.packageinstaller`): it **skips the online security scan and the
pre-install fingerprint prompt** when sideloading APKs — tap and install, no waiting.

## 为什么做这个模块 / Why

荣耀 MagicOS 的安装器在每次安装第三方 APK 时：

1. **强制联网安全检测**：先请求荣耀云端安全策略，再调用手机管家做本地病毒扫描。检测期间「安装」按钮转圈不可用；即使你断网或用 root 禁止安装器联网，它也会**死等回调超时**，转圈时间反而更长。
2. **安装前指纹/锁屏验证**：检测完成后，云端下发的策略还可能要求指纹或锁屏密码确认，才能继续安装。

关键点：这套检测逻辑在**安装器自身**（PackageInstaller + 内置安全插件），并不是手机管家弹的窗，所以禁网、冻结手机管家都绕不开。

On Honor MagicOS, every sideloaded APK triggers a mandatory online security check
(cloud policy + local system-manager scan) inside the stock PackageInstaller itself.
The install button spins until the check completes — blocking the network only makes
it wait for the timeout. Depending on the cloud-issued policy, a fingerprint or
lock-screen prompt may also gate the install. This module removes both.

## 功能一览 / Features

| 功能 Feature | 说明 Description |
| --- | --- |
| 跳过联网安全检测 / Skip online security check | Hook 检测总入口 `ly1.h()`，直接投递官方「免检测通过」结果；安装按钮秒亮，不转圈、不联网、不调手机管家扫描 / Hooks the single check entry point and immediately delivers the system's own "trusted source, no check needed" result — install button lights up instantly |
| 跳过安装前指纹 / Skip pre-install fingerprint | Hook 安装流程指纹入口 `qf2.g()`，直接回调验证成功 / Hooks the install-flow fingerprint entry and reports success immediately |
| 不动设置页验证 / Settings prompts untouched | 安装器设置页的指纹入口 `qf2.h()` 未 Hook，其余系统行为不受影响 / The settings-page auth entry is deliberately left alone |

## 原理 / How it works

通过 jadx 逆向 `PackageInstaller.apk` 与内置安全插件 `HnSecurityPluginBase.apk` 得到检测链路（类名为混淆名，已在 DEX 中逐一验证）：

```
点击 APK
  └─ PackageInstallerActivity.W0()
       └─ ly1.h(…, sy1回调)                    ← 检测总入口（全 App 唯一调用点）
            ├─ py1  新荣耀检测（云端 + 本地）
            └─ qy1  旧华为检测（fz1 云请求 / w92 名单 / bz1 手机管家扫描）
                 └─ 完成后回调 sy1.b(oy1结果, jz1)
点击「安装」
  └─ iz1.p() / my1  →  qf2.g()  →  FusionAuth 指纹/锁屏弹窗
```

模块共 2 个 Hook：

1. **`ly1.h(boolean, sy1)`** → 替换为：调用官方自带的「可信来源免检测」结果工厂 `ly1.a()` 构造通过结果，主线程直接回调 `sy1.b()`。这是系统给可信来源设计的原生放行路径，后续 UI 流程零改动。
2. **`qf2.g(Context, qf2$a)`** → 替换为：直接回调 `onAuthStart` + `onAuthResult(true)`。

The detection chain was recovered with jadx (obfuscated class names verified
directly in the DEX). The module has exactly two hooks: it replaces the single
check entry `ly1.h` with an immediate delivery of the system's own
"trusted-source pass" result (`ly1.a()`), and replaces the install-flow
fingerprint entry `qf2.g` with an instant success callback.

## 环境要求 / Requirements

* 已 root 的荣耀 MagicOS 设备：Magisk / KernelSU / ReSukkiSU + LSPosed（或 Vector 等第三方管理器）。
  A rooted Honor MagicOS device: Magisk / KernelSU / ReSukkiSU + LSPosed (or a third-party manager such as Vector).
* 实测适配：MagicOS 11（Android 17）自带 PackageInstaller。
  Tested against the PackageInstaller shipping with MagicOS 11 (Android 17).

> 安装器经过代码混淆，其他系统版本的类名可能变化导致功能静默失效；
> 可在 LSPosed 日志过滤 `InstallBypass` 查看 `hook ok / skipped / bypassed` 行确认 Hook 是否命中。
> The installer APK is obfuscated; class names may change across versions and hooks
> can silently miss. Filter LSPosed logs for `InstallBypass` to confirm hooks are in place.

## 使用方法 / Usage

1. 下载安装 [最新 Release](../../releases) 的 APK（模块无界面，装完在 LSPosed/Vector 里可见）。
   Install the APK from [Releases](../../releases) (no launcher UI; it appears in your LSPosed manager).
2. 在 LSPosed/Vector 中启用「荣耀安装器净化」，作用域勾选 **软件包安装程序**。
   Enable the module and select the **Package Installer** (`com.android.packageinstaller`) scope.
3. 强制停止「软件包安装程序」或重启手机使 Hook 生效。
   Force-stop Package Installer (or reboot) for the hooks to take effect.
4. 点击任意 APK 测试：应直接出现「开始安装」，无转圈、无指纹弹窗。
   Tap any APK: the install button should be ready immediately, no spinner, no fingerprint.

如需恢复原版行为：在 LSPosed/Vector 中停用模块（或卸载），再强停一次软件包安装程序即可。
To revert: disable or uninstall the module, then force-stop Package Installer again.

## 从源码构建 / Build from source

本项目刻意不依赖 Gradle / Android Studio，使用最小命令行工具链：

```
powershell -ExecutionPolicy Bypass -File build.ps1
```

流程：`ecj` 编译 Java 8 → `d8` 转 dex → `aapt2` 打包 → `zipalign` 对齐 → `apksigner` 签名。
产物输出至 `build/HonorInstallerPurify.apk`。

需自行下载放入 `tools/`（体积原因未提交本仓库）：

| 文件 | 来源 |
| --- | --- |
| `ecj.jar` | Maven Central `org.eclipse.jdt:ecj` |
| `android.jar` | Android SDK `platform-36`（从 platform zip 中提取） |
| `build-tools/` | Android SDK build-tools r34+（需 `d8`/`aapt2`/`zipalign`/`apksigner`） |
| `api-82.jar` | Xposed API 82（仅编译期引用，运行时由框架提供） |

`build.ps1` 顶部的 `$jreBin` 指向任意 JDK/JRE 8+ 的 `bin` 目录即可（用 `java`/`keytool`）。

The build deliberately avoids Gradle and Android Studio: compile with plain `ecj`,
dex with `d8`, package with `aapt2`, align with `zipalign`, sign with `apksigner`.
Download the four toolchain pieces listed above into `tools/` and point `$jreBin`
at any JRE 8+.

## 项目结构 / Project layout

```
├── AndroidManifest.xml        # xposedmodule 元数据与作用域 / module meta + scope
├── assets/xposed_init         # Xposed 入口声明 / entry declaration
├── res/values/strings.xml     # 应用名与 xposedscope / app name + scope array
├── src/io/github/voreul_ch/installerpurify/
│   └── MainHook.java          # 全部 Hook 逻辑 / all hook logic
└── build.ps1                  # 一键构建流水线 / one-shot build pipeline
```

## 免责声明 / Disclaimer

* 本项目仅供学习与研究 Android Hook 技术使用，请勿用于商业用途。
  For learning and research on Android hook techniques only; do not use commercially.
* 与荣耀公司/HONOR 无任何关联；「软件包安装程序」相关商标与原应用版权归原厂所有。
  Not affiliated with Honor Device Co., Ltd. All trademarks and rights to the
  original app belong to their owner.
* **跳过安全检测会削弱系统对恶意应用的拦截能力**，请只为来源可信的 APK 使用本模块。
  **Skipping security checks weakens protection against malicious apps** — only
  install APKs from sources you trust.
* 使用本模块产生的任何后果由使用者自行承担。
  Use at your own risk.
