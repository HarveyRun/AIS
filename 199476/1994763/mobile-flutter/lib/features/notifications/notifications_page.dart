import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/network/realtime_service.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/support_models.dart';

class NotificationsPage extends ConsumerStatefulWidget {
  const NotificationsPage({super.key});
  @override
  ConsumerState<NotificationsPage> createState() => _NotificationsPageState();
}

class _NotificationsPageState extends ConsumerState<NotificationsPage> {
  List<AppNotification> _items = const [];
  bool _loading = true;
  StreamSubscription<RealtimeEvent>? _subscription;

  @override
  void initState() {
    super.initState();
    ref.read(notificationPagePresenceProvider).enter();
    _subscription = ref.read(realtimeProvider).events.listen((event) {
      if (event.type == 'NOTIFICATION_CREATED' ||
          event.type == 'ANNOUNCEMENT_WITHDRAWN') {
        _load(showLoading: false);
      }
    });
    _load();
  }

  @override
  void dispose() {
    _subscription?.cancel();
    ref.read(notificationPagePresenceProvider).leave();
    super.dispose();
  }

  Future<void> _load({bool showLoading = true}) async {
    if (showLoading) setState(() => _loading = true);
    try {
      final items = await ref.read(repositoryProvider).notifications();
      if (mounted) {
        setState(() => _items = items);
        ref.read(notificationCountProvider.notifier).state = items
            .where((item) => !item.read)
            .length;
      }
    } catch (error) {
      if (showLoading && mounted) AppMessage.show(context, '$error');
    } finally {
      if (showLoading && mounted) setState(() => _loading = false);
    }
  }

  Future<void> _read(AppNotification item) async {
    if (!item.read) {
      await ref.read(repositoryProvider).readNotification(item);
      if (!mounted) return;
      setState(() {
        _items = _items
            .map(
              (entry) => entry.id == item.id
                  ? AppNotification(
                      id: entry.id,
                      sourceType: entry.sourceType,
                      title: entry.title,
                      content: entry.content,
                      targetPath: entry.targetPath,
                      read: true,
                      createdAt: entry.createdAt,
                    )
                  : entry,
            )
            .toList(growable: false);
      });
      ref.read(notificationCountProvider.notifier).state = _items
          .where((entry) => !entry.read)
          .length;
    }
  }

  Future<void> _readAll() async {
    await ref.read(repositoryProvider).readAllNotifications();
    await _load();
    ref.read(notificationCountProvider.notifier).state = 0;
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: Theme.of(context).scaffoldBackgroundColor,
    appBar: AppBar(
      backgroundColor: Theme.of(context).colorScheme.surface,
      title: const Text('通知'),
      actions: [
        if (_items.any((item) => !item.read))
          TextButton(onPressed: _readAll, child: const Text('全部已读')),
      ],
    ),
    body: _loading
        ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
        : RefreshIndicator(
            onRefresh: _load,
            child: _items.isEmpty
                ? ListView(
                    padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
                    children: const [
                      SizedBox(height: 180),
                      Icon(
                        Icons.notifications_none_rounded,
                        size: 32,
                        color: Color(0xFF9A9A9A),
                      ),
                      SizedBox(height: 12),
                      Center(child: Text('暂无消息')),
                    ],
                  )
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
                    itemCount: _items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final item = _items[index];
                      return Container(
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.surface,
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 12,
                          ),
                          leading: Container(
                            width: 40,
                            height: 40,
                            decoration: BoxDecoration(
                              color: Theme.of(
                                context,
                              ).colorScheme.surfaceContainerHighest,
                              shape: BoxShape.circle,
                            ),
                            child: Icon(
                              item.isAnnouncement
                                  ? Icons.campaign_outlined
                                  : Icons.notifications_none_rounded,
                              color: Theme.of(context).colorScheme.primary,
                            ),
                          ),
                          title: Row(
                            children: [
                              if (!item.read) ...[
                                Container(
                                  width: 7,
                                  height: 7,
                                  decoration: BoxDecoration(
                                    color: Theme.of(
                                      context,
                                    ).colorScheme.primary,
                                    shape: BoxShape.circle,
                                  ),
                                ),
                                const SizedBox(width: 7),
                              ],
                              Expanded(child: Text(item.title)),
                            ],
                          ),
                          subtitle: Padding(
                            padding: const EdgeInsets.only(top: 5),
                            child: Text(
                              '${item.content}\n${item.createdAt == null ? '' : DateFormat('yyyy-MM-dd HH:mm').format(item.createdAt!)}',
                              maxLines: 3,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          onTap: () => _read(item),
                        ),
                      );
                    },
                  ),
          ),
  );
}
