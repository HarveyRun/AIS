import 'certification_models.dart';

class AnswererExperience {
  const AnswererExperience({
    required this.certificationId,
    required this.title,
    required this.description,
    required this.businessType,
    required this.canInquire,
    required this.materials,
  });

  factory AnswererExperience.fromJson(Map<String, dynamic> json) {
    return AnswererExperience(
      certificationId: _nullableInt(json['certificationId']),
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      businessType: json['businessType']?.toString() ?? 'MONETIZED',
      canInquire: json['canInquire'] == true,
      materials:
          [
                ...(json['materials'] as List<dynamic>? ?? const []),
                ...(json['publicMedia'] as List<dynamic>? ?? const []),
              ]
              .whereType<Map>()
              .map(
                (item) => CertificationMaterial.fromJson(
                  Map<String, dynamic>.from(item),
                ),
              )
              .toList(growable: false),
    );
  }

  final int? certificationId;
  final String title;
  final String description;
  final String businessType;
  final bool canInquire;
  final List<CertificationMaterial> materials;

  bool get isPublicWelfare => businessType == 'PUBLIC_WELFARE';
}

class Answerer {
  const Answerer({
    required this.id,
    required this.uid,
    required this.nickname,
    required this.avatarUrl,
    required this.identityVerified,
    required this.acceptingInquiries,
    required this.inquiryPriceMin,
    required this.inquiryPriceMax,
    required this.mainJob,
    required this.experiences,
  });

  factory Answerer.fromJson(Map<String, dynamic> json) {
    final experienceData = json['experiences'] as List<dynamic>? ?? const [];
    return Answerer(
      id: _int(json['id']),
      uid: json['uid']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString() ?? '',
      identityVerified: json['identityVerified'] == true,
      acceptingInquiries: json['acceptingInquiries'] == true,
      inquiryPriceMin: _boundedInt(json['inquiryPriceMin'], 1),
      inquiryPriceMax: _boundedInt(json['inquiryPriceMax'], 5000),
      mainJob: json['mainJob']?.toString() ?? '-',
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
  final bool identityVerified;
  final bool acceptingInquiries;
  final int inquiryPriceMin;
  final int inquiryPriceMax;
  final String mainJob;
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

int _int(Object? value) {
  if (value is num) return value.toInt();
  return num.tryParse('$value')?.toInt() ?? 0;
}

int? _nullableInt(Object? value) => value == null ? null : _int(value);

int _boundedInt(Object? value, int fallback) {
  final parsed = value is num ? value.toInt() : int.tryParse('$value');
  if (parsed == null) return fallback;
  if (parsed < 1) return 1;
  if (parsed > 5000) return 5000;
  return parsed;
}
