import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/curated_chat_models.dart';

class CuratedMembersPage extends ConsumerStatefulWidget {
  const CuratedMembersPage({super.key});

  @override
  ConsumerState<CuratedMembersPage> createState() => _State();
}

class _State extends ConsumerState<CuratedMembersPage> {
  List<CuratedMember> items = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final page = await ref.read(repositoryProvider).curatedMembers(size: 50);
      if (mounted) {
        setState(() {
          items = page.content;
          loading = false;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => loading = false);
        AppMessage.show(context, '$error');
      }
    }
  }

  Future<void> _open(CuratedMember member) async {
    try {
      final conversation = await ref
          .read(repositoryProvider)
          .openCuratedConversation(member.id);
      if (mounted) context.push('/curated/chat/${conversation.id}');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  Widget build(BuildContext context) {
    final unread = ref.watch(curatedChatUnreadProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('严选直聊'),
        actions: [
          IconButton(
            tooltip: '聊天列表',
            onPressed: () => context.push('/curated/chats'),
            icon: Badge(
              label: Text(unread > 99 ? '99+' : '$unread'),
              isLabelVisible: unread > 0,
              child: const Icon(Icons.chat_bubble_outline_rounded),
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: loading
            ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
            : items.isEmpty
            ? ListView(
                children: const [
                  SizedBox(height: 180),
                  Icon(
                    Icons.people_outline_rounded,
                    size: 46,
                    color: Colors.grey,
                  ),
                  SizedBox(height: 10),
                  Center(child: Text('当前暂无成员')),
                ],
              )
            : ListView.separated(
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 30),
                itemCount: items.length,
                separatorBuilder: (_, _) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  final item = items[index];
                  return Material(
                    color: Theme.of(context).colorScheme.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(18),
                    child: Padding(
                      padding: const EdgeInsets.all(15),
                      child: Row(
                        children: [
                          AppAvatar(
                            url: item.avatarUrl,
                            name: item.nickname,
                            radius: 25,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Flexible(
                                      child: Text(
                                        item.nickname,
                                        style: const TextStyle(
                                          fontSize: 16,
                                          fontWeight: FontWeight.w700,
                                        ),
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                    const SizedBox(width: 7),
                                    _Presence(online: item.online),
                                  ],
                                ),
                                const SizedBox(height: 5),
                                Text(
                                  'UID ${item.uid}',
                                  style: Theme.of(context).textTheme.bodySmall,
                                ),
                                const SizedBox(height: 3),
                                Text(
                                  '${item.jobTitle} · ${item.jobYears}年',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          ),
                          TextButton(
                            onPressed: item.online ? () => _open(item) : null,
                            child: Text(item.online ? '开始聊天' : '当前离线'),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
      ),
    );
  }
}

class _Presence extends StatelessWidget {
  const _Presence({required this.online});

  final bool online;

  @override
  Widget build(BuildContext context) {
    final color = online
        ? const Color(0xFF35B56A)
        : Theme.of(context).colorScheme.outline;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 7,
          height: 7,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 4),
        Text(
          online ? '在线' : '离线',
          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: color),
        ),
      ],
    );
  }
}
