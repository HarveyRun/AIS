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
  bool _refreshingNotifications = false;
  bool _refreshingInquiries = false;
  bool _signedInRefreshScheduled = false;

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
      if (event.type.startsWith('INQUIRY_')) {
        unawaited(_refreshInquiryUnreadCount());
      }
    });
  }

  Future<void> _refreshUnreadCount() async {
    if (_refreshingNotifications ||
        ref.read(notificationPagePresenceProvider).visible ||
        !ref.read(authControllerProvider).signedIn) {
      return;
    }
    _refreshingNotifications = true;
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
      _refreshingNotifications = false;
    }
  }

  Future<void> _refreshInquiryUnreadCount() async {
    if (_refreshingInquiries || !ref.read(authControllerProvider).signedIn) {
      return;
    }
    _refreshingInquiries = true;
    try {
      final count = await ref.read(repositoryProvider).inquiryUnreadCount();
      if (mounted) {
        ref.read(inquiryUnreadCountProvider.notifier).state = count;
      }
    } catch (_) {
      // 静默同步失败时保留当前角标，下一次业务事件会再次校准。
    } finally {
      _refreshingInquiries = false;
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final signedIn = ref.watch(authControllerProvider).signedIn;
    if (signedIn && !_signedInRefreshScheduled) {
      _signedInRefreshScheduled = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        unawaited(_refreshUnreadCount());
        unawaited(_refreshInquiryUnreadCount());
      });
    } else if (!signedIn) {
      _signedInRefreshScheduled = false;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || ref.read(authControllerProvider).signedIn) return;
        ref.read(notificationCountProvider.notifier).state = 0;
        ref.read(inquiryUnreadCountProvider.notifier).state = 0;
      });
    }
    return widget.child;
  }
}
