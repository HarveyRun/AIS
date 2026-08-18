String formatMoney(num value) {
  final amount = value.toDouble();
  if (amount == amount.roundToDouble()) return amount.round().toString();
  return amount
      .toStringAsFixed(2)
      .replaceFirst(RegExp(r'0+$'), '')
      .replaceFirst(RegExp(r'\.$'), '');
}
