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
