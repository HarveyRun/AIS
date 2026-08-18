import 'package:url_launcher/url_launcher.dart';

abstract final class LegalLinks {
  static final privacyPolicy = Uri.parse('https://private.inlightus.com/');
  static final userAgreement = Uri.parse('https://agreement.inlightus.com/');

  static Future<bool> openPrivacyPolicy() {
    return launchUrl(privacyPolicy, mode: LaunchMode.externalApplication);
  }

  static Future<bool> openUserAgreement() {
    return launchUrl(userAgreement, mode: LaunchMode.externalApplication);
  }
}
