# 事先问 App 图标交付文件

本目录由 `tool/generate_brand_assets.ps1` 根据高分辨率母版统一生成。所有 PNG 均为正方形、无圆角、无透明通道，由操作系统或应用商店负责应用最终蒙版。

## 母版与通用尺寸

`universal/` 中保留 2048、1024、512、432、216、192、180、167、152、144、128、120、96、87、80、76、72、64、60、58、48、40、29、20 像素版本。

## Android 与应用商店

`android/` 中包含：

- Google Play：512×512
- 华为 AppGallery：216×216
- 通用商店备用：512×512、1024×1024
- Android Launcher：48、72、96、144、192 像素，对应 mdpi 至 xxxhdpi

Flutter Android 工程中的 `android/app/src/main/res/mipmap-*/ic_launcher.png` 已同步替换。

## Apple App Store

`ios/AppIcon.appiconset/` 是可直接用于 Xcode Asset Catalog 的完整默认图标集，包含：

- App Store 营销图标：1024×1024
- iPhone：40、58、60、80、87、120、180 像素
- iPad：20、29、40、58、76、80、152、167 像素
- `Contents.json`

Apple 图标均使用 24 位 RGB PNG，不包含 Alpha 透明通道。当前 Flutter 项目尚无 `ios/` 工程；后续创建 iOS 工程后，将整个 `AppIcon.appiconset` 放入 `Runner/Assets.xcassets/` 即可。

Apple 的深色与着色图标属于可选外观，不是默认图标上架的必需文件，因此本次未生成，以免改变品牌色。
