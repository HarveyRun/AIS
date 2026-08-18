import 'package:flutter/services.dart';

abstract final class AppInputFormatters {
  static final search = <TextInputFormatter>[
    LengthLimitingTextInputFormatter(15),
  ];

  static List<TextInputFormatter> description([int maxLength = 500]) => [
    LengthLimitingTextInputFormatter(maxLength),
  ];

  static List<TextInputFormatter> positiveInteger({required int max}) => [
    FilteringTextInputFormatter.digitsOnly,
    PositiveIntegerInputFormatter(max: max),
  ];
}

class PositiveIntegerInputFormatter extends TextInputFormatter {
  const PositiveIntegerInputFormatter({required this.max});

  final int max;

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (newValue.text.isEmpty) return newValue;
    final normalized = newValue.text.replaceFirst(RegExp(r'^0+'), '');
    if (normalized.isEmpty) {
      return const TextEditingValue();
    }
    final value = int.tryParse(normalized);
    if (value == null || value > max) return oldValue;
    return TextEditingValue(
      text: normalized,
      selection: TextSelection.collapsed(offset: normalized.length),
    );
  }
}
