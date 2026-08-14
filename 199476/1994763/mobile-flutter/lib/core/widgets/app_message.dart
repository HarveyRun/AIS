import 'package:flutter/material.dart';

class AppMessage {
  static void show(BuildContext context, String text) {
    final messenger = ScaffoldMessenger.of(context);
    messenger
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            text,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          behavior: SnackBarBehavior.floating,
          elevation: 4,
          width: MediaQuery.sizeOf(context).width.clamp(180, 320),
          backgroundColor: Theme.of(context).brightness == Brightness.dark
              ? const Color(0xE637383D)
              : const Color(0xEEFFFFFF),
          duration: const Duration(seconds: 2),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      );
  }
}
