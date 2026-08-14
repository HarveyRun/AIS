import 'package:dio/dio.dart';

import '../../core/network/api_client.dart';
import '../models/answerer_models.dart';
import '../models/certification_models.dart';
import '../models/discovery_models.dart';
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

  Future<AppUser> updateProfile({required String nickname}) async {
    final data = await _api.put<Map<String, dynamic>>(
      '/users/me',
      data: {'nickname': nickname},
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

  Future<AppUser> setAcceptingInquiries(bool accepting) async {
    final data = await _api.patch<Map<String, dynamic>>(
      '/users/me/accepting-inquiries',
      data: {'accepting': accepting},
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
  }) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/answerers',
      query: {'page': page, 'size': size, 'keyword': keyword},
    );
    return AnswererPageData.fromJson(data);
  }

  Future<Answerer> answerer(String uid) async {
    final data = await _api.get<Map<String, dynamic>>('/answerers/$uid');
    return Answerer.fromJson(data);
  }

  Future<List<Answerer>> answerersByMatter(int matterId) async {
    final data = await _api.get<List<dynamic>>(
      '/answerers/by-matter/$matterId',
    );
    return _answererList(data);
  }

  Future<List<Answerer>> answerersByExperience(int experienceId) async {
    final data = await _api.get<List<dynamic>>(
      '/answerers/by-experience',
      query: {'experienceId': experienceId},
    );
    return _answererList(data);
  }

  Future<List<DiscoveryCategory>> matterCategories(String category) async {
    final data = await _api.get<List<dynamic>>(
      '/public/discovery/matter-categories',
      query: {'mainCategory': category},
    );
    return _categoryList(data);
  }

  Future<List<DiscoveryCategory>> experienceCategories(String category) async {
    final data = await _api.get<List<dynamic>>(
      '/public/discovery/experience-categories',
      query: {'mainCategory': category},
    );
    return _categoryList(data);
  }

  Future<List<DiscoverySearchItem>> searchMatters(String keyword) async {
    final data = await _api.get<List<dynamic>>(
      '/public/discovery/matters/search',
      query: {'keyword': keyword},
    );
    return _searchList(data);
  }

  Future<List<DiscoverySearchItem>> searchExperiences(String keyword) async {
    final data = await _api.get<List<dynamic>>(
      '/public/discovery/experiences/search',
      query: {'keyword': keyword},
    );
    return _searchList(data);
  }

  Future<DiscoveryMatter> discoveryMatter(int id) async {
    final data = await _api.get<Map<String, dynamic>>(
      '/public/discovery/matters/$id',
    );
    return DiscoveryMatter.fromJson(data);
  }

  Future<List<InquirySummary>> inquiries() async {
    final data = await _api.get<List<dynamic>>('/inquiries');
    return data
        .whereType<Map>()
        .map((item) => InquirySummary.fromJson(Map<String, dynamic>.from(item)))
        .toList(growable: false);
  }

  Future<InquiryDetail> inquiry(int id) async {
    final data = await _api.get<Map<String, dynamic>>('/inquiries/$id');
    return InquiryDetail.fromJson(data);
  }

  Future<InquirySummary> createInquiry({
    required int answererId,
    required String topic,
    required String sourceType,
    required String question,
    required double amount,
  }) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries',
      data: {
        'answererId': answererId,
        'topic': topic,
        'sourceType': sourceType,
        'question': question,
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
  Future<void> continueInquiry(int id) =>
      _api.post<Object?>('/inquiries/$id/continue');
  Future<void> confirmInquiryEnd(int id) =>
      _api.post<Object?>('/inquiries/$id/confirm-end');
  Future<void> markInquiryRead(int id) =>
      _api.put<Object?>('/inquiries/$id/read');

  Future<ChatMessage> sendInquiryMessage(int id, String content) async {
    final data = await _api.post<Map<String, dynamic>>(
      '/inquiries/$id/messages',
      data: {'content': content},
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

  Future<BankCardInfo?> bankCard() async {
    final data = await _api.get<Object?>('/wallet/bank-card');
    if (data is! Map) return null;
    return BankCardInfo.fromJson(Map<String, dynamic>.from(data));
  }

  Future<BankCardInfo> bindBankCard({
    required String holderName,
    required String bankName,
    required String cardNumber,
  }) async {
    final data = await _api.put<Map<String, dynamic>>(
      '/wallet/bank-card',
      data: {
        'holderName': holderName,
        'bankName': bankName,
        'cardNumber': cardNumber,
      },
    );
    return BankCardInfo.fromJson(data);
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

  Future<void> withdraw(double amount) {
    return _api.post<Object?>('/wallet/withdrawals', data: {'amount': amount});
  }

  Future<Map<String, dynamic>> rechargeCapability() {
    return _api.get<Map<String, dynamic>>('/recharges/capability');
  }

  Future<Map<String, dynamic>> createRecharge(double amount) {
    return _api.post<Map<String, dynamic>>(
      '/recharges',
      data: {'amount': amount},
    );
  }

  Future<List<CertificationRecord>> certifications() async {
    final data = await _api.get<List<dynamic>>('/certifications/me');
    return data
        .whereType<Map>()
        .map(
          (item) =>
              CertificationRecord.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<void> submitBasicCertification(
    String type,
    List<UploadFile> files,
  ) async {
    await _api.post<Object?>(
      '/certifications/basic/$type',
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
    required List<UploadFile> files,
  }) async {
    await _api.post<Object?>(
      '/certifications/experiences',
      data: FormData.fromMap({
        if (existingId != null) 'existingId': existingId,
        'title': title,
        'description': description,
        'files': await Future.wait(
          files.map(
            (file) => MultipartFile.fromFile(file.path, filename: file.name),
          ),
        ),
      }),
    );
  }

  Future<List<AppNotification>> notifications() async {
    final data = await _api.get<List<dynamic>>('/notifications');
    return data
        .whereType<Map>()
        .map(
          (item) => AppNotification.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<int> notificationUnreadCount() async {
    final data = await _api.get<Object?>('/notifications/unread-count');
    if (data is num) return data.toInt();
    if (data is Map) return _int(data['count'] ?? data['unreadCount']);
    return _int(data);
  }

  Future<void> readNotification(int id) =>
      _api.put<Object?>('/notifications/$id/read');
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

  Future<List<CustomerServiceMessage>> customerServiceMessages() async {
    final data = await _api.get<List<dynamic>>(
      '/support/customer-service/messages',
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

  Future<int> customerServiceUnreadCount() async {
    final data = await _api.get<Object?>(
      '/support/customer-service/unread-count',
    );
    if (data is num) return data.toInt();
    if (data is Map) return _int(data['count'] ?? data['unreadCount']);
    return _int(data);
  }

  Future<void> readCustomerServiceMessages() {
    return _api.put<Object?>('/support/customer-service/read');
  }

  Future<Map<String, dynamic>> realtimeTicket() {
    return _api.post<Map<String, dynamic>>('/realtime/tickets');
  }

  List<Answerer> _answererList(List<dynamic> data) => data
      .whereType<Map>()
      .map((item) => Answerer.fromJson(Map<String, dynamic>.from(item)))
      .toList(growable: false);

  List<DiscoveryCategory> _categoryList(List<dynamic> data) => data
      .whereType<Map>()
      .map(
        (item) => DiscoveryCategory.fromJson(Map<String, dynamic>.from(item)),
      )
      .toList(growable: false);

  List<DiscoverySearchItem> _searchList(List<dynamic> data) => data
      .whereType<Map>()
      .map(
        (item) => DiscoverySearchItem.fromJson(Map<String, dynamic>.from(item)),
      )
      .toList(growable: false);
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
