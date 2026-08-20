import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'dart:math';

class AppStorage {
  static const _tokenKey = 'access_token';
  static const _themeKey = 'theme_mode';
  static const _privacyConsentKey = 'privacy_consent_accepted';
  static const _deviceIdKey = 'device_id';

  const AppStorage({
    FlutterSecureStorage secureStorage = const FlutterSecureStorage(),
  }) : _secureStorage = secureStorage;

  final FlutterSecureStorage _secureStorage;

  Future<String?> readToken() => _secureStorage.read(key: _tokenKey);

  Future<void> writeToken(String token) {
    return _secureStorage.write(key: _tokenKey, value: token);
  }

  Future<void> deleteToken() => _secureStorage.delete(key: _tokenKey);

  Future<String> readOrCreateDeviceId() async {
    final existing = await _secureStorage.read(key: _deviceIdKey);
    if (existing != null && existing.length >= 8) return existing;
    final random = Random.secure();
    final bytes = List<int>.generate(24, (_) => random.nextInt(256));
    final value = base64UrlEncode(bytes).replaceAll('=', '');
    await _secureStorage.write(key: _deviceIdKey, value: value);
    return value;
  }

  Future<bool> readDarkMode() async {
    final preferences = await SharedPreferences.getInstance();
    return preferences.getString(_themeKey) == 'dark';
  }

  Future<void> writeDarkMode(bool enabled) async {
    final preferences = await SharedPreferences.getInstance();
    await preferences.setString(_themeKey, enabled ? 'dark' : 'light');
  }

  Future<bool> readPrivacyConsent() async {
    final preferences = await SharedPreferences.getInstance();
    return preferences.getBool(_privacyConsentKey) ?? false;
  }

  Future<void> writePrivacyConsent(bool accepted) async {
    final preferences = await SharedPreferences.getInstance();
    await preferences.setBool(_privacyConsentKey, accepted);
  }
}
