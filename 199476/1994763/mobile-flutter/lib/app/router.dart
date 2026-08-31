import 'package:go_router/go_router.dart';

import '../features/answerer/answerer_detail_page.dart';
import '../features/auth/login_page.dart';
import '../features/certification/basic_certification_page.dart';
import '../features/certification/basic_certification_apply_page.dart';
import '../features/certification/certification_home_page.dart';
import '../features/certification/experience_certification_page.dart';
import '../features/certification/experience_form_page.dart';
import '../features/contribution/content_contribution_detail_page.dart';
import '../features/contribution/content_contribution_form_page.dart';
import '../features/contribution/content_contribution_page.dart';
import '../features/discovery/discovery_list_page.dart';
import '../features/discovery/discovery_results_page.dart';
import '../features/home/home_page.dart';
import '../features/inquiry/chat_page.dart';
import '../features/inquiry/inquiries_page.dart';
import '../features/notifications/notifications_page.dart';
import '../features/profile/account_settings_page.dart';
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
            builder: (context, state) =>
                AnswererDetailPage(uid: state.pathParameters['uid']!),
          ),
          GoRoute(
            name: 'discoveryList',
            path: '/discover/:type',
            builder: (context, state) =>
                DiscoveryListPage(type: state.pathParameters['type']!),
          ),
          GoRoute(
            name: 'discoveryResults',
            path: '/discover/:type/:id/results',
            builder: (context, state) => DiscoveryResultsPage(
              type: state.pathParameters['type']!,
              id: int.parse(state.pathParameters['id']!),
              title: state.uri.queryParameters['title'] ?? '',
            ),
          ),
          GoRoute(
            name: 'notifications',
            path: '/notices',
            builder: (context, state) => const NotificationsPage(),
          ),
          GoRoute(
            name: 'contentContributions',
            path: '/content-contributions',
            builder: (context, state) => const ContentContributionPage(),
          ),
          GoRoute(
            name: 'accountSettings',
            path: '/profile/settings',
            builder: (context, state) => const AccountSettingsPage(),
          ),
          GoRoute(
            name: 'wallet',
            path: '/profile/wallet',
            builder: (context, state) => const WalletPage(),
          ),
          GoRoute(
            name: 'certifications',
            path: '/profile/certifications',
            builder: (context, state) => const CertificationHomePage(),
          ),
          GoRoute(
            name: 'basicCertification',
            path: '/profile/certifications/basic',
            builder: (context, state) => const BasicCertificationPage(),
          ),
          GoRoute(
            name: 'basicCertificationApply',
            path: '/profile/certifications/basic/:type/apply',
            builder: (context, state) => BasicCertificationApplyPage(
              type: state.pathParameters['type']!,
              record: state.extra as CertificationRecord?,
            ),
          ),
          GoRoute(
            name: 'experienceCertifications',
            path: '/profile/certifications/experiences',
            builder: (context, state) => const ExperienceCertificationPage(),
          ),
          GoRoute(
            name: 'experienceCreate',
            path: '/profile/certifications/experiences/new',
            builder: (context, state) => const ExperienceFormPage(),
          ),
          GoRoute(
            name: 'experienceDetail',
            path: '/profile/certifications/experiences/:id',
            builder: (context, state) =>
                ExperienceFormPage(id: int.parse(state.pathParameters['id']!)),
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
        name: 'contentContributionCreate',
        path: '/content-contributions/new',
        builder: (context, state) => const ContentContributionFormPage(),
      ),
      GoRoute(
        name: 'contentContributionDetail',
        path: '/content-contributions/:id',
        builder: (context, state) => ContentContributionDetailPage(
          id: int.parse(state.pathParameters['id']!),
        ),
      ),
      GoRoute(
        name: 'customerService',
        path: '/profile/customer-service',
        builder: (context, state) => const CustomerServicePage(),
      ),
    ],
  );
}
