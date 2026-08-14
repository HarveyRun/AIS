import 'dart:async';
import 'dart:convert';

import 'package:web_socket_channel/web_socket_channel.dart';

import '../../data/repositories/app_repository.dart';
import '../config/app_config.dart';

class RealtimeEvent {
  const RealtimeEvent({required this.type, required this.payload});

  factory RealtimeEvent.fromJson(Map<String, dynamic> json) {
    return RealtimeEvent(
      type: json['type']?.toString() ?? '',
      payload: json['payload'] is Map
          ? Map<String, dynamic>.from(json['payload'] as Map)
          : const {},
    );
  }

  final String type;
  final Map<String, dynamic> payload;
}

class RealtimeService {
  RealtimeService(this._repository);

  final AppRepository _repository;
  final StreamController<RealtimeEvent> _events = StreamController.broadcast();
  WebSocketChannel? _channel;
  StreamSubscription<dynamic>? _subscription;
  Timer? _retryTimer;
  bool _enabled = false;

  Stream<RealtimeEvent> get events => _events.stream;

  Future<void> connect() async {
    _enabled = true;
    await _open();
  }

  Future<void> _open() async {
    if (!_enabled || _channel != null) return;
    try {
      final ticketData = await _repository.realtimeTicket();
      final ticket = ticketData['ticket']?.toString() ?? '';
      if (ticket.isEmpty) return;
      _channel = WebSocketChannel.connect(AppConfig.realtimeUri(ticket));
      _subscription = _channel!.stream.listen(
        (raw) {
          final decoded = jsonDecode(raw.toString());
          if (decoded is Map) {
            _events.add(
              RealtimeEvent.fromJson(Map<String, dynamic>.from(decoded)),
            );
          }
        },
        onError: (_) => _reconnect(),
        onDone: _reconnect,
      );
    } catch (_) {
      _reconnect();
    }
  }

  void _reconnect() {
    _subscription?.cancel();
    _subscription = null;
    _channel = null;
    if (!_enabled || _retryTimer?.isActive == true) return;
    _retryTimer = Timer(const Duration(seconds: 3), _open);
  }

  Future<void> disconnect() async {
    _enabled = false;
    _retryTimer?.cancel();
    await _subscription?.cancel();
    await _channel?.sink.close();
    _subscription = null;
    _channel = null;
  }

  Future<void> dispose() async {
    await disconnect();
    await _events.close();
  }
}
