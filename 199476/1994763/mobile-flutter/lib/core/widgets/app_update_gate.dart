import 'dart:async';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../app/providers.dart';
import '../../data/models/app_version_models.dart';
import 'app_message.dart';

class AppUpdateGate extends ConsumerStatefulWidget {
  const AppUpdateGate({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<AppUpdateGate> createState() => _AppUpdateGateState();
}

class _AppUpdateGateState extends ConsumerState<AppUpdateGate>
    with WidgetsBindingObserver {
  AppUpdateInfo? _update;
  PackageInfo? _packageInfo;
  bool _checking = false;
  bool _openingStore = false;
  int? _dismissedOptionalVersion;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((_) => _check());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_check());
    }
  }

  Future<void> _check() async {
    if (_checking) return;
    _checking = true;
    try {
      final packageInfo = _packageInfo ?? await PackageInfo.fromPlatform();
      _packageInfo = packageInfo;
      final currentVersionCode = int.tryParse(packageInfo.buildNumber) ?? 1;
      final result = await ref
          .read(repositoryProvider)
          .checkAppVersion(currentVersionCode);
      if (!mounted) return;
      if (!result.hasUpdate) {
        setState(() => _update = null);
        return;
      }
      if (!result.forceUpdate &&
          _dismissedOptionalVersion == result.latestVersionCode) {
        return;
      }
      setState(() => _update = result);
    } catch (_) {
      // 版本检查失败不能阻断用户正常使用，下一次回到前台时会再次检查。
    } finally {
      _checking = false;
    }
  }

  Future<void> _openStore() async {
    final update = _update;
    if (update == null || _openingStore) return;
    setState(() => _openingStore = true);
    try {
      final packageInfo = _packageInfo ?? await PackageInfo.fromPlatform();
      _packageInfo = packageInfo;
      var opened = false;
      if (Platform.isAndroid) {
        final marketUri = Uri(
          scheme: 'market',
          host: 'details',
          queryParameters: {'id': packageInfo.packageName},
        );
        try {
          opened = await launchUrl(
            marketUri,
            mode: LaunchMode.externalApplication,
          );
        } catch (_) {
          opened = false;
        }
      }
      if (!opened && update.downloadUrl.isNotEmpty) {
        opened = await launchUrl(
          Uri.parse(update.downloadUrl),
          mode: LaunchMode.externalApplication,
        );
      }
      if (!opened && mounted) {
        AppMessage.show(context, '暂时无法打开应用商店');
      }
    } catch (_) {
      if (mounted) AppMessage.show(context, '暂时无法打开应用商店');
    } finally {
      if (mounted) setState(() => _openingStore = false);
    }
  }

  void _later() {
    final update = _update;
    if (update == null || update.forceUpdate) return;
    setState(() {
      _dismissedOptionalVersion = update.latestVersionCode;
      _update = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final update = _update;
    return PopScope(
      canPop: update?.forceUpdate != true,
      child: Stack(
        children: [
          widget.child,
          if (update != null) ...[
            const Positioned.fill(child: ColoredBox(color: Color(0x73000000))),
            Positioned.fill(
              child: SafeArea(
                child: Center(
                  child: CupertinoTheme(
                    data: const CupertinoThemeData(
                      brightness: Brightness.light,
                    ),
                    child: CupertinoAlertDialog(
                      title: Text(
                        update.title,
                        style: const TextStyle(
                          color: CupertinoColors.black,
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      content: Padding(
                        padding: const EdgeInsets.only(top: 12),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '版本 ${update.latestVersionName}',
                              style: const TextStyle(
                                color: CupertinoColors.systemGrey,
                                fontSize: 12,
                              ),
                            ),
                            const SizedBox(height: 8),
                            ConstrainedBox(
                              constraints: BoxConstraints(
                                maxHeight:
                                    MediaQuery.sizeOf(context).height * 0.3,
                              ),
                              child: SingleChildScrollView(
                                child: Text(
                                  update.updateContent,
                                  style: const TextStyle(
                                    color: CupertinoColors.black,
                                    fontSize: 14,
                                    height: 1.5,
                                  ),
                                  textAlign: TextAlign.start,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      actions: [
                        if (!update.forceUpdate)
                          CupertinoDialogAction(
                            onPressed: _later,
                            child: const Text('稍后再说'),
                          ),
                        CupertinoDialogAction(
                          isDefaultAction: true,
                          onPressed: _openingStore ? null : _openStore,
                          child: Text(
                            _openingStore ? '正在打开…' : '立即更新',
                            style: const TextStyle(
                              color: CupertinoColors.destructiveRed,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
