class CertificationMaterial {
  const CertificationMaterial({
    required this.id,
    required this.kind,
    required this.name,
    required this.url,
    required this.size,
    required this.contentType,
  });

  factory CertificationMaterial.fromJson(Map<String, dynamic> json) {
    return CertificationMaterial(
      id: _int(json['id']),
      kind: json['kind']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      url: json['url']?.toString() ?? '',
      size: _int(json['size']),
      contentType: json['contentType']?.toString() ?? '',
    );
  }

  final int id;
  final String kind;
  final String name;
  final String url;
  final int size;
  final String contentType;
}

class CertificationRecord {
  const CertificationRecord({
    required this.id,
    required this.category,
    required this.type,
    required this.title,
    required this.description,
    required this.years,
    required this.authenticityPercent,
    required this.jobReapplyAvailableAt,
    required this.required,
    required this.status,
    required this.enabled,
    required this.rejectionReason,
    required this.materials,
  });

  factory CertificationRecord.fromJson(Map<String, dynamic> json) {
    return CertificationRecord(
      id: _int(json['id']),
      category: json['category']?.toString() ?? '',
      type: json['type']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      years: _nullableInt(json['years']),
      authenticityPercent: _nullableInt(json['authenticityPercent']),
      jobReapplyAvailableAt: _nullableDateTime(json['jobReapplyAvailableAt']),
      required: json['required'] == true,
      status: json['status']?.toString() ?? '',
      enabled: json['enabled'] != false,
      rejectionReason: json['rejectionReason']?.toString() ?? '',
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
  final int? years;
  final int? authenticityPercent;
  final DateTime? jobReapplyAvailableAt;
  final bool required;
  final String status;
  final bool enabled;
  final String rejectionReason;
  final List<CertificationMaterial> materials;

  bool get approved => status.toUpperCase() == 'APPROVED' || status == '已认证';
  bool get pending => status.toUpperCase() == 'PENDING' || status == '审核中';
}

class JobCertificationAppointment {
  const JobCertificationAppointment({
    required this.id,
    required this.appointmentAt,
    required this.city,
    required this.status,
  });

  factory JobCertificationAppointment.fromJson(Map<String, dynamic> json) {
    return JobCertificationAppointment(
      id: _int(json['id']),
      appointmentAt: DateTime.parse(json['appointmentAt'].toString()),
      city: json['city']?.toString() ?? '北京',
      status: json['status']?.toString() ?? '',
    );
  }

  final int id;
  final DateTime appointmentAt;
  final String city;
  final String status;
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
int? _nullableInt(Object? value) => value == null ? null : _int(value);
DateTime? _nullableDateTime(Object? value) =>
    value == null ? null : DateTime.tryParse(value.toString());
