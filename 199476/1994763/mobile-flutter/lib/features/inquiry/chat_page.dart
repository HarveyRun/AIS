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
import '../../data/models/app_global_settings.dart';
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
  AudioAppointment? _audioAppointment;
  bool _loading = true;
  bool _emojiOpen = false;
  bool _moreOpen = false;
  InquiryQualityOptions? _quality;
  AppGlobalSettings? _settings;

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
      AudioAppointment? audioAppointment;
      InquiryQualityOptions? quality;

      Future<void> fetchDetail() async {
        final repository = ref.read(repositoryProvider);
        _settings ??= await repository.appGlobalSettings();
        detail = await repository.inquiry(widget.id, showLoading: false);
        audioAppointment = await repository.latestAudioAppointment(widget.id);
        if (!detail!.inquiry.isIncoming &&
            detail!.inquiry.status.toUpperCase() == 'COMPLETED') {
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
        _audioAppointment = audioAppointment;
        _quality = quality;
      });
      _scheduleScrollToEnd(animate: false);
      await ref.read(repositoryProvider).markInquiryRead(widget.id);
      final unreadCount = await ref
          .read(repositoryProvider)
          .inquiryUnreadCount();
      if (mounted) {
        ref.read(inquiryUnreadCountProvider.notifier).state = unreadCount;
      }
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
      await _load(silent: true);
      if (!mounted) return;
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

  Future<void> _createAudioCall() async {
    final inquiry = _detail?.inquiry;
    if (inquiry == null) return;
    final confirmed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 22),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      '发起语音通话',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(sheetContext, false),
                    icon: const Icon(Icons.close_rounded),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Text('对方的询问费用为 ¥${inquiry.hourlyRateSnapshot}/小时，接通后开始计费。'),
              const SizedBox(height: 20),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => Navigator.pop(sheetContext, true),
                  child: const Text('呼叫'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      final call = await ref
          .read(repositoryProvider)
          .createAudioCall(inquiryId: widget.id);
      if (!mounted) return;
      await context.push('/voice-call/${widget.id}/${call.id}?initiator=1');
      if (mounted) await _load(silent: true);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _enterAudioCall() async {
    final appointment = _audioAppointment;
    if (appointment == null || !appointment.exists) return;
    await context.push(
      '/voice-call/${widget.id}/${appointment.id}?initiator=${appointment.isIncoming ? 0 : 1}',
    );
    if (mounted) await _load(silent: true);
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
      await _load(silent: true);
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

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    final inquiry = detail?.inquiry;
    final status = inquiry?.status.toUpperCase();
    final canChat = inquiry?.canChat == true;
    final appointment = _audioAppointment;
    final appointmentOpen = appointment?.isOpen == true;
    final allowImages = status == 'PAID_ACTIVE';
    final canEnd =
        inquiry != null &&
        !inquiry.isIncoming &&
        status == 'ACTIVE' &&
        !appointmentOpen;
    final canCreateAudioCall =
        inquiry?.canCreateAudioAppointment == true &&
        (appointment == null || !appointment.isOpen);
    final hasMoreActions =
        inquiry != null && (allowImages || canEnd || canCreateAudioCall);
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
                    audioAppointment: appointment,
                    quality: _quality,
                    onAction: _action,
                    onEvaluate: _evaluate,
                    onEnterAudioCall: _enterAudioCall,
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
                  disabledHint: inquiry.communicationBlocked
                      ? '你与对方已无法继续交流'
                      : status == 'PENDING'
                      ? '对方接受后可以聊天'
                      : '本次交流暂不能聊天',
                  emojiOpen: _emojiOpen,
                  moreOpen: _moreOpen,
                  moreEnabled: hasMoreActions,
                  onSubmitted: _sendText,
                  onEmoji: _toggleEmojiPanel,
                  onMore: _toggleMorePanel,
                  maxLength: inquiry.isCurrentFlow
                      ? (_settings?.textMessageMaxLength ?? 100)
                      : 500,
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
                if (_moreOpen && hasMoreActions)
                  _MorePanel(
                    allowImages: allowImages,
                    onPhoto: () => _sendPhoto(ImageSource.gallery),
                    onCamera: () => _sendPhoto(ImageSource.camera),
                    onAudioCall: canCreateAudioCall
                        ? () {
                            setState(() => _moreOpen = false);
                            _createAudioCall();
                          }
                        : null,
                    onEnd: canEnd
                        ? () {
                            setState(() => _moreOpen = false);
                            final repository = ref.read(repositoryProvider);
                            _confirmEnd(repository);
                          }
                        : null,
                  ),
              ],
            ),
    );
  }

  Future<void> _confirmEnd(AppRepository repository) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        titlePadding: const EdgeInsets.fromLTRB(24, 14, 8, 0),
        title: Row(
          children: [
            const Expanded(child: Text('结束本次询问？')),
            IconButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              tooltip: '关闭',
              icon: const Icon(Icons.close_rounded),
            ),
          ],
        ),
        content: const Text('确定结束本次询问吗？'),
        actions: [
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('确认结束'),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      _action(repository.endInquiry, '本次询问已结束');
    }
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
    required this.audioAppointment,
    required this.quality,
    required this.onAction,
    required this.onEvaluate,
    required this.onEnterAudioCall,
  });
  final InquirySummary inquiry;
  final AudioAppointment? audioAppointment;
  final InquiryQualityOptions? quality;
  final void Function(Future<void> Function(int), String) onAction;
  final VoidCallback onEvaluate;
  final VoidCallback onEnterAudioCall;

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
      'TEXT_LIMIT_REACHED' => '文字交流额度已用完',
      'TEXT_ENDED' => '文字交流已经结束',
      'PAID_ACTIVE' => '语音通话进行中',
      'COMPLETED' || 'ENDED' => '本次交流已经结束',
      'TIMEOUT_REFUNDED' => '本次询问已因超时全额退款',
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
                if (!inquiry.isCurrentFlow || inquiry.amount > 0)
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
                      inquiry.isIncoming ? '用户想问' : '我要问的',
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      inquiry.question.isEmpty ? '-' : inquiry.question,
                      style: Theme.of(
                        context,
                      ).textTheme.bodyMedium?.copyWith(height: 1.45),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      inquiry.isIncoming
                          ? '请在72小时内处理，超时后询问将自动结束'
                          : '对方72小时内未处理，询问将自动结束',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
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
                  onPressed: () => onAction(repository.cancelInquiry, '询问已撤销'),
                  child: const Text('撤销询问'),
                ),
              ),
            ],
            if (inquiry.isCurrentFlow &&
                const {
                  'PENDING',
                  'ACTIVE',
                  'TEXT_LIMIT_REACHED',
                  'TEXT_ENDED',
                }.contains(status)) ...[
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      const {
                            'TEXT_LIMIT_REACHED',
                            'TEXT_ENDED',
                          }.contains(status)
                          ? (status == 'TEXT_LIMIT_REACHED'
                                ? '免费消息额度已用完'
                                : '文字交流已主动结束')
                          : '免费消息剩余 ${inquiry.remainingTextCount}/${inquiry.textMessageLimit}',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ),
                ],
              ),
            ],
            if (audioAppointment?.exists == true) ...[
              const SizedBox(height: 10),
              _AudioAppointmentCard(
                appointment: audioAppointment!,
                onEnter: onEnterAudioCall,
              ),
            ],
            if (inquiry.isCurrentFlow &&
                status == 'PAID_ACTIVE' &&
                audioAppointment?.exists != true) ...[
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  '${inquiry.purchasedMinutes}分钟 · 文字消息不限量${inquiry.paidSessionEndsAt == null ? '' : ' · ${DateFormat('HH:mm').format(inquiry.paidSessionEndsAt!)}结束'}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
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
        'REJECTED' || 'CANCELLED' || 'EXPIRED' || 'TIMEOUT_REFUNDED' => '实际收入',
        _ => '预计收入',
      };
    }
    return switch (status) {
      'COMPLETED' || 'ENDED' => '实际支付',
      'REJECTED' || 'CANCELLED' || 'EXPIRED' || 'TIMEOUT_REFUNDED' => '已退款',
      _ when inquiry.timeoutCount > 0 => '剩余金额',
      _ => '询问金额',
    };
  }

  double _amountValue(InquirySummary inquiry, String status) {
    const noIncomeStatuses = {
      'REJECTED',
      'CANCELLED',
      'EXPIRED',
      'TIMEOUT_REFUNDED',
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
    this.onRetry,
  });
  final ChatMessage message;
  final bool showTime;
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

class _AudioAppointmentCard extends StatelessWidget {
  const _AudioAppointmentCard({
    required this.appointment,
    required this.onEnter,
  });

  final AudioAppointment appointment;
  final VoidCallback onEnter;

  @override
  Widget build(BuildContext context) {
    final status = appointment.status.toUpperCase();
    final title = switch (status) {
      'CONNECTING' => appointment.isIncoming ? '语音来电' : '正在呼叫',
      'ACTIVE' => '语音通话中',
      'REJECTED' => '对方未接听',
      'CANCELLED' => '已取消呼叫',
      'EXPIRED' => '对方未接听',
      'COMPLETED' => '语音通话已结束',
      'CONNECTION_FAILED' => '对方未接听',
      _ => '语音通话',
    };

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(12, 11, 12, 10),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.phone_in_talk_outlined,
                size: 18,
                color: Theme.of(context).colorScheme.primary,
              ),
              const SizedBox(width: 7),
              Expanded(
                child: Text(
                  title,
                  style: Theme.of(context).textTheme.titleSmall,
                ),
              ),
              Text(
                status == 'COMPLETED'
                    ? '实付 ¥${formatMoney(appointment.actualAmount)}'
                    : '¥${appointment.hourlyRateSnapshot}/小时',
                style: TextStyle(
                  color: Theme.of(context).colorScheme.primary,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const SizedBox(height: 7),
          Text(
            status == 'COMPLETED'
                ? '实际通话 ${_formatSeconds(appointment.actualDurationSeconds)}'
                : status == 'ACTIVE'
                ? '通话已接通'
                : '等待接听',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          if (const {'CONNECTING', 'ACTIVE'}.contains(status)) ...[
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: onEnter,
                icon: const Icon(Icons.call_rounded),
                label: Text(
                  status == 'CONNECTING' && appointment.isIncoming
                      ? '查看来电'
                      : '进入通话',
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

String _formatSeconds(int seconds) {
  if (seconds < 60) return '$seconds秒';
  final hours = seconds ~/ 3600;
  final minutes = (seconds % 3600) ~/ 60;
  final rest = seconds % 60;
  if (hours > 0) return '$hours小时$minutes分$rest秒';
  return '$minutes分$rest秒';
}

class _Composer extends StatelessWidget {
  const _Composer({
    required this.controller,
    required this.focusNode,
    required this.enabled,
    required this.disabledHint,
    required this.emojiOpen,
    required this.moreOpen,
    required this.moreEnabled,
    required this.onSubmitted,
    required this.onEmoji,
    required this.onMore,
    required this.maxLength,
  });
  final TextEditingController controller;
  final FocusNode focusNode;
  final bool enabled;
  final String disabledHint;
  final bool emojiOpen;
  final bool moreOpen;
  final bool moreEnabled;
  final VoidCallback onSubmitted;
  final VoidCallback onEmoji;
  final VoidCallback onMore;
  final int maxLength;
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
              inputFormatters: AppInputFormatters.description(maxLength),
              decoration: InputDecoration(
                hintText: enabled ? '说点什么…' : disabledHint,
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
            onPressed: moreEnabled ? onMore : null,
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
    required this.onAudioCall,
    required this.onEnd,
    required this.allowImages,
  });
  final VoidCallback onPhoto;
  final VoidCallback onCamera;
  final VoidCallback? onAudioCall;
  final VoidCallback? onEnd;
  final bool allowImages;

  @override
  Widget build(BuildContext context) {
    final items = <Widget>[
      if (allowImages)
        _MoreItem(
          icon: Icons.photo_camera_outlined,
          label: '拍照',
          onTap: onCamera,
        ),
      if (allowImages)
        _MoreItem(icon: Icons.photo_outlined, label: '相册', onTap: onPhoto),
      if (onAudioCall != null)
        _MoreItem(
          icon: Icons.phone_in_talk_outlined,
          label: '语音通话',
          onTap: onAudioCall!,
        ),
      if (onEnd != null)
        _MoreItem(
          icon: Icons.stop_circle_outlined,
          label: '结束交流',
          onTap: onEnd!,
        ),
    ];

    return SizedBox(
      height: 190,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: List.generate(4, (index) {
          return Expanded(
            child: index < items.length
                ? Align(alignment: Alignment.topCenter, child: items[index])
                : const SizedBox.shrink(),
          );
        }),
      ),
    );
  }
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
    child: Padding(
      padding: const EdgeInsets.fromLTRB(6, 18, 6, 4),
      child: Column(
        mainAxisSize: MainAxisSize.min,
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
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    ),
  );
}
