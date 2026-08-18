import 'package:flutter/material.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';

class AppMessage {
  static void show(BuildContext context, String text) {
    if (text.trim().isEmpty) return;
    SmartDialog.showToast(
      text.trim(),
      displayTime: const Duration(seconds: 2),
      alignment: Alignment.center,
      builder: (_) => Container(
        margin: const EdgeInsets.symmetric(horizontal: 30, vertical: 50),
        padding: const EdgeInsets.symmetric(horizontal: 25, vertical: 10),
        decoration: BoxDecoration(
          color: Colors.black,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(text.trim(), style: const TextStyle(color: Colors.white)),
      ),
    );
  }
}
