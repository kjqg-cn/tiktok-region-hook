# TikTok Region Hook

简体中文 | [English](README_EN.md)

一个仅作用于 TikTok 进程的 LSPosed 模块，用于统一 TikTok 读取到的地区、SIM、运营商和商店区域信息。

当前版本：`1.4.1`

## 功能

- 支持美国、英国、日本、韩国、新加坡、德国、法国、加拿大和澳大利亚预设。
- 同步覆盖 country ISO、MCC/MNC、运营商、SIM、系统地区和 store region 等地区信号。
- 支持 `com.zhiliaoapp.musically` 和 `com.ss.android.ugc.trill` 两种 TikTok 包名。
- 在 LSPosed 作用域页面将已安装的 TikTok 标记为“推荐应用”并置顶。
- 可选择跳过 TikTok 冷启动时可关闭的登录引导。
- “应用并重启 TikTok”会保存地区、强制停止 TikTok 并重新打开。
- “清除地区并重启 TikTok”会停用全部地区覆盖，恢复使用真实 SIM 和系统地区。
- 配置会缓存到 TikTok 进程可读取的位置，TikTok Region Hook 无需常驻后台。

## 兼容性

| 项目 | 状态 |
| --- | --- |
| 已验证 TikTok 版本 | Google Play 46.1.3 |
| 已验证包名 | `com.zhiliaoapp.musically`、`com.ss.android.ugc.trill` |
| 已验证 Android 版本 | Android 12、Android 16 |
| 最低 Android 版本 | Android 8.1（API 27） |
| 运行环境 | Root + LSPosed/Vector |

模块同时 Hook Android 稳定 Telephony API 和 TikTok 内部地区提供器。TikTok 的内部混淆类可能随版本变化；升级 TikTok 后如果功能异常，需要针对新版本重新适配。

## 安装

1. 确认设备已具备可用的 Root 和 LSPosed/Vector 环境。
2. 从 [Releases](https://github.com/kjqg-cn/tiktok-region-hook/releases) 下载并安装 TikTok Region Hook APK。
3. 在 LSPosed 中启用模块，确认已安装的 TikTok 显示为“推荐应用”且已勾选。
4. 打开 TikTok Region Hook，选择地区后点击“应用并重启 TikTok”。
5. KernelSU/Magisk 首次询问 Root 权限时选择长期允许，否则应用无法强制停止 TikTok。

更新模块后，建议在 Vector/LSPosed 中重新优化模块，再强制停止并重新打开 TikTok。覆盖安装通常会保留 Root 授权；卸载重装会清除授权，需要重新授予。

## 使用

### 切换地区

选择一个地区预设，保持“启用地区覆盖”开启，然后点击“应用并重启 TikTok”。地区变化必须在 TikTok 进程完全重启后才能稳定生效。

### 恢复真实 SIM 地区

插入目标国家或地区的 SIM 卡时，可以点击“清除地区并重启 TikTok”。模块会写入停用状态并重启 TikTok，不会删除 TikTok 数据；上次选择的预设仍会保留，但不会生效。

### 跳过启动登录引导

该选项只处理 TikTok 新进程启动后短时间内可关闭的登录引导，不会绕过账号登录、验证或其他访问限制。

## 权限与安全

- Root 仅用于执行 TikTok 的 `am force-stop` 和显式 Activity 启动。
- 模块不修改系统地区、系统属性、方向锁定或其他全局设置。
- 地区 Hook 仅安装在受支持的 TikTok 进程中。
- 模块不包含网络权限，不上传配置或设备信息。
- 本项目不会修改或重签 TikTok APK。

## 已知限制

- 本模块不能替代可用的国际网络环境；TikTok 服务端仍会结合 IP、账号和其他风控信号判断可用性。
- TikTok 更新可能改变内部混淆类，使部分版本专用 Hook 失效。
- 信息流能够加载不代表登录、用户主页和所有地区接口都一定兼容，应在更新 TikTok 后完整回归。
- 该方案依赖 Root 和 LSPosed，不适用于未 Root 设备。

## 从源码构建

要求：

- JDK 21
- Android SDK，包含 API 34
- 项目自带的 Gradle Wrapper

调试构建：

```powershell
.\gradlew.bat clean lintDebug assembleDebug
```

Linux/macOS：

```bash
./gradlew clean lintDebug assembleDebug
```

发布构建需要本地 `keystore.properties`。复制 `keystore.properties.example`，填写自己的签名库信息；不要提交签名库、密码或 `keystore.properties`。

```powershell
.\gradlew.bat clean lintRelease assembleRelease
```

维护现有发行签名时，必须继续使用相同证书，否则无法覆盖安装旧版本。发布 APK 应上传到 [Releases](https://github.com/kjqg-cn/tiktok-region-hook/releases)，不建议提交到 Git 仓库。

## 项目结构

```text
app/src/main/java/com/local/tiktokregion/  模块与配置源码
app/src/main/resources/META-INF/xposed/    Xposed 作用域元数据
app/src/main/res/                          Android 资源
app/libs/api-82.jar                        Xposed API 编译依赖
AGENTS.md                                  版本适配与真机验收流程
```

维护者在适配新 TikTok 版本前应先阅读 [AGENTS.md](AGENTS.md)。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 发布，版权所有 (c) 2026 [kjqg-cn](https://github.com/kjqg-cn)。第三方组件遵循其各自许可证，详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 免责声明

本项目是独立的开源研究与设备自定义工具，与 TikTok、ByteDance、LSPosed、Vector、KernelSU、Magisk 及其关联方不存在隶属、合作、授权、认可或赞助关系。TikTok 及相关名称、商标和标识归其各自权利人所有。

本项目不包含、不分发、不修改也不重签 TikTok APK，不提供 TikTok 专有代码、账号、网络代理或地区服务。项目仅在用户自行安装的受支持 TikTok 进程中调整部分本地运行时返回值。

使用者应确保其对设备、软件和账号具有合法管理权限，并自行遵守所在国家或地区的法律法规、TikTok 服务条款、社区规范及其他适用协议。本项目无意用于绕过付费机制、账号验证、访问控制、安全措施、制裁限制，或实施任何未经授权的访问。当地法律或适用协议禁止时，请勿使用。

Root、LSPosed 和运行时 Hook 可能导致应用异常、设备不稳定、数据丢失、隐私或安全风险，也可能触发平台风控、功能限制、账号暂停或封禁。使用前请自行备份重要数据，并理解相关技术和账号风险。

本软件按“原样”提供，不对可用性、适销性、特定用途适用性、不侵权性、持续兼容性、地区访问能力或账号安全作任何明示或默示保证。TikTok 更新、服务端策略、网络环境或设备差异均可能导致本项目部分或全部失效。

在适用法律允许的最大范围内，项目维护者和贡献者不对因使用、无法使用、错误配置或分发本项目所产生的任何直接、间接、附带、特殊、惩罚性或后果性损失承担责任，包括但不限于数据丢失、设备故障、账号受限、业务中断或利润损失。本免责声明不排除或限制依法不得排除或限制的责任。

下载、安装、修改、分发或使用本项目，即表示使用者已理解并自行承担相关风险。
