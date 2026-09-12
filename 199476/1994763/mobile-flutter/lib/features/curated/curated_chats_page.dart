import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../app/providers.dart';
import '../../core/network/realtime_service.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/curated_chat_models.dart';

class CuratedChatsPage extends ConsumerStatefulWidget {
  const CuratedChatsPage({super.key});
  @override
  ConsumerState<CuratedChatsPage> createState() => _State();
}

class _State extends ConsumerState<CuratedChatsPage> {
  List<CuratedConversation> items = [];
  StreamSubscription<RealtimeEvent>? sub;
  @override
  void initState() {
    super.initState();
    _load();
    sub = ref.read(realtimeProvider).events.listen((e) {
      if (e.type.startsWith('CURATED_')) _load(silent: true);
    });
  }

  @override
  void dispose() {
    sub?.cancel();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    try {
      final x = await ref
          .read(repositoryProvider)
          .curatedConversations(showLoading: !silent);
      if (!mounted) return;
      setState(() => items = x);
      ref.read(curatedChatUnreadProvider.notifier).state = x.fold(
        0,
        (n, c) => n + c.unreadCount,
      );
    } catch (e) {
      if (mounted && !silent) AppMessage.show(context, '$e');
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('严选直聊消息')),
    body: items.isEmpty
        ? const Center(child: Text('暂无聊天记录'))
        : ListView.separated(
            padding: const EdgeInsets.all(10),
            itemCount: items.length,
            separatorBuilder: (_, __) => const SizedBox(height: 5),
            itemBuilder: (context, i) {
              final x = items[i], last = x.lastMessage;
              return ListTile(
                leading: AppAvatar(
                  url: x.otherUser.avatarUrl,
                  name: x.otherUser.nickname,
                  radius: 24,
                ),
                title: Text(
                  x.otherUser.nickname,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                subtitle: Text(
                  last?.content ?? '',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                trailing: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    if (x.lastMessageAt != null)
                      Text(
                        DateFormat('MM-dd HH:mm').format(x.lastMessageAt!),
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    if (x.unreadCount > 0)
                      Container(
                        margin: const EdgeInsets.only(top: 5),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 6,
                          vertical: 2,
                        ),
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.primary,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          x.unreadCount > 99 ? '99+' : '${x.unreadCount}',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 10,
                          ),
                        ),
                      ),
                  ],
                ),
                onTap: () async {
                  await context.push('/curated/chat/${x.id}');
                  _load(silent: true);
                },
              );
            },
          ),
  );
}
