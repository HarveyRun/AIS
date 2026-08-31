import 'package:flutter/material.dart';
import 'package:flutter_smart_dialog/flutter_smart_dialog.dart';

class AppMessage {
  static final Set<String> _activeMessages = <String>{};

  static void show(BuildContext context, String text) {
    final message = text.trim();
    if (message.isEmpty || !_activeMessages.add(message)) return;
    SmartDialog.showToast(
      message,
      displayTime: const Duration(seconds: 2),
      alignment: Alignment.center,
      onDismiss: () => _activeMessages.remove(message),
      builder: (_) => Container(
        margin: const EdgeInsets.symmetric(horizontal: 30, vertical: 50),
        padding: const EdgeInsets.symmetric(horizontal: 25, vertical: 10),
        decoration: BoxDecoration(
          color: Colors.black,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(message, style: const TextStyle(color: Colors.white)),
      ),
    );
  }
}
