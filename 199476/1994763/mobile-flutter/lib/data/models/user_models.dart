class AppUser {
  const AppUser({
    required this.id,
    required this.uid,
    required this.phone,
    required this.nickname,
    required this.avatarUrl,
    required this.jobTitle,
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
      jobTitle: json['jobTitle']?.toString() ?? '',
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
  final String jobTitle;
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
    String? jobTitle,
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
      jobTitle: jobTitle ?? this.jobTitle,
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

class ViolationCounter {
  const ViolationCounter({
    required this.level,
    required this.usedCount,
    required this.thresholdCount,
    required this.remainingCount,
  });

  factory ViolationCounter.fromJson(Map<String, dynamic> json) {
    return ViolationCounter(
      level: _int(json['level']),
      usedCount: _int(json['usedCount']),
      thresholdCount: _int(json['thresholdCount']),
      remainingCount: _int(json['remainingCount']),
    );
  }

  final int level;
  final int usedCount;
  final int thresholdCount;
  final int remainingCount;
}

class AccountDeletionEligibility {
  const AccountDeletionEligibility({
    required this.eligible,
    required this.availableBalanceCleared,
    required this.frozenBalanceCleared,
    required this.noActiveInquiries,
    required this.availableBalance,
    required this.frozenBalance,
  });

  factory AccountDeletionEligibility.fromJson(Map<String, dynamic> json) {
    return AccountDeletionEligibility(
      eligible: json['eligible'] == true,
      availableBalanceCleared: json['availableBalanceCleared'] == true,
      frozenBalanceCleared: json['frozenBalanceCleared'] == true,
      noActiveInquiries: json['noActiveInquiries'] == true,
      availableBalance: _amount(json['availableBalance']),
      frozenBalance: _amount(json['frozenBalance']),
    );
  }

  final bool eligible;
  final bool availableBalanceCleared;
  final bool frozenBalanceCleared;
  final bool noActiveInquiries;
  final double availableBalance;
  final double frozenBalance;
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;

double _amount(Object? value) =>
    value is num ? value.toDouble() : double.tryParse('$value') ?? 0;

int _boundedInt(Object? value, int fallback) {
  final parsed = value is num ? value.toInt() : int.tryParse('$value');
  if (parsed == null) return fallback;
  if (parsed < 1) return 1;
  if (parsed > 5000) return 5000;
  return parsed;
}

DateTime? _dateTime(Object? value) =>
    value == null ? null : DateTime.tryParse(value.toString());
