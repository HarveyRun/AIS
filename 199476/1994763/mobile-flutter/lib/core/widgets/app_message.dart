import 'package:flutter/material.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';

class AppMessage {
  static void show(BuildContext context, String text) {
    if (text.trim().isEmpty) return;
    SmartDialog.showToast(
      text.trim(),
      displayTime: const Duration(seconds: 2),
      alignment: Alignment.center,
    );
  }
}
