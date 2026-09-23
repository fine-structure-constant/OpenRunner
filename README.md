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
- 跑步时会在本机保留经纬度轨迹，并为每个有效 GPS 轨迹点保存“相对时间—累计里程”采样。带本地定位点或曲线数据的记录使用粗边框标识，点按后可查看起止时间、里程、用时、步数、最快配速、可缩放的历史路线，以及时间—里程和时间—配速曲线；配速由短窗口差分计算并截断在 `1.5–10 min/km`。已有本地轨迹即使没有曲线采样也能打开详情；仅从服务器取得且没有本机详情的记录保持原样。保存成功后立即清除跑步页和虚拟定位页的当前路线，不清除已持久化数据或结束时的统计；保存失败则保留路线供重试。
- 记录页以橙色标识待上传、蓝色标识上传中或已上传但验证不合格、绿色标识已上传合格、红色标识真正上传失败；服务端错误码会解释为里程范围、速度、位置、时间、照片或摘要问题。服务端记录 ID 视为不透明字符串，同时兼容旧的数字 ID，避免成功上传后因解析错误被误标为失败。
- 应用提供跟随系统、浅色和深色三种外观模式；选择会持久化，地图样式与系统栏图标同步变化。
- 左侧“虚拟定位”提供应用内的手动测试数据源：开启后点击“加点”，点按地图选择位置，再点击“点按确定”。每次确认产生一个定位样本，跑步计时和菜单切换不中断，关闭后恢复真实定位。含虚拟定位的记录永久标为紫色测试记录，保留本地详情但不提供官方上传或图片入口；仓库层也拒绝上传此类记录。每次进程启动默认关闭虚拟定位，不注册系统 mock provider，也不依赖 Hook。
- 虚拟定位页不再显示旧项目的五四/未名参考中心和距离。地图只显示交互选点、当前虚拟位置和当前跑步路线。具体有效范围仍由服务端决定。本功能用于本地轨迹、采样和页面测试，不会向正式服务器提交模拟记录。
- 天气页面已迁移到中国气象局 NMC 接口，可从左侧菜单刷新当前天气和预报。
- 登录后左侧“我的账户”页面显示学号、姓名、Token、跑步汇总和历史记录；历程按时间倒序排列，日期缺失的记录放在最后。Token 缺失或服务端判定失效时才显示登录入口。
- 左侧菜单顶部展示封面、欢迎语、姓名、学号及官方有效里程/目标、奖励里程和有效次数。摘要按缓存刷新：一次成功获取后 30 秒内重开菜单直接复用，登录结果与本地记录变化才强制获取（并受 5 秒地板约束，进行中的请求不会并发重复发起），因此频繁开关菜单不会反复打网络。已有数据时状态行保持显示数据，只有还没有任何数据时才显示同步提示。网络错误保留上次数据并标出未同步。未上传或虚拟测试里程不会混入官方统计，退出或切换账号会清除旧账号的概览。
- `app/src/main/res/drawable-nodpi/cover.jpg` 是应用封面，用于侧栏和登录页；原始图片来自项目上一级的 `cover.jpg`。登录页支持滚动，避免小屏或键盘遮挡表单。
- 桌面图标刻意与旧版拉开距离，否则几个共存版本摆在同一台设备的桌面上分不出来：燕园红底 + 宋体「跑」字。字形轮廓直接从内置的 `or_serif_semibold.ttf` 提取，所以图标与界面标题同源，而且是矢量。Android 8.0 及以上走自适应图标（`mipmap-anydpi-v26/`，只给纯色底与居中前景，遮罩形状交给桌面），API 25 及以下用五档密度的位图（`mipmap-*/ic_launcher.png`，圆形启动器另用 `ic_launcher_round.png`）。底色是固定的 `launcher_icon_background`，不随深色模式变化。重新生成：`python tools/build_launcher_icon.py`，样张见 `design/launcher_icon_preview.png`。
- 记录详情的顺序为基本信息、时间—里程曲线、时间—配速曲线、跑步路径。最后的路径地图采用高德 `TextureMapView`，使用同一视图合成流程跟随页面滚动与裁剪，避免独立 Surface 滚动滞后露出黑底；加载完成前显示不透明占位层，保留地图拖动、缩放和夜景功能，不使用截图替代或放大遮挡。
- 登录页面按官方 IAAA 流程处理用户名、密码、SMS/OTP 二次认证，再调用 /user 换取会话。
- 界面排版使用内置的 Noto Serif SC 子集：中文标题与印章走 SemiBold，数字与西文走衬线 Regular。数字已改为等宽，跑步计时每秒刷新时整串数字不会左右跳动。授权与来源见 `app/src/main/assets/licenses/`。
- 记录详情的时间—里程与时间—配速曲线由一次归一化同时得出；配速窗口用两个只前进的游标单趟求出，曲线控件在数据变化时预计算坐标轴上限，两者都是 O(n)。

## 构建

在 Windows PowerShell 中：

~~~powershell
$env:JAVA_HOME = 'D:\.ENV\Java\jdk-temurin-17.0.20.1'
$env:ANDROID_SDK_ROOT = 'D:\.DevTOOLs\Android\Sdk'
.\gradlew.bat :app:assembleDebug
~~~

生成文件位于 app/build/outputs/apk/debug/app-debug.apk。

当前构建的安装包 ID 是 `cn.edu.pku.openrunner.newui`，可与旧版 `cn.edu.pku.openrunner`、原 v2 `cn.edu.pku.openrunner.v2` 以及默认外观的 v2-pro `cn.edu.pku.openrunner.v2pro` 同时安装；源码 namespace 仍保持 `cn.edu.pku.openrunner`。

桌面图标名称由 `@string/app_launcher_name`（「OpenRunner 新UI」）单独提供，与应用内工具栏标题 `@string/app_name` 分开。共存版本在桌面上靠两处区分：名字，以及图标本身 —— 本构建是燕园红底 + 宋体「跑」字，与旧版的绿色牛油果标记一眼可分。

如需使用自己的高德 Key，可在项目 `gradle.properties` 增加：

~~~properties
AMAP_API_KEY=你的高德 Android Key
~~~

高德 Android Key 需要绑定当前构建的安装包 ID `cn.edu.pku.openrunner.newui` 和实际签名证书 SHA-1。旧版、原 v2 或 v2-pro 的 Key 都不能用于本构建。

当前调试签名（`~/.android/debug.keystore`，别名 `androiddebugkey`）的 SHA-1：

~~~
9F:0A:69:8B:50:C7:79:39:83:14:45:6A:16:30:BE:35:6A:E2:97:16
~~~

换了安装包 ID 或换了签名证书后，都需要到高德控制台重新申请 Key，否则跑步页与记录详情页的地图会加载失败。

新版高德 SDK 要求在初始化地图和定位前完成隐私合规状态设置，因此首次打开跑步地图会显示一次高德地图与定位隐私说明。

## 第三方资源

内置字体只有一套，来自 Google Fonts 的 Noto Serif SC（可变字体，wght 轴）：

| 资源 | 用途 | 授权 |
| --- | --- | --- |
| `res/font/or_serif_regular.ttf` | 数字、西文、单位（wght=400） | SIL OFL 1.1 |
| `res/font/or_serif_semibold.ttf` | 中文标题、印章（wght=600） | SIL OFL 1.1 |

两个字重都是可变字体固化后的**子集**：按界面实际用到的字符裁剪，数字改成等宽，family 名改为 `OpenRunner Serif`。这是 OFL 意义上的 Modified Version，因此不得沿用上游保留字体名，且必须随附授权文本 —— 授权正文与来源说明放在 `app/src/main/assets/licenses/`，会随 APK 一起打包。上游版权、商标与授权记录也保留在字体二进制的 name 表里（ID 0 / 7 / 13 / 14），保证机器可读。

重新生成：`python tools/build_serif_font.py`（读取本机 `C:\Windows\Fonts\NotoSerifSC-VF.ttf`）。新增界面文案后重跑，然后执行单元测试：`FontCoverageTest` 会在文案用到的字缺字形时失败。样张见 `design/font_specimen.png`，可用 `python tools/render_font_specimen.py` 重新渲染。

桌面图标也取自同一套字体：`tools/build_launcher_icon.py` 从 `or_serif_semibold.ttf` 取出「跑」字的轮廓，导出成 VectorDrawable 前景层与五档密度的位图。因此图标属于上述字体的衍生使用，同样受 SIL OFL 1.1 约束。字体授权文本见 `app/src/main/assets/licenses/`。

## 设计约定

- 包名全部使用小写。
- UI 只负责展示状态，网络和数据转换放到 Repository。
- 服务器返回的状态必须在 ApiModels.kt 中命名，不在 UI 中散落数字常量。
- 官方 API 的路径和字段以 docs/PKUNEWYOUTH_API.md 为准。
