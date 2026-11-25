# LexiNote

**Offline English Reading & Phonetic Annotation App**

LexiNote 是一款面向英语阅读的离线 Android 应用。它希望把文章阅读、发音标注和词汇积累放在一个简单的本地工作流中，减少阅读时频繁切换工具的需要。

> 当前仓库处于第一阶段开源发布准备中，尚未正式发布公开安装包。

## 下载 Android APK

**[前往 Gitee Releases 下载 Android APK（发布后启用）](#)**

目前尚无 Gitee Release 或公开 APK。待 Release 上传成功后，请将此处占位链接替换为真实下载地址。普通用户安装已发布的 APK 不需要安装 Android Studio。

## 核心功能

- 创建、编辑、阅读和管理英语文章。
- 标准模式：点击单词查看完整的离线词典释义及词形信息。
- 快速音标模式：点击单词直接保留或取消音标标注。
- 在原文上方显示音标，并以音标、原文、译文三行方式排版阅读。
- 使用离线英汉词典查询，并查询英语词形变化。
- 收藏单词、查看浏览记录，并在生词库中复习。
- 文章、音标标注和词汇记录保存在设备本地；Room 使用 SQLite 存储。

## 截图

真实截图计划存放于 [`docs/screenshots/`](docs/screenshots/)。目前尚未加入截图素材，以下位置待后续补充：

| 界面 | 截图 |
| --- | --- |
| 首页 | 待补充：`docs/screenshots/home.png` |
| 阅读器标准模式 | 待补充：`docs/screenshots/reader-standard.png` |
| 阅读器快速模式 | 待补充：`docs/screenshots/reader-quick.png` |
| 离线词典弹窗 | 待补充：`docs/screenshots/dictionary.png` |
| 生词库 | 待补充：`docs/screenshots/vocabulary.png` |

## 安装与使用

Release 发布后，在项目 Releases 页面下载适用于 Android 的 APK，在手机上打开并按系统提示安装；无需 Android Studio。Android 版本要求以正式构建的 APK 配置为准（当前项目最低 SDK 为 Android 8.0 / API 26）。

首次使用可创建文章并进入阅读器；标准模式点击单词查看词典，快速音标模式点击单词切换该处音标标注。收藏的词汇可在生词库中查看和复习。

## 技术栈

- Kotlin
- Android Jetpack Compose / Material 3
- AndroidX Lifecycle、ViewModel、Kotlin Coroutines/Flow
- Room（本地 SQLite 持久化）与 KSP
- JUnit、AndroidX Instrumentation/UI 测试依赖

## 开发与构建

使用 Android Studio 打开仓库中的 `android/` 工程，并配置兼容的 Android SDK、JDK 与 Android Gradle Plugin 环境。正常情况下可在 Android Studio 同步 Gradle 后运行 `app` 配置。

Gradle Wrapper 使用官方 Gradle 9.6.0 分发地址：`https://services.gradle.org/distributions/gradle-9.6.0-bin.zip`。请使用与 Android Gradle Plugin、Android SDK 要求兼容的 JDK 和 Android Studio 环境；项目构建尚未在其他开发环境验证。

## 项目结构

```text
.
├── android/                   # Android Studio 工程
│   └── app/src/main/
│       ├── assets/             # 离线词典及第三方数据声明
│       └── java/com/lexinote/  # Kotlin 应用源码
├── docs/screenshots/           # 项目截图（待补充）
├── CHANGELOG.md                # 阶段开发记录
├── LICENSE                     # LexiNote 原创代码许可证
└── THIRD_PARTY_NOTICES.md      # 第三方数据声明
```

## 后续计划

- 支持 TXT、Markdown、Word 文件导入。
- 探索 AI 语境释义与 AI 文章生成。
- 补充真实界面截图，完善公开构建配置并发布 APK。

以上为规划，尚未实现。

## 第三方词典数据与许可证

- ECDICT：[https://github.com/skywind3000/ECDICT](https://github.com/skywind3000/ECDICT)（MIT License）。
- UniMorph English：[https://github.com/unimorph/eng](https://github.com/unimorph/eng)（CC BY-SA 3.0）。

项目自有且有权授权的原创程序代码依根目录 [MIT License](LICENSE) 授权。该许可不覆盖第三方词典数据；详见[第三方数据详细声明](THIRD_PARTY_NOTICES.md)。

## 贡献与问题反馈

欢迎通过 Gitee 项目 Issues 反馈问题、提出建议或讨论贡献。仓库公开地址确定后，请在此补充实际项目链接。提交贡献前请说明变更内容，并确认所提交内容具备相应授权。
