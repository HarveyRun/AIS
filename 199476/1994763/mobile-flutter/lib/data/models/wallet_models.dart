class WalletInfo {
  const WalletInfo({
    required this.availableBalance,
    required this.frozenBalance,
    required this.rechargeBalance,
    required this.withdrawableIncome,
    required this.pendingIncome,
    required this.totalWithdrawn,
  });

  factory WalletInfo.fromJson(Map<String, dynamic> json) {
    return WalletInfo(
      availableBalance: _double(json['availableBalance']),
      frozenBalance: _double(json['frozenBalance']),
      rechargeBalance: _double(json['rechargeBalance']),
      withdrawableIncome: _double(json['withdrawableIncome']),
      pendingIncome: _double(json['pendingIncome']),
      totalWithdrawn: _double(json['totalWithdrawn']),
    );
  }

  final double availableBalance;
  final double frozenBalance;
  final double rechargeBalance;
  final double withdrawableIncome;
  final double pendingIncome;
  final double totalWithdrawn;
}

class WalletTransaction {
  const WalletTransaction({
    required this.id,
    required this.type,
    required this.direction,
    required this.amount,
    required this.availableAfter,
    required this.frozenAfter,
    required this.description,
    required this.createdAt,
  });

  factory WalletTransaction.fromJson(Map<String, dynamic> json) {
    return WalletTransaction(
      id: _int(json['id']),
      type: json['type']?.toString() ?? '',
      direction: json['direction']?.toString() ?? '',
      amount: _double(json['amount']),
      availableAfter: _double(json['availableAfter']),
      frozenAfter: _double(json['frozenAfter']),
      description: json['description']?.toString() ?? '',
      createdAt: _date(json['createdAt']),
    );
  }

  final int id;
  final String type;
  final String direction;
  final double amount;
  final double availableAfter;
  final double frozenAfter;
  final String description;
  final DateTime? createdAt;
}

class AlipayAccountInfo {
  const AlipayAccountInfo({
    required this.id,
    required this.displayName,
    required this.identifierType,
    required this.accountMasked,
    required this.authorizedAt,
  });

  factory AlipayAccountInfo.fromJson(Map<String, dynamic> json) {
    return AlipayAccountInfo(
      id: _int(json['id']),
      displayName: json['displayName']?.toString() ?? '已授权支付宝账户',
      identifierType: json['identifierType']?.toString() ?? '',
      accountMasked: json['accountMasked']?.toString() ?? '',
      authorizedAt: _date(json['authorizedAt']),
    );
  }

  final int id;
  final String displayName;
  final String identifierType;
  final String accountMasked;
  final DateTime? authorizedAt;
}

class WithdrawalRecord {
  const WithdrawalRecord({
    required this.id,
    required this.amount,
    required this.payeeName,
    required this.alipayAccount,
    required this.status,
    required this.batchNo,
    required this.exportedAt,
    required this.createdAt,
  });

  factory WithdrawalRecord.fromJson(Map<String, dynamic> json) {
    return WithdrawalRecord(
      id: _int(json['id']),
      amount: _double(json['amount']),
      payeeName: json['payeeName']?.toString() ?? '',
      alipayAccount: json['alipayAccount']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      batchNo: json['batchNo']?.toString() ?? '',
      exportedAt: _date(json['exportedAt']),
      createdAt: _date(json['createdAt']),
    );
  }

  final int id;
  final double amount;
  final String payeeName;
  final String alipayAccount;
  final String status;
  final String batchNo;
  final DateTime? exportedAt;
  final DateTime? createdAt;
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
double _double(Object? value, {double fallback = 0}) =>
    value is num ? value.toDouble() : double.tryParse('$value') ?? fallback;
DateTime? _date(Object? value) =>
    value == null ? null : DateTime.tryParse('$value')?.toLocal();
