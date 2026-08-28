import 'dart:async';

import 'package:flutter/widgets.dart';

import 'analytics_service.dart';

class AnalyticsNavigatorObserver extends NavigatorObserver {
  AnalyticsNavigatorObserver(this.analytics);

  final AnalyticsService analytics;

  @override
  void didPush(Route<dynamic> route, Route<dynamic>? previousRoute) {
    super.didPush(route, previousRoute);
    _track(route);
  }

  @override
  void didReplace({Route<dynamic>? newRoute, Route<dynamic>? oldRoute}) {
    super.didReplace(newRoute: newRoute, oldRoute: oldRoute);
    if (newRoute != null) _track(newRoute);
  }

  @override
  void didPop(Route<dynamic> route, Route<dynamic>? previousRoute) {
    super.didPop(route, previousRoute);
    if (previousRoute != null) _track(previousRoute);
  }

  void _track(Route<dynamic> route) {
    final name = route.settings.name;
    if (name != null && name.isNotEmpty) {
      unawaited(analytics.trackPage(name));
    }
  }
}
