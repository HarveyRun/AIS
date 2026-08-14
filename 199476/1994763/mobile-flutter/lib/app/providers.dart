import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/network/api_client.dart';
import '../core/network/realtime_service.dart';
import '../core/storage/app_storage.dart';
import '../data/models/user_models.dart';
import '../data/repositories/app_repository.dart';

final storageProvider = Provider<AppStorage>((ref) => const AppStorage());

final apiClientProvider = Provider<ApiClient>((ref) {
  final client = ApiClient(ref.read(storageProvider));
  ref.onDispose(client.dispose);
  return client;
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
final inquiryUnreadCountProvider = StateProvider<int>((ref) => 0);
final customerServiceUnreadProvider = StateProvider<int>((ref) => 0);

class AuthController extends ChangeNotifier {
  AuthController({
    required AppStorage storage,
    required AppRepository repository,
    required ApiClient apiClient,
    required RealtimeService realtime,
  }) : _storage = storage,
       _repository = repository,
       _apiClient = apiClient,
       _realtime = realtime;

  final AppStorage _storage;
  final AppRepository _repository;
  final ApiClient _apiClient;
  final RealtimeService _realtime;
  StreamSubscription<void>? _unauthorizedSubscription;

  AppUser? user;
  bool initialized = false;
  bool busy = false;

  bool get signedIn => user != null;

  Future<void> initialize() async {
    _unauthorizedSubscription = _apiClient.unauthorizedEvents.listen((_) {
      user = null;
      _realtime.disconnect();
      notifyListeners();
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
      final result = await _repository.login(phone, code);
      await _storage.writeToken(result.token);
      user = result.user;
      await _realtime.connect();
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
    try {
      await _repository.logout();
    } finally {
      await _storage.deleteToken();
      await _realtime.disconnect();
      user = null;
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

  @override
  void dispose() {
    _unauthorizedSubscription?.cancel();
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
