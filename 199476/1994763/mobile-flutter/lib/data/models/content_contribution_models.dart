class ContentContributionSummary {
  const ContentContributionSummary({
    required this.totalSubmitted,
    required this.totalLimit,
    required this.pendingCount,
    required this.pendingLimit,
    required this.adoptedCount,
    required this.earnedReward,
    required this.canSubmit,
    required this.minimumReward,
    required this.maximumReward,
  });

  factory ContentContributionSummary.fromJson(Map<String, dynamic> json) {
    return ContentContributionSummary(
      totalSubmitted: _int(json['totalSubmitted']),
      totalLimit: _int(json['totalLimit']),
      pendingCount: _int(json['pendingCount']),
      pendingLimit: _int(json['pendingLimit']),
      adoptedCount: _int(json['adoptedCount']),
      earnedReward: _amount(json['earnedReward']) ?? 0,
      canSubmit: json['canSubmit'] == true,
      minimumReward: _int(json['minimumReward']),
      maximumReward: _int(json['maximumReward']),
    );
  }

  final int totalSubmitted;
  final int totalLimit;
  final int pendingCount;
  final int pendingLimit;
  final int adoptedCount;
  final double earnedReward;
  final bool canSubmit;
  final int minimumReward;
  final int maximumReward;
}

class ContentContributionJob {
  const ContentContributionJob({
    required this.id,
    required this.jobName,
    required this.responsibility,
    required this.sortOrder,
  });

  factory ContentContributionJob.fromJson(Map<String, dynamic> json) {
    return ContentContributionJob(
      id: _int(json['id']),
      jobName: json['jobName']?.toString() ?? '',
      responsibility: json['responsibility']?.toString() ?? '',
      sortOrder: _int(json['sortOrder']),
    );
  }

  final int id;
  final String jobName;
  final String responsibility;
  final int sortOrder;
}

class ContentContribution {
  const ContentContribution({
    required this.id,
    required this.matterName,
    required this.jobs,
    required this.status,
    required this.standardMatterName,
    required this.rewardAmount,
    required this.reviewReason,
    required this.violationLevel,
    required this.createdAt,
    required this.reviewedAt,
  });

  factory ContentContribution.fromJson(Map<String, dynamic> json) {
    return ContentContribution(
      id: _int(json['id']),
      matterName: json['matterName']?.toString() ?? '',
      jobs: (json['jobs'] as List<dynamic>? ?? const [])
          .whereType<Map>()
          .map(
            (item) => ContentContributionJob.fromJson(
              Map<String, dynamic>.from(item),
            ),
          )
          .toList(growable: false),
      status: json['status']?.toString() ?? 'PENDING',
      standardMatterName: json['standardMatterName']?.toString() ?? '',
      rewardAmount: _amount(json['rewardAmount']),
      reviewReason: json['reviewReason']?.toString() ?? '',
      violationLevel: json['violationLevel'] == null
          ? null
          : _int(json['violationLevel']),
      createdAt: _date(json['createdAt']),
      reviewedAt: _date(json['reviewedAt']),
    );
  }

  final int id;
  final String matterName;
  final List<ContentContributionJob> jobs;
  final String status;
  final String standardMatterName;
  final double? rewardAmount;
  final String reviewReason;
  final int? violationLevel;
  final DateTime? createdAt;
  final DateTime? reviewedAt;

  bool get isPending => status == 'PENDING';
  bool get isAdopted => status == 'ADOPTED';
  bool get isViolation => status == 'VIOLATION_REJECTED';
}

class ContentContributionPageData {
  const ContentContributionPageData({required this.items, required this.total});

  factory ContentContributionPageData.fromJson(Map<String, dynamic> json) {
    return ContentContributionPageData(
      items: (json['items'] as List<dynamic>? ?? const [])
          .whereType<Map>()
          .map(
            (item) =>
                ContentContribution.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList(growable: false),
      total: _int(json['total']),
    );
  }

  final List<ContentContribution> items;
  final int total;
}

class ContentContributionDraftJob {
  const ContentContributionDraftJob({
    required this.jobName,
    required this.responsibility,
  });

  final String jobName;
  final String responsibility;

  Map<String, dynamic> toJson() => {
    'jobName': jobName,
    'responsibility': responsibility,
  };
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
double? _amount(Object? value) =>
    value == null ? null : double.tryParse('$value');
DateTime? _date(Object? value) =>
    value == null ? null : DateTime.tryParse('$value')?.toLocal();
