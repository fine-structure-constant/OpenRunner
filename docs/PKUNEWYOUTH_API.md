# PKU New Youth 官方 API

本文档根据旧客户端的 Retrofit 接口整理。Base URL、路径和字段保持兼容；服务器的实际校验规则以服务端为准。

## 基础信息

Base URL：

    https://pkunewyouth.pku.edu.cn/

客户端统一发送以下请求头：

    Platform: Android
    Manufacturer: <设备制造商>
    ClientVersion: <客户端版本>
    Authorization: <登录后服务端返回的原始 access token>

Authorization 使用服务端返回的原始 Token。客户端源码没有自动添加 Bearer 前缀。

除版本接口外，大多数接口都会返回：

    {
      "success": true,
      "code": 0,
      "message": "...",
      "data": {}
    }

失败时应先检查 success，再根据 code 和 message 显示错误。不要把 Token 写入日志或提交到仓库。

## IAAA 官方认证流程

IAAA 与 PKU New Youth 是两个服务。v2 的 IAAA 客户端位于 feature/auth/data/IaaaClient.kt，认证完成后才调用 PKU New Youth 的 POST /user。

IAAA Base URL：

    https://iaaa.pku.edu.cn/

应用标识沿用旧客户端协议：

    appId=PKU_Runner

查询二次认证方式：

    POST /iaaa/svc/authen/isMobileAuthen.do
    表单：appId、userName

返回的 authenMode 可能是 SMS、OTP 或空值。只有 authenMode=OTP 且 isBind=true 时才启用 OTP。

发送短信验证码：

    POST /iaaa/svc/authen/sendSMSCode.do
    表单：appId、userName

密码登录：

    POST /iaaa/svc/authen/login.do
    表单：appId、userName、password、randCode、smsCode、otpCode

IAAA 请求需要把按字典序排列的表单字符串与客户端协议签名拼接，并以 msgAbs 字段携带 MD5 摘要。v2 保留该协议，但不记录用户名、密码或 Token。

IAAA 返回的 token 不是 PKU New Youth API 的最终会话。v2 会将它作为 access_token 提交到 POST /user，再保存服务器返回的 id 和 access_token。

## 登录和会话

### 将 IAAA Token 换成 OpenRunner 会话

    POST /user
    Content-Type: application/x-www-form-urlencoded

表单：

    access_token=<IAAA access token>

返回的 data 包含用户信息，关键字段：

    id             服务端用户 ID，学生账号通常是学号
    access_token   PKU New Youth API 后续请求使用的 Token
    name
    department
    sex
    isPESpecialty

v2 对应接口：PkuNewYouthApi.exchangeIaaaToken。

## 任务和成就

    GET /badge/user/{userId}

返回：

    DataPack<Map<String, Task>>

任务字段：

    id
    activityId
    name
    description
    requirement
    status

状态：

    0  隐藏
    1  未获得
    2  已获得

旧客户端会过滤状态0，并按 activityId、id 排序。v2 在 TaskRepository 中保留了这一行为。

## 跑步记录

获取列表：

    GET /record/{userId}

获取单条记录：

    GET /record/{userId}/{recordId}

获取用户跑步汇总状态：

    GET /record/status/{userId}

汇总状态字段沿用旧客户端模型：

    beginDate   学期统计开始时间（日期字符串）
    endDate     学期统计结束时间（日期字符串）
    current     当前有效里程，米
    bonus       奖励里程，米
    target      目标里程，米
    validCount  有效次数
    isPassed    是否达标

上传记录：

    POST /record/{userId}
    Content-Type: multipart/form-data

字段：

    duration   运行秒数
    distance   米
    date       Unix 时间戳，毫秒
    detail     轨迹 JSON
    misc       JSON，例如客户端信息
    step       步数
    abstract   服务端要求的摘要字段
    photo      可选 JPEG 文件，multipart 文件名固定为 image.jpg

v2 在 `PkuNewYouthApi.uploadRecord` 中声明上传边界。结束跑步时先保存本地记录，再由用户触发上传；网络失败的记录会保留在“记录”页供分别重试。可选图片通过系统文件选择器读取，在应用私有目录中压缩为不超过 640×480、JPEG 质量 50，并随对应记录上传。

兼容旧 Android 客户端的字段生成规则：

    date       开始时间 + 实际计时秒数，单位毫秒
    detail     [[longitude, latitude, status], ...]
    status     首点为 1，末点为 2，中间点为 0
    misc       {"agent":"Android v1.2+"}
    step       加速度计采集的步数，上传前按旧版协议以 17 步向下取整
    abstract   SHA-256(userId + "_" + date + "_YCVNc92y") 的前 32 个十六进制字符

本地记录还保留旧版 `check` 完整性字段：以 `userId + "_" + date` 为 AES 密钥，用 `AES/ECB/PKCS5Padding` 加密固定协议文本。该字段不随 multipart 上传，只在上传前由客户端校验本地记录是否与账号、完成时间匹配。

上传成功只表示服务器收到了记录；是否计为有效成绩由返回记录中的 `verified` 决定。`verified=false` 时，v2 会根据 `invalidReason` 显示旧版含义：

    7   跑步距离或里程范围不合格
    8   跑步速度不合格
    9   跑步位置不合格
    18  无有效照片
    23  跑步时间不合格
    24  消息摘要错误

服务端记录中的 `date` 是日期字符串而不是整数时间戳，`duration` 可能包含小数；`detail` 的实际结构是二维数组：

    [[longitude, latitude, status], ...]

v2 的 `RunRecordDto` 按该旧版协议建模，避免将日期强制转换为 Long 导致 `NumberFormatException`。

## 场馆记录

获取场馆记录：

    GET /record2/{userId}

验证场馆记录：

    POST /record2/{userId}/{recordId}
    Content-Type: application/x-www-form-urlencoded

表单：

    token=<场馆记录令牌>

## 活动报名

通用活动报名：

    POST /activity/{activityId}/user/{userId}/team/{color}

旧版本遗留活动清理接口：

    POST /activity/20180420/user/{userId}/team/purple

这两个接口已保留在 PkuNewYouthApi，但 v2 不会在 UI 中默认触发旧活动逻辑。

## 客户端版本

当前版本：

    GET /public/client/android/curr_version

最低版本：

    GET /public/client/android/min_version

## 外部服务

旧客户端还使用两个非 PKU New Youth 服务：

天气：

    GET https://nmc.cn/rest/weather?stationid=fElIR

高德逆地理编码：

    GET https://restapi.amap.com/v3/geocode/regeo
    参数：output=json、extensions=base、location=经度,纬度、key、radius=1000

天气已迁移到 `feature/weather/data/WeatherRepository`，使用独立的 NMC Retrofit 客户端；跑步地图和连续定位使用高德 3D 地图/定位合包，定位结果直接采用 GCJ-02。

天气：

    GET https://nmc.cn/rest/weather?stationid=fElIR

高德地图：

    SDK: com.amap.api:3dmap-location-search:11.2.100_loc11.2.100_sea9.8.1
    Manifest key: com.amap.api.v2.apikey

构建时可在 `gradle.properties` 覆盖 `AMAP_API_KEY`。地图使用普通/夜景矢量底图；定位使用高德 `Sport` 运动场景。开跑时调用后台定位接口显示前台通知并持有有限时长唤醒锁，结束后关闭后台能力、停止通知并释放唤醒锁。

## curl 示例

仅用于查询自己账号、并且必须使用自己当前有效的登录 Token：

    curl.exe --request GET ^
      "https://pkunewyouth.pku.edu.cn/badge/user/<userId>" ^
      --header "Authorization: <raw-token>" ^
      --header "Platform: Android" ^
      --header "Manufacturer: Windows-curl" ^
      --header "ClientVersion: 2.0.0-dev"

不要在公共聊天、提交记录或 CI 日志中暴露真实 Token。
