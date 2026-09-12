import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../network/realtime_service.dart';

class VoiceCallRealtimeGate extends ConsumerStatefulWidget {
  const VoiceCallRealtimeGate({
    super.key,
    required this.router,
    required this.child,
  });

  final GoRouter router;
  final Widget child;

  @override
  ConsumerState<VoiceCallRealtimeGate> createState() =>
      _VoiceCallRealtimeGateState();
}

class _VoiceCallRealtimeGateState extends ConsumerState<VoiceCallRealtimeGate> {
  StreamSubscription<RealtimeEvent>? _subscription;
  int? _openingVoiceCallId;

  @override
  void initState() {
    super.initState();
    _subscription = ref.read(realtimeProvider).events.listen(_handleEvent);
  }

  void _handleEvent(RealtimeEvent event) {
    if (event.type != 'VOICE_CALL_STARTED' ||
        !ref.read(authControllerProvider).signedIn) {
      return;
    }
    final inquiryId = int.tryParse('${event.payload['inquiryId'] ?? ''}');
    final voiceCallId = int.tryParse('${event.payload['voiceCallId'] ?? ''}');
    if (inquiryId == null || voiceCallId == null) return;
    if (_openingVoiceCallId == voiceCallId) return;

    final path = '/voice-call/$inquiryId/$voiceCallId';
    if (widget.router.routerDelegate.currentConfiguration.uri.path == path) {
      return;
    }
    _openingVoiceCallId = voiceCallId;
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;
      try {
        await widget.router.push('$path?initiator=0');
      } finally {
        if (mounted && _openingVoiceCallId == voiceCallId) {
          _openingVoiceCallId = null;
        }
      }
    });
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
