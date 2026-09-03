import 'package:go_router/go_router.dart';

import '../features/answerer/answerer_detail_page.dart';
import '../features/auth/login_page.dart';
import '../features/certification/basic_certification_page.dart';
import '../features/certification/basic_certification_apply_page.dart';
import '../features/certification/experience_certification_page.dart';
import '../features/certification/experience_form_page.dart';
import '../features/certification/public_welfare_experience_form_page.dart';
import '../features/certification/monetized_experience_form_page.dart';
import '../features/home/home_page.dart';
import '../features/inquiry/chat_page.dart';
import '../features/inquiry/inquiries_page.dart';
import '../features/notifications/notifications_page.dart';
import '../features/profile/account_settings_page.dart';
import '../features/profile/inquiry_settings_page.dart';
import '../features/profile/profile_page.dart';
import '../features/support/business_page.dart';
import '../features/support/customer_service_page.dart';
import '../features/support/faq_page.dart';
import '../features/support/feedback_page.dart';
import '../features/wallet/wallet_page.dart';
import '../data/models/certification_models.dart';
import 'providers.dart';
import '../core/analytics/analytics_navigator_observer.dart';
import '../core/analytics/analytics_service.dart';
import 'app_route_observer.dart';
import 'shell_page.dart';

GoRouter createAppRouter(AuthController auth, AnalyticsService analytics) {
  return GoRouter(
    observers: [AnalyticsNavigatorObserver(analytics), appRouteObserver],
    initialLocation: '/home',
    refreshListenable: auth,
    redirect: (context, state) {
      final publicPath = state.matchedLocation == '/login';
      if (!auth.signedIn && !publicPath) return '/login';
      if (auth.signedIn && state.matchedLocation == '/login') return '/home';
      return null;
    },
    routes: [
      GoRoute(
        name: 'login',
        path: '/login',
        builder: (context, state) => const LoginPage(),
      ),
      ShellRoute(
        builder: (context, state, child) =>
            AppShellPage(location: state.uri.path, child: child),
        routes: [
          GoRoute(
            name: 'home',
            path: '/home',
            builder: (context, state) => const HomePage(),
          ),
          GoRoute(
            name: 'inquiries',
            path: '/inquiries',
            builder: (context, state) => const InquiriesPage(),
          ),
          GoRoute(
            name: 'profile',
            path: '/profile',
            builder: (context, state) => const ProfilePage(),
          ),
          GoRoute(
            name: 'answererDetail',
            path: '/answerers/:uid',
            builder: (context, state) => AnswererDetailPage(
              uid: state.pathParameters['uid']!,
              experienceCertificationId: int.tryParse(
                state.uri.queryParameters['experienceId'] ?? '',
              ),
            ),
          ),
          GoRoute(
            name: 'notifications',
            path: '/notices',
            builder: (context, state) => const NotificationsPage(),
          ),
          GoRoute(
            name: 'accountSettings',
            path: '/profile/settings',
            builder: (context, state) => const AccountSettingsPage(),
          ),
          GoRoute(
            name: 'inquirySettings',
            path: '/profile/inquiry-settings',
            builder: (context, state) => const InquirySettingsPage(),
          ),
          GoRoute(
            name: 'wallet',
            path: '/profile/wallet',
            builder: (context, state) => const WalletPage(),
          ),
          GoRoute(
            name: 'basicCertification',
            path: '/profile/certifications/basic',
            builder: (context, state) => const BasicCertificationPage(),
          ),
          GoRoute(
            name: 'basicCertificationApply',
            path: '/profile/certifications/basic/IDENTITY/apply',
            builder: (context, state) => BasicCertificationApplyPage(
              record: state.extra as CertificationRecord?,
            ),
          ),
          GoRoute(
            name: 'experienceCertifications',
            path: '/profile/certifications/experiences',
            builder: (context, state) => const ExperienceCertificationPage(),
          ),
          GoRoute(
            name: 'publicWelfareExperienceCreate',
            path: '/profile/certifications/experiences/public-welfare/new',
            builder: (context, state) =>
                const PublicWelfareExperienceFormPage(),
          ),
          GoRoute(
            name: 'publicWelfareExperienceDetail',
            path: '/profile/certifications/experiences/public-welfare/:id',
            builder: (context, state) => PublicWelfareExperienceFormPage(
              id: int.parse(state.pathParameters['id']!),
            ),
          ),
          GoRoute(
            name: 'monetizedExperienceCreate',
            path: '/profile/certifications/experiences/monetized/new',
            builder: (context, state) => MonetizedExperienceFormPage(
              upgradeSourceId: int.tryParse(
                state.uri.queryParameters['upgradeSourceId'] ?? '',
              ),
            ),
          ),
          GoRoute(
            name: 'monetizedExperienceDetail',
            path: '/profile/certifications/experiences/monetized/:id',
            builder: (context, state) => MonetizedExperienceFormPage(
              id: int.parse(state.pathParameters['id']!),
            ),
          ),
          GoRoute(
            name: 'experienceCreateLegacy',
            path: '/profile/certifications/experiences/new',
            builder: (context, state) => const ExperienceFormPage(
              businessType: ExperienceBusinessType.monetized,
            ),
          ),
          GoRoute(
            name: 'experienceDetailLegacy',
            path: '/profile/certifications/experiences/:id',
            builder: (context, state) => ExperienceFormPage(
              id: int.parse(state.pathParameters['id']!),
              businessType: ExperienceBusinessType.monetized,
            ),
          ),
          GoRoute(
            name: 'feedback',
            path: '/profile/feedback',
            builder: (context, state) => const FeedbackPage(),
          ),
          GoRoute(
            name: 'faq',
            path: '/profile/faq',
            builder: (context, state) => const FaqPage(),
          ),
          GoRoute(
            name: 'business',
            path: '/profile/business',
            builder: (context, state) => const BusinessPage(),
          ),
        ],
      ),
      GoRoute(
        name: 'chat',
        path: '/chat/:id',
        builder: (context, state) =>
            ChatPage(id: int.parse(state.pathParameters['id']!)),
      ),
      GoRoute(
        name: 'customerService',
        path: '/profile/customer-service',
        builder: (context, state) => const CustomerServicePage(),
      ),
    ],
  );
}
