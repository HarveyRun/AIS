import 'package:flutter/material.dart';

abstract final class AppColors {
  static const primary = Color(0xFFC23B32);
  static const primaryPressed = Color(0xFF8F2924);
  static const primarySoft = Color(0xFFF5F3F0);
  static const page = Color(0xFFFFFFFF);
  static const surface = Color(0xFFFFFFFF);
  static const surfaceSubtle = Color(0xFFF5F3F0);
  static const border = Color(0xFFE5DDD5);
  static const text = Color(0xFF312C28);
  static const textSecondary = Color(0xFF8F847C);
  static const success = Color(0xFF65A27A);
  static const warning = Color(0xFFB36B18);
  static const darkPage = Color(0xFF121315);
  static const darkSurface = Color(0xFF1A1B1F);
  static const darkSurfaceSubtle = Color(0xFF26272D);
  static const darkBorder = Color(0xFF303238);
}

abstract final class AppTheme {
  static ThemeData light() => _build(Brightness.light);
  static ThemeData dark() => _build(Brightness.dark);

  static ThemeData _build(Brightness brightness) {
    final dark = brightness == Brightness.dark;
    final baseScheme = ColorScheme.fromSeed(
      seedColor: AppColors.primary,
      brightness: brightness,
      primary: AppColors.primary,
      surface: dark ? AppColors.darkSurface : AppColors.surface,
    );
    final textColor = dark ? const Color(0xFFF2F2F3) : AppColors.text;
    final secondary = dark ? const Color(0xFFA8A8B0) : AppColors.textSecondary;
    final border = dark ? AppColors.darkBorder : AppColors.border;
    final scheme = baseScheme.copyWith(
      surface: dark ? AppColors.darkSurface : AppColors.page,
      surfaceDim: dark ? AppColors.darkPage : const Color(0xFFF5F3F0),
      surfaceBright: dark ? AppColors.darkSurfaceSubtle : AppColors.surface,
      surfaceContainerLowest: dark ? AppColors.darkPage : AppColors.surface,
      surfaceContainerLow: dark
          ? const Color(0xFF17181B)
          : const Color(0xFFFFFDF9),
      surfaceContainer: dark
          ? AppColors.darkSurfaceSubtle
          : AppColors.surfaceSubtle,
      surfaceContainerHigh: dark
          ? const Color(0xFF212226)
          : const Color(0xFFF5F3F0),
      surfaceContainerHighest: dark
          ? AppColors.darkSurfaceSubtle
          : const Color(0xFFE9E4DE),
      onSurface: textColor,
      onPrimary: Colors.white,
      outline: border,
      outlineVariant: border,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: scheme,
      scaffoldBackgroundColor: dark ? AppColors.darkPage : AppColors.page,
      dividerColor: border,
      splashFactory: InkRipple.splashFactory,
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: _OpaqueFadePageTransitionsBuilder(),
          TargetPlatform.iOS: _OpaqueFadePageTransitionsBuilder(),
          TargetPlatform.macOS: _OpaqueFadePageTransitionsBuilder(),
          TargetPlatform.windows: _OpaqueFadePageTransitionsBuilder(),
          TargetPlatform.linux: _OpaqueFadePageTransitionsBuilder(),
        },
      ),
      textTheme: TextTheme(
        displaySmall: TextStyle(
          fontSize: 26,
          fontWeight: FontWeight.w700,
          color: textColor,
        ),
        headlineLarge: TextStyle(
          fontSize: 21,
          fontWeight: FontWeight.w700,
          color: textColor,
        ),
        headlineMedium: TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.w700,
          color: textColor,
        ),
        titleLarge: TextStyle(
          fontSize: 17,
          fontWeight: FontWeight.w700,
          color: textColor,
        ),
        titleMedium: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w600,
          color: textColor,
        ),
        bodyLarge: TextStyle(fontSize: 13, height: 1.42, color: textColor),
        bodyMedium: TextStyle(fontSize: 11, height: 1.42, color: textColor),
        bodySmall: TextStyle(fontSize: 9, height: 1.38, color: secondary),
        labelLarge: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: textColor,
        ),
        labelMedium: TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.w500,
          color: secondary,
        ),
      ),
      appBarTheme: AppBarTheme(
        elevation: 0,
        scrolledUnderElevation: 0,
        toolbarHeight: 64,
        centerTitle: true,
        backgroundColor: dark ? AppColors.darkPage : AppColors.page,
        surfaceTintColor: Colors.transparent,
        foregroundColor: textColor,
        titleTextStyle: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w700,
          color: textColor,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: dark ? AppColors.darkSurfaceSubtle : AppColors.surfaceSubtle,
        hintStyle: TextStyle(color: secondary),
        labelStyle: TextStyle(color: secondary),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 14,
          vertical: 11,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.primary, width: 1),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          elevation: 0,
          minimumSize: const Size.fromHeight(46),
          foregroundColor: Colors.white,
          backgroundColor: AppColors.primary,
          disabledBackgroundColor: dark
              ? AppColors.darkSurfaceSubtle
              : const Color(0xFFE7E7E9),
          disabledForegroundColor: secondary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
          textStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          elevation: 0,
          foregroundColor: Colors.white,
          backgroundColor: AppColors.primary,
          disabledBackgroundColor: dark
              ? AppColors.darkSurfaceSubtle
              : const Color(0xFFE7E7E9),
          disabledForegroundColor: secondary,
          minimumSize: const Size.fromHeight(44),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 11),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(13),
          ),
          textStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(44),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 11),
          side: BorderSide(color: border),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(13),
          ),
          textStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
        ),
      ),
      tabBarTheme: TabBarThemeData(
        dividerColor: Colors.transparent,
        labelColor: AppColors.primary,
        unselectedLabelColor: secondary,
        labelStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
        unselectedLabelStyle: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w500,
        ),
        indicatorColor: AppColors.primary,
        indicatorSize: TabBarIndicatorSize.label,
      ),
      listTileTheme: ListTileThemeData(
        dense: true,
        minVerticalPadding: 6,
        iconColor: secondary,
        titleTextStyle: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w500,
          color: textColor,
        ),
        subtitleTextStyle: TextStyle(
          fontSize: 9,
          height: 1.4,
          color: secondary,
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: dark
            ? AppColors.darkSurfaceSubtle
            : AppColors.surfaceSubtle,
        selectedColor: dark
            ? AppColors.darkSurfaceSubtle
            : const Color(0xFFEDEDEC),
        disabledColor: dark
            ? AppColors.darkSurfaceSubtle
            : AppColors.surfaceSubtle,
        side: BorderSide(color: border),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(9)),
        labelStyle: TextStyle(fontSize: 10, color: textColor),
        secondaryLabelStyle: const TextStyle(
          fontSize: 10,
          color: AppColors.primary,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 3, vertical: 0),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: dark ? AppColors.darkSurface : AppColors.page,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: secondary,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        margin: EdgeInsets.zero,
        color: dark ? AppColors.darkSurface : AppColors.surface,
        shape: RoundedRectangleBorder(
          side: BorderSide(color: border),
          borderRadius: BorderRadius.circular(18),
        ),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: dark ? AppColors.darkSurface : AppColors.page,
        modalBackgroundColor: dark ? AppColors.darkSurface : AppColors.page,
        showDragHandle: false,
        dragHandleColor: secondary.withValues(alpha: .45),
        surfaceTintColor: Colors.transparent,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: dark ? AppColors.darkSurface : AppColors.page,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      ),
    );
  }
}

class _OpaqueFadePageTransitionsBuilder extends PageTransitionsBuilder {
  const _OpaqueFadePageTransitionsBuilder();

  @override
  Widget buildTransitions<T>(
    PageRoute<T> route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    if (route.settings.name == Navigator.defaultRouteName) return child;
    final opacity = CurvedAnimation(
      parent: animation,
      curve: const Interval(0, .55, curve: Curves.easeOut),
      reverseCurve: const Interval(.45, 1, curve: Curves.easeIn),
    );
    return ColoredBox(
      color: Theme.of(context).scaffoldBackgroundColor,
      child: FadeTransition(opacity: opacity, child: child),
    );
  }
}
