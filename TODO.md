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

# 降屎山 TODO

## 1. 结构与文档对齐
- [x] 对齐 `docs/engineering.md` 与实际模块：要么补齐 core/sdk 模块，要么更新文档为当前结构
- [ ] 梳理并标注“占位功能”与“已实现功能”，同步到 `docs/features.md`

## 2. 组合式 UI 真迁移
- [ ] 拆掉 Activity 里的 `AndroidView` 包装，迁移为纯 Compose Screen
- [ ] 替换 RecyclerView + Adapter 为 `LazyColumn`/`LazyRow`，删掉 `findViewById` 依赖
- [ ] 统一导航入口（Compose Nav 或单 Activity + 多 Screen）

## 3. 大类拆分与职责收敛
- [ ] 细化 `DefaultRssRepository`：拆出解析、缓存、离线、网络、去重模块
- [ ] 将 `DetailActivity` 逻辑迁移到 ViewModel + Compose，保留最小 Activity 容器
- [ ] 为复杂工具类加清晰边界（如内容解析、图片加载、滑动交互）

## 4. 风险点与稳定性
- [ ] 清理 `!!` 与隐式空指针路径，补空值兜底与错误提示
- [ ] 对第三方/非官方 API 加明确降级策略与开关
- [ ] 增加关键流程的错误日志与埋点（登录、刷新、离线下载）

## 5. 测试与验证
- [ ] 至少补齐 RSS 解析/去重/离线下载单测
- [ ] 为 Bili SDK 加 MockWebServer 回归测试
- [ ] 核心页面增加最小化 UI 自动化（启动、列表、详情）

## 6. 仓库卫生
- [ ] 清理/迁移敏感文件（`keystore.properties`、`release.jks`、`local.properties`）出仓库
- [ ] 增加 `.gitignore` 规则，移除 `build/`、`tmp/` 等生成物
