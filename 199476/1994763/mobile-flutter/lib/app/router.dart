import 'package:go_router/go_router.dart';

import '../features/answerer/answerer_detail_page.dart';
import '../features/auth/login_page.dart';
import '../features/certification/experience_certification_page.dart';
import '../features/certification/experience_additional_info_page.dart';
import '../features/certification/experience_form_page.dart';
import '../features/certification/identity_certification_page.dart';
import '../features/curated/curated_chat_page.dart';
import '../features/curated/curated_chats_page.dart';
import '../features/curated/curated_membership_page.dart';
import '../features/curated/curated_voice_call_page.dart';
import '../features/home/home_page.dart';
import '../features/inquiry/chat_page.dart';
import '../features/inquiry/inquiries_page.dart';
import '../features/inquiry/voice_call_page.dart';
import '../features/notifications/notifications_page.dart';
import '../features/profile/account_settings_page.dart';
import '../features/profile/experience_library_page.dart';
import '../features/profile/inquiry_settings_page.dart';
import '../features/profile/profile_page.dart';
import '../features/support/business_page.dart';
import '../features/support/customer_service_page.dart';
import '../features/support/faq_page.dart';
import '../features/support/feedback_page.dart';
import '../features/wallet/wallet_page.dart';
import '../features/wallet/wallet_transaction_detail_page.dart';
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
            name: 'experienceFavorites',
            path: '/profile/favorites',
            builder: (context, state) => const ExperienceLibraryPage(
              type: ExperienceLibraryType.favorites,
            ),
          ),
          GoRoute(
            name: 'experienceRecent',
            path: '/profile/recent',
            builder: (context, state) =>
                const ExperienceLibraryPage(type: ExperienceLibraryType.recent),
          ),
          GoRoute(
            name: 'experienceLibraryLegacy',
            path: '/profile/experience-library',
            redirect: (context, state) =>
                state.uri.queryParameters['tab'] == 'recent'
                ? '/profile/recent'
                : '/profile/favorites',
          ),
          GoRoute(
            name: 'inquirySettings',
            path: '/profile/inquiry-settings',
            builder: (context, state) => const InquirySettingsPage(),
          ),
          GoRoute(
            name: 'experienceCertifications',
            path: '/profile/certifications/experiences',
            builder: (context, state) => const ExperienceCertificationPage(),
          ),
          GoRoute(
            name: 'experienceCreateLegacy',
            path: '/profile/certifications/experiences/new',
            builder: (context, state) => ExperienceFormPage(
              resumeDraft: state.uri.queryParameters['resumeDraft'] == '1',
            ),
          ),
          GoRoute(
            name: 'experienceAdditionalInfo',
            path: '/profile/certifications/experiences/additional-info',
            builder: (context, state) => ExperienceAdditionalInfoPage(
              initialValue:
                  state.extra as ExperienceAdditionalInfo? ??
                  const ExperienceAdditionalInfo(),
            ),
          ),
          GoRoute(
            name: 'experienceDetailLegacy',
            path: '/profile/certifications/experiences/:id',
            builder: (context, state) => ExperienceFormPage(
              id: int.parse(state.pathParameters['id']!),
              resumeDraft: state.uri.queryParameters['resumeDraft'] == '1',
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
        name: 'identityCertification',
        path: '/profile/identity',
        builder: (context, state) => const IdentityCertificationPage(),
      ),
      GoRoute(
        name: 'wallet',
        path: '/profile/wallet',
        builder: (context, state) => const WalletPage(),
      ),
      GoRoute(
        name: 'walletTransactionDetail',
        path: '/profile/wallet/transactions/:id',
        builder: (context, state) => WalletTransactionDetailPage(
          transactionId: int.parse(state.pathParameters['id']!),
        ),
      ),
      GoRoute(
        name: 'chat',
        path: '/chat/:id',
        builder: (context, state) =>
            ChatPage(id: int.parse(state.pathParameters['id']!)),
      ),
      GoRoute(
        name: 'curatedMembership',
        path: '/curated',
        builder: (context, state) => const CuratedMembershipPage(),
      ),
      GoRoute(
        name: 'curatedApplication',
        path: '/curated/apply',
        builder: (context, state) => const CuratedApplicationPage(),
      ),
      GoRoute(
        name: 'curatedChats',
        path: '/curated/chats',
        builder: (context, state) => const CuratedChatsPage(),
      ),
      GoRoute(
        name: 'curatedChat',
        path: '/curated/chat/:id',
        builder: (context, state) =>
            CuratedChatPage(id: int.parse(state.pathParameters['id']!)),
      ),
      GoRoute(
        name: 'curatedVoiceCall',
        path: '/curated/voice/:callId',
        builder: (context, state) => CuratedVoiceCallPage(
          callId: int.parse(state.pathParameters['callId']!),
          initiator: state.uri.queryParameters['initiator'] == '1',
        ),
      ),
      GoRoute(
        name: 'voiceCall',
        path: '/voice-call/:inquiryId/:voiceCallId',
        builder: (context, state) => VoiceCallPage(
          inquiryId: int.parse(state.pathParameters['inquiryId']!),
          voiceCallId: int.parse(state.pathParameters['voiceCallId']!),
          initiator: state.uri.queryParameters['initiator'] == '1',
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
