import 'package:flutter_test/flutter_test.dart';
import 'package:shixianwen_mobile/core/config/app_config.dart';
import 'package:shixianwen_mobile/core/legal/legal_links.dart';

void main() {
  test('默认接口地址包含 API 前缀', () {
    expect(AppConfig.apiBaseUrl, contains('/api'));
  });

  test('协议地址使用指定的线上页面', () {
    expect(
      LegalLinks.privacyPolicy.toString(),
      'https://private.inlightus.com/',
    );
    expect(
      LegalLinks.userAgreement.toString(),
      'https://agreement.inlightus.com/',
    );
  });
}
