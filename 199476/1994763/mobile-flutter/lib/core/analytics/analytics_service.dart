import 'dart:async';
import 'dart:io';
import 'dart:math';

import 'package:flutter/widgets.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../network/api_client.dart';
import '../storage/app_storage.dart';

class AnalyticsService with WidgetsBindingObserver {
  AnalyticsService(this._api, this._storage);

  static const _maxQueueSize = 500;
  static const _batchSize = 20;
  static const _maxAge = Duration(days: 7);

  final ApiClient _api;
  final AppStorage _storage;
  final Random _random = Random.secure();
  final List<Map<String, dynamic>> _queue = [];

  bool _initialized = false;
  bool _flushing = false;
  String _appVersion = '';
  String _sessionId = '';
  String? _currentPage;

  String get platform => Platform.isIOS ? 'ios' : 'android';

  Future<void> initialize() async {
    if (_initialized) return;
    _initialized = true;
    WidgetsBinding.instance.addObserver(this);
    final package = await PackageInfo.fromPlatform();
    _appVersion = '${package.version}+${package.buildNumber}';
    _sessionId = _randomId();
    final stored = await _storage.readAnalyticsEvents();
    final oldest = DateTime.now().subtract(_maxAge);
    _queue.addAll(
      stored.where((event) {
        final occurredAt = DateTime.tryParse('${event['occurredAt'] ?? ''}');
        return occurredAt != null && occurredAt.isAfter(oldest);
      }),
    );
    await track('app_open');
    unawaited(flush());
  }

  Future<void> track(
    String eventName, {
    String? pageName,
    String? sourcePage,
    Map<String, Object?> properties = const {},
    bool flushImmediately = false,
  }) async {
    if (!_initialized) return;
    final safeProperties = <String, Object>{};
    properties.forEach((key, value) {
      if (value != null) safeProperties[key] = value;
    });
    _queue.add({
      'eventId': _randomId(),
      'eventName': eventName,
      'eventVersion': 1,
      'sessionId': _sessionId,
      'pageName': pageName ?? _currentPage,
      'sourcePage': sourcePage,
      'occurredAt': DateTime.now().toUtc().toIso8601String(),
      'properties': safeProperties,
    });
    if (_queue.length > _maxQueueSize) {
      _queue.removeRange(0, _queue.length - _maxQueueSize);
    }
    await _persist();
    if (flushImmediately || _queue.length >= _batchSize) {
      unawaited(flush());
    }
  }

  Future<void> trackPage(String pageName) async {
    final previous = _currentPage;
    _currentPage = pageName;
    final specificEvent = switch (pageName) {
      'login' => 'login_page_view',
      'home' => 'home_view',
      'chat' => 'chat_open',
      'experienceCertifications' => 'experience_list_view',
      'faq' => 'faq_view',
      'customerService' => 'customer_service_open',
      _ => 'page_view',
    };
    await track(
      specificEvent,
      pageName: pageName,
      sourcePage: previous,
      properties: {'route_name': pageName},
    );
  }

  Future<void> flush() async {
    if (!_initialized || _flushing || _queue.isEmpty) return;
    final token = await _storage.readToken();
    if (token == null || token.isEmpty) return;
    _flushing = true;
    final batch = _queue
        .take(_batchSize)
        .map(Map<String, dynamic>.from)
        .toList();
    var hasMore = false;
    try {
      await _api.post<Map<String, dynamic>>(
        '/analytics/events',
        data: {
          'anonymousId': await _storage.readOrCreateDeviceId(),
          'platform': platform,
          'appVersion': _appVersion,
          'events': batch,
        },
        showLoading: false,
      );
      final ids = batch.map((item) => item['eventId']).toSet();
      _queue.removeWhere((item) => ids.contains(item['eventId']));
      await _persist();
      hasMore = _queue.isNotEmpty;
    } catch (_) {
      // 埋点失败留在本地，下次用户产生行为或App进入后台时再重试。
    } finally {
      _flushing = false;
    }
    if (hasMore) unawaited(flush());
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.inactive ||
        state == AppLifecycleState.detached) {
      unawaited(flush());
    }
  }

  Future<void> _persist() => _storage.writeAnalyticsEvents(_queue);

  String _randomId() {
    const alphabet =
        'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-';
    return List.generate(
      32,
      (_) => alphabet[_random.nextInt(alphabet.length)],
    ).join();
  }

  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
  }
}
