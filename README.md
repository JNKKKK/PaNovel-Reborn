# PaNovel

本项目基于原始 [PaNovel](https://github.com/AoEiuV020/PaNovel) 项目复活维护。

一款开源的 Android 小说阅读器，支持在线小说源和本地文件阅读。

## 功能

- 24 个在线小说源（笔趣阁系列、速读谷、夏雨书屋等）
- 本地 TXT / EPUB 文件阅读
- 可插拔的小说源系统（网站爬虫），易于新增
- WebDAV 备份与阅读进度同步
- 书架管理、书单、搜索
- 自定义阅读界面（字体、背景、边距、翻页动画等）
- 下载章节离线阅读

## 构建

需要 JDK 21。

```bash
./gradlew assembleDebug
```

## 技术栈

Kotlin 1.9 · Gradle 8.7 · AGP 8.3 · Room · Coroutines · kotlinx-serialization · JSoup · OkHttp 4 · Glide · Timber · AndroidX Preference

## 项目结构

| 模块 | 用途 |
|------|------|
| app | 主应用（Activity、Presenter、Fragment） |
| scraper | 小说网站爬虫（JSoup + Rhino） |
| core | 共享工具库（JSON、正则、SSL） |
| rhino | JavaScript 引擎封装 |
| local | 本地文件支持（TXT、EPUB） |
| reader | 阅读器 UI |
| pager | 翻页库 |
| IronDB | 文件型键值存储（kotlinx-serialization） |
| filepicker | 文件选择器 |
