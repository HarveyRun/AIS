import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app/providers.dart';
import '../network/realtime_service.dart';

class NotificationRealtimeGate extends ConsumerStatefulWidget {
  const NotificationRealtimeGate({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<NotificationRealtimeGate> createState() =>
      _NotificationRealtimeGateState();
}

class _NotificationRealtimeGateState
    extends ConsumerState<NotificationRealtimeGate> {
  StreamSubscription<RealtimeEvent>? _subscription;
  bool _refreshing = false;

  @override
  void initState() {
    super.initState();
    _subscription = ref.read(realtimeProvider).events.listen((event) {
      if (event.type == 'NOTIFICATION_CREATED' ||
          event.type == 'ANNOUNCEMENT_WITHDRAWN' ||
          event.type == 'NOTIFICATION_READ' ||
          event.type == 'NOTIFICATIONS_READ_ALL') {
        unawaited(_refreshUnreadCount());
      }
    });
  }

  Future<void> _refreshUnreadCount() async {
    if (_refreshing ||
        ref.read(notificationPagePresenceProvider).visible ||
        !ref.read(authControllerProvider).signedIn) {
      return;
    }
    _refreshing = true;
    try {
      final count = await ref
          .read(repositoryProvider)
          .notificationUnreadCount();
      if (mounted) {
        ref.read(notificationCountProvider.notifier).state = count;
      }
    } catch (_) {
      // 实时刷新失败不打断用户操作，下次进入通知页时会重新查询。
    } finally {
      _refreshing = false;
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
