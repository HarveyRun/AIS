import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppStorage {
  static const _tokenKey = 'access_token';
  static const _themeKey = 'theme_mode';
  static const _privacyConsentKey = 'privacy_consent_accepted';

  const AppStorage({
    FlutterSecureStorage secureStorage = const FlutterSecureStorage(),
  }) : _secureStorage = secureStorage;

  final FlutterSecureStorage _secureStorage;

  Future<String?> readToken() => _secureStorage.read(key: _tokenKey);

  Future<void> writeToken(String token) {
    return _secureStorage.write(key: _tokenKey, value: token);
  }

  Future<void> deleteToken() => _secureStorage.delete(key: _tokenKey);

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
