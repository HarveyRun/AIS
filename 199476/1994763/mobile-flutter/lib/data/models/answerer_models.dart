class AnswererExperience {
  const AnswererExperience({
    required this.certificationId,
    required this.title,
    required this.description,
    required this.years,
    required this.discoveryCategoryId,
    required this.discoveryExperienceId,
  });

  factory AnswererExperience.fromJson(Map<String, dynamic> json) {
    return AnswererExperience(
      certificationId: _nullableInt(json['certificationId']),
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      years: _nullableInt(json['years']),
      discoveryCategoryId: _nullableInt(json['discoveryCategoryId']),
      discoveryExperienceId: _nullableInt(json['discoveryExperienceId']),
    );
  }

  final int? certificationId;
  final String title;
  final String description;
  final int? years;
  final int? discoveryCategoryId;
  final int? discoveryExperienceId;
}

class Answerer {
  const Answerer({
    required this.id,
    required this.uid,
    required this.nickname,
    required this.avatarUrl,
    required this.acceptingInquiries,
    required this.mainJob,
    required this.mainJobYears,
    required this.capabilityDescription,
    required this.experiences,
  });

  factory Answerer.fromJson(Map<String, dynamic> json) {
    final experienceData = json['experiences'] as List<dynamic>? ?? const [];
    return Answerer(
      id: _int(json['id']),
      uid: json['uid']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString() ?? '',
      acceptingInquiries: json['acceptingInquiries'] == true,
      mainJob: json['mainJob']?.toString() ?? '-',
      mainJobYears: _nullableInt(json['mainJobYears']) ?? 0,
      capabilityDescription: json['capabilityDescription']?.toString() ?? '',
      experiences: experienceData
          .whereType<Map<String, dynamic>>()
          .map(AnswererExperience.fromJson)
          .toList(growable: false),
    );
  }

  final int id;
  final String uid;
  final String nickname;
  final String avatarUrl;
  final bool acceptingInquiries;
  final String mainJob;
  final int mainJobYears;
  final String capabilityDescription;
  final List<AnswererExperience> experiences;

  String get displayName => nickname.trim().isEmpty ? 'UID $uid' : nickname;
}

class AnswererPageData {
  const AnswererPageData({
    required this.items,
    required this.page,
    required this.hasMore,
  });

  factory AnswererPageData.fromJson(Map<String, dynamic> json) {
    return AnswererPageData(
      items: (json['items'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(Answerer.fromJson)
          .toList(growable: false),
      page: _int(json['page']),
      hasMore: json['hasMore'] == true,
    );
  }

  final List<Answerer> items;
  final int page;
  final bool hasMore;
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
int? _nullableInt(Object? value) => value == null ? null : _int(value);
