import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../../core/network/api_client.dart';
import '../models/answerer_models.dart';
import '../models/app_version_models.dart';
import '../models/certification_models.dart';
import '../models/home_banner_models.dart';
import '../models/inquiry_models.dart';
import '../models/support_models.dart';
import '../models/user_models.dart';
import '../models/wallet_models.dart';

class UploadFile {
  const UploadFile({required this.path, required this.name});

  final String path;
  final String name;
}

class AppRepository {
  const AppRepository(this._api);

  final ApiClient _api;

  Future<AppUpdateInfo> checkAppVersion(int currentVersionCode) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/public/app-version',
      query: {'platform': 'ANDROID', 'currentVersionCode': currentVersionCode},
    );
    return AppUpdateInfo.fromJson(data);
  }

  Future<List<HomeBannerItem>> homeBanners() async {
    final data = await _api.get<List<dynamic>>('/public/banners');
    return data
        .whereType<Map>()
        .map((item) => HomeBannerItem.fromJson(Map<String, dynamic>.from(item)))
        .toList(growable: false);
  }

  Future<HomeBannerAvailability> homeBannerAvailability(int bannerId) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/public/banners/$bannerId/availability',
    );
    return HomeBannerAvailability.fromJson(data);
  }

  Future<void> sendVerificationCode(String phone) {
    return _api.post<Object?>(
      '/auth/verification-codes',
      data: {'phone': phone},
    );
  }

  Future<LoginResult> login(String phone, String code) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/auth/login',
      data: {'phone': phone, 'code': code},
    );
    return LoginResult.fromJson(data);
  }

  Future<void> logout() => _api.post<Object?>('/auth/logout');

  Future<AppUser> me() async {
    final data = await _api.get<Map<String, dynamic>>('/users/me');
    return AppUser.fromJson(data);
  }

  Future<AppUser> updateProfile({
    required String nickname,
    required String jobTitle,
  }) async {
    final data = await _api.put<Map<String, dynamic>>(
      '/users/me',
      data: {'nickname': nickname, 'jobTitle': jobTitle},
    );
    return AppUser.fromJson(data);
  }

  Future<AppUser> updateAvatar(UploadFile file) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/users/me/avatar',
      data: FormData.fromMap({
        'avatar': await MultipartFile.fromFile(file.path, filename: file.name),
      }),
    );
    return AppUser.fromJson(data);
  }

  Future<void> deleteAccount() => _api.delete<Object?>('/users/me');

  Future<Map<String, dynamic>> redeemExperienceInvitationReward(
    String invitedUid,
    String invitedPhone,
  ) {
    return _api.post<Map<String, dynamic>>(
      '/experience-invitation-rewards/redeem',
      data: {'invitedUid': invitedUid, 'invitedPhone': invitedPhone},
    );
  }

  Future<AccountDeletionEligibility> accountDeletionEligibility() async {
    final data = await _api.get<Map<String, dynamic>>(
      '/users/me/deletion-eligibility',
    );
    return AccountDeletionEligibility.fromJson(data);
  }

  Future<List<ViolationCounter>> violationCounters() async {
    final data = await _api.get<List<dynamic>>('/users/me/violation-counters');
    return data
        .whereType<Map>()
        .map(
          (item) => ViolationCounter.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<AppUser> dismissPlatformIntroduction() async {
    final data = await _api.post<Map<String, dynamic>>(
      '/users/me/platform-introduction/dismiss',
    );
    return AppUser.fromJson(data);
  }

  Future<AppUser> setAcceptingInquiries(bool accepting) async {
    final data = await _api.patch<Map<String, dynamic>>(
      '/users/me/accepting-inquiries',
      data: {'accepting': accepting},
    );
    return AppUser.fromJson(data);
  }

  Future<AppUser> setInquiryPriceRange({
    required int minimum,
    required int maximum,
  }) async {
    final data = await _api.patch<Map<String, dynamic>>(
      '/users/me/inquiry-price-range',
      data: {'minimum': minimum, 'maximum': maximum},
    );
    return AppUser.fromJson(data);
  }

  Future<Map<String, dynamic>> answererEligibility() {
    return _api.get<Map<String, dynamic>>('/users/me/answerer-eligibility');
  }

  Future<AnswererPageData> answerers({
    int page = 0,
    int size = 10,
    String keyword = '',
    String experienceType = 'ALL',
  }) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/answerers',
      query: {
        'page': page,
        'size': size,
        'keyword': keyword,
        'experienceType': experienceType,
      },
    );
    return AnswererPageData.fromJson(data);
  }

  Future<Answerer> answerer(String uid) async {
    final data = await _api.get<Map<String, dynamic>>('/answerers/$uid');
    return Answerer.fromJson(data);
  }

  Future<void> tipExperience({
    required int certificationId,
    required int amount,
    required String requestId,
  }) {
    return _api.post<Object?>(
      '/experience-tips',
      data: {
        'certificationId': certificationId,
        'amount': amount,
        'requestId': requestId,
      },
    );
  }

  Future<List<InquirySummary>> inquiries({bool showLoading = true}) async {
    final data = await _api.get<List<dynamic>>(
      '/inquiries',
      showLoading: showLoading,
    );
    return data
        .whereType<Map>()
        .map((item) => InquirySummary.fromJson(Map<String, dynamic>.from(item)))
        .toList(growable: false);
  }

  Future<int> inquiryUnreadCount() async {
    final data = await _api.get<Object?>(
      '/inquiries/unread-count',
      showLoading: false,
    );
    if (data is num) return data.toInt();
    if (data is Map) return _int(data['count'] ?? data['unreadCount']);
    return _int(data);
  }

  Future<InquiryDetail> inquiry(int id, {bool showLoading = true}) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/inquiries/$id',
      showLoading: showLoading,
    );
    return InquiryDetail.fromJson(data);
  }

  Future<InquirySummary> createInquiry({
    required int answererId,
    required int sourceExperienceCertificationId,
    required int amount,
  }) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries',
      data: {
        'answererId': answererId,
        'sourceExperienceCertificationId': sourceExperienceCertificationId,
        'amount': amount,
      },
    );
    return InquirySummary.fromJson(data);
  }

  Future<void> acceptInquiry(int id) =>
      _api.post<Object?>('/inquiries/$id/accept');
  Future<void> rejectInquiry(int id) =>
      _api.post<Object?>('/inquiries/$id/reject');
  Future<void> cancelInquiry(int id) =>
      _api.post<Object?>('/inquiries/$id/cancel');
  Future<void> requestInquiryEnd(int id) =>
      _api.post<Object?>('/inquiries/$id/request-end');
  Future<void> disagreeInquiryEnd(int id) =>
      _api.post<Object?>('/inquiries/$id/disagree-end');
  Future<void> confirmInquiryEnd(int id) =>
      _api.post<Object?>('/inquiries/$id/confirm-end');
  Future<void> markInquiryRead(int id) =>
      _api.put<Object?>('/inquiries/$id/read', showLoading: false);

  Future<InquiryQualityOptions> inquiryQualityOptions(
    int id, {
    bool showLoading = true,
  }) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/inquiries/$id/quality',
      showLoading: showLoading,
    );
    return InquiryQualityOptions.fromJson(data);
  }

  Future<void> evaluateInquiry(int id, Map<String, dynamic> data) =>
      _api.post<Object?>('/inquiries/$id/quality/evaluation', data: data);

  Future<ChatMessage> sendInquiryMessage(int id, String content) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$id/messages',
      data: {'content': content},
      showLoading: false,
    );
    return ChatMessage.fromJson(data);
  }

  Future<ChatMessage> sendInquiryImage(int id, UploadFile file) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$id/images',
      data: FormData.fromMap({
        'image': await MultipartFile.fromFile(file.path, filename: file.name),
      }),
    );
    return ChatMessage.fromJson(data);
  }

  Future<void> reportInquiryMessage(
    int inquiryId,
    String messageId,
    String reportType,
  ) => _api.post<Object?>(
    '/inquiries/$inquiryId/messages/$messageId/reports',
    data: {'reportType': reportType},
  );

  Future<bool> hasReportedInquiryMessage(
    int inquiryId,
    String messageId,
  ) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/inquiries/$inquiryId/messages/$messageId/reports/status',
    );
    return data['reported'] == true;
  }

  Future<WalletInfo> wallet() async {
    final data = await _api.get<Map<String, dynamic>>('/wallet');
    return WalletInfo.fromJson(data);
  }

  Future<List<WalletTransaction>> walletTransactions() async {
    final data = await _api.get<List<dynamic>>('/wallet/transactions');
    return data
        .whereType<Map>()
        .map(
          (item) => WalletTransaction.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<AlipayAccountInfo?> alipayAccount() async {
    final data = await _api.get<Object?>('/wallet/alipay-account');
    if (data is! Map) return null;
    return AlipayAccountInfo.fromJson(Map<String, dynamic>.from(data));
  }

  Future<String> alipayAuthorizationPayload() async {
    final data = await _api.get<Map<String, dynamic>>(
      '/wallet/alipay-authorization/payload',
    );
    return data['authPayload']?.toString() ?? '';
  }

  Future<AlipayAccountInfo> completeAlipayAuthorization(String authCode) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/wallet/alipay-authorization/complete',
      data: {'authCode': authCode},
    );
    return AlipayAccountInfo.fromJson(data);
  }

  Future<List<WithdrawalRecord>> withdrawals() async {
    final data = await _api.get<List<dynamic>>('/wallet/withdrawals');
    return data
        .whereType<Map>()
        .map(
          (item) => WithdrawalRecord.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<void> sendWalletVerificationCode(String purpose) {
    return _api.post<Object?>(
      '/wallet/verification-codes',
      data: {'purpose': purpose},
    );
  }

  Future<WithdrawalRecord> withdraw(
    int amount,
    String requestId,
    String verificationCode,
  ) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/wallet/withdrawals',
      data: {
        'amount': amount,
        'requestId': requestId,
        'verificationCode': verificationCode,
      },
    );
    return WithdrawalRecord.fromJson(data);
  }

  Future<Map<String, dynamic>> rechargeCapability() {
    return _api.get<Map<String, dynamic>>('/recharges/capability');
  }

  Future<Map<String, dynamic>> createRecharge(int amount, String requestId) {
    return _api.post<Map<String, dynamic>>(
      '/recharges',
      data: {'amount': amount, 'requestId': requestId},
    );
  }

  Future<Map<String, dynamic>> recharge(String orderNo) {
    return _api.get<Map<String, dynamic>>('/recharges/$orderNo');
  }

  Future<List<CertificationRecord>> certifications({
    bool showLoading = true,
  }) async {
    final data = await _api.get<List<dynamic>>(
      '/certifications/me',
      showLoading: showLoading,
    );
    return data
        .whereType<Map>()
        .map(
          (item) =>
              CertificationRecord.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<void> submitIdentityCertification(List<UploadFile> files) async {
    await _api.post<Object?>(
      '/certifications/basic/IDENTITY',
      data: FormData.fromMap({
        'files': await Future.wait(
          files.map(
            (file) => MultipartFile.fromFile(file.path, filename: file.name),
          ),
        ),
      }),
    );
  }

  Future<void> submitExperienceCertification({
    int? existingId,
    required String title,
    required String description,
    required bool privacyConfirmed,
    required Uint8List signatureBytes,
    required String detailMode,
    UploadFile? reviewOriginal,
    UploadFile? proofArchive,
    UploadFile? detailVideo,
  }) async {
    await _api.post<Object?>(
      '/certifications/experiences',
      data: FormData.fromMap({
        if (existingId != null) 'existingId': existingId,
        'title': title,
        'description': description,
        'privacyConfirmed': privacyConfirmed,
        'detailMode': detailMode,
        'signature': MultipartFile.fromBytes(
          signatureBytes,
          filename: 'signature.png',
        ),
        if (reviewOriginal != null)
          'reviewOriginal': await MultipartFile.fromFile(
            reviewOriginal.path,
            filename: reviewOriginal.name,
          ),
        if (proofArchive != null)
          'proofArchive': await MultipartFile.fromFile(
            proofArchive.path,
            filename: proofArchive.name,
          ),
        if (detailVideo != null)
          'detailVideo': await MultipartFile.fromFile(
            detailVideo.path,
            filename: detailVideo.name,
          ),
      }),
      sendTimeout: const Duration(hours: 2),
      receiveTimeout: const Duration(hours: 2),
    );
  }

  Future<void> submitPublicWelfareExperience({
    int? existingId,
    required String title,
    required String description,
    required String detailMode,
    UploadFile? proofArchive,
    UploadFile? detailVideo,
  }) async {
    await _api.post<Object?>(
      '/certifications/experiences/public-welfare',
      data: FormData.fromMap({
        if (existingId != null) 'existingId': existingId,
        'title': title,
        'description': description,
        'detailMode': detailMode,
        if (proofArchive != null)
          'proofArchive': await MultipartFile.fromFile(
            proofArchive.path,
            filename: proofArchive.name,
          ),
        if (detailVideo != null)
          'detailVideo': await MultipartFile.fromFile(
            detailVideo.path,
            filename: detailVideo.name,
          ),
      }),
      sendTimeout: const Duration(hours: 2),
      receiveTimeout: const Duration(hours: 2),
    );
  }

  Future<void> submitMonetizedExperience({
    int? existingId,
    int? upgradeSourceId,
    required String title,
    required String description,
    required Uint8List signatureBytes,
    required String detailMode,
    UploadFile? reviewOriginal,
    UploadFile? proofArchive,
    UploadFile? detailVideo,
  }) async {
    await _api.post<Object?>(
      '/certifications/experiences/monetized',
      data: FormData.fromMap({
        if (existingId != null) 'existingId': existingId,
        if (upgradeSourceId != null) 'upgradeSourceId': upgradeSourceId,
        'title': title,
        'description': description,
        'privacyConfirmed': true,
        'detailMode': detailMode,
        'signature': MultipartFile.fromBytes(
          signatureBytes,
          filename: 'signature.png',
        ),
        if (reviewOriginal != null)
          'reviewOriginal': await MultipartFile.fromFile(
            reviewOriginal.path,
            filename: reviewOriginal.name,
          ),
        if (proofArchive != null)
          'proofArchive': await MultipartFile.fromFile(
            proofArchive.path,
            filename: proofArchive.name,
          ),
        if (detailVideo != null)
          'detailVideo': await MultipartFile.fromFile(
            detailVideo.path,
            filename: detailVideo.name,
          ),
      }),
      sendTimeout: const Duration(hours: 2),
      receiveTimeout: const Duration(hours: 2),
    );
  }

  Future<ExperiencePublicMediaView> experiencePublicMedia(
    int certificationId,
  ) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/certifications/experiences/$certificationId/public-media',
    );
    return ExperiencePublicMediaView.fromJson(data);
  }

  Future<ExperiencePublicMediaView> updateExperiencePublicMedia(
    int certificationId,
    Iterable<int> selectedIds,
  ) async {
    final data = await _api.put<Map<String, dynamic>>(
      '/certifications/experiences/$certificationId/public-media',
      data: {'selectedIds': selectedIds.toList(growable: false)},
    );
    return ExperiencePublicMediaView.fromJson(data);
  }

  Future<List<AppNotification>> notifications({bool showLoading = true}) async {
    final data = await _api.get<List<dynamic>>(
      '/notifications',
      showLoading: showLoading,
    );
    return data
        .whereType<Map>()
        .map(
          (item) => AppNotification.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<int> notificationUnreadCount() async {
    final data = await _api.get<Object?>(
      '/notifications/unread-count',
      showLoading: false,
    );
    if (data is num) return data.toInt();
    if (data is Map) return _int(data['count'] ?? data['unreadCount']);
    return _int(data);
  }

  Future<void> readNotification(AppNotification notification) =>
      _api.put<Object?>(
        '/notifications/${notification.sourceType}/${notification.id}/read',
        showLoading: false,
      );
  Future<void> readAllNotifications() =>
      _api.put<Object?>('/notifications/read-all');

  Future<List<FeedbackRecord>> feedbackRecords() async {
    final data = await _api.get<List<dynamic>>('/support/feedback');
    return data
        .whereType<Map>()
        .map((item) => FeedbackRecord.fromJson(Map<String, dynamic>.from(item)))
        .toList(growable: false);
  }

  Future<void> submitFeedback({
    required String type,
    required String category,
    required String content,
  }) {
    return _api.post<Object?>(
      '/support/feedback',
      data: {'type': type, 'category': category, 'content': content},
    );
  }

  Future<void> submitBusinessCooperation(Map<String, dynamic> data) {
    return _api.post<Object?>('/support/business-cooperations', data: data);
  }

  Future<List<CustomerServiceMessage>> customerServiceMessages({
    bool showLoading = true,
  }) async {
    final data = await _api.get<List<dynamic>>(
      '/support/customer-service/messages',
      showLoading: showLoading,
    );
    return data
        .whereType<Map>()
        .map(
          (item) =>
              CustomerServiceMessage.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<CustomerServiceMessage> sendCustomerServiceMessage(
    String content,
  ) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/support/customer-service/messages',
      data: {'content': content},
    );
    return CustomerServiceMessage.fromJson(data);
  }

  Future<CustomerServiceMessage> sendCustomerServiceImage(
    UploadFile file,
  ) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/support/customer-service/images',
      data: FormData.fromMap({
        'image': await MultipartFile.fromFile(file.path, filename: file.name),
      }),
    );
    return CustomerServiceMessage.fromJson(data);
  }

  Future<int> customerServiceUnreadCount() async {
    final data = await _api.get<Object?>(
      '/support/customer-service/unread-count',
      showLoading: false,
    );
    if (data is num) return data.toInt();
    if (data is Map) return _int(data['count'] ?? data['unreadCount']);
    return _int(data);
  }

  Future<void> readCustomerServiceMessages() {
    return _api.put<Object?>(
      '/support/customer-service/read',
      showLoading: false,
    );
  }

  Future<Map<String, dynamic>> realtimeTicket() {
    return _api.post<Map<String, dynamic>>(
      '/realtime/tickets',
      showLoading: false,
    );
  }
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
