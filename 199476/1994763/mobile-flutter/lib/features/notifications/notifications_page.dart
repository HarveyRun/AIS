import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
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

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final items = await ref.read(repositoryProvider).notifications();
      if (mounted) setState(() => _items = items);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _read(AppNotification item) async {
    if (!item.read) {
      await ref.read(repositoryProvider).readNotification(item.id);
      await _load();
      ref.read(notificationCountProvider.notifier).state = _items
          .where((entry) => !entry.read)
          .length;
    }
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(item.title),
        content: Text(item.content),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('知道了'),
          ),
        ],
      ),
    );
  }

  Future<void> _readAll() async {
    await ref.read(repositoryProvider).readAllNotifications();
    await _load();
    ref.read(notificationCountProvider.notifier).state = 0;
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: Theme.of(context).colorScheme.surface,
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
                    padding: const EdgeInsets.fromLTRB(17, 8, 17, 28),
                    children: const [
                      SizedBox(height: 180),
                      Icon(Icons.notifications_none_rounded, size: 44),
                      SizedBox(height: 12),
                      Center(child: Text('暂无消息')),
                    ],
                  )
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(17, 8, 17, 28),
                    itemCount: _items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final item = _items[index];
                      return Container(
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.surface,
                          border: Border.all(
                            color: Theme.of(context).colorScheme.outlineVariant,
                          ),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 13,
                            vertical: 8,
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
                              Icons.notifications_none_rounded,
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
