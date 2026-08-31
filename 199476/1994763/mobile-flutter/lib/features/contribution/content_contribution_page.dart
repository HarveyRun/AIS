import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/content_contribution_models.dart';

class ContentContributionPage extends ConsumerStatefulWidget {
  const ContentContributionPage({super.key});

  @override
  ConsumerState<ContentContributionPage> createState() =>
      _ContentContributionPageState();
}

class _ContentContributionPageState
    extends ConsumerState<ContentContributionPage> {
  ContentContributionSummary? _summary;
  List<ContentContribution> _items = const [];

  @override
  void initState() {
    super.initState();
    _load();
    ref
        .read(analyticsProvider)
        .track('content_contribution_open', properties: {'source': 'banner'});
  }

  Future<void> _load() async {
    try {
      final results = await Future.wait([
        ref.read(repositoryProvider).contentContributionSummary(),
        ref.read(repositoryProvider).contentContributions(),
      ]);
      if (!mounted) return;
      setState(() {
        _summary = results[0] as ContentContributionSummary;
        _items = (results[1] as ContentContributionPageData).items;
      });
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _create() async {
    final summary = _summary;
    if (summary == null) return;
    if (!summary.canSubmit) {
      AppMessage.show(
        context,
        summary.totalSubmitted >= summary.totalLimit
            ? '您累计已提交${summary.totalLimit}条内容，不能继续提交'
            : '已有${summary.pendingLimit}条内容等待审核，请审核完成后再提交',
      );
      return;
    }
    final created = await context.push<bool>('/content-contributions/new');
    if (created == true) await _load();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('内容共建')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(18, 17, 18, 18),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 5,
                      ),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.primary.withValues(alpha: .08),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        '共建「按事情找人」',
                        style: theme.textTheme.labelMedium?.copyWith(
                          color: theme.colorScheme.primary,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    const SizedBox(height: 13),
                    Text(
                      '帮大家知道，一件事该问谁',
                      style: theme.textTheme.headlineSmall?.copyWith(
                        color: theme.colorScheme.onSurface,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 7),
                    Text(
                      '内容被采用后，会进入「按事情找人」，供所有用户查找。',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                        height: 1.55,
                      ),
                    ),
                    const SizedBox(height: 9),
                    Text(
                      '每采用一条，可获得1～3元内容共建奖励。',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.primary,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 17),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton(
                        onPressed: _summary == null ? null : _create,
                        child: const Text('提交一条候选内容'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 14),
            Card(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(18, 18, 18, 17),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '什么样的事情可以提交',
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 14),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.fromLTRB(15, 14, 15, 14),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.primary.withValues(alpha: .07),
                        borderRadius: BorderRadius.circular(15),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '多数普通人可能遇到',
                            style: theme.textTheme.titleMedium?.copyWith(
                              color: theme.colorScheme.primary,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            '并且，它应当是一件能够说清大致开始和结束的完整事情。',
                            style: theme.textTheme.bodySmall?.copyWith(
                              height: 1.5,
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 18),
                    const Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: _ContributionExample(
                            accepted: true,
                            title: '适合提交',
                            examples: ['房屋装修', '劳动仲裁', '买房', '搬家', '找工作'],
                          ),
                        ),
                        SizedBox(width: 10),
                        Expanded(
                          child: _ContributionExample(
                            accepted: false,
                            title: '不适合提交',
                            examples: [
                              '卫生间漏水怎么办',
                              '公司拖欠工资怎么办',
                              '怎么选装修公司',
                              '装修报价是否合理',
                              '造航母',
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 14),
            if (_summary != null) _QuotaCard(summary: _summary!),
            const SizedBox(height: 22),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('我的共建', style: theme.textTheme.titleLarge),
                if (_items.isNotEmpty)
                  Text(
                    '${_summary?.totalSubmitted ?? _items.length}条',
                    style: theme.textTheme.bodySmall,
                  ),
              ],
            ),
            const SizedBox(height: 10),
            if (_items.isEmpty)
              const _EmptyContribution()
            else
              ..._items.map(
                (item) => Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: _ContributionCard(
                    item: item,
                    onTap: () =>
                        context.push('/content-contributions/${item.id}'),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _ContributionExample extends StatelessWidget {
  const _ContributionExample({
    required this.accepted,
    required this.title,
    required this.examples,
  });

  final bool accepted;
  final String title;
  final List<String> examples;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = accepted ? const Color(0xFF287A54) : const Color(0xFF8A6554);
    final background = accepted
        ? const Color(0xFFF1F7F3)
        : const Color(0xFFF8F3F0);

    return Container(
      padding: const EdgeInsets.fromLTRB(13, 13, 12, 12),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(15),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: theme.textTheme.labelLarge?.copyWith(
              color: color,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 9),
          ...examples.map(
            (example) => Padding(
              padding: const EdgeInsets.only(bottom: 5),
              child: Text(
                example,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurface,
                ),
              ),
            ),
          ),
          Text(
            accepted ? '完整事项' : '具体问题或少见事项',
            style: theme.textTheme.labelSmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

class _QuotaCard extends StatelessWidget {
  const _QuotaCard({required this.summary});

  final ContentContributionSummary summary;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 16, 14, 14),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: _QuotaItem(
                    label: '已提交',
                    value: '${summary.totalSubmitted}',
                  ),
                ),
                Expanded(
                  child: _QuotaItem(
                    label: '已被采用',
                    value: '${summary.adoptedCount}',
                    highlighted: true,
                  ),
                ),
                Expanded(
                  child: _QuotaItem(
                    label: '共获奖励',
                    value: '¥${_formatAmount(summary.earnedReward)}',
                    highlighted: true,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              '最多提交${summary.totalLimit}条，同时最多有${summary.pendingLimit}条等待审核',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _QuotaItem extends StatelessWidget {
  const _QuotaItem({
    required this.label,
    required this.value,
    this.highlighted = false,
  });

  final String label;
  final String value;
  final bool highlighted;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      children: [
        Text(
          value,
          style: theme.textTheme.titleMedium?.copyWith(
            color: highlighted ? theme.colorScheme.primary : null,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 3),
        Text(label, style: theme.textTheme.bodySmall),
      ],
    );
  }
}

class _ContributionCard extends StatelessWidget {
  const _ContributionCard({required this.item, required this.onTap});

  final ContentContribution item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final status = _ContributionStatus.of(item.status);
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.matterName, style: theme.textTheme.titleMedium),
                    const SizedBox(height: 7),
                    Text(
                      '${item.jobs.length}个岗位${item.createdAt == null ? '' : ' · ${DateFormat('yyyy-MM-dd').format(item.createdAt!)}'}',
                      style: theme.textTheme.bodySmall,
                    ),
                    if (item.rewardAmount != null) ...[
                      const SizedBox(height: 8),
                      Text(
                        '已获得${_formatAmount(item.rewardAmount!)}元奖励',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: const Color(0xFF27845A),
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
                decoration: BoxDecoration(
                  color: status.color.withValues(alpha: .11),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  status.label,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: status.color,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _EmptyContribution extends StatelessWidget {
  const _EmptyContribution();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 34),
      child: Column(
        children: [
          Icon(
            Icons.article_outlined,
            size: 34,
            color: Theme.of(context).colorScheme.outline,
          ),
          const SizedBox(height: 9),
          Text('还没有提交过共建内容', style: Theme.of(context).textTheme.bodyMedium),
        ],
      ),
    );
  }
}

class _ContributionStatus {
  const _ContributionStatus(this.label, this.color);

  final String label;
  final Color color;

  static _ContributionStatus of(String value) {
    return switch (value) {
      'ADOPTED' => const _ContributionStatus('已采用', Color(0xFF27845A)),
      'REJECTED' => const _ContributionStatus('未采用', Color(0xFF777B82)),
      'VIOLATION_REJECTED' => const _ContributionStatus(
        '违规驳回',
        Color(0xFFC33D32),
      ),
      _ => const _ContributionStatus('待审核', Color(0xFFC67A12)),
    };
  }
}

String _formatAmount(double value) {
  return value == value.roundToDouble()
      ? value.toInt().toString()
      : value.toStringAsFixed(2);
}
