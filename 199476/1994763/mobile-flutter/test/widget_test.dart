import 'package:flutter_test/flutter_test.dart';
import 'package:shixianwen_mobile/core/config/app_config.dart';

void main() {
  test('默认接口地址指向 Android 模拟器宿主机', () {
    expect(AppConfig.apiBaseUrl, contains('/api'));
  });
}
