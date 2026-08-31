import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/content_contribution_models.dart';

class ContentContributionDetailPage extends ConsumerStatefulWidget {
  const ContentContributionDetailPage({required this.id, super.key});

  final int id;

  @override
  ConsumerState<ContentContributionDetailPage> createState() =>
      _ContentContributionDetailPageState();
}

class _ContentContributionDetailPageState
    extends ConsumerState<ContentContributionDetailPage> {
  ContentContribution? _item;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final item = await ref
          .read(repositoryProvider)
          .contentContribution(widget.id);
      if (mounted) setState(() => _item = item);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  Widget build(BuildContext context) {
    final item = _item;
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('共建内容详情')),
      body: item == null
          ? const SizedBox.shrink()
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
                children: [
                  _ResultHeader(item: item),
                  const SizedBox(height: 14),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(17),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('事情名称', style: theme.textTheme.bodySmall),
                          const SizedBox(height: 6),
                          Text(
                            item.matterName,
                            style: theme.textTheme.titleLarge,
                          ),
                          if (item.createdAt != null) ...[
                            const SizedBox(height: 8),
                            Text(
                              '提交于 ${DateFormat('yyyy-MM-dd HH:mm').format(item.createdAt!)}',
                              style: theme.textTheme.bodySmall,
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    '填写的岗位（${item.jobs.length}）',
                    style: theme.textTheme.titleLarge,
                  ),
                  const SizedBox(height: 9),
                  ...List.generate(
                    item.jobs.length,
                    (index) => Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '${index + 1}'.padLeft(2, '0'),
                                style: theme.textTheme.labelMedium?.copyWith(
                                  color: theme.colorScheme.primary,
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      item.jobs[index].jobName,
                                      style: theme.textTheme.titleMedium,
                                    ),
                                    const SizedBox(height: 5),
                                    Text(item.jobs[index].responsibility),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}

class _ResultHeader extends StatelessWidget {
  const _ResultHeader({required this.item});

  final ContentContribution item;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final presentation = _presentation(item.status);
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: presentation.color.withValues(alpha: .1),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(presentation.icon, color: presentation.color),
              const SizedBox(width: 9),
              Text(
                presentation.label,
                style: theme.textTheme.titleLarge?.copyWith(
                  color: presentation.color,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
          if (item.isAdopted) ...[
            const SizedBox(height: 11),
            Text(
              '本次获得 ${_formatAmount(item.rewardAmount ?? 0)} 元内容共建奖励，已计入可提现收入。',
              style: theme.textTheme.bodyMedium,
            ),
          ] else if (item.reviewReason.isNotEmpty) ...[
            const SizedBox(height: 11),
            Text(item.reviewReason, style: theme.textTheme.bodyMedium),
          ] else if (item.isPending) ...[
            const SizedBox(height: 11),
            Text(
              '不支持编辑或撤回',
              style: theme.textTheme.bodyMedium,
            ),
          ],
          if (item.isAdopted && item.standardMatterName.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              '对应标准事项：${item.standardMatterName}',
              style: theme.textTheme.bodySmall,
            ),
          ],
        ],
      ),
    );
  }
}

({String label, Color color, IconData icon}) _presentation(String status) {
  return switch (status) {
    'ADOPTED' => (
      label: '内容已采用',
      color: const Color(0xFF27845A),
      icon: Icons.check_circle_rounded,
    ),
    'REJECTED' => (
      label: '本次未采用',
      color: const Color(0xFF777B82),
      icon: Icons.info_rounded,
    ),
    'VIOLATION_REJECTED' => (
      label: '违规驳回',
      color: const Color(0xFFC33D32),
      icon: Icons.gpp_bad_rounded,
    ),
    _ => (
      label: '等待审核',
      color: const Color(0xFFC67A12),
      icon: Icons.schedule_rounded,
    ),
  };
}

String _formatAmount(double value) {
  return value == value.roundToDouble()
      ? value.toInt().toString()
      : value.toStringAsFixed(2);
}
