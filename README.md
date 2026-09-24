## 全新的ui
宣纸风了，之后有空画点校园建筑剪影
动效ui，说不上加了好还是没加好，甚至记录的仪表还有点卡（卡的是动画而非性能负担）
没错我又把包名换了

## Android Studio 打包

1. 在 Android Studio 中打开当前 `OpenRunner` 目录（即直接包含 `settings.gradle` 的目录），不要打开上一层聚合目录。
2. Gradle JDK 选择 JDK 17，完成 Gradle Sync。
3. 选择 **Build > Build App Bundle(s) or APK(s) > Build APK(s)**。

Debug APK 会生成在 `app/build/outputs/apk/debug/app-debug.apk`。需要发布包时，使用 **Build > Generate Signed App Bundle or APK** 并选择自己的签名证书。
