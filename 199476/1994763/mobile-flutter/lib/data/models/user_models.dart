class AppUser {
  const AppUser({
    required this.id,
    required this.uid,
    required this.phone,
    required this.nickname,
    required this.avatarUrl,
    required this.acceptingInquiries,
    required this.acceptingInquiriesUpdatedAt,
    required this.inquiryPriceMin,
    required this.inquiryPriceMax,
    required this.inquiryPriceUpdatedAt,
    required this.answererStatus,
    required this.platformIntroductionRequired,
  });

  factory AppUser.fromJson(Map<String, dynamic> json) {
    return AppUser(
      id: _int(json['id']),
      uid: json['uid']?.toString() ?? '',
      phone: json['phone']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString() ?? '',
      acceptingInquiries: json['acceptingInquiries'] == true,
      acceptingInquiriesUpdatedAt: _dateTime(
        json['acceptingInquiriesUpdatedAt'],
      ),
      inquiryPriceMin: _boundedInt(json['inquiryPriceMin'], 1),
      inquiryPriceMax: _boundedInt(json['inquiryPriceMax'], 5000),
      inquiryPriceUpdatedAt: _dateTime(json['inquiryPriceUpdatedAt']),
      answererStatus: json['answererStatus']?.toString() ?? '',
      platformIntroductionRequired:
          json['platformIntroductionRequired'] == true,
    );
  }

  final int id;
  final String uid;
  final String phone;
  final String nickname;
  final String avatarUrl;
  final bool acceptingInquiries;
  final DateTime? acceptingInquiriesUpdatedAt;
  final int inquiryPriceMin;
  final int inquiryPriceMax;
  final DateTime? inquiryPriceUpdatedAt;
  final String answererStatus;
  final bool platformIntroductionRequired;

  String get displayName => nickname.trim().isEmpty ? 'UID $uid' : nickname;

  AppUser copyWith({
    String? nickname,
    String? avatarUrl,
    bool? acceptingInquiries,
    DateTime? acceptingInquiriesUpdatedAt,
    int? inquiryPriceMin,
    int? inquiryPriceMax,
    DateTime? inquiryPriceUpdatedAt,
    String? answererStatus,
    bool? platformIntroductionRequired,
  }) {
    return AppUser(
      id: id,
      uid: uid,
      phone: phone,
      nickname: nickname ?? this.nickname,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      acceptingInquiries: acceptingInquiries ?? this.acceptingInquiries,
      acceptingInquiriesUpdatedAt:
          acceptingInquiriesUpdatedAt ?? this.acceptingInquiriesUpdatedAt,
      inquiryPriceMin: inquiryPriceMin ?? this.inquiryPriceMin,
      inquiryPriceMax: inquiryPriceMax ?? this.inquiryPriceMax,
      inquiryPriceUpdatedAt:
          inquiryPriceUpdatedAt ?? this.inquiryPriceUpdatedAt,
      answererStatus: answererStatus ?? this.answererStatus,
      platformIntroductionRequired:
          platformIntroductionRequired ?? this.platformIntroductionRequired,
    );
  }
}

class LoginResult {
  const LoginResult({required this.token, required this.user});

  factory LoginResult.fromJson(Map<String, dynamic> json) {
    return LoginResult(
      token: json['token']?.toString() ?? '',
      user: AppUser.fromJson(json['user'] as Map<String, dynamic>),
    );
  }

  final String token;
  final AppUser user;
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;

int _boundedInt(Object? value, int fallback) {
  final parsed = value is num ? value.toInt() : int.tryParse('$value');
  if (parsed == null) return fallback;
  if (parsed < 1) return 1;
  if (parsed > 5000) return 5000;
  return parsed;
}

DateTime? _dateTime(Object? value) =>
    value == null ? null : DateTime.tryParse(value.toString());
