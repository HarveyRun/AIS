class AppGlobalSettings {
  const AppGlobalSettings({
    required this.androidLaborFeeRate,
    required this.iosLaborFeeRate,
    required this.tipPresetAmounts,
    required this.tipMaxAmount,
    required this.withdrawalMinAmount,
    required this.withdrawalMaxAmount,
    required this.withdrawalDailyLimit,
    required this.incomeHoldHours,
    required this.payoutAccountCooldownHours,
    required this.initialTextMessageLimit,
    required this.textMessageMaxLength,
    required this.voiceRewardSeconds,
    required this.voiceRewardMessages,
    required this.voiceRingTimeoutSeconds,
    required this.voiceReconnectTimeoutSeconds,
    required this.freePendingInquiryLimit,
    required this.inquiryResponseTimeoutHours,
    required this.inquiryMaxDurationDays,
    required this.inquiryDepositAmount,
    required this.homePageSize,
    required this.hourlyRateMin,
    required this.hourlyRateMax,
    required this.experienceTitleMaxLength,
    required this.experienceDescriptionMaxLength,
    required this.proofArchiveMaxBytes,
    required this.rechargeEnabled,
    required this.withdrawalEnabled,
    required this.voiceCallEnabled,
    required this.experienceTipEnabled,
  });

  factory AppGlobalSettings.fromJson(Map<String, dynamic> json) {
    final rules = json['rules'] is Map
        ? Map<String, dynamic>.from(json['rules'] as Map)
        : json;
    return AppGlobalSettings(
      androidLaborFeeRate: _double(json['androidLaborFeeRate'], .05),
      iosLaborFeeRate: _double(json['iosLaborFeeRate'], .05),
      tipPresetAmounts:
          (rules['tipPresetAmounts'] as List? ?? const [1, 3, 5, 18, 68])
              .map((v) => _int(v, 1))
              .toList(),
      tipMaxAmount: _int(rules['tipMaxAmount'], 5000),
      withdrawalMinAmount: _double(rules['withdrawalMinAmount'], 1),
      withdrawalMaxAmount: _double(rules['withdrawalMaxAmount'], 9999),
      withdrawalDailyLimit: _double(rules['withdrawalDailyLimit'], 20000),
      incomeHoldHours: _int(rules['incomeHoldHours'], 24),
      payoutAccountCooldownHours: _int(rules['payoutAccountCooldownHours'], 24),
      initialTextMessageLimit: _int(rules['initialTextMessageLimit'], 50),
      textMessageMaxLength: _int(rules['textMessageMaxLength'], 100),
      voiceRewardSeconds: _int(rules['voiceRewardSeconds'], 300),
      voiceRewardMessages: _int(rules['voiceRewardMessages'], 50),
      voiceRingTimeoutSeconds: _int(rules['voiceRingTimeoutSeconds'], 60),
      voiceReconnectTimeoutSeconds: _int(
        rules['voiceReconnectTimeoutSeconds'],
        60,
      ),
      freePendingInquiryLimit: _int(rules['freePendingInquiryLimit'], 3),
      inquiryResponseTimeoutHours: _int(
        rules['inquiryResponseTimeoutHours'],
        72,
      ),
      inquiryMaxDurationDays: _int(rules['inquiryMaxDurationDays'], 20),
      inquiryDepositAmount: _double(rules['inquiryDepositAmount'], 2),
      homePageSize: _int(rules['homePageSize'], 20),
      hourlyRateMin: _int(rules['hourlyRateMin'], 1),
      hourlyRateMax: _int(rules['hourlyRateMax'], 5000),
      experienceTitleMaxLength: _int(rules['experienceTitleMaxLength'], 18),
      experienceDescriptionMaxLength: _int(
        rules['experienceDescriptionMaxLength'],
        400,
      ),
      proofArchiveMaxBytes: _int(rules['proofArchiveMaxBytes'], 2147483648),
      rechargeEnabled: rules['rechargeEnabled'] != false,
      withdrawalEnabled: rules['withdrawalEnabled'] != false,
      voiceCallEnabled: rules['voiceCallEnabled'] != false,
      experienceTipEnabled: rules['experienceTipEnabled'] != false,
    );
  }

  final double androidLaborFeeRate;
  final double iosLaborFeeRate;
  final List<int> tipPresetAmounts;
  final int tipMaxAmount;
  final double withdrawalMinAmount;
  final double withdrawalMaxAmount;
  final double withdrawalDailyLimit;
  final int incomeHoldHours;
  final int payoutAccountCooldownHours;
  final int initialTextMessageLimit;
  final int textMessageMaxLength;
  final int voiceRewardSeconds;
  final int voiceRewardMessages;
  final int voiceRingTimeoutSeconds;
  final int voiceReconnectTimeoutSeconds;
  final int freePendingInquiryLimit;
  final int inquiryResponseTimeoutHours;
  final int inquiryMaxDurationDays;
  final double inquiryDepositAmount;
  final int homePageSize;
  final int hourlyRateMin;
  final int hourlyRateMax;
  final int experienceTitleMaxLength;
  final int experienceDescriptionMaxLength;
  final int proofArchiveMaxBytes;
  final bool rechargeEnabled;
  final bool withdrawalEnabled;
  final bool voiceCallEnabled;
  final bool experienceTipEnabled;
}

int _int(Object? value, int fallback) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? fallback;
double _double(Object? value, double fallback) =>
    value is num ? value.toDouble() : double.tryParse('$value') ?? fallback;
