class CertificationMaterial {
  const CertificationMaterial({
    required this.id,
    required this.kind,
    required this.name,
    required this.url,
    required this.size,
    required this.contentType,
    this.selected = false,
  });

  factory CertificationMaterial.fromJson(Map<String, dynamic> json) {
    return CertificationMaterial(
      id: _int(json['id']),
      kind: json['kind']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      url: json['url']?.toString() ?? '',
      size: _int(json['size']),
      contentType: json['contentType']?.toString() ?? '',
      selected: json['selected'] == true,
    );
  }

  final int id;
  final String kind;
  final String name;
  final String url;
  final int size;
  final String contentType;
  final bool selected;
}

class ExperiencePublicMediaView {
  const ExperiencePublicMediaView({
    required this.certificationId,
    required this.processingStatus,
    required this.processingError,
    required this.items,
  });

  factory ExperiencePublicMediaView.fromJson(Map<String, dynamic> json) {
    return ExperiencePublicMediaView(
      certificationId: _int(json['certificationId']),
      processingStatus: json['processingStatus']?.toString() ?? '',
      processingError: json['processingError']?.toString() ?? '',
      items: (json['items'] as List<dynamic>? ?? const [])
          .whereType<Map>()
          .map(
            (item) =>
                CertificationMaterial.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList(growable: false),
    );
  }

  final int certificationId;
  final String processingStatus;
  final String processingError;
  final List<CertificationMaterial> items;
}

class ExperienceAdditionalInfo {
  const ExperienceAdditionalInfo({
    this.location = '',
    this.startDate = '',
    this.endDate = '',
    this.count,
    this.role = '',
    this.ageRange = '',
    this.education = '',
    this.job = '',
  });

  factory ExperienceAdditionalInfo.fromJson(Map<String, dynamic> json) {
    return ExperienceAdditionalInfo(
      location: json['experienceLocation']?.toString() ?? '',
      startDate: json['experienceStartDate']?.toString() ?? '',
      endDate: json['experienceEndDate']?.toString() ?? '',
      count: _nullableInt(json['experienceCount']),
      role: json['experienceRole']?.toString() ?? '',
      ageRange: json['experienceAgeRange']?.toString() ?? '',
      education: json['experienceEducation']?.toString() ?? '',
      job: json['experienceJob']?.toString() ?? '',
    );
  }

  final String location;
  final String startDate;
  final String endDate;
  final int? count;
  final String role;
  final String ageRange;
  final String education;
  final String job;

  bool get isEmpty =>
      location.isEmpty &&
      startDate.isEmpty &&
      endDate.isEmpty &&
      count == null &&
      role.isEmpty &&
      ageRange.isEmpty &&
      education.isEmpty &&
      job.isEmpty;

  int get completedCount => [
    location.isNotEmpty,
    startDate.isNotEmpty,
    endDate.isNotEmpty,
    count != null,
    role.isNotEmpty,
    ageRange.isNotEmpty,
    education.isNotEmpty,
    job.isNotEmpty,
  ].where((item) => item).length;
}

class CertificationRecord {
  const CertificationRecord({
    required this.id,
    required this.category,
    required this.type,
    required this.title,
    required this.description,
    required this.additionalInfo,
    required this.status,
    required this.enabled,
    required this.rejectionReason,
    required this.mediaProcessingStatus,
    required this.mediaProcessingError,
    required this.lastOperatedAt,
    required this.referenceIndex,
    required this.materialSupportScore,
    required this.commonRelevanceScore,
    required this.learnabilityScore,
    required this.clarityScore,
    required this.logicConsistencyScore,
    required this.materials,
  });

  factory CertificationRecord.fromJson(Map<String, dynamic> json) {
    return CertificationRecord(
      id: _int(json['id']),
      category: json['category']?.toString() ?? '',
      type: json['type']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      additionalInfo: ExperienceAdditionalInfo.fromJson(json),
      status: json['status']?.toString() ?? '',
      enabled: json['enabled'] != false,
      rejectionReason: json['rejectionReason']?.toString() ?? '',
      mediaProcessingStatus:
          json['mediaProcessingStatus']?.toString() ?? 'NOT_REQUIRED',
      mediaProcessingError: json['mediaProcessingError']?.toString() ?? '',
      lastOperatedAt: DateTime.tryParse(
        json['lastOperatedAt']?.toString() ?? '',
      ),
      referenceIndex: _nullableInt(json['referenceIndex']),
      materialSupportScore: _nullableInt(json['materialSupportScore']),
      commonRelevanceScore: _nullableInt(json['commonRelevanceScore']),
      learnabilityScore: _nullableInt(json['learnabilityScore']),
      clarityScore: _nullableInt(json['clarityScore']),
      logicConsistencyScore: _nullableInt(json['logicConsistencyScore']),
      materials: (json['materials'] as List<dynamic>? ?? const [])
          .whereType<Map>()
          .map(
            (item) =>
                CertificationMaterial.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList(growable: false),
    );
  }

  final int id;
  final String category;
  final String type;
  final String title;
  final String description;
  final ExperienceAdditionalInfo additionalInfo;
  final String status;
  final bool enabled;
  final String rejectionReason;
  final String mediaProcessingStatus;
  final String mediaProcessingError;
  final DateTime? lastOperatedAt;
  final int? referenceIndex;
  final int? materialSupportScore;
  final int? commonRelevanceScore;
  final int? learnabilityScore;
  final int? clarityScore;
  final int? logicConsistencyScore;
  final List<CertificationMaterial> materials;

  bool get approved => status.toUpperCase() == 'APPROVED' || status == '已认证';
  bool get pending => status.toUpperCase() == 'PENDING' || status == '审核中';
}

int _int(Object? value) {
  if (value is num) return value.toInt();
  return num.tryParse('$value')?.toInt() ?? 0;
}

int? _nullableInt(Object? value) {
  if (value == null || '$value'.trim().isEmpty) return null;
  if (value is num) return value.toInt();
  return int.tryParse('$value');
}
