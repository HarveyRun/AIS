import 'package:go_router/go_router.dart';

import '../features/answerer/answerer_detail_page.dart';
import '../features/auth/login_page.dart';
import '../features/certification/basic_certification_page.dart';
import '../features/certification/basic_certification_apply_page.dart';
import '../features/certification/certification_home_page.dart';
import '../features/certification/experience_certification_page.dart';
import '../features/certification/experience_form_page.dart';
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
import 'shell_page.dart';

GoRouter createAppRouter(AuthController auth) {
  return GoRouter(
    initialLocation: '/home',
    refreshListenable: auth,
    redirect: (context, state) {
      final publicPath = state.matchedLocation == '/login';
      if (!auth.signedIn && !publicPath) return '/login';
      if (auth.signedIn && state.matchedLocation == '/login') return '/home';
      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (context, state) => const LoginPage()),
      ShellRoute(
        builder: (context, state, child) =>
            AppShellPage(location: state.uri.path, child: child),
        routes: [
          GoRoute(path: '/home', builder: (context, state) => const HomePage()),
          GoRoute(
            path: '/inquiries',
            builder: (context, state) => const InquiriesPage(),
          ),
          GoRoute(
            path: '/profile',
            builder: (context, state) => const ProfilePage(),
          ),
          GoRoute(
            path: '/answerers/:uid',
            builder: (context, state) =>
                AnswererDetailPage(uid: state.pathParameters['uid']!),
          ),
          GoRoute(
            path: '/discover/:type',
            builder: (context, state) =>
                DiscoveryListPage(type: state.pathParameters['type']!),
          ),
          GoRoute(
            path: '/discover/:type/:id/results',
            builder: (context, state) => DiscoveryResultsPage(
              type: state.pathParameters['type']!,
              id: int.parse(state.pathParameters['id']!),
              title: state.uri.queryParameters['title'] ?? '',
            ),
          ),
          GoRoute(
            path: '/notices',
            builder: (context, state) => const NotificationsPage(),
          ),
          GoRoute(
            path: '/profile/settings',
            builder: (context, state) => const AccountSettingsPage(),
          ),
          GoRoute(
            path: '/profile/wallet',
            builder: (context, state) => const WalletPage(),
          ),
          GoRoute(
            path: '/profile/certifications',
            builder: (context, state) => const CertificationHomePage(),
          ),
          GoRoute(
            path: '/profile/certifications/basic',
            builder: (context, state) => const BasicCertificationPage(),
          ),
          GoRoute(
            path: '/profile/certifications/basic/:type/apply',
            builder: (context, state) => BasicCertificationApplyPage(
              type: state.pathParameters['type']!,
              record: state.extra as CertificationRecord?,
            ),
          ),
          GoRoute(
            path: '/profile/certifications/experiences',
            builder: (context, state) => const ExperienceCertificationPage(),
          ),
          GoRoute(
            path: '/profile/certifications/experiences/new',
            builder: (context, state) => const ExperienceFormPage(),
          ),
          GoRoute(
            path: '/profile/certifications/experiences/:id',
            builder: (context, state) =>
                ExperienceFormPage(id: int.parse(state.pathParameters['id']!)),
          ),
          GoRoute(
            path: '/profile/feedback',
            builder: (context, state) => const FeedbackPage(),
          ),
          GoRoute(
            path: '/profile/faq',
            builder: (context, state) => const FaqPage(),
          ),
          GoRoute(
            path: '/profile/business',
            builder: (context, state) => const BusinessPage(),
          ),
        ],
      ),
      GoRoute(
        path: '/chat/:id',
        builder: (context, state) =>
            ChatPage(id: int.parse(state.pathParameters['id']!)),
      ),
      GoRoute(
        path: '/profile/customer-service',
        builder: (context, state) => const CustomerServicePage(),
      ),
    ],
  );
}
