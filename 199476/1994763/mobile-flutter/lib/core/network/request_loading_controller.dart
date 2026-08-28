import 'package:flutter/foundation.dart';

class RequestLoadingController extends ChangeNotifier {
  int _activeRequests = 0;
  bool _notificationScheduled = false;
  bool _disposed = false;

  bool get isLoading => _activeRequests > 0;

  void begin() {
    _activeRequests++;
    if (_activeRequests == 1) {
      _scheduleNotification();
    }
  }

  void end() {
    if (_activeRequests == 0) return;
    _activeRequests--;
    if (_activeRequests == 0) {
      _scheduleNotification();
    }
  }

  Future<T> run<T>(Future<T> Function() action) async {
    begin();
    try {
      return await action();
    } finally {
      end();
    }
  }

  void _scheduleNotification() {
    if (_notificationScheduled || _disposed) return;
    _notificationScheduled = true;
    Future<void>.microtask(() {
      _notificationScheduled = false;
      if (!_disposed) notifyListeners();
    });
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
