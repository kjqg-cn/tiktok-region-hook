# TikTok Region Hook 维护规则

## 目标与边界

维护本目录中的 Root + LSPosed 模块，使受支持的 Google Play TikTok 在未登录状态下按选定地区正常使用，并能恢复真实 SIM/系统地区。除非用户重新明确要求，不要修改或重签 TikTok APK。

不要提交 TikTok APK、反编译产物、设备数据、发布签名库、签名密码、`local.properties`、`keystore.properties`、构建目录或生成的 APK。

## 当前基线

- 模块：`com.local.tiktokregion` 1.4.2（`versionCode 41`）
- 已验证地区覆盖：Google Play 46.1.3、46.5.3
- 46.5.3 当前测试账号仍存在点赞、关注状态消失，不能把地区覆盖通过表述为登录后交互完整兼容。
- 当前发布包：`dist/TikTokRegionHook-1.4.2.apk`
- 当前发布包 SHA-256：`8EF792FD9A3E74901346C4828120356D2A630CDD77D7E1A3658F494CDADF8307`
- 目标包：`com.zhiliaoapp.musically`、`com.ss.android.ugc.trill`
- 已验证 Android：Android 12、Android 16
- 发布证书 SHA-256：`b6522c8682fa15850bfc97504a7d0d6a629b44b63105456047f331fba8b539df`
- 编译：JDK 21、Android API 34、Gradle Wrapper 8.9

每次发版后同步更新本文件和中英文 README 中的模块版本、已验证 TikTok 版本及兼容性。

## 必须保持的行为

- 提供 US、GB、JP、KR、SG、DE、FR、CA、AU 地区预设，并保持 ISO、MCC/MNC、运营商值成组一致。
- 同时支持两个 TikTok 包名。
- “应用并重启 TikTok”保存配置，强停受支持 TikTok，再显式启动解析出的 Launcher Activity。
- “清除地区并重启 TikTok”写入 `enabled=false`，不安装任何地区 Hook，使用真实 SIM/系统信号。
- TikTok 保留目标应用私有配置缓存，模块应用不能依赖常驻后台。
- Root 只能用于 `am force-stop` 和 `am start -n`，不能修改全局 Android 设置。
- 推荐作用域同时维护 Manifest `xposedscope`、`res/values/arrays.xml` 和 `META-INF/xposed/scope.list`。

## 代码入口

- `app/src/main/java/com/local/tiktokregion/MainActivity.java`：UI、保存、广播和 Root 重启。
- `RegionHook.java`：Xposed 入口、配置加载和 Hook。
- `RegionProfile.java`：地区预设。
- `ConfigContract.java`：包名和配置契约。
- `ProfileProvider.java`：只读配置 Provider。
- `AndroidManifest.xml`、`arrays.xml`、`scope.list`：模块元数据和推荐作用域。

新增目标包时同步修改 `ConfigContract.TARGET_PACKAGES`、Manifest `<queries>`、`xposed_scope` 和 `scope.list`。

## 版本相关 Hook

稳定层覆盖 `TelephonyManager`、`SubscriptionInfo` 和 `ServiceState` 的地区、SIM 与运营商 API。

46.1.3 内部 Hook 当前涉及：

- `X.0VV8`、`X.11ga`：SIM/网络/应用地区。
- `X.0VV4`：Locale 地区。
- `X.0V1x`、`X.0V1y`：当前 SIM 信息。
- `X.0W7B`、`X.0W6U`、`X.0Wdy`、`X.0We0`、`X.0XBC`、`StoreRegionSource`：store region。
- `I18nSignUpActivityWithNoAnimation`：启动登录引导。

混淆类名不是稳定接口。适配新版时必须从新版完整 APK 集合重新确认类、方法签名、返回类型和构造参数；找不到时保留框架 Hook、记录跳过项，不要凭相似名称替换。

重点搜索：`region`、`carrier_region`、`mcc_mnc`、`sys_region`、`app_region`、`network_sim_region`、`curr_sim_country_code`、`current_region`、`store_region`、`op_region`、`sim_region`。

## 新 TikTok 版本适配

1. 用 `adb devices -l` 获取当前设备，不复用历史序列号。
2. 查询包名、版本、ABI、`pm path` 和真实 SIM ISO/MCC-MNC。
3. 拉取 base 与全部 split 到新的版本目录，记录 SHA-256，不覆盖历史输入。
4. 用 JADX 同时分析完整 APK 集合，逐项核对现有内部 Hook 和地区字段调用链。
5. 小步更新版本相关 Hook，保持稳定框架 Hook 和两个包变体。
6. 递增 `versionCode`/`versionName`，同步中英文 README 和本文件。
7. 完成构建、签名、LSPosed 作用域和真机回归后再交付。

## 构建与签名

公开源码默认可以生成调试包：

```powershell
.\gradlew.bat clean lintDebug assembleDebug
```

发布构建从未提交的 `keystore.properties` 读取签名。没有该文件时可以编译 unsigned release，但不能作为现有版本的更新包。

```powershell
.\gradlew.bat clean lintRelease assembleRelease
```

发布前用 Android SDK `apksigner verify --verbose --print-certs` 验证 v2 签名，并确认发布证书 SHA-256 未变化。APK 上传到 Releases，不提交到 Git。

## 真机回归

- 覆盖安装模块；全新安装会丢失 KernelSU/Magisk 授权，提示用户给模块“始终允许”。
- 在 LSPosed/Vector 中确认已安装 TikTok 被置顶、标记“推荐应用”且勾选。不要清空用户作用域来测试首次默认值。
- 更新模块 APK 后，确认作用域仍启用，然后强停并重新启动 TikTok 即可加载新 Hook；不把“重新优化”作为常规必需步骤。只有在目标进程重启后仍疑似未加载新模块、或需要排除 ART 优化缓存因素时，才将重新优化作为额外排查手段。
- 测试 US 和至少一个非 US 地区；每次切换必须冷启动 TikTok。
- 验证手机号前缀、信息流、评论、用户主页头像与统计、作品列表。
- 强停模块应用和 TikTok，只启动 TikTok，验证模块无需驻留。
- 测试二次切换，避免只在首次进程生效。
- 在国外 SIM 上测试清除地区，确认 UI 为“跟随系统”、`enabled=0` 且真实地区可用。
- 登录流程没有实际展开时，不得声称手机号前缀已验证。

## 设备安全

- 禁止使用 `monkey`。它已被确认会改变 `accelerometer_rotation`，关闭系统方向锁定。
- 用 `cmd package resolve-activity --brief` 或 PackageManager 解析组件，再用 `am start -n` 显式启动。
- 禁止执行或加入 `settings put`、`wm`、`setprop` 等全局设置操作。
- 不探测或修改 KernelSU、Magisk、Vector、LSPosed 的内部数据库；Root 授权由用户管理。
- 未获当次明确许可，不卸载应用、不清除 TikTok 数据、不清空作用域、不删除已有文件。
- 查询模块 Provider 会启动模块进程；做非驻留测试时不要在最终冷启动前查询。
- 测试前后核对 `accelerometer_rotation` 和 `user_rotation` 保持不变。

## 发布标准

- `clean lintRelease assembleRelease` 和 Lint 成功。
- APK v2 签名有效，发布证书不变，并报告 APK SHA-256。
- 已安装版本、推荐作用域、地区切换、二次切换、非驻留和清除地区通过。
- 信息流、评论、用户主页通过，或明确列出未验证项。
- 全局系统状态未变化。
