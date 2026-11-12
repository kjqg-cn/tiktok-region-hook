# TikTok Region Hook 维护规则

## 目标与边界

维护本目录中的 Root + LSPosed 模块，使受支持的 Google Play TikTok 在未登录状态下按选定地区正常使用，并能恢复真实 SIM/系统地区。除非用户重新明确要求，不要修改或重签 TikTok APK。

不要提交 TikTok APK、反编译产物、设备数据、发布签名库、签名密码、`local.properties`、`keystore.properties`、构建目录或生成的 APK。

## 隐私边界

- 可提交文件只保留通用功能、版本兼容性和匿名验证结论。禁止写入设备序列号、型号、主机用户名或绝对路径、账号信息及状态、真实 SIM 信息、IP、网络环境、私人使用配置和带系统状态栏或通知的真机截图。
- 原始日志、截图、设备验证矩阵和本机环境记录仅放在仓库外的本地研究目录；不得复制进 README、维护规则、源码注释或提交说明。
- 公开界面图片默认使用生成的示意图；用户明确要求真机截图时，只使用裁除系统栏、通知和私人内容并移除元数据的副本。提交前检查 Git 跟踪文件和未忽略的新文件，并检查图片内容及元数据。私密文件仅加入忽略规则不能清除已经被跟踪的内容，必须单独处理。

## 当前基线

- 模块：`com.local.tiktokregion` 1.6.0（`versionCode 52`），显示名“TikTok地区助手”。
- 最新已测 TikTok：Google Play 47.7.3。已核对地区特征接口和商店地区模型，开发验证覆盖信息流、评论、用户主页、US/JP 切换、非驻留和清除地区配置。
- 连续播放：模块停用、US/JP 覆盖、模块启用但关闭覆盖四组各冷启动后上滑 7 条，均加载并观察到画面变化；该结果不证明推荐内容地域归属，最终构建未重跑完整矩阵。
- 设置回归：整行操作、城市选择后快速切地区、关于页返回保留国家/城市、非美国保存配置冷启动，以及小尺寸和大字号布局已检查。底部按钮在 320dp 宽度下 100%、130%、200% 字号均完整显示，末项可滚动到达；200% 时按钮高约 79.7dp。
- 未验证项：手机号前缀、国外 SIM 恢复及完整交互矩阵；首次安装摘要分支仅静态检查，未清数据重装。
- 历史已验证 TikTok：46.1.3、46.5.3、46.6.3，来自当时模块；当前代码保留旧 Hook 分支，未逐版本重新回归。
- 目标包：`com.zhiliaoapp.musically`、`com.ss.android.ugc.trill`。
- 已验证 Android：Android 12、Android 16。
- 发布构建：`dist/release-1.6.0-build52/TikTokRegionHook-1.6.0.apk`。
- APK SHA-256：`8F51965CB7FDAD83E8154357FDF454BF64422C8E44DE37B72B2C9B74A5114283`。
- 发布证书 SHA-256：`b6522c8682fa15850bfc97504a7d0d6a629b44b63105456047f331fba8b539df`。
- 编译：JDK 21、Android API 34、Gradle Wrapper 8.9。

## 1.6.0 维护要点

- 本次交付合并地区接口适配、加载状态与只读模式、紧凑整行设置、独立城市选择框、启动误触与页面恢复修复；详细功能见中英文 README。
- 本地中间构建属于未发布测试过程，不在公开文档中逐版列出。公开版本名按实际发布计划维护；`versionCode` 必须高于已安装构建，以保证覆盖更新。
- 推荐作用域和模块包名保持原契约；模块自身探针不代表 TikTok 作用域或全部 Hook 已生效。
- README 仅引用 `docs/images/module-active.png`、`docs/images/module-inactive.png`。两张脱敏图片必须随文档提交，原图和处理脚本保留在仓库外。
- 发版同步核对 Gradle 版本、关于页的最新已测 TikTok 版本和范围、中英文 README、维护基线及 APK 摘要。

## 必须保持的行为

- 提供 US、GB、JP、KR、SG、DE、FR、CA、AU 地区预设，并保持 ISO、MCC/MNC、运营商值成组一致。
- 同时支持两个 TikTok 包名。
- “应用并重启 TikTok”保存配置，强停受支持 TikTok，再显式启动解析出的 Launcher Activity。
- “清除地区并重启 TikTok”写入 `enabled=false`，不安装任何地区 Hook，使用真实 SIM/系统信号。
- TikTok 保留目标应用私有配置缓存，模块应用不能依赖常驻后台。
- Root 只能用于 `am force-stop` 和 `am start -n`，不能修改全局 Android 设置。
- 推荐作用域同时维护 Manifest `xposedscope`、`res/values/arrays.xml` 和 `META-INF/xposed/scope.list`。
- 模块设置项必须整行可操作，触摸目标至少 48dp；小屏和较大字号下设置、地区列表可统一滚动，操作按钮固定可见。
- 底部按钮高度使用 WRAP_CONTENT、最小高度 48dp，保留上下内边距，不能用固定 48dp 限制换行文字。回归须包含 320dp 宽度、200% 字号，检查文字布局高度、完整可见性和末项滚动。
- 城市/时区用独立选择框，不在固定区域内展开城市列表；未加载模块时禁止打开选择框和修改配置。

## 设置页关于入口

- 设置页右上角应提供一个可点击的更多/关于图标。
- 点击图标后应弹出“关于”入口；进入关于页后至少显示当前模块版本、已验证的最新 TikTok 版本、源代码入口和反馈入口。
- “源代码”指向项目主页：`https://github.com/kjqg-cn/tiktok-region-hook`。
- “反馈”指向 GitHub Issues：`https://github.com/kjqg-cn/tiktok-region-hook/issues`。
- 关于页可继续扩展许可证、构建信息、免责声明等内容；新增外链必须使用明确的可访问性描述，并通过系统浏览器打开。

## 代码入口

- `app/src/main/java/com/local/tiktokregion/MainActivity.java`：UI、保存、广播和 Root 重启。
- `RegionHook.java`：Xposed 入口、配置加载和 Hook。
- `RegionProfile.java`：地区预设。
- `ConfigContract.java`：包名和配置契约。
- `ProfileProvider.java`：只读配置 Provider。
- `AndroidManifest.xml`、`arrays.xml`、`scope.list`：模块元数据和推荐作用域。

新增目标包时同步修改 `ConfigContract.TARGET_PACKAGES`、Manifest `<queries>`、`xposed_scope` 和 `scope.list`。

## 版本相关 Hook

稳定层覆盖 `TelephonyManager`、`SubscriptionInfo` 和 `ServiceState` 的地区、SIM 与运营商 API，并覆盖 `TimeZone.getDefault`、`ZoneId.systemDefault` 的时区 API。

以下是从 46.1.3 分析得到、当前实现沿用的内部 Hook。46.6.3 的静态核对显示部分混淆类名已重新分配到无关类，但 FeatureProducer 公共特征字段路径和稳定 Android API 仍可工作；失效的版本专用 Hook 必须允许跳过。后续适配其他新版本时，必须从对应版本重新确认这些类和方法，并更新本段的版本说明：

- `X.0VV8`、`X.11ga`：SIM/网络/应用地区。
- `X.0VV4`：Locale 地区。
- `X.0V1x`、`X.0V1y`：当前 SIM 信息。
- `X.0W7B`、`X.0W6U`、`X.0Wdy`、`X.0We0`、`X.0XBC`、`StoreRegionSource`：商店地区。
- `I18nSignUpActivityWithNoAnimation`：启动登录引导。

混淆类名不是稳定接口。适配新版时必须从新版完整 APK 集合重新确认类、方法签名、返回类型和构造参数；找不到时保留框架 Hook、记录跳过项，不要凭相似名称替换。

重点搜索：`region`、`carrier_region`、`mcc_mnc`、`sys_region`、`app_region`、`network_sim_region`、`curr_sim_country_code`、`current_region`、`store_region`、`op_region`、`sim_region`。

47.7.3：旧混淆类已改为无关类；`FeatureProducer.getStringFeature` 及 `$default` 的字段参数位置和 String 返回类型仍匹配。`StoreRegionSource.LIZ()` 返回 `X.05Tc`，构造函数为 `(String region, String source)`，经 `X.05TT` 发布到 `RegionManager`。新分支仅在返回类型严格匹配时安装，其他版本保留旧分支。通用常量 Hook 必须检查返回类型，避免将字符串返回给新版同名布尔方法。

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

- 模块开启、模块关闭、地区覆盖开启和关闭均不能只看首条缓存视频：冷启动后连续上滑至少 5–10 条，逐条确认后续内容加载及播放；记录广告/图集/加载异常，不将能看到首帧等同于视频已播放。
- 界面加载状态由 ModuleStatus 自身 Hook 探针判断；未加载时禁止编辑和保存，关于页仍可访问。管理器启停后重开模块应用刷新，当前进程状态不等于管理器实时总开关或 TikTok 作用域状态。
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
