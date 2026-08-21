class InvitationCampaignStatus {
  const InvitationCampaignStatus({
    required this.active,
    required this.ownInvitationCode,
    required this.eligible,
    required this.submitted,
    required this.inviterUid,
    required this.status,
    required this.rewardAmount,
  });

  factory InvitationCampaignStatus.fromJson(Map<String, dynamic> json) {
    return InvitationCampaignStatus(
      active: json['active'] == true,
      ownInvitationCode: json['ownInvitationCode']?.toString() ?? '',
      eligible: json['eligible'] == true,
      submitted: json['submitted'] == true,
      inviterUid: json['inviterUid']?.toString(),
      status: json['status']?.toString() ?? '',
      rewardAmount: _amount(json['rewardAmount']),
    );
  }

  final bool active;
  final String ownInvitationCode;
  final bool eligible;
  final bool submitted;
  final String? inviterUid;
  final String status;
  final double rewardAmount;
}

double _amount(Object? value) {
  if (value is num) return value.toDouble();
  return double.tryParse('$value') ?? 0;
}
