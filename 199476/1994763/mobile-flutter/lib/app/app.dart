import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';
import 'package:go_router/go_router.dart';

import '../core/theme/app_theme.dart';
import 'providers.dart';
import 'router.dart';

class ShixianwenApp extends ConsumerStatefulWidget {
  const ShixianwenApp({super.key});

  @override
  ConsumerState<ShixianwenApp> createState() => _ShixianwenAppState();
}

class _ShixianwenAppState extends ConsumerState<ShixianwenApp> {
  GoRouter? _router;

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authControllerProvider);
    final theme = ref.watch(themeControllerProvider);

    if (!auth.initialized || !theme.initialized) {
      return MaterialApp(
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light(),
        builder: FlutterSmartDialog.init(builder: _systemChromeBuilder),
        home: const Scaffold(
          body: Center(child: CircularProgressIndicator(strokeWidth: 2)),
        ),
      );
    }

    _router ??= createAppRouter(auth);
    return MaterialApp.router(
      title: '事先问',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: theme.dark ? ThemeMode.dark : ThemeMode.light,
      routerConfig: _router,
      builder: FlutterSmartDialog.init(builder: _systemChromeBuilder),
    );
  }

  Widget _systemChromeBuilder(BuildContext context, Widget? child) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final pageColor = Theme.of(context).scaffoldBackgroundColor;
    final statusColor = dark ? pageColor : Colors.white;
    final navigationColor = dark ? const Color(0xFF191A1D) : Colors.white;
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle(
        statusBarColor: statusColor,
        statusBarIconBrightness: dark ? Brightness.light : Brightness.dark,
        statusBarBrightness: dark ? Brightness.dark : Brightness.light,
        systemNavigationBarColor: navigationColor,
        systemNavigationBarIconBrightness: dark
            ? Brightness.light
            : Brightness.dark,
        systemNavigationBarDividerColor: Colors.transparent,
      ),
      child: child ?? const SizedBox.shrink(),
    );
  }
}
