import 'dart:io';

import 'package:dio/dio.dart';

import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../models/answerer_models.dart';
import '../models/app_version_models.dart';
import '../models/app_global_settings.dart';
import '../models/certification_models.dart';
import '../models/curated_chat_models.dart';
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

  Future<AppGlobalSettings> appGlobalSettings() async {
    final data = await _api.get<Map<String, dynamic>>('/app-settings');
    return AppGlobalSettings.fromJson(data);
  }

  Future<List<ExperienceCategoryOption>> experienceCategories() async {
    final data = await _api.get<List<dynamic>>('/experience-categories');
    return data.whereType<Map>().map((item) => ExperienceCategoryOption.fromJson(
      Map<String, dynamic>.from(item),
    )).toList(growable: false);
  }

  Future<CuratedMembership> curatedMembership({
    bool showLoading = true,
  }) async => CuratedMembership.fromJson(
    await _api.get<Map<String, dynamic>>(
      '/curated-chat/membership',
      showLoading: showLoading,
    ),
  );
  Future<CuratedMembership> submitCuratedApplication({
    List<UploadFile> jobFiles = const [],
  }) async {
    final jobs = <MultipartFile>[];
    for (final f in jobFiles) {
      jobs.add(await MultipartFile.fromFile(f.path, filename: f.name));
    }
    final parts = <String, dynamic>{'jobFiles': jobs};
    final data = await _api.post<Map<String, dynamic>>(
      '/curated-chat/membership/application',
      data: FormData.fromMap(parts),
    );
    return CuratedMembership.fromJson(data);
  }

  Future<List<CuratedCertificationMaterial>> curatedMaterials({
    bool showLoading = true,
  }) async =>
      (await _api.get<List<dynamic>>(
            '/curated-chat/membership/materials',
            showLoading: showLoading,
          ))
          .whereType<Map>()
          .map((item) {
            return CuratedCertificationMaterial.fromJson(
              Map<String, dynamic>.from(item),
            );
          })
          .toList(growable: false);

  Future<CuratedMembershipQuote> curatedMembershipQuote() async =>
      CuratedMembershipQuote.fromJson(
        await _api.get<Map<String, dynamic>>('/curated-chat/membership/quote'),
      );

  Future<CuratedPayment> createCuratedPayment(
    String requestId, {
    required int durationMonths,
    required double price,
  }) async => CuratedPayment.fromJson(
    await _api.post<Map<String, dynamic>>(
      '/curated-chat/membership/orders',
      data: {
        'requestId': requestId,
        'durationMonths': durationMonths,
        'price': price,
      },
    ),
  );
  Future<CuratedPayment> curatedPayment(String orderNo) async =>
      CuratedPayment.fromJson(
        await _api.get<Map<String, dynamic>>(
          '/curated-chat/membership/orders/$orderNo',
        ),
      );
  Future<CuratedMemberPage> curatedMembers({
    String keyword = '',
    int page = 0,
    int size = 20,
  }) async => CuratedMemberPage.fromJson(
    await _api.get<Map<String, dynamic>>(
      '/curated-chat/members',
      query: {'keyword': keyword, 'page': page, 'size': size},
    ),
  );
  Future<CuratedConversation> openCuratedConversation(int otherUserId) async =>
      CuratedConversation.fromJson(
        await _api.post<Map<String, dynamic>>(
          '/curated-chat/conversations',
          data: {'otherUserId': otherUserId},
        ),
      );
  Future<List<CuratedConversation>> curatedConversations({
    bool showLoading = true,
  }) async =>
      (await _api.get<List<dynamic>>(
            '/curated-chat/conversations',
            showLoading: showLoading,
          ))
          .whereType<Map>()
          .map(
            (x) => CuratedConversation.fromJson(Map<String, dynamic>.from(x)),
          )
          .toList();
  Future<int> curatedUnreadCount() async {
    final v = await _api.get<Object?>(
      '/curated-chat/conversations/unread-count',
      showLoading: false,
    );
    return v is num ? v.toInt() : int.tryParse('$v') ?? 0;
  }

  Future<CuratedConversation> curatedConversation(
    int id, {
    bool showLoading = true,
  }) async => CuratedConversation.fromJson(
    await _api.get<Map<String, dynamic>>(
      '/curated-chat/conversations/$id',
      showLoading: showLoading,
    ),
  );
  Future<List<CuratedMessage>> curatedMessages(
    int id, {
    int afterId = 0,
    int beforeId = 0,
    bool showLoading = false,
  }) async =>
      (await _api.get<List<dynamic>>(
            '/curated-chat/conversations/$id/messages',
            query: {'afterId': afterId, 'beforeId': beforeId},
            showLoading: showLoading,
          ))
          .whereType<Map>()
          .map((x) => CuratedMessage.fromJson(Map<String, dynamic>.from(x)))
          .toList();
  Future<void> readCuratedConversation(int id) => _api.put<Object?>(
    '/curated-chat/conversations/$id/read',
    showLoading: false,
  );
  Future<CuratedMessage> sendCuratedText(int id, String content) async =>
      CuratedMessage.fromJson(
        await _api.post<Map<String, dynamic>>(
          '/curated-chat/conversations/$id/messages',
          data: {'content': content},
          showLoading: false,
        ),
      );
  Future<CuratedMessage> sendCuratedImage(int id, UploadFile file) async =>
      CuratedMessage.fromJson(
        await _api.post<Map<String, dynamic>>(
          '/curated-chat/conversations/$id/images',
          data: FormData.fromMap({
            'image': await MultipartFile.fromFile(
              file.path,
              filename: file.name,
            ),
          }),
          showLoading: false,
        ),
      );
  Future<void> blockCuratedUser(int conversationId) =>
      _api.post<Object?>('/curated-chat/conversations/$conversationId/block');
  Future<CuratedVoiceCall> startCuratedVoiceCall(int conversationId) async =>
      CuratedVoiceCall.fromJson(
        await _api.post<Map<String, dynamic>>(
          '/curated-chat/conversations/$conversationId/voice-calls',
        ),
      );
  Future<CuratedVoiceCall> curatedVoiceCall(int callId) async =>
      CuratedVoiceCall.fromJson(
        await _api.get<Map<String, dynamic>>(
          '/curated-chat/voice-calls/$callId',
          showLoading: false,
        ),
      );
  Future<VoiceIceConfig> curatedVoiceIceConfig(int callId) async =>
      VoiceIceConfig.fromJson(
        await _api.get<Map<String, dynamic>>(
          '/curated-chat/voice-calls/$callId/ice-config',
          showLoading: false,
        ),
      );
  Future<CuratedVoiceCall> answerCuratedVoiceCall(int callId) async =>
      CuratedVoiceCall.fromJson(
        await _api.post<Map<String, dynamic>>(
          '/curated-chat/voice-calls/$callId/answer',
          showLoading: false,
        ),
      );
  Future<CuratedVoiceCall> rejectCuratedVoiceCall(int callId) async =>
      CuratedVoiceCall.fromJson(
        await _api.post<Map<String, dynamic>>(
          '/curated-chat/voice-calls/$callId/reject',
          showLoading: false,
        ),
      );
  Future<CuratedVoiceCall> connectCuratedVoiceCall(int callId) async =>
      CuratedVoiceCall.fromJson(
        await _api.post<Map<String, dynamic>>(
          '/curated-chat/voice-calls/$callId/connected',
          showLoading: false,
        ),
      );
  Future<CuratedVoiceCall> endCuratedVoiceCall(int callId) async =>
      CuratedVoiceCall.fromJson(
        await _api.post<Map<String, dynamic>>(
          '/curated-chat/voice-calls/$callId/end',
          showLoading: false,
        ),
      );
  Future<CuratedVoiceSignal> sendCuratedVoiceSignal(
    int callId,
    String type,
    String payload,
  ) async => CuratedVoiceSignal.fromJson(
    await _api.post<Map<String, dynamic>>(
      '/curated-chat/voice-calls/$callId/signals',
      data: {'type': type, 'payload': payload},
      showLoading: false,
    ),
  );
  Future<List<CuratedVoiceSignal>> curatedVoiceSignals(
    int callId, {
    int afterId = 0,
  }) async =>
      (await _api.get<List<dynamic>>(
            '/curated-chat/voice-calls/$callId/signals',
            query: {'afterId': afterId},
            showLoading: false,
          ))
          .whereType<Map>()
          .map((x) => CuratedVoiceSignal.fromJson(Map<String, dynamic>.from(x)))
          .toList();

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

  Future<AppUser> setInquiryHourlyRate(int hourlyRate) async {
    final data = await _api.patch<Map<String, dynamic>>(
      '/users/me/inquiry-hourly-rate',
      data: {'hourlyRate': hourlyRate},
    );
    return AppUser.fromJson(data);
  }

  Future<AnswererPageData> answerers({
    int page = 0,
    int size = 10,
    String keyword = '',
    String? sortBy,
    String? sortDirection,
    int? categoryId,
  }) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/answerers',
      query: {
        'page': page,
        'size': size,
        'keyword': keyword,
        if (sortBy != null) 'sortBy': sortBy,
        if (sortDirection != null) 'sortDirection': sortDirection,
        if (categoryId != null) 'categoryId': categoryId,
      },
    );
    return AnswererPageData.fromJson(data);
  }

  Future<Answerer> answerer(String uid) async {
    final data = await _api.get<Map<String, dynamic>>('/answerers/$uid');
    return Answerer.fromJson(data);
  }

  Future<ExperienceLikeState> setExperienceLike({
    required String uid,
    required int certificationId,
    required bool liked,
  }) async {
    final data = await _api.put<Map<String, dynamic>>(
      '/answerers/$uid/experiences/$certificationId/like',
      data: {'liked': liked},
      showLoading: false,
    );
    return ExperienceLikeState.fromJson(data);
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
    required String question,
  }) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries',
      data: {
        'answererId': answererId,
        'sourceExperienceCertificationId': sourceExperienceCertificationId,
        'question': question,
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
  Future<void> endInquiry(int id) => _api.post<Object?>('/inquiries/$id/end');
  Future<VoiceCall> latestVoiceCall(int inquiryId) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls/latest',
      showLoading: false,
    );
    return VoiceCall.fromJson(data);
  }

  Future<VoiceCall> createVoiceCall({required int inquiryId}) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls',
    );
    return VoiceCall.fromJson(data);
  }

  Future<VoiceCall> answerVoiceCall(int inquiryId, int voiceCallId) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls/$voiceCallId/answer',
    );
    return VoiceCall.fromJson(data);
  }

  Future<VoiceCall> rejectVoiceCall(int inquiryId, int voiceCallId) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls/$voiceCallId/reject',
    );
    return VoiceCall.fromJson(data);
  }

  Future<VoiceCall> joinVoiceCall(int inquiryId, int voiceCallId) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls/$voiceCallId/join',
      showLoading: false,
    );
    return VoiceCall.fromJson(data);
  }

  Future<VoiceCall> markVoiceCallConnected(
    int inquiryId,
    int voiceCallId,
  ) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls/$voiceCallId/connected',
      showLoading: false,
    );
    return VoiceCall.fromJson(data);
  }

  Future<VoiceCall> markVoiceCallDisconnected(
    int inquiryId,
    int voiceCallId,
    String reason,
  ) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls/$voiceCallId/disconnected',
      data: {'reason': reason},
      showLoading: false,
    );
    return VoiceCall.fromJson(data);
  }

  Future<VoiceCall> finishVoiceCall(int inquiryId, int voiceCallId) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls/$voiceCallId/finish',
      showLoading: false,
    );
    return VoiceCall.fromJson(data);
  }

  Future<VoiceIceConfig> voiceIceConfig(int inquiryId, int voiceCallId) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls/$voiceCallId/voice/ice-config',
      showLoading: false,
    );
    return VoiceIceConfig.fromJson(data);
  }

  Future<List<VoiceSignal>> voiceSignals(
    int inquiryId,
    int voiceCallId, {
    int afterId = 0,
  }) async {
    final data = await _api.get<List<dynamic>>(
      '/inquiries/$inquiryId/voice-calls/$voiceCallId/voice/signals',
      query: {'afterId': afterId},
      showLoading: false,
    );
    return data
        .whereType<Map>()
        .map((item) => VoiceSignal.fromJson(Map<String, dynamic>.from(item)))
        .toList(growable: false);
  }

  Future<VoiceSignal> sendVoiceSignal({
    required int inquiryId,
    required int voiceCallId,
    required String signalType,
    required String payload,
  }) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$inquiryId/voice-calls/$voiceCallId/voice/signals',
      data: {'signalType': signalType, 'payload': payload},
      showLoading: false,
    );
    return VoiceSignal.fromJson(data);
  }

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

  Future<bool> hasComplainedAboutInquiry(int inquiryId) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/inquiries/$inquiryId/complaints/status',
    );
    return data['complained'] == true;
  }

  Future<void> complainAboutInquiry({
    required int inquiryId,
    required String category,
    required String content,
  }) => _api.post<Object?>(
    '/inquiries/$inquiryId/complaints',
    data: {'category': category, 'content': content},
  );

  Future<void> blockInquiryUser(int inquiryId) =>
      _api.post<Object?>('/inquiries/$inquiryId/block');

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
      '/certifications/identity',
      data: FormData.fromMap({
        'files': await Future.wait(
          files.map(
            (file) => MultipartFile.fromFile(file.path, filename: file.name),
          ),
        ),
      }),
    );
  }

  Future<void> submitExperience({
    int? existingId,
    required String title,
    required String description,
    required ExperienceAdditionalInfo additionalInfo,
    UploadFile? proofArchive,
    String? proofUploadId,
    bool removeProofArchive = false,
  }) async {
    final fields = <String, dynamic>{
      if (existingId != null) 'existingId': existingId,
      'title': title,
      'description': description,
      if (additionalInfo.categoryId != null)
        'experienceCategoryId': additionalInfo.categoryId,
      if (additionalInfo.location.isNotEmpty)
        'experienceLocation': additionalInfo.location,
      if (additionalInfo.startDate.isNotEmpty)
        'experienceStartDate': additionalInfo.startDate,
      if (additionalInfo.endDate.isNotEmpty)
        'experienceEndDate': additionalInfo.endDate,
      if (additionalInfo.count != null) 'experienceCount': additionalInfo.count,
      if (additionalInfo.role.isNotEmpty) 'experienceRole': additionalInfo.role,
      if (additionalInfo.ageRange.isNotEmpty)
        'experienceAgeRange': additionalInfo.ageRange,
      if (additionalInfo.education.isNotEmpty)
        'experienceEducation': additionalInfo.education,
      if (additionalInfo.job.isNotEmpty) 'experienceJob': additionalInfo.job,
      'removeProofArchive': removeProofArchive,
      if (proofUploadId != null) 'proofUploadId': proofUploadId,
    };
    await _api.post<Object?>(
      '/certifications/experiences',
      data: proofArchive == null
          ? fields
          : FormData.fromMap({
              ...fields,
              'proofArchive': await MultipartFile.fromFile(
                proofArchive.path,
                filename: proofArchive.name,
              ),
            }),
    );
  }

  Future<String?> uploadExperienceProof(
    UploadFile file, {
    void Function(double progress)? onProgress,
  }) async {
    final source = File(file.path);
    final size = await source.length();
    final capabilities = await _api.get<Map<String, dynamic>>(
      '/certifications/experience-uploads/capabilities',
      showLoading: false,
    );
    if (capabilities['directUploadAvailable'] != true) {
      if (size > 500 * 1024 * 1024) {
        throw const ApiException('本地存储模式下，证明资料不能超过500MB');
      }
      return null;
    }
    final initiated = await _api.post<Map<String, dynamic>>(
      '/certifications/experience-uploads',
      data: {'name': file.name, 'size': size},
      showLoading: false,
    );
    final uploadId = initiated['uploadId'] as String;
    final partSize = initiated['partSize'] as int;
    final partCount = initiated['partCount'] as int;
    final client = HttpClient()
      ..connectionTimeout = const Duration(seconds: 15)
      ..idleTimeout = const Duration(minutes: 3);
    try {
      for (var part = 1; part <= partCount; part++) {
        final offset = (part - 1) * partSize;
        final end = offset + partSize > size ? size : offset + partSize;
        var uploaded = false;
        for (var attempt = 0; attempt < 3 && !uploaded; attempt++) {
          HttpClientRequest? request;
          try {
            final signed = await _api.post<Map<String, dynamic>>(
              '/certifications/experience-uploads/$uploadId/parts/$part',
              showLoading: false,
            );
            final url = Uri.parse(signed['url'] as String);
            if (url.scheme != 'https') {
              throw StateError('文件上传地址不安全');
            }
            request = await client.putUrl(url);
            request.contentLength = end - offset;
            await request
                .addStream(source.openRead(offset, end))
                .timeout(const Duration(minutes: 3));
            final response = await request.close().timeout(
              const Duration(minutes: 3),
            );
            await response.drain<void>();
            if (response.statusCode < 200 || response.statusCode >= 300) {
              throw HttpException('OSS 上传失败（${response.statusCode}）');
            }
            uploaded = true;
          } catch (_) {
            request?.abort();
            if (attempt == 2) rethrow;
            await Future<void>.delayed(Duration(seconds: attempt + 1));
          }
        }
        onProgress?.call(end / size);
      }
      for (var attempt = 0; attempt < 3; attempt++) {
        try {
          await _api.post<Map<String, dynamic>>(
            '/certifications/experience-uploads/$uploadId/complete',
            showLoading: false,
          );
          break;
        } catch (_) {
          if (attempt == 2) rethrow;
          await Future<void>.delayed(Duration(seconds: attempt + 1));
        }
      }
      return uploadId;
    } catch (_) {
      try {
        await _api.post<Object?>(
          '/certifications/experience-uploads/$uploadId/abort',
          showLoading: false,
        );
      } catch (_) {}
      rethrow;
    } finally {
      client.close(force: true);
    }
  }

  Future<void> deleteExperience(int certificationId) async {
    await _api.delete<Object?>('/certifications/experiences/$certificationId');
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
