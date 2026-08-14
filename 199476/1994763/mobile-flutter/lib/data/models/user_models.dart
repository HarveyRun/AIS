class AppUser {
  const AppUser({
    required this.id,
    required this.uid,
    required this.phone,
    required this.nickname,
    required this.avatarUrl,
    required this.acceptingInquiries,
    required this.answererStatus,
  });

  factory AppUser.fromJson(Map<String, dynamic> json) {
    return AppUser(
      id: _int(json['id']),
      uid: json['uid']?.toString() ?? '',
      phone: json['phone']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString() ?? '',
      acceptingInquiries: json['acceptingInquiries'] == true,
      answererStatus: json['answererStatus']?.toString() ?? '',
    );
  }

  final int id;
  final String uid;
  final String phone;
  final String nickname;
  final String avatarUrl;
  final bool acceptingInquiries;
  final String answererStatus;

  String get displayName => nickname.trim().isEmpty ? 'UID $uid' : nickname;

  AppUser copyWith({
    String? nickname,
    String? avatarUrl,
    bool? acceptingInquiries,
    String? answererStatus,
  }) {
    return AppUser(
      id: id,
      uid: uid,
      phone: phone,
      nickname: nickname ?? this.nickname,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      acceptingInquiries: acceptingInquiries ?? this.acceptingInquiries,
      answererStatus: answererStatus ?? this.answererStatus,
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
