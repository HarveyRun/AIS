import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/network/api_client.dart';
import '../core/analytics/analytics_service.dart';
import '../core/network/realtime_service.dart';
import '../core/network/request_loading_controller.dart';
import '../core/storage/app_storage.dart';
import '../data/models/user_models.dart';
import '../data/repositories/app_repository.dart';

final storageProvider = Provider<AppStorage>((ref) => const AppStorage());

final requestLoadingProvider = ChangeNotifierProvider<RequestLoadingController>(
  (ref) {
    return RequestLoadingController();
  },
);

final apiClientProvider = Provider<ApiClient>((ref) {
  final client = ApiClient(
    ref.read(storageProvider),
    ref.read(requestLoadingProvider),
  );
  ref.onDispose(client.dispose);
  return client;
});

final analyticsProvider = Provider<AnalyticsService>((ref) {
  final service = AnalyticsService(
    ref.read(apiClientProvider),
    ref.read(storageProvider),
  );
  ref.onDispose(service.dispose);
  return service;
});

final repositoryProvider = Provider<AppRepository>(
  (ref) => AppRepository(ref.read(apiClientProvider)),
);

final realtimeProvider = Provider<RealtimeService>((ref) {
  final service = RealtimeService(ref.read(repositoryProvider));
  ref.onDispose(service.dispose);
  return service;
});

final authControllerProvider = ChangeNotifierProvider<AuthController>((ref) {
  final controller = AuthController(
    storage: ref.read(storageProvider),
    repository: ref.read(repositoryProvider),
    apiClient: ref.read(apiClientProvider),
    realtime: ref.read(realtimeProvider),
    analytics: ref.read(analyticsProvider),
    requestLoading: ref.read(requestLoadingProvider),
  );
  controller.initialize();
  ref.onDispose(controller.dispose);
  return controller;
});

final themeControllerProvider = ChangeNotifierProvider<ThemeController>((ref) {
  final controller = ThemeController(ref.read(storageProvider));
  controller.initialize();
  return controller;
});

final notificationCountProvider = StateProvider<int>((ref) => 0);
final notificationPagePresenceProvider = Provider(
  (ref) => NotificationPagePresence(),
);
final inquiryUnreadCountProvider = StateProvider<int>((ref) => 0);
final customerServiceUnreadProvider = StateProvider<int>((ref) => 0);

class NotificationPagePresence {
  bool _visible = false;

  bool get visible => _visible;

  void enter() {
    _visible = true;
  }

  void leave() {
    _visible = false;
  }
}

class AuthController extends ChangeNotifier {
  AuthController({
    required AppStorage storage,
    required AppRepository repository,
    required ApiClient apiClient,
    required RealtimeService realtime,
    required AnalyticsService analytics,
    required RequestLoadingController requestLoading,
  }) : _storage = storage,
       _repository = repository,
       _apiClient = apiClient,
       _realtime = realtime,
       _analytics = analytics,
       _requestLoading = requestLoading;

  final AppStorage _storage;
  final AppRepository _repository;
  final ApiClient _apiClient;
  final RealtimeService _realtime;
  final AnalyticsService _analytics;
  final RequestLoadingController _requestLoading;
  StreamSubscription<void>? _unauthorizedSubscription;
  StreamSubscription<AccountPenaltyNotice>? _penaltySubscription;
  StreamSubscription<RealtimeEvent>? _realtimeSubscription;

  AppUser? user;
  AccountPenaltyNotice? penaltyNotice;
  bool initialized = false;
  bool busy = false;

  bool get signedIn => user != null;

  Future<void> initialize() async {
    _unauthorizedSubscription = _apiClient.unauthorizedEvents.listen((_) {
      user = null;
      _realtime.disconnect();
      notifyListeners();
    });
    _penaltySubscription = _apiClient.accountPenaltyEvents.listen((notice) {
      unawaited(_applyPenalty(notice));
    });
    _realtimeSubscription = _realtime.events.listen((event) {
      if (event.type != 'ACCOUNT_PENALTY') return;
      unawaited(_applyPenalty(AccountPenaltyNotice.fromJson(event.payload)));
    });
    final token = await _storage.readToken();
    if (token != null && token.isNotEmpty) {
      try {
        user = await _repository.me();
        await _realtime.connect();
      } catch (_) {
        await _storage.deleteToken();
      }
    }
    initialized = true;
    notifyListeners();
  }

  Future<void> sendCode(String phone) =>
      _repository.sendVerificationCode(phone);

  Future<void> login(String phone, String code) async {
    busy = true;
    notifyListeners();
    try {
      await _requestLoading.run(() async {
        await _analytics.track('login_submit');
        final result = await _repository.login(phone, code);
        await _storage.writeToken(result.token);
        penaltyNotice = null;
        user = result.user;
        await _realtime.connect();
        await _analytics.flush();
      });
    } finally {
      busy = false;
      notifyListeners();
    }
  }

  Future<AppUser> refreshUser() async {
    final latest = await _repository.me();
    user = latest;
    notifyListeners();
    return latest;
  }

  void replaceUser(AppUser value) {
    user = value;
    notifyListeners();
  }

  Future<void> logout() async {
    if (busy) return;
    busy = true;
    notifyListeners();
    try {
      await _requestLoading.run(() async {
        try {
          await _analytics.track('logout_success');
          await _analytics.flush();
          await _repository.logout();
        } catch (_) {
          // 退出登录以清除本地登录态为准，服务端暂时不可用时也应正常退出。
        } finally {
          await _storage.deleteToken();
          await _realtime.disconnect();
          user = null;
          notifyListeners();
        }
      });
    } finally {
      busy = false;
      notifyListeners();
    }
  }

  Future<void> deleteAccount() async {
    await _repository.deleteAccount();
    await _storage.deleteToken();
    await _realtime.disconnect();
    user = null;
    notifyListeners();
  }

  Future<void> dismissPlatformIntroduction() async {
    user = await _repository.dismissPlatformIntroduction();
    notifyListeners();
  }

  void acknowledgePenalty() {
    penaltyNotice = null;
    notifyListeners();
  }

  Future<void> _applyPenalty(AccountPenaltyNotice notice) async {
    penaltyNotice = notice;
    user = null;
    notifyListeners();
    await _storage.deleteToken();
    await _realtime.disconnect();
  }

  @override
  void dispose() {
    _unauthorizedSubscription?.cancel();
    _penaltySubscription?.cancel();
    _realtimeSubscription?.cancel();
    super.dispose();
  }
}

class ThemeController extends ChangeNotifier {
  ThemeController(this._storage);

  final AppStorage _storage;
  bool dark = false;
  bool initialized = false;

  Future<void> initialize() async {
    dark = await _storage.readDarkMode();
    initialized = true;
    notifyListeners();
  }

  Future<void> toggle() async {
    dark = !dark;
    notifyListeners();
    await _storage.writeDarkMode(dark);
  }
}
