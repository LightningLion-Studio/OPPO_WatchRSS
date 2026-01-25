# Compose Migration TODO

## Per-page flow
- [ ] For each migrated page, run: `./gradlew :app:installDebug`, then `adb shell am start -n com.lightningstudio.watchrss/.MainActivity`, then check logs with `adb logcat` until no errors.

## Pages to migrate (View -> Compose)
- [x] AddRssActivity
- [x] ChannelDetailActivity
- [x] MainActivity
- [x] ChannelSettingsActivity
- [x] ProfileActivity
- [x] ItemActionsActivity
- [x] WebViewActivity
- [x] ShareQrActivity
- [x] InfoActivity
- [x] ChannelActionsActivity
- [x] SettingsActivity
- [x] BiliEntryActivity
- [x] HeytapWidgetDemoActivity
- [x] SavedItemsActivity
- [x] FeedActivity
- [x] DouyinEntryActivity
- [x] RssRecommendGroupActivity
- [x] AboutActivity
- [x] RssRecommendActivity
- [x] DetailActivity

## Remaining non-Compose / View-based usage to remove or isolate
- [x] Replace `AndroidView` wrapper + XML in `MainActivity` with pure Compose UI + `LazyColumn`.
- [x] Replace `AndroidView` wrapper + XML in `FeedActivity` with pure Compose UI + `LazyColumn`.
- [x] Replace `AndroidView` wrapper + XML in `SavedItemsActivity` with pure Compose UI + `LazyColumn`.
- [x] Replace `AndroidView` wrapper + XML in `DetailActivity` with pure Compose UI container (keep only minimal Activity).
- [x] Remove RecyclerView/Adapter dependencies for main flows:
  - [x] `HomeEntryAdapter` -> Compose list item + swipe actions.
  - [x] `FeedEntryAdapter` -> Compose list item + swipe actions.
  - [x] `SavedItemAdapter` -> Compose list item + swipe actions.
  - [x] `RssRecommendAdapter` / `RssRecommendChannelAdapter` -> Compose list.
  - [x] `RssChannelAdapter` / `RssItemAdapter` / `SettingsEntryAdapter` / `CacheLimitAdapter` -> Compose list.
- [x] Remove or re-implement RecyclerView swipe helper utilities (`SwipeRevealCallback`, `SafeHeytapRecyclerView`) in Compose.

# 降屎山 TODO

## 1. 结构与文档对齐
- [x] 对齐 `docs/engineering.md` 与实际模块：要么补齐 core/sdk 模块，要么更新文档为当前结构
- [x] 梳理并标注“占位功能”与“已实现功能”，同步到 `docs/features.md`
  - [x] 核对云同步/账号同步等描述是否已实现；未实现的标注为规划或移至 roadmap。
  - [x] 标注 B站/抖音登录与播放的现状/限制，避免与实现不一致。
  - [x] 标注 RSS 阅读模式/HTML 渲染实现范围与差异。

## 2. 组合式 UI 真迁移
- [x] 拆掉 Activity 里的 `AndroidView` 包装，迁移为纯 Compose Screen
- [x] 替换 RecyclerView + Adapter 为 `LazyColumn`/`LazyRow`，删掉 `findViewById` 依赖
- [ ] 统一导航入口（Compose Nav 或单 Activity + 多 Screen）
  - [x] `MainActivity` -> Compose 列表 + 顶部/底部操作区。
  - [x] `FeedActivity` -> Compose 列表 + 刷新/加载更多。
  - [x] `SavedItemsActivity` -> Compose 列表 + 删除/撤销交互。
  - [ ] `DetailActivity` -> Compose 详情页（将内容渲染逻辑迁移 ViewModel）。

## 3. 大类拆分与职责收敛
- [ ] 细化 `DefaultRssRepository`：拆出解析、缓存、离线、网络、去重模块
- [x] 将 `DetailActivity` 逻辑迁移到 ViewModel + Compose，保留最小 Activity 容器
- [ ] 为复杂工具类加清晰边界（如内容解析、图片加载、滑动交互）
  - [ ] `DefaultRssRepository` 拆分建议：
    - [x] 网络拉取（HTTP/超时/UA）独立为 `RssFetchService`。
    - [x] RSS 解析与去重策略独立为 `RssParseService`。
    - [x] 原文抓取/HTML 可读性提取独立为 `RssReadableService`。
    - [x] 离线下载与缓存管理独立为 `RssOfflineStore`。
  - [ ] `DetailActivity` 拆出：阅读进度/恢复位置/分享动作/内容渲染缓存。

## 4. 风险点与稳定性
- [x] 清理 `!!` 与隐式空指针路径，补空值兜底与错误提示
- [x] 对第三方/非官方 API 加明确降级策略与开关
- [ ] 增加关键流程的错误日志与埋点（登录、刷新、离线下载）
  - [x] 移除 `DetailScreen` 中的 `!!`，以空态/占位内容替代。
  - [x] 离线媒体下载失败时记录错误并允许重试。
  - [x] 原文抓取失败需降级为 RSS 内容（并提示）。

## 5. 测试与验证
- [ ] 至少补齐 RSS 解析/去重/离线下载单测
  - [x] RSS 解析单测
  - [x] RSS 去重 key 单测
  - [x] RSS 离线下载单测
- [ ] 为 Bili SDK 加 MockWebServer 回归测试
- [ ] 核心页面增加最小化 UI 自动化（启动、列表、详情）
  - [ ] `DefaultRssRepository` 的去重/缓存策略单测。
  - [ ] 原文抽取/HTML 可读性逻辑单测（边界输入）。
  - [ ] Bili API 请求签名/鉴权流程单测 + MockWebServer。
  - [ ] Main/Feed/Detail 最小 UI 流程测试（打开/返回/加载状态）。

## 6. 仓库卫生
- [x] 清理/迁移敏感文件（`keystore.properties`、`release.jks`、`local.properties`）出仓库
- [x] 增加 `.gitignore` 规则，移除 `build/`、`tmp/` 等生成物
  - [x] 确认上述文件是否被 Git 追踪，若已追踪则从索引中移除。
  - [x] 清理已提交的 `build/`、`tmp/` 等生成物。

## 7. 配置与密钥管理
- [x] 将 Bili SDK `appKey/appSec` 等配置迁移到 `local.properties` 或安全配置文件，并在文档标明注入方式。
