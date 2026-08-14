# 事先问移动端

这是“事先问”的 Flutter 移动端工程，只包含 Android/iOS 客户端代码，复用项目根目录中的 Java 后端接口。

## 目录

- `lib/app`：应用入口、路由和全局状态
- `lib/core`：网络、主题、存储和通用组件
- `lib/data`：接口数据模型和数据仓库
- `lib/features`：按页面拆分的业务功能

## 本地运行

Android 模拟器默认访问 `http://10.0.2.2:8080/api`。真机通过 USB 调试时，可以先执行：

```powershell
adb reverse tcp:8080 tcp:8080
flutter run --dart-define=API_BASE_URL=http://127.0.0.1:8080/api
```

同一局域网也可以将 `API_BASE_URL` 换为电脑的局域网地址。

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://docs.flutter.dev/cookbook)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.
