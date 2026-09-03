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

class CertificationRecord {
  const CertificationRecord({
    required this.id,
    required this.category,
    required this.type,
    required this.title,
    required this.description,
    required this.required,
    required this.status,
    required this.enabled,
    required this.rejectionReason,
    required this.experienceBusinessType,
    required this.upgradeSourceId,
    required this.mediaProcessingStatus,
    required this.mediaProcessingError,
    required this.lastOperatedAt,
    required this.materials,
  });

  factory CertificationRecord.fromJson(Map<String, dynamic> json) {
    return CertificationRecord(
      id: _int(json['id']),
      category: json['category']?.toString() ?? '',
      type: json['type']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      required: json['required'] == true,
      status: json['status']?.toString() ?? '',
      enabled: json['enabled'] != false,
      rejectionReason: json['rejectionReason']?.toString() ?? '',
      experienceBusinessType:
          json['experienceBusinessType']?.toString() ?? 'MONETIZED',
      upgradeSourceId: _nullableInt(json['upgradeSourceId']),
      mediaProcessingStatus:
          json['mediaProcessingStatus']?.toString() ?? 'NOT_REQUIRED',
      mediaProcessingError: json['mediaProcessingError']?.toString() ?? '',
      lastOperatedAt: DateTime.tryParse(
        json['lastOperatedAt']?.toString() ?? '',
      ),
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
  final bool required;
  final String status;
  final bool enabled;
  final String rejectionReason;
  final String experienceBusinessType;
  final int? upgradeSourceId;
  final String mediaProcessingStatus;
  final String mediaProcessingError;
  final DateTime? lastOperatedAt;
  final List<CertificationMaterial> materials;

  bool get approved => status.toUpperCase() == 'APPROVED' || status == '已认证';
  bool get pending => status.toUpperCase() == 'PENDING' || status == '审核中';
  bool get isPublicWelfare => experienceBusinessType == 'PUBLIC_WELFARE';
  bool get isMonetized => !isPublicWelfare;
}

int _int(Object? value) {
  if (value is num) return value.toInt();
  return num.tryParse('$value')?.toInt() ?? 0;
}

int? _nullableInt(Object? value) => value == null ? null : _int(value);
