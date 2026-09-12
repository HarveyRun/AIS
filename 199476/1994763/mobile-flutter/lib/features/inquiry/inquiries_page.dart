import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/formatters/money_formatter.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/network/realtime_service.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/inquiry_models.dart';
import 'ended_inquiry_actions.dart';
import 'experience_tip_sheet.dart';

class InquiriesPage extends ConsumerStatefulWidget {
  const InquiriesPage({super.key});

  @override
  ConsumerState<InquiriesPage> createState() => _InquiriesPageState();
}

class _InquiriesPageState extends ConsumerState<InquiriesPage>
    with SingleTickerProviderStateMixin {
  late final TabController _tabs;
  StreamSubscription<RealtimeEvent>? _subscription;
  Timer? _realtimeRefreshTimer;
  List<InquirySummary> _items = const [];
  bool _loading = true;
  bool _refreshing = false;

  @override
  void initState() {
    super.initState();
    _tabs = TabController(length: 2, vsync: this);
    _load();
    _subscription = ref.read(realtimeProvider).events.listen((event) {
      if (!event.type.startsWith('INQUIRY_')) return;
      _realtimeRefreshTimer?.cancel();
      _realtimeRefreshTimer = Timer(
        const Duration(milliseconds: 300),
        () => _load(silent: true),
      );
    });
  }

  @override
  void dispose() {
    _tabs.dispose();
    _realtimeRefreshTimer?.cancel();
    _subscription?.cancel();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    if (_refreshing) return;
    _refreshing = true;
    if (!silent) setState(() => _loading = true);
    try {
      final items = await ref
          .read(repositoryProvider)
          .inquiries(showLoading: !silent);
      if (!mounted) return;
      setState(() => _items = items);
      ref.read(inquiryUnreadCountProvider.notifier).state = items.fold<int>(
        0,
        (total, item) => total + item.unreadCount,
      );
    } catch (error) {
      if (!silent && mounted) AppMessage.show(context, '$error');
    } finally {
      _refreshing = false;
      if (mounted && !silent) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final outgoingUnread = _items.any(
      (item) => !item.isIncoming && item.unreadCount > 0,
    );
    final incomingUnread = _items.any(
      (item) => item.isIncoming && item.unreadCount > 0,
    );
    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.surface,
        automaticallyImplyLeading: false,
        title: const Text('我的询问'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(58),
          child: Container(
            height: 46,
            margin: const EdgeInsets.fromLTRB(10, 0, 10, 12),
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceContainer,
              borderRadius: BorderRadius.circular(13),
            ),
            child: TabBar(
              controller: _tabs,
              dividerColor: Colors.transparent,
              indicatorSize: TabBarIndicatorSize.tab,
              indicator: BoxDecoration(
                color: Theme.of(context).colorScheme.surface,
                borderRadius: BorderRadius.circular(10),
              ),
              tabs: [
                Tab(
                  child: _TabLabel(text: '我发起的', unread: outgoingUnread),
                ),
                Tab(
                  child: _TabLabel(text: '我收到的', unread: incomingUnread),
                ),
              ],
            ),
          ),
        ),
      ),
      body: _loading
          ? const SizedBox.shrink()
          : TabBarView(
              controller: _tabs,
              children: [
                _InquiryList(
                  items: _items.where((item) => !item.isIncoming).toList(),
                  onRefresh: _load,
                ),
                _InquiryList(
                  items: _items.where((item) => item.isIncoming).toList(),
                  onRefresh: _load,
                ),
              ],
            ),
    );
  }
}

class _TabLabel extends StatelessWidget {
  const _TabLabel({required this.text, required this.unread});
  final String text;
  final bool unread;
  @override
  Widget build(BuildContext context) => Row(
    mainAxisSize: MainAxisSize.min,
    children: [
      Text(text),
      if (unread) ...[
        const SizedBox(width: 6),
        Container(
          width: 7,
          height: 7,
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.primary,
            shape: BoxShape.circle,
          ),
        ),
      ],
    ],
  );
}

class _InquiryList extends ConsumerWidget {
  const _InquiryList({required this.items, required this.onRefresh});
  final List<InquirySummary> items;
  final Future<void> Function({bool silent}) onRefresh;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (items.isEmpty) {
      return RefreshIndicator(
        onRefresh: onRefresh,
        child: ListView(
          children: const [
            SizedBox(height: 150),
            Icon(
              Icons.chat_bubble_outline_rounded,
              size: 32,
              color: Color(0xFF9A9A9A),
            ),
            SizedBox(height: 12),
            Center(child: Text('这里还没有询问')),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: onRefresh,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
        itemCount: items.length,
        separatorBuilder: (_, _) => const SizedBox(height: 10),
        itemBuilder: (context, index) {
          final item = items[index];
          return Material(
            color: Theme.of(context).colorScheme.surface,
            borderRadius: BorderRadius.circular(16),
            clipBehavior: Clip.antiAlias,
            child: InkWell(
              onTap: () => context.push('/chat/${item.id}'),
              onLongPress: _isEnded(item)
                  ? () => _openEndedActions(context, ref, item)
                  : null,
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 14,
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    AppAvatar(
                      url: item.otherAvatar,
                      name: item.otherName,
                      radius: 22,
                    ),
                    const SizedBox(width: 11),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  item.otherName.isEmpty
                                      ? '对方'
                                      : item.otherName,
                                  style: Theme.of(
                                    context,
                                  ).textTheme.titleMedium,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              Text(
                                _formatTime(item.lastMessageAt),
                                style: Theme.of(context).textTheme.bodySmall,
                              ),
                            ],
                          ),
                          const SizedBox(height: 9),
                          Row(
                            children: [
                              if (!_hideCompletedIncome(item) &&
                                  (!item.isCurrentFlow || item.amount > 0)) ...[
                                Text(
                                  '${item.isIncoming ? '预计收入' : '询问金额'} ¥${formatMoney(item.visibleAmount)}',
                                  style: TextStyle(
                                    color: Theme.of(
                                      context,
                                    ).colorScheme.primary,
                                    fontSize: 13,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                                const SizedBox(width: 9),
                              ],
                              _StatusChip(item: item),
                              const Spacer(),
                              if (_canTip(item))
                                TextButton(
                                  onPressed: () => _tip(context, item),
                                  style: TextButton.styleFrom(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 8,
                                      vertical: 4,
                                    ),
                                    minimumSize: Size.zero,
                                    tapTargetSize:
                                        MaterialTapTargetSize.shrinkWrap,
                                  ),
                                  child: const Text('打赏'),
                                ),
                              if (item.unreadCount > 0)
                                Badge(
                                  label: Text(
                                    item.unreadCount > 99
                                        ? '99+'
                                        : '${item.unreadCount}',
                                  ),
                                ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  String _formatTime(DateTime? value) {
    if (value == null) return '尚未聊天';
    final now = DateTime.now();
    return DateUtils.isSameDay(value, now)
        ? DateFormat('HH:mm').format(value)
        : DateFormat('M/d HH:mm').format(value);
  }

  bool _hideCompletedIncome(InquirySummary item) {
    final status = item.status.toUpperCase();
    return item.isIncoming && (status == 'COMPLETED' || status == 'ENDED');
  }

  bool _canTip(InquirySummary item) {
    if (item.isIncoming || item.sourceExperienceCertificationId <= 0) {
      return false;
    }
    return const {'COMPLETED', 'ENDED'}.contains(item.status.toUpperCase());
  }

  bool _isEnded(InquirySummary item) {
    return const {'COMPLETED', 'ENDED'}.contains(item.status.toUpperCase());
  }

  Future<void> _openEndedActions(
    BuildContext context,
    WidgetRef ref,
    InquirySummary item,
  ) async {
    final action = await showEndedInquiryActions(context);
    if (action == null || !context.mounted) return;

    if (action == EndedInquiryAction.complaint) {
      await _complain(context, ref, item);
      return;
    }
    await _block(context, ref, item);
  }

  Future<void> _complain(
    BuildContext context,
    WidgetRef ref,
    InquirySummary item,
  ) async {
    try {
      final complained = await ref
          .read(repositoryProvider)
          .hasComplainedAboutInquiry(item.id);
      if (!context.mounted) return;
      if (complained) {
        AppMessage.show(context, '本次询问已提交过投诉');
        return;
      }
    } catch (error) {
      if (context.mounted) AppMessage.show(context, '$error');
      return;
    }

    final complaint = await showInquiryComplaintDialog(context);
    if (complaint == null || !context.mounted) return;
    try {
      await ref
          .read(repositoryProvider)
          .complainAboutInquiry(
            inquiryId: item.id,
            category: complaint.category,
            content: complaint.content,
          );
      if (context.mounted) AppMessage.show(context, '投诉已提交');
    } catch (error) {
      if (context.mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _block(
    BuildContext context,
    WidgetRef ref,
    InquirySummary item,
  ) async {
    final confirmed = await showBlockInquiryConfirmation(
      context,
      item.otherName,
    );
    if (!confirmed || !context.mounted) return;
    try {
      await ref.read(repositoryProvider).blockInquiryUser(item.id);
      if (context.mounted) AppMessage.show(context, '已拉黑对方');
    } catch (error) {
      if (context.mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _tip(BuildContext context, InquirySummary item) async {
    final amount = await showExperienceTipSheet(
      context,
      certificationId: item.sourceExperienceCertificationId,
      experienceTitle: item.topic,
    );
    if (amount != null && context.mounted) {
      AppMessage.show(context, '打赏成功');
    }
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.item});
  final InquirySummary item;
  @override
  Widget build(BuildContext context) {
    final status = item.status.toUpperCase();
    final label = switch (status) {
      'PENDING' => item.isIncoming ? '待你处理' : '等待接受',
      'ACTIVE' => '交流中',
      'TEXT_LIMIT_REACHED' => '文字已结束',
      'COMPLETED' || 'ENDED' => '已结束',
      'REJECTED' => item.isIncoming ? '已拒绝' : '未接受',
      'CANCELLED' => '已撤销',
      'EXPIRED' => '已超时',
      _ => '查看',
    };
    final color = appStatusStyle(context, status).foreground;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 5,
          height: 5,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 5),
        Text(label, style: TextStyle(fontSize: 11, color: color)),
      ],
    );
  }
}
