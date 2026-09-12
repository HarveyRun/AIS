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
import '../../data/models/curated_chat_models.dart';
import '../../data/repositories/app_repository.dart';

class CuratedChatPage extends ConsumerStatefulWidget {
  const CuratedChatPage({super.key, required this.id});
  final int id;
  @override
  ConsumerState<CuratedChatPage> createState() => _State();
}

class _State extends ConsumerState<CuratedChatPage> {
  final input = TextEditingController(),
      scroll = ScrollController(),
      picker = ImagePicker();
  CuratedConversation? data;
  List<CuratedMessage> messages = [];
  StreamSubscription<RealtimeEvent>? sub;
  bool sending = false;
  bool loadingOlder = false;
  bool hasOlder = true;
  @override
  void initState() {
    super.initState();
    _load();
    sub = ref.read(realtimeProvider).events.listen((e) {
      if (e.type == 'CURATED_MESSAGE_CREATED' &&
          '${e.payload['conversationId']}' == '${widget.id}') {
        _load(silent: true);
      }
      if (e.type == 'CURATED_CHAT_BLOCKED' &&
          '${e.payload['conversationId']}' == '${widget.id}') {
        _load(silent: true);
      }
    });
  }

  @override
  void dispose() {
    sub?.cancel();
    input.dispose();
    scroll.dispose();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    try {
      final x = await ref
          .read(repositoryProvider)
          .curatedConversation(widget.id, showLoading: !silent);
      await ref.read(repositoryProvider).readCuratedConversation(widget.id);
      if (!mounted) return;
      setState(() {
        data = x;
        if (messages.isEmpty || !silent) {
          messages = x.messages;
          hasOlder = x.messages.length >= 100;
        } else {
          final merged = <int, CuratedMessage>{
            for (final item in messages) item.id: item,
            for (final item in x.messages) item.id: item,
          };
          messages = merged.values.toList()
            ..sort((a, b) => a.id.compareTo(b.id));
        }
      });
      ref.read(curatedChatUnreadProvider.notifier).state = await ref
          .read(repositoryProvider)
          .curatedUnreadCount();
      WidgetsBinding.instance.addPostFrameCallback((_) => _bottom());
    } catch (e) {
      if (mounted && !silent) AppMessage.show(context, '$e');
    }
  }

  Future<void> _loadOlder() async {
    if (loadingOlder || !hasOlder || messages.isEmpty) return;
    final oldExtent = scroll.hasClients ? scroll.position.maxScrollExtent : 0.0;
    try {
      setState(() => loadingOlder = true);
      final older = await ref
          .read(repositoryProvider)
          .curatedMessages(
            widget.id,
            beforeId: messages.first.id,
            showLoading: false,
          );
      if (!mounted) return;
      setState(() {
        messages = [...older, ...messages];
        hasOlder = older.length >= 100;
      });
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!scroll.hasClients) return;
        final added = scroll.position.maxScrollExtent - oldExtent;
        scroll.jumpTo(added.clamp(0, scroll.position.maxScrollExtent));
      });
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => loadingOlder = false);
    }
  }

  void _bottom() {
    if (scroll.hasClients) {
      scroll.animateTo(
        scroll.position.maxScrollExtent,
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
      );
    }
  }

  Future<void> _send() async {
    final text = input.text.trim();
    if (text.isEmpty || sending) return;
    input.clear();
    try {
      setState(() => sending = true);
      await ref.read(repositoryProvider).sendCuratedText(widget.id, text);
      await _load(silent: true);
    } catch (e) {
      input.text = text;
      if (mounted) AppMessage.show(context, '$e');
    } finally {
      if (mounted) setState(() => sending = false);
    }
  }

  Future<void> _image(ImageSource source) async {
    final x = await picker.pickImage(source: source, imageQuality: 88);
    if (x == null) return;
    try {
      await ref
          .read(repositoryProvider)
          .sendCuratedImage(widget.id, UploadFile(path: x.path, name: x.name));
      await _load(silent: true);
    } catch (e) {
      if (mounted) AppMessage.show(context, '$e');
    }
  }

  Future<void> _call() async {
    try {
      final call = await ref
          .read(repositoryProvider)
          .startCuratedVoiceCall(widget.id);
      if (mounted) context.push('/curated/voice/${call.id}?initiator=1');
    } catch (e) {
      if (mounted) AppMessage.show(context, '$e');
    }
  }

  Future<void> _block() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('确认拉黑对方？'),
        content: const Text('拉黑后，双方将不能继续聊天或发起语音通话。'),
        actions: [
          IconButton(
            onPressed: () => Navigator.pop(c, false),
            icon: const Icon(Icons.close),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(c, true),
            child: const Text('确认拉黑'),
          ),
        ],
      ),
    );
    if (ok == true) {
      try {
        await ref.read(repositoryProvider).blockCuratedUser(widget.id);
        await _load();
      } catch (e) {
        if (mounted) AppMessage.show(context, '$e');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final x = data;
    return Scaffold(
      appBar: AppBar(
        title: x == null
            ? const Text('严选直聊')
            : Row(
                children: [
                  AppAvatar(
                    url: x.otherUser.avatarUrl,
                    name: x.otherUser.nickname,
                    radius: 17,
                  ),
                  const SizedBox(width: 9),
                  Flexible(child: Text(x.otherUser.nickname)),
                ],
              ),
        actions: [
          PopupMenuButton<String>(
            onSelected: (v) {
              if (v == 'block') _block();
            },
            itemBuilder: (_) => const [
              PopupMenuItem(value: 'block', child: Text('拉黑')),
            ],
          ),
        ],
      ),
      body: x == null
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : Column(
              children: [
                Expanded(
                  child: GestureDetector(
                    onTap: () => FocusScope.of(context).unfocus(),
                    child: ListView.builder(
                      controller: scroll,
                      padding: const EdgeInsets.all(12),
                      itemCount: messages.length + (hasOlder ? 1 : 0),
                      itemBuilder: (context, i) {
                        if (hasOlder && i == 0) {
                          return Center(
                            child: TextButton(
                              onPressed: loadingOlder ? null : _loadOlder,
                              child: Text(loadingOlder ? '正在加载' : '查看更早消息'),
                            ),
                          );
                        }
                        final index = i - (hasOlder ? 1 : 0);
                        final item = messages[index];
                        return _Message(
                          item: item,
                          mine:
                              item.senderId ==
                              ref.read(authControllerProvider).user?.id,
                        );
                      },
                    ),
                  ),
                ),
                if (!x.writable)
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(13),
                    color: Theme.of(context).colorScheme.surfaceContainerHigh,
                    child: const Text(
                      '当前会员资格已到期、暂停或双方已拉黑，聊天记录仅可查看',
                      textAlign: TextAlign.center,
                    ),
                  ),
                if (x.writable)
                  SafeArea(
                    top: false,
                    child: Container(
                      padding: const EdgeInsets.fromLTRB(8, 7, 8, 7),
                      color: Theme.of(context).colorScheme.surface,
                      child: Row(
                        children: [
                          IconButton(
                            onPressed: () => _image(ImageSource.camera),
                            icon: const Icon(Icons.photo_camera_outlined),
                          ),
                          IconButton(
                            onPressed: () => _image(ImageSource.gallery),
                            icon: const Icon(Icons.photo_library_outlined),
                          ),
                          IconButton(
                            onPressed: _call,
                            icon: const Icon(Icons.call_outlined),
                          ),
                          Expanded(
                            child: TextField(
                              controller: input,
                              maxLength: 2000,
                              minLines: 1,
                              maxLines: 4,
                              textInputAction: TextInputAction.send,
                              onSubmitted: (_) => _send(),
                              decoration: const InputDecoration(
                                counterText: '',
                                hintText: '输入消息',
                              ),
                            ),
                          ),
                          IconButton(
                            onPressed: sending ? null : _send,
                            icon: sending
                                ? const SizedBox(
                                    width: 18,
                                    height: 18,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                    ),
                                  )
                                : const Icon(Icons.send_rounded),
                          ),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
    );
  }
}

class _Message extends StatelessWidget {
  const _Message({required this.item, required this.mine});
  final CuratedMessage item;
  final bool mine;
  @override
  Widget build(BuildContext context) {
    if (item.system) {
      return Container(
        margin: const EdgeInsets.symmetric(vertical: 10),
        padding: const EdgeInsets.all(13),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.tertiaryContainer,
          borderRadius: BorderRadius.circular(14),
        ),
        child: Text(item.content, style: const TextStyle(height: 1.5)),
      );
    }
    return Align(
      alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
      child: Column(
        crossAxisAlignment: mine
            ? CrossAxisAlignment.end
            : CrossAxisAlignment.start,
        children: [
          Container(
            constraints: const BoxConstraints(maxWidth: 290),
            margin: const EdgeInsets.symmetric(vertical: 4),
            padding: item.type == 'IMAGE'
                ? const EdgeInsets.all(4)
                : const EdgeInsets.symmetric(horizontal: 13, vertical: 9),
            decoration: BoxDecoration(
              color: mine
                  ? Theme.of(context).colorScheme.primaryContainer
                  : Theme.of(context).colorScheme.surfaceContainerHigh,
              borderRadius: BorderRadius.circular(14),
            ),
            child: item.type == 'IMAGE'
                ? GestureDetector(
                    onTap: () => _previewImage(context, item.attachmentUrl),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(11),
                      child: Image.network(
                        AppConfig.resolveResource(
                          item.attachmentUrl,
                        ).toString(),
                        width: 190,
                        fit: BoxFit.cover,
                      ),
                    ),
                  )
                : Text(item.content),
          ),
          if (item.createdAt != null)
            Text(
              DateFormat('HH:mm').format(item.createdAt!),
              style: Theme.of(context).textTheme.bodySmall,
            ),
        ],
      ),
    );
  }

  void _previewImage(BuildContext context, String url) {
    showDialog<void>(
      context: context,
      barrierColor: Colors.black.withValues(alpha: .92),
      builder: (dialogContext) => Dialog.fullscreen(
        backgroundColor: Colors.black,
        child: SafeArea(
          child: Stack(
            children: [
              Center(
                child: InteractiveViewer(
                  minScale: .5,
                  maxScale: 5,
                  child: Image.network(
                    AppConfig.resolveResource(url).toString(),
                    fit: BoxFit.contain,
                  ),
                ),
              ),
              Positioned(
                top: 8,
                right: 8,
                child: IconButton(
                  onPressed: () => Navigator.pop(dialogContext),
                  color: Colors.white,
                  icon: const Icon(Icons.close_rounded),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
