import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/config/app_config.dart';
import '../../core/network/realtime_service.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/inquiry_models.dart';
import '../../data/repositories/app_repository.dart';

class ChatPage extends ConsumerStatefulWidget {
  const ChatPage({super.key, required this.id});
  final int id;

  @override
  ConsumerState<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends ConsumerState<ChatPage> {
  final _textController = TextEditingController();
  final _scrollController = ScrollController();
  final _picker = ImagePicker();
  StreamSubscription<RealtimeEvent>? _subscription;
  InquiryDetail? _detail;
  bool _loading = true;
  bool _emojiOpen = false;
  bool _moreOpen = false;

  static const _emojis = [
    '😀',
    '😄',
    '😂',
    '😊',
    '🥰',
    '😎',
    '🤔',
    '👍',
    '👏',
    '🙏',
    '💪',
    '🎉',
    '❤️',
    '👌',
    '🌹',
    '🤝',
  ];

  @override
  void initState() {
    super.initState();
    _load();
    _subscription = ref.read(realtimeProvider).events.listen((event) {
      final inquiryId = int.tryParse(
        '${event.payload['inquiryId'] ?? event.payload['id'] ?? ''}',
      );
      if (event.type.startsWith('INQUIRY_') &&
          (inquiryId == null || inquiryId == widget.id)) {
        _load(silent: true);
      }
    });
  }

  @override
  void dispose() {
    _subscription?.cancel();
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    if (!silent) setState(() => _loading = true);
    try {
      final detail = await ref.read(repositoryProvider).inquiry(widget.id);
      if (!mounted) return;
      setState(() => _detail = detail);
      WidgetsBinding.instance.addPostFrameCallback((_) => _scrollToEnd());
      await ref.read(repositoryProvider).markInquiryRead(widget.id);
    } catch (error) {
      if (!silent && mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted && !silent) setState(() => _loading = false);
    }
  }

  void _scrollToEnd() {
    if (!_scrollController.hasClients) return;
    _scrollController.animateTo(
      _scrollController.position.maxScrollExtent,
      duration: const Duration(milliseconds: 240),
      curve: Curves.easeOut,
    );
  }

  Future<void> _sendText() async {
    final content = _textController.text.trim();
    if (content.isEmpty || !(_detail?.inquiry.canChat ?? false)) return;
    final user = ref.read(authControllerProvider).user!;
    final pending = ChatMessage(
      id: 'sending-${DateTime.now().microsecondsSinceEpoch}',
      senderId: user.id,
      senderName: user.displayName,
      senderAvatar: user.avatarUrl,
      type: 'TEXT',
      content: content,
      attachmentUrl: '',
      attachmentName: '',
      attachmentSize: 0,
      createdAt: DateTime.now(),
      sending: true,
    );
    _textController.clear();
    setState(
      () => _detail = InquiryDetail(
        inquiry: _detail!.inquiry,
        messages: [..._detail!.messages, pending],
      ),
    );
    WidgetsBinding.instance.addPostFrameCallback((_) => _scrollToEnd());
    try {
      final saved = await ref
          .read(repositoryProvider)
          .sendInquiryMessage(widget.id, content);
      if (!mounted) return;
      setState(() => _replaceMessage(pending.id, saved));
    } catch (error) {
      if (!mounted) return;
      setState(
        () => _replaceMessage(
          pending.id,
          pending.copyWith(sending: false, failed: true),
        ),
      );
      AppMessage.show(context, '$error');
    }
  }

  InquiryDetail _replaceMessage(String id, ChatMessage replacement) {
    return InquiryDetail(
      inquiry: _detail!.inquiry,
      messages: _detail!.messages
          .map((item) => item.id == id ? replacement : item)
          .toList(),
    );
  }

  Future<void> _sendPhoto() async {
    setState(() => _moreOpen = false);
    final image = await _picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 88,
      maxWidth: 2048,
    );
    if (image == null) return;
    try {
      await ref
          .read(repositoryProvider)
          .sendInquiryImage(
            widget.id,
            UploadFile(path: image.path, name: image.name),
          );
      await _load(silent: true);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _action(
    Future<void> Function(int) operation,
    String message,
  ) async {
    try {
      await operation(widget.id);
      await _load(silent: true);
      if (mounted) AppMessage.show(context, message);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    final inquiry = detail?.inquiry;
    final canChat = inquiry?.canChat ?? false;
    return Scaffold(
      resizeToAvoidBottomInset: true,
      appBar: AppBar(
        leading: IconButton(
          onPressed: () => context.pop(),
          icon: const Icon(Icons.arrow_back_rounded),
        ),
        title: Text(
          inquiry?.otherName.isNotEmpty == true ? inquiry!.otherName : '询问',
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : detail == null
          ? const Center(child: Text('询问不存在'))
          : Column(
              children: [
                _ChatStatus(inquiry: inquiry!, onAction: _action),
                Expanded(
                  child: Container(
                    color: Theme.of(context).brightness == Brightness.dark
                        ? const Color(0xFF151619)
                        : const Color(0xFFF2F2F2),
                    child: ListView.builder(
                      controller: _scrollController,
                      padding: const EdgeInsets.fromLTRB(14, 18, 14, 18),
                      itemCount: detail.messages.length,
                      itemBuilder: (context, index) {
                        final message = detail.messages[index];
                        final previous = index == 0
                            ? null
                            : detail.messages[index - 1];
                        final showTime =
                            previous == null ||
                            (message.createdAt != null &&
                                previous.createdAt != null &&
                                message.createdAt!
                                        .difference(previous.createdAt!)
                                        .inMinutes >=
                                    10);
                        return _MessageBubble(
                          message: message,
                          showTime: showTime,
                        );
                      },
                    ),
                  ),
                ),
                _Composer(
                  controller: _textController,
                  enabled: canChat,
                  emojiOpen: _emojiOpen,
                  moreOpen: _moreOpen,
                  onSubmitted: _sendText,
                  onEmoji: () => setState(() {
                    _emojiOpen = !_emojiOpen;
                    _moreOpen = false;
                  }),
                  onMore: () => setState(() {
                    _moreOpen = !_moreOpen;
                    _emojiOpen = false;
                  }),
                ),
                if (_emojiOpen && canChat)
                  _EmojiPanel(
                    emojis: _emojis,
                    onTap: (emoji) {
                      _textController.text += emoji;
                      _textController.selection = TextSelection.collapsed(
                        offset: _textController.text.length,
                      );
                    },
                  ),
                if (_moreOpen && canChat)
                  _MorePanel(
                    onPhoto: _sendPhoto,
                    onEnd: () {
                      setState(() => _moreOpen = false);
                      final repository = ref.read(repositoryProvider);
                      if (inquiry.isIncoming) {
                        _action(repository.requestInquiryEnd, '结束申请已发送');
                      } else {
                        _confirmEnd(repository);
                      }
                    },
                  ),
              ],
            ),
    );
  }

  Future<void> _confirmEnd(AppRepository repository) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('结束本次询问？'),
        content: const Text('结束后，本次费用将结算给回答者，聊天不能继续。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('继续交流'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('确认结束'),
          ),
        ],
      ),
    );
    if (confirmed == true) _action(repository.confirmInquiryEnd, '本次交流已结束');
  }
}

class _ChatStatus extends ConsumerWidget {
  const _ChatStatus({required this.inquiry, required this.onAction});
  final InquirySummary inquiry;
  final void Function(Future<void> Function(int), String) onAction;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final repository = ref.read(repositoryProvider);
    final status = inquiry.status.toUpperCase();
    final text = switch (status) {
      'PENDING' => inquiry.isIncoming ? '等待你的决定' : '等待对方接受',
      'ACTIVE' => '交流进行中',
      'AWAITING_CONFIRMATION' => inquiry.isIncoming ? '等待提问者确认结束' : '对方申请结束',
      'COMPLETED' || 'ENDED' => '本次交流已经结束',
      'REJECTED' => '本次询问未接受',
      'CANCELLED' => '本次询问已撤销',
      'EXPIRED' => '本次询问已超时',
      _ => '本次询问正在处理中',
    };
    return Material(
      color: Theme.of(context).colorScheme.surface,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
        child: Column(
          children: [
            Row(
              children: [
                Icon(
                  status == 'ACTIVE'
                      ? Icons.chat_bubble_outline_rounded
                      : Icons.schedule_rounded,
                  size: 18,
                  color: Theme.of(context).colorScheme.primary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    text,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                Text(
                  '¥${inquiry.amount.toStringAsFixed(2)}',
                  style: TextStyle(
                    fontWeight: FontWeight.w700,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
              ],
            ),
            if (status == 'PENDING' && inquiry.isIncoming) ...[
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () =>
                          onAction(repository.rejectInquiry, '已拒绝询问'),
                      child: const Text('暂不接受'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton(
                      onPressed: () =>
                          onAction(repository.acceptInquiry, '已接受询问'),
                      child: const Text('同意交流'),
                    ),
                  ),
                ],
              ),
            ],
            if (status == 'PENDING' && !inquiry.isIncoming) ...[
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () =>
                      onAction(repository.cancelInquiry, '询问已撤销，金额已退回'),
                  child: const Text('撤销询问'),
                ),
              ),
            ],
            if (status == 'AWAITING_CONFIRMATION' && !inquiry.isIncoming) ...[
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () =>
                          onAction(repository.continueInquiry, '已继续交流'),
                      child: const Text('继续交流'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton(
                      onPressed: () =>
                          onAction(repository.confirmInquiryEnd, '本次交流已结束'),
                      child: const Text('确认结束'),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _MessageBubble extends ConsumerWidget {
  const _MessageBubble({required this.message, required this.showTime});
  final ChatMessage message;
  final bool showTime;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final me = message.senderId == ref.read(authControllerProvider).user?.id;
    final name = message.senderName.trim().isEmpty ? '用户' : message.senderName;
    return Column(
      children: [
        if (showTime && message.createdAt != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 14),
            child: Text(
              DateFormat('M月d日 HH:mm').format(message.createdAt!),
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: me
              ? MainAxisAlignment.end
              : MainAxisAlignment.start,
          children: [
            if (!me) ...[
              AppAvatar(url: message.senderAvatar, name: name, radius: 19),
              const SizedBox(width: 8),
            ],
            Flexible(
              child: Column(
                crossAxisAlignment: me
                    ? CrossAxisAlignment.end
                    : CrossAxisAlignment.start,
                children: [
                  Text(name, style: Theme.of(context).textTheme.bodySmall),
                  const SizedBox(height: 4),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (me && message.sending) ...[
                        const SizedBox.square(
                          dimension: 13,
                          child: CircularProgressIndicator(strokeWidth: 1.5),
                        ),
                        const SizedBox(width: 6),
                      ],
                      if (me && message.failed) ...[
                        Icon(
                          Icons.error_rounded,
                          size: 17,
                          color: Theme.of(context).colorScheme.error,
                        ),
                        const SizedBox(width: 6),
                      ],
                      Flexible(
                        child: Container(
                          padding: message.type.toUpperCase() == 'IMAGE'
                              ? const EdgeInsets.all(3)
                              : const EdgeInsets.symmetric(
                                  horizontal: 13,
                                  vertical: 10,
                                ),
                          decoration: BoxDecoration(
                            color: me
                                ? const Color(0xFFDFE9F2)
                                : Theme.of(context).colorScheme.surface,
                            borderRadius: me
                                ? const BorderRadius.only(
                                    topLeft: Radius.circular(14),
                                    bottomLeft: Radius.circular(14),
                                    bottomRight: Radius.circular(14),
                                    topRight: Radius.circular(4),
                                  )
                                : const BorderRadius.only(
                                    topLeft: Radius.circular(4),
                                    topRight: Radius.circular(14),
                                    bottomLeft: Radius.circular(14),
                                    bottomRight: Radius.circular(14),
                                  ),
                          ),
                          child:
                              message.type.toUpperCase() == 'IMAGE' &&
                                  message.attachmentUrl.isNotEmpty
                              ? ClipRRect(
                                  borderRadius: BorderRadius.circular(10),
                                  child: Image.network(
                                    AppConfig.resolveResource(
                                      message.attachmentUrl,
                                    ).toString(),
                                    width: 180,
                                    fit: BoxFit.cover,
                                  ),
                                )
                              : Text(message.content),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            if (me) ...[
              const SizedBox(width: 8),
              AppAvatar(url: message.senderAvatar, name: name, radius: 19),
            ],
          ],
        ),
        const SizedBox(height: 16),
      ],
    );
  }
}

class _Composer extends StatelessWidget {
  const _Composer({
    required this.controller,
    required this.enabled,
    required this.emojiOpen,
    required this.moreOpen,
    required this.onSubmitted,
    required this.onEmoji,
    required this.onMore,
  });
  final TextEditingController controller;
  final bool enabled;
  final bool emojiOpen;
  final bool moreOpen;
  final VoidCallback onSubmitted;
  final VoidCallback onEmoji;
  final VoidCallback onMore;
  @override
  Widget build(BuildContext context) => SafeArea(
    top: false,
    child: Container(
      padding: const EdgeInsets.fromLTRB(10, 8, 10, 8),
      color: Theme.of(context).colorScheme.surface,
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: controller,
              enabled: enabled,
              minLines: 1,
              maxLines: 4,
              textInputAction: TextInputAction.send,
              onSubmitted: (_) => onSubmitted(),
              decoration: InputDecoration(
                hintText: enabled ? '说点什么…' : '本次交流暂不能聊天',
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 13,
                  vertical: 10,
                ),
              ),
            ),
          ),
          const SizedBox(width: 5),
          IconButton(
            onPressed: enabled ? onEmoji : null,
            visualDensity: VisualDensity.compact,
            icon: Icon(
              emojiOpen
                  ? Icons.keyboard_alt_outlined
                  : Icons.sentiment_satisfied_alt_outlined,
            ),
          ),
          IconButton(
            onPressed: enabled ? onMore : null,
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.add_circle_outline_rounded),
          ),
        ],
      ),
    ),
  );
}

class _EmojiPanel extends StatelessWidget {
  const _EmojiPanel({required this.emojis, required this.onTap});
  final List<String> emojis;
  final ValueChanged<String> onTap;
  @override
  Widget build(BuildContext context) => SizedBox(
    height: 168,
    child: GridView.count(
      padding: const EdgeInsets.all(12),
      crossAxisCount: 8,
      children: emojis
          .map(
            (emoji) => InkWell(
              onTap: () => onTap(emoji),
              child: Center(
                child: Text(emoji, style: const TextStyle(fontSize: 24)),
              ),
            ),
          )
          .toList(),
    ),
  );
}

class _MorePanel extends StatelessWidget {
  const _MorePanel({required this.onPhoto, required this.onEnd});
  final VoidCallback onPhoto;
  final VoidCallback onEnd;
  @override
  Widget build(BuildContext context) => SizedBox(
    height: 140,
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _MoreItem(icon: Icons.photo_outlined, label: '照片', onTap: onPhoto),
        _MoreItem(icon: Icons.payments_outlined, label: '结束并结算', onTap: onEnd),
      ],
    ),
  );
}

class _MoreItem extends StatelessWidget {
  const _MoreItem({
    required this.icon,
    required this.label,
    required this.onTap,
  });
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => InkWell(
    onTap: onTap,
    child: SizedBox(
      width: 100,
      child: Padding(
        padding: const EdgeInsets.only(top: 18),
        child: Column(
          children: [
            Container(
              width: 52,
              height: 52,
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(icon),
            ),
            const SizedBox(height: 7),
            Text(label, style: Theme.of(context).textTheme.bodySmall),
          ],
        ),
      ),
    ),
  );
}
