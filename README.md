# PaNovel

本项目基于原始 [PaNovel](https://github.com/AoEiuV020/PaNovel) 项目复活维护。

一款开源的 Android 小说阅读器，支持在线小说源和本地文件阅读。

## 功能

- 24 个在线小说源（笔趣阁系列、速读谷、夏雨书屋等）
- 本地 TXT / EPUB 文件阅读
- 可插拔的小说源系统（网站爬虫），易于新增
- 书源可用性面板：每个书源展示最近的可达性状态条（绿=可用、黄=重试恢复、红=不可用），数据来自本地每日探测
- 备份与恢复（书架 / 书单 / 历史 / 设置，含已缓存章节，导出 / 导入到本地文件）
- 书架管理、书单、搜索
- 自定义阅读界面（字体、背景、边距、翻页动画等）
- 下载章节离线阅读
- 阅读时长按查词：弹出内置词典（超级新华字典）的拼音与释义，支持贪婪匹配多字词/成语

## 构建

需要 JDK 17+。

```bash
./gradlew assembleDebug
```

## 技术栈

Kotlin 2.4 · Gradle 9.6 · AGP 9.3 · compileSdk 36 · Room · Coroutines · kotlinx-serialization · JSoup · OkHttp 5 · Glide · Timber · SLF4J · AndroidX Preference

## 项目结构

| 模块 | 用途 |
|------|------|
| app | 主应用（Activity、Presenter、Fragment） |
| scraper | 小说网站爬虫（JSoup） |
| shared | 共享工具库（JSoup 辅助、JSON、正则、SSL） |
| bookfile | 本地文件格式（TXT、EPUB 解析与导出） |
| mdict | MDX（MDict）词典读取（长按查词） |
| reader | 阅读器 UI |
| pager | 翻页库 |
| IronDB | 文件型键值存储（kotlinx-serialization） |
