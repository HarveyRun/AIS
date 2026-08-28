import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/config/app_config.dart';
import '../../core/formatters/money_formatter.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/network/realtime_service.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/inquiry_models.dart';
import '../../data/repositories/app_repository.dart';
import 'inquiry_quality_sheet.dart';

class ChatPage extends ConsumerStatefulWidget {
  const ChatPage({super.key, required this.id});
  final int id;

  @override
  ConsumerState<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends ConsumerState<ChatPage>
    with WidgetsBindingObserver {
  final _textController = TextEditingController();
  final _composerFocusNode = FocusNode();
  final _scrollController = ScrollController();
  final _picker = ImagePicker();
  StreamSubscription<RealtimeEvent>? _subscription;
  InquiryDetail? _detail;
  bool _loading = true;
  bool _emojiOpen = false;
  bool _moreOpen = false;
  InquiryQualityOptions? _quality;

  static const _emojis = [
    '😀',
    '😃',
    '😄',
    '😁',
    '😆',
    '😅',
    '😂',
    '🤣',
    '😊',
    '😇',
    '🙂',
    '🙃',
    '😉',
    '😌',
    '😍',
    '🥰',
    '😘',
    '😋',
    '😛',
    '😜',
    '🤪',
    '😎',
    '🤓',
    '🧐',
    '🤔',
    '🤭',
    '🤫',
    '🤗',
    '🫡',
    '😐',
    '😑',
    '😶',
    '🙄',
    '😏',
    '😒',
    '😔',
    '😢',
    '😭',
    '😤',
    '😠',
    '😡',
    '🤯',
    '😳',
    '🥺',
    '😴',
    '🤢',
    '🤮',
    '🤧',
    '😷',
    '🤒',
    '😱',
    '😨',
    '😰',
    '😥',
    '😓',
    '🫣',
    '🫠',
    '👍',
    '👎',
    '✌️',
    '🤞',
    '👏',
    '🙌',
    '👐',
    '🙏',
    '💪',
    '👀',
    '🎉',
    '🎊',
    '✨',
    '⭐',
    '🔥',
    '💯',
    '❤️',
    '🧡',
    '💛',
    '💚',
    '💙',
    '💜',
    '🖤',
    '🤍',
    '💔',
    '💕',
    '👌',
    '🌹',
    '🤝',
    '🎁',
    '☕',
    '🍺',
    '🍻',
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _composerFocusNode.addListener(_handleComposerFocusChanged);
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
    WidgetsBinding.instance.removeObserver(this);
    _subscription?.cancel();
    _composerFocusNode.removeListener(_handleComposerFocusChanged);
    _textController.dispose();
    _composerFocusNode.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  void didChangeMetrics() {
    _scheduleScrollToEnd(animate: false);
  }

  void _handleComposerFocusChanged() {
    if (!_composerFocusNode.hasFocus || (!_emojiOpen && !_moreOpen)) return;
    setState(() {
      _emojiOpen = false;
      _moreOpen = false;
    });
  }

  void _toggleEmojiPanel() {
    if (_emojiOpen) {
      setState(() => _emojiOpen = false);
      _composerFocusNode.requestFocus();
      return;
    }
    FocusManager.instance.primaryFocus?.unfocus();
    setState(() {
      _emojiOpen = true;
      _moreOpen = false;
    });
    _scheduleScrollToEnd(animate: false);
  }

  void _toggleMorePanel() {
    FocusManager.instance.primaryFocus?.unfocus();
    setState(() {
      _moreOpen = !_moreOpen;
      _emojiOpen = false;
    });
    _scheduleScrollToEnd(animate: false);
  }

  void _dismissInputPanels() {
    FocusManager.instance.primaryFocus?.unfocus();
    if (!_emojiOpen && !_moreOpen) return;
    setState(() {
      _emojiOpen = false;
      _moreOpen = false;
    });
  }

  Future<void> _load({bool silent = false}) async {
    if (!silent) setState(() => _loading = true);
    try {
      InquiryDetail? detail;
      InquiryQualityOptions? quality;

      Future<void> fetchDetail() async {
        final repository = ref.read(repositoryProvider);
        detail = await repository.inquiry(widget.id, showLoading: false);
        if (!detail!.inquiry.isIncoming &&
            [
              'COMPLETED',
              'QUALITY_REFUNDED',
              'DISPUTED',
            ].contains(detail!.inquiry.status.toUpperCase())) {
          quality = await repository.inquiryQualityOptions(
            widget.id,
            showLoading: false,
          );
        }
      }

      if (silent) {
        await fetchDetail();
      } else {
        await ref.read(requestLoadingProvider).run(fetchDetail);
      }
      if (!mounted) return;
      setState(() {
        _detail = detail;
        _quality = quality;
      });
      _scheduleScrollToEnd(animate: false);
      await ref.read(repositoryProvider).markInquiryRead(widget.id);
    } catch (error) {
      if (!silent && mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted && !silent) setState(() => _loading = false);
    }
  }

  void _scrollToEnd({bool animate = true}) {
    if (!_scrollController.hasClients) return;
    final target = _scrollController.position.maxScrollExtent;
    if (animate) {
      _scrollController.animateTo(
        target,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOutCubic,
      );
    } else {
      _scrollController.jumpTo(target);
    }
  }

  void _scheduleScrollToEnd({bool animate = true}) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _scrollToEnd(animate: animate);
      Future<void>.delayed(const Duration(milliseconds: 120), () {
        if (!mounted) return;
        _scrollToEnd(animate: animate);
      });
      Future<void>.delayed(const Duration(milliseconds: 360), () {
        if (!mounted) return;
        _scrollToEnd(animate: false);
      });
    });
  }

  Future<void> _sendText() async {
    final content = _textController.text.trim();
    if (content.isEmpty || !(_detail?.inquiry.canChat ?? false)) return;
    final user = ref.read(authControllerProvider).user;
    if (user == null) return;
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
      reportable: false,
      sending: true,
    );
    _textController.clear();
    setState(
      () => _detail = InquiryDetail(
        inquiry: _detail!.inquiry,
        messages: [..._detail!.messages, pending],
      ),
    );
    _composerFocusNode.requestFocus();
    _scheduleScrollToEnd();
    try {
      final saved = await ref
          .read(repositoryProvider)
          .sendInquiryMessage(widget.id, content);
      if (!mounted) return;
      setState(() => _detail = _replaceMessage(pending.id, saved));
      _composerFocusNode.requestFocus();
      _scheduleScrollToEnd();
    } catch (error) {
      if (!mounted) return;
      setState(
        () => _detail = _replaceMessage(
          pending.id,
          pending.copyWith(sending: false, failed: true),
        ),
      );
      AppMessage.show(context, '$error');
    }
  }

  InquiryDetail _replaceMessage(String id, ChatMessage replacement) {
    final messages = <ChatMessage>[];
    var replaced = false;
    for (final item in _detail!.messages) {
      if (item.id == id) {
        if (!messages.any((message) => message.id == replacement.id)) {
          messages.add(replacement);
        }
        replaced = true;
      } else if (item.id != replacement.id) {
        messages.add(item);
      }
    }
    if (!replaced && !messages.any((item) => item.id == replacement.id)) {
      messages.add(replacement);
    }
    return InquiryDetail(inquiry: _detail!.inquiry, messages: messages);
  }

  Future<void> _retryMessage(ChatMessage failedMessage) async {
    if (failedMessage.sending || !(_detail?.inquiry.canChat ?? false)) return;
    final sending = failedMessage.copyWith(sending: true, failed: false);
    setState(() => _detail = _replaceMessage(failedMessage.id, sending));
    _scheduleScrollToEnd();
    try {
      final saved = await ref
          .read(repositoryProvider)
          .sendInquiryMessage(widget.id, failedMessage.content);
      if (!mounted) return;
      setState(() => _detail = _replaceMessage(failedMessage.id, saved));
    } catch (error) {
      if (!mounted) return;
      setState(
        () => _detail = _replaceMessage(
          failedMessage.id,
          failedMessage.copyWith(sending: false, failed: true),
        ),
      );
      AppMessage.show(context, '$error');
    }
  }

  Future<void> _sendPhoto(ImageSource source) async {
    setState(() => _moreOpen = false);
    final image = await _picker.pickImage(
      source: source,
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

  Future<void> _reportMessage(ChatMessage chatMessage) async {
    if (!chatMessage.reportable) return;
    try {
      final reported = await ref
          .read(repositoryProvider)
          .hasReportedInquiryMessage(widget.id, chatMessage.id);
      if (!mounted) return;
      if (reported) {
        AppMessage.show(context, '你已经举报过这条消息');
        return;
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
      return;
    }
    final reportType = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (_) => const _MessageReportDialog(),
    );
    if (reportType == null || !mounted) return;
    try {
      await ref
          .read(repositoryProvider)
          .reportInquiryMessage(widget.id, chatMessage.id, reportType);
      if (mounted) AppMessage.show(context, '举报已提交');
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
          ? const SizedBox.shrink()
          : detail == null
          ? const Center(child: Text('询问不存在'))
          : Column(
              children: [
                Container(
                  color: Theme.of(context).brightness == Brightness.dark
                      ? const Color(0xFF151619)
                      : const Color(0xFFF2F2F2),
                  padding: const EdgeInsets.fromLTRB(10, 8, 10, 8),
                  child: _ChatStatus(
                    inquiry: inquiry!,
                    quality: _quality,
                    onAction: _action,
                    onEvaluate: _evaluate,
                  ),
                ),
                Expanded(
                  child: GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: _dismissInputPanels,
                    child: Container(
                      color: Theme.of(context).brightness == Brightness.dark
                          ? const Color(0xFF151619)
                          : const Color(0xFFF2F2F2),
                      child: ListView.builder(
                        controller: _scrollController,
                        keyboardDismissBehavior:
                            ScrollViewKeyboardDismissBehavior.onDrag,
                        padding: const EdgeInsets.fromLTRB(10, 18, 10, 18),
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
                            onReport:
                                message.reportable &&
                                    message.senderId !=
                                        ref
                                            .read(authControllerProvider)
                                            .user
                                            ?.id
                                ? () => _reportMessage(message)
                                : null,
                            onRetry: message.failed
                                ? () => _retryMessage(message)
                                : null,
                          );
                        },
                      ),
                    ),
                  ),
                ),
                _Composer(
                  controller: _textController,
                  focusNode: _composerFocusNode,
                  enabled: canChat,
                  emojiOpen: _emojiOpen,
                  moreOpen: _moreOpen,
                  onSubmitted: _sendText,
                  onEmoji: _toggleEmojiPanel,
                  onMore: _toggleMorePanel,
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
                    onPhoto: () => _sendPhoto(ImageSource.gallery),
                    onCamera: () => _sendPhoto(ImageSource.camera),
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

  Future<void> _evaluate() async {
    final data = await showInquiryEvaluationSheet(context);
    if (data == null || !mounted) return;
    try {
      await ref.read(repositoryProvider).evaluateInquiry(widget.id, data);
      await _load(silent: true);
      if (mounted) AppMessage.show(context, '评价已提交');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }
}

class _ChatStatus extends ConsumerWidget {
  const _ChatStatus({
    required this.inquiry,
    required this.quality,
    required this.onAction,
    required this.onEvaluate,
  });
  final InquirySummary inquiry;
  final InquiryQualityOptions? quality;
  final void Function(Future<void> Function(int), String) onAction;
  final VoidCallback onEvaluate;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final repository = ref.read(repositoryProvider);
    final status = inquiry.status.toUpperCase();
    final statusStyle = appStatusStyle(context, status);
    final amountLabel = _amountLabel(inquiry, status);
    final amountValue = _amountValue(inquiry, status);
    final text = switch (status) {
      'PENDING' => inquiry.isIncoming ? '等待你的决定' : '等待对方接受',
      'ACTIVE' => '交流进行中',
      'AWAITING_CONFIRMATION' => inquiry.isIncoming ? '等待提问者确认结束' : '对方申请结束',
      'COMPLETED' || 'ENDED' => '本次交流已经结束',
      'DISPUTED' => '平台正在核对本次交流',
      'QUALITY_REFUNDED' => '本次询问已经全额退款',
      'TIMEOUT_REFUNDED' => '本次询问已因超时全额退款',
      'END_DISPUTE_REFUNDED' => '平台已退回本次询问剩余金额',
      'REJECTED' => '本次询问未接受',
      'CANCELLED' => '本次询问已撤销',
      'EXPIRED' => '本次询问已超时',
      _ => '本次询问正在处理中',
    };
    return Material(
      color: Theme.of(context).colorScheme.surface,
      borderRadius: BorderRadius.circular(14),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
        child: Column(
          children: [
            Row(
              children: [
                Icon(
                  switch (status) {
                    'ACTIVE' => Icons.chat_bubble_outline_rounded,
                    'COMPLETED' ||
                    'ENDED' => Icons.check_circle_outline_rounded,
                    'REJECTED' ||
                    'CANCELLED' ||
                    'EXPIRED' => Icons.error_outline_rounded,
                    _ => Icons.schedule_rounded,
                  },
                  size: 18,
                  color: statusStyle.foreground,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    text,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      color: statusStyle.foreground,
                    ),
                  ),
                ),
                Text(
                  '$amountLabel ¥${formatMoney(amountValue)}',
                  style: TextStyle(
                    fontWeight: FontWeight.w700,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
              ],
            ),
            if (status == 'PENDING') ...[
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 11,
                  vertical: 9,
                ),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.surfaceContainer,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      inquiry.isIncoming ? '用户想问：' : '我要问的：',
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      inquiry.question.isEmpty ? '未填写' : inquiry.question,
                      style: Theme.of(
                        context,
                      ).textTheme.bodyMedium?.copyWith(height: 1.45),
                    ),
                  ],
                ),
              ),
            ],
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
                          onAction(repository.disagreeInquiryEnd, '已提交平台处理'),
                      child: const Text('不同意'),
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
            if (!inquiry.isIncoming && quality?.canEvaluate == true) ...[
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: onEvaluate,
                      child: const Text('评价本次交流'),
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

  String _amountLabel(InquirySummary inquiry, String status) {
    if (inquiry.isIncoming) {
      return switch (status) {
        'COMPLETED' || 'ENDED' => '实际收入',
        'DISPUTED' => '待结算',
        'REJECTED' ||
        'CANCELLED' ||
        'EXPIRED' ||
        'QUALITY_REFUNDED' ||
        'TIMEOUT_REFUNDED' ||
        'END_DISPUTE_REFUNDED' => '实际收入',
        _ => '预计收入',
      };
    }
    return switch (status) {
      'COMPLETED' || 'ENDED' => '实际支付',
      'DISPUTED' => '待处理金额',
      'REJECTED' ||
      'CANCELLED' ||
      'EXPIRED' ||
      'QUALITY_REFUNDED' ||
      'TIMEOUT_REFUNDED' ||
      'END_DISPUTE_REFUNDED' => '已退款',
      _ when inquiry.timeoutCount > 0 => '剩余金额',
      _ => '询问金额',
    };
  }

  double _amountValue(InquirySummary inquiry, String status) {
    const noIncomeStatuses = {
      'REJECTED',
      'CANCELLED',
      'EXPIRED',
      'QUALITY_REFUNDED',
      'TIMEOUT_REFUNDED',
      'END_DISPUTE_REFUNDED',
    };
    if (inquiry.isIncoming) {
      return noIncomeStatuses.contains(status)
          ? 0
          : inquiry.answererIncomeAmount;
    }
    return noIncomeStatuses.contains(status)
        ? inquiry.amount
        : inquiry.settleableAmount;
  }
}

class _MessageBubble extends ConsumerWidget {
  const _MessageBubble({
    required this.message,
    required this.showTime,
    this.onReport,
    this.onRetry,
  });
  final ChatMessage message;
  final bool showTime;
  final VoidCallback? onReport;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final me = message.senderId == ref.read(authControllerProvider).user?.id;
    final name = message.senderName.trim().isEmpty ? '用户' : message.senderName;
    final systemMessage =
        message.senderId == 0 ||
        const {'SYSTEM', 'TIMEOUT_NOTICE'}.contains(message.type.toUpperCase());
    if (systemMessage) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 16),
        child: Center(
          child: Container(
            constraints: const BoxConstraints(maxWidth: 310),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(9),
            ),
            child: Text(
              message.content,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ),
      );
    }
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onLongPress: onReport,
      child: Column(
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
                          InkWell(
                            onTap: onRetry,
                            borderRadius: BorderRadius.circular(10),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(vertical: 4),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(
                                    Icons.error_rounded,
                                    size: 17,
                                    color: Theme.of(context).colorScheme.error,
                                  ),
                                  const SizedBox(width: 3),
                                  Text(
                                    '发送失败',
                                    style: Theme.of(context).textTheme.bodySmall
                                        ?.copyWith(
                                          color: Theme.of(
                                            context,
                                          ).colorScheme.error,
                                        ),
                                  ),
                                ],
                              ),
                            ),
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
                                  ? Theme.of(context).brightness ==
                                            Brightness.dark
                                        ? const Color(0xFF263746)
                                        : const Color(0xFFDFE9F2)
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
                                    child: GestureDetector(
                                      onTap: () => Navigator.of(context).push(
                                        MaterialPageRoute<void>(
                                          builder: (_) => _ChatImageViewer(
                                            imageUrl: AppConfig.resolveImage(
                                              message.attachmentUrl,
                                            ).toString(),
                                          ),
                                        ),
                                      ),
                                      child: Image.network(
                                        AppConfig.resolveImage(
                                          message.attachmentUrl,
                                        ).toString(),
                                        width: 180,
                                        fit: BoxFit.cover,
                                        errorBuilder: (_, _, _) =>
                                            const SizedBox(
                                              width: 180,
                                              height: 120,
                                              child: Center(
                                                child: Icon(
                                                  Icons.broken_image_outlined,
                                                ),
                                              ),
                                            ),
                                      ),
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
      ),
    );
  }
}

class _MessageReportDialog extends StatefulWidget {
  const _MessageReportDialog();

  @override
  State<_MessageReportDialog> createState() => _MessageReportDialogState();
}

class _MessageReportDialogState extends State<_MessageReportDialog> {
  String? _selected;

  static const _options = <(String, String)>[
    ('LOW_RELEVANCE', '回复内容与原始提问基本无关'),
    ('PORN_GAMBLING_DRUGS', '黄赌毒内容'),
    ('NATIONAL_SECURITY', '危害国家安全、破坏社会稳定的言论'),
  ];

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      child: AlertDialog(
        insetPadding: const EdgeInsets.symmetric(horizontal: 24),
        titlePadding: const EdgeInsets.fromLTRB(24, 14, 10, 8),
        title: Row(
          children: [
            const Expanded(child: Text('举报这条消息')),
            IconButton(
              onPressed: () => Navigator.pop(context),
              tooltip: '关闭',
              icon: const Icon(Icons.close_rounded),
            ),
          ],
        ),
        content: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: _options
                .map(
                  (option) => _ReportOption(
                    label: option.$2,
                    selected: _selected == option.$1,
                    onTap: () => setState(() => _selected = option.$1),
                  ),
                )
                .toList(growable: false),
          ),
        ),
        actions: [
          FilledButton(
            onPressed: _selected == null
                ? null
                : () => Navigator.pop(context, _selected),
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }
}

class _ReportOption extends StatelessWidget {
  const _ReportOption({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Material(
        color: selected
            ? Theme.of(context).colorScheme.primary
            : Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(12),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 12),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    label,
                    style: TextStyle(
                      color: selected
                          ? Theme.of(context).colorScheme.onPrimary
                          : Theme.of(context).colorScheme.onSurface,
                      fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ChatImageViewer extends StatelessWidget {
  const _ChatImageViewer({required this.imageUrl});

  final String imageUrl;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: const Text('图片'),
      ),
      body: Center(
        child: InteractiveViewer(
          minScale: 1,
          maxScale: 5,
          child: Image.network(
            imageUrl,
            fit: BoxFit.contain,
            loadingBuilder: (context, child, progress) {
              if (progress == null) return child;
              return const SizedBox.square(
                dimension: 24,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              );
            },
            errorBuilder: (_, _, _) => const Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.broken_image_outlined,
                  color: Colors.white70,
                  size: 38,
                ),
                SizedBox(height: 10),
                Text('图片加载失败', style: TextStyle(color: Colors.white70)),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Composer extends StatelessWidget {
  const _Composer({
    required this.controller,
    required this.focusNode,
    required this.enabled,
    required this.emojiOpen,
    required this.moreOpen,
    required this.onSubmitted,
    required this.onEmoji,
    required this.onMore,
  });
  final TextEditingController controller;
  final FocusNode focusNode;
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
              focusNode: focusNode,
              enabled: enabled,
              minLines: 1,
              maxLines: 4,
              textInputAction: TextInputAction.send,
              onEditingComplete: () {},
              onSubmitted: (_) => onSubmitted(),
              inputFormatters: AppInputFormatters.description(500),
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
    height: 232,
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
  const _MorePanel({
    required this.onPhoto,
    required this.onCamera,
    required this.onEnd,
  });
  final VoidCallback onPhoto;
  final VoidCallback onCamera;
  final VoidCallback onEnd;
  @override
  Widget build(BuildContext context) => SizedBox(
    height: 190,
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _MoreItem(icon: Icons.photo_outlined, label: '照片', onTap: onPhoto),
        _MoreItem(
          icon: Icons.photo_camera_outlined,
          label: '拍照',
          onTap: onCamera,
        ),
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
