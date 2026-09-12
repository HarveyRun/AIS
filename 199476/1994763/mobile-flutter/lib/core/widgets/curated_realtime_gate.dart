import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../network/realtime_service.dart';

class CuratedRealtimeGate extends ConsumerStatefulWidget {
  const CuratedRealtimeGate({
    super.key,
    required this.router,
    required this.child,
  });

  final GoRouter router;
  final Widget child;

  @override
  ConsumerState<CuratedRealtimeGate> createState() =>
      _CuratedRealtimeGateState();
}

class _CuratedRealtimeGateState extends ConsumerState<CuratedRealtimeGate> {
  StreamSubscription<RealtimeEvent>? _subscription;
  int? _openingCallId;

  @override
  void initState() {
    super.initState();
    _subscription = ref.read(realtimeProvider).events.listen(_handleEvent);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && ref.read(authControllerProvider).signedIn) {
        unawaited(_refreshUnread());
      }
    });
  }

  void _handleEvent(RealtimeEvent event) {
    if (!ref.read(authControllerProvider).signedIn) return;
    if (event.type == 'CONNECTED' ||
        event.type == 'CURATED_MESSAGE_CREATED' ||
        event.type == 'CURATED_UNREAD_UPDATED') {
      unawaited(_refreshUnread());
    }
    if (event.type != 'CURATED_VOICE_RINGING') return;
    final callId = int.tryParse('${event.payload['callId'] ?? ''}');
    if (callId == null || _openingCallId == callId) return;
    final path = '/curated/voice/$callId';
    if (widget.router.routerDelegate.currentConfiguration.uri.path == path) {
      return;
    }
    _openingCallId = callId;
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;
      try {
        await widget.router.push('$path?initiator=0');
      } finally {
        if (mounted && _openingCallId == callId) _openingCallId = null;
      }
    });
  }

  Future<void> _refreshUnread() async {
    try {
      final count = await ref.read(repositoryProvider).curatedUnreadCount();
      if (mounted) ref.read(curatedChatUnreadProvider.notifier).state = count;
    } catch (_) {
      // 断线重连时下一条实时事件或页面刷新会再次同步。
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
