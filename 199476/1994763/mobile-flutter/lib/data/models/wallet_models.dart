class WalletInfo {
  const WalletInfo({
    required this.availableBalance,
    required this.frozenBalance,
    required this.totalWithdrawn,
  });

  factory WalletInfo.fromJson(Map<String, dynamic> json) {
    return WalletInfo(
      availableBalance: _double(json['availableBalance']),
      frozenBalance: _double(json['frozenBalance']),
      totalWithdrawn: _double(json['totalWithdrawn']),
    );
  }

  final double availableBalance;
  final double frozenBalance;
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

class BankCardInfo {
  const BankCardInfo({
    required this.id,
    required this.holderName,
    required this.bankName,
    required this.lastFour,
  });

  factory BankCardInfo.fromJson(Map<String, dynamic> json) {
    return BankCardInfo(
      id: _int(json['id']),
      holderName: json['holderName']?.toString() ?? '',
      bankName: json['bankName']?.toString() ?? '',
      lastFour: json['lastFour']?.toString() ?? '',
    );
  }

  final int id;
  final String holderName;
  final String bankName;
  final String lastFour;
}

class WithdrawalRecord {
  const WithdrawalRecord({
    required this.id,
    required this.amount,
    required this.fee,
    required this.arrivalAmount,
    required this.bankName,
    required this.lastFour,
    required this.status,
    required this.createdAt,
  });

  factory WithdrawalRecord.fromJson(Map<String, dynamic> json) {
    return WithdrawalRecord(
      id: _int(json['id']),
      amount: _double(json['amount']),
      fee: _double(json['fee']),
      arrivalAmount: _double(json['arrivalAmount']),
      bankName: json['bankName']?.toString() ?? '',
      lastFour: json['lastFour']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      createdAt: _date(json['createdAt']),
    );
  }

  final int id;
  final double amount;
  final double fee;
  final double arrivalAmount;
  final String bankName;
  final String lastFour;
  final String status;
  final DateTime? createdAt;
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
double _double(Object? value) =>
    value is num ? value.toDouble() : double.tryParse('$value') ?? 0;
DateTime? _date(Object? value) =>
    value == null ? null : DateTime.tryParse('$value')?.toLocal();
