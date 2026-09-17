# OpenRunner v2-pro

这是 OpenRunner 的可维护重构起点。它使用 Kotlin、AndroidX、Retrofit、OkHttp、协程和 ViewModel，包名统一为 cn.edu.pku.openrunner。

## 当前状态

- 网络层已集中到 core/network，保留 PKU New Youth 官方 API。
- 会话由 core/session/SessionStore 管理，不再由静态 Data 对象承载全部状态。
- 任务功能已经迁移到 feature/tasks，使用 Repository + ViewModel + StateFlow。
- 跑步轨迹距离计算已提取到可测试的 feature/run/domain。
- 记录列表已接入官方记录接口，使用独立 Repository + ViewModel。
- 跑步页面使用高德定位 SDK 的运动场景，负责权限、开始/结束和轨迹距离状态；定位结果直接采用与地图及上传格式一致的 GCJ-02。
- 跑步页面已升级到高德 3D 矢量地图 11.2.100；进入页面即显示当前位置与精度范围，跑步时实时绘制轨迹，并随应用深色模式切换夜景底图。
- 开跑后启用高德前台定位通知与有限时长的 CPU 唤醒锁，保证退到后台或息屏后继续定位和计步；结束跑步后立即关闭并释放资源。
- 跑步页使用单调时钟实时计时，并根据累计时间和距离显示平均 `min/km` 配速；计步沿用旧版的无权限加速度计方案。结束跑步需要二次确认，结束后选择“保存记录”或直接“删除”，处理完成前不能开始新跑步。图片（压缩为不超过 640×480、JPEG 质量 50）和上传业务仅在“记录”页处理，可分别处理多条待上传记录。
- 跑步时会为每个有效 GPS 轨迹点在本机保存“相对时间—累计里程”采样。带采样数据的记录使用粗边框标识，点按后可查看起止时间、里程、用时、步数、最快配速，以及时间—里程和时间—配速曲线；配速由短窗口差分计算并截断在 `1.5–10 min/km`。这些增强数据不进入官方上传字段，旧记录和仅从服务器取得的记录保持原样。
- 记录页以橙色标识待上传、蓝色标识上传中或已上传但验证不合格、绿色标识已上传合格、红色标识真正上传失败；服务端错误码会解释为里程范围、速度、位置、时间、照片或摘要问题。服务端记录 ID 视为不透明字符串，同时兼容旧的数字 ID，避免成功上传后因解析错误被误标为失败。
- 应用提供跟随系统、浅色和深色三种外观模式；选择会持久化，地图样式与系统栏图标同步变化。
- 左侧“虚拟定位”提供应用内的手动测试数据源：开启后点击“加点”，点按地图选择位置，再点击“点按确定”。每次确认产生一个定位样本，跑步计时和菜单切换不中断，关闭后恢复真实定位。含虚拟定位的记录永久标为青色测试记录，保留本地详情但不提供官方上传或图片入口；仓库层也拒绝上传此类记录。每次进程启动默认关闭虚拟定位，不注册系统 mock provider，也不依赖 Hook。
- 虚拟定位地图标出旧项目的五四/未名参考中心及点到中心的距离，但不把这些点或距离宣称为官方允许区域。具体有效范围仍由服务端决定。本功能用于本地轨迹、采样和页面测试，不会向正式服务器提交模拟记录。
- 天气页面已迁移到中国气象局 NMC 接口，可从左侧菜单刷新当前天气和预报。
- 登录后左侧“我的账户”页面显示学号、姓名、Token、跑步汇总和历史记录；Token 缺失或服务端判定失效时才显示登录入口。
- 登录页面按官方 IAAA 流程处理用户名、密码、SMS/OTP 二次认证，再调用 /user 换取会话。

## 构建

在 Windows PowerShell 中：

~~~powershell
$env:JAVA_HOME = 'D:\.ENV\Java\jdk-temurin-17.0.20.1'
$env:ANDROID_SDK_ROOT = 'D:\.DevTOOLs\Android\Sdk'
.\gradlew.bat :app:assembleDebug
~~~

生成文件位于 app/build/outputs/apk/debug/app-debug.apk。

v2-pro 的安装包 ID 是 `cn.edu.pku.openrunner.v2pro`，可与旧版 `cn.edu.pku.openrunner` 和原 v2 `cn.edu.pku.openrunner.v2` 同时安装；源码 namespace 仍保持 `cn.edu.pku.openrunner`。

如需使用自己的高德 Key，可在项目 `gradle.properties` 增加：

~~~properties
AMAP_API_KEY=你的高德 Android Key
~~~

高德 Android Key 需要绑定 v2-pro 的安装包 ID `cn.edu.pku.openrunner.v2pro` 和实际签名证书 SHA-1。旧版或原 v2 的 Key 不一定能用于 v2-pro。

新版高德 SDK 要求在初始化地图和定位前完成隐私合规状态设置，因此首次打开跑步地图会显示一次高德地图与定位隐私说明。

## 设计约定

- 包名全部使用小写。
- UI 只负责展示状态，网络和数据转换放到 Repository。
- 服务器返回的状态必须在 ApiModels.kt 中命名，不在 UI 中散落数字常量。
- 官方 API 的路径和字段以 docs/PKUNEWYOUTH_API.md 为准。
