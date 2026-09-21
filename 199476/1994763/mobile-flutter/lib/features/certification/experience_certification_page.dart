import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';

class ExperienceCertificationPage extends ConsumerStatefulWidget {
  const ExperienceCertificationPage({super.key});
  @override
  ConsumerState<ExperienceCertificationPage> createState() =>
      _ExperienceCertificationPageState();
}

class _ExperienceCertificationPageState
    extends ConsumerState<ExperienceCertificationPage> {
  List<CertificationRecord> _items = const [];
  bool _loading = true;

  bool get _hasApprovedExperience =>
      _items.any((item) => item.status.toUpperCase() == 'APPROVED');

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final repository = ref.read(repositoryProvider);
      final items = await repository.certifications();
      if (mounted) {
        setState(() {
          _items =
              items
                  .where(
                    (item) =>
                        item.category == 'EXPERIENCE' ||
                        item.type == 'EXPERIENCE',
                  )
                  .toList()
                ..sort((left, right) {
                  final leftTime = left.lastOperatedAt;
                  final rightTime = right.lastOperatedAt;
                  if (leftTime == null && rightTime == null) {
                    return right.id.compareTo(left.id);
                  }
                  if (leftTime == null) return 1;
                  if (rightTime == null) return -1;
                  final timeComparison = rightTime.compareTo(leftTime);
                  return timeComparison == 0
                      ? right.id.compareTo(left.id)
                      : timeComparison;
                });
        });
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _add() async {
    try {
      final repository = ref.read(repositoryProvider);
      final records = await repository.certifications();
      final unapprovedExperiences = records.where((record) {
        final experience =
            record.category == 'EXPERIENCE' || record.type == 'EXPERIENCE';
        return experience && record.status.toUpperCase() != 'APPROVED';
      }).length;
      if (unapprovedExperiences >= 3) {
        if (mounted) {
          AppMessage.show(context, '添加已达上限，请等待审核完成后再添加');
        }
        return;
      }
      if (!mounted) return;
      await context.push('/profile/certifications/experiences/new');
      await _load();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _deleteExperience(CertificationRecord item) async {
    if (item.status.toUpperCase() != 'REJECTED') return;
    final confirmed = await showModalBottomSheet<bool>(
      context: context,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final colors = Theme.of(sheetContext).colorScheme;
        return Container(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
          decoration: BoxDecoration(
            color: colors.surface,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 36,
                  height: 4,
                  decoration: BoxDecoration(
                    color: colors.outlineVariant,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Text(
                '删除这段经历？',
                style: Theme.of(
                  sheetContext,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 8),
              Text(
                '删除后将不再显示，且无法恢复。',
                style: Theme.of(sheetContext).textTheme.bodyMedium?.copyWith(
                  color: colors.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 22),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => Navigator.pop(sheetContext, false),
                      child: const Text('取消'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton(
                      style: FilledButton.styleFrom(
                        backgroundColor: colors.error,
                        foregroundColor: colors.onError,
                      ),
                      onPressed: () => Navigator.pop(sheetContext, true),
                      child: const Text('确认删除'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
    if (confirmed != true || !mounted) return;

    try {
      await ref.read(repositoryProvider).deleteExperience(item.id);
      if (!mounted) return;
      AppMessage.show(context, '已删除');
      await _load();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _openExperience(CertificationRecord item) async {
    await context.push('/profile/certifications/experiences/${item.id}');
    await _load();
  }

  Future<void> _showReviewScores(CertificationRecord item) {
    final scores = <({String label, int? value})>[
      (label: '证明材料完整度', value: item.materialSupportScore),
      (label: '普通人发生概率', value: item.commonRelevanceScore),
      (label: '普通人可复制性', value: item.learnabilityScore),
      (label: '表述清晰顺畅', value: item.clarityScore),
      (label: '表述逻辑自洽', value: item.logicConsistencyScore),
    ];
    return showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        insetPadding: const EdgeInsets.symmetric(horizontal: 30, vertical: 24),
        titlePadding: const EdgeInsets.fromLTRB(22, 18, 10, 0),
        contentPadding: const EdgeInsets.fromLTRB(22, 14, 22, 24),
        title: Row(
          children: [
            Expanded(
              child: Text(
                '参考指数评分',
                style: Theme.of(
                  dialogContext,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
              ),
            ),
            IconButton(
              tooltip: '关闭',
              onPressed: () => Navigator.pop(dialogContext),
              icon: const Icon(Icons.close_rounded),
            ),
          ],
        ),
        content: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              for (var index = 0; index < scores.length; index++) ...[
                _ReviewScoreRow(
                  label: scores[index].label,
                  score: scores[index].value == null
                      ? null
                      : scores[index].value!.clamp(0, 10) * 10,
                ),
                if (index != scores.length - 1) const SizedBox(height: 14),
              ],
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('我的经历'),
      actions: [
        Container(
          height: 40,
          margin: const EdgeInsets.only(right: 10),
          padding: const EdgeInsets.symmetric(horizontal: 2),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surfaceContainerLow,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              IconButton(
                onPressed: _add,
                icon: const Icon(Icons.add_rounded, size: 26),
                tooltip: '添加经历',
                visualDensity: VisualDensity.compact,
              ),
              if (_hasApprovedExperience)
                IconButton(
                  onPressed: () => context.push('/profile/inquiry-settings'),
                  icon: const Icon(Icons.settings_outlined, size: 22),
                  tooltip: '询问设置',
                  visualDensity: VisualDensity.compact,
                ),
            ],
          ),
        ),
      ],
    ),
    body: _loading
        ? const SizedBox.shrink()
        : _buildExperienceList(_items, emptyText: '还没有经历'),
  );

  Widget _buildExperienceList(
    List<CertificationRecord> items, {
    required String emptyText,
  }) {
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
        children: [
          if (items.isEmpty) ...[
            const SizedBox(height: 86),
            const Icon(
              Icons.route_outlined,
              size: 32,
              color: Color(0xFF9A9A9A),
            ),
            const SizedBox(height: 12),
            Center(child: Text(emptyText)),
            const SizedBox(height: 18),
            Center(
              child: FilledButton.tonalIcon(
                onPressed: _add,
                icon: const Icon(Icons.add_rounded),
                label: const Text('添加经历'),
              ),
            ),
          ] else
            ...items.indexed.map((entry) {
              final index = entry.$1;
              final item = entry.$2;
              final statusStyle = appStatusStyle(context, item.status);
              return Padding(
                padding: EdgeInsets.only(
                  bottom: index == items.length - 1 ? 0 : 10,
                ),
                child: Material(
                  color: Theme.of(context).colorScheme.surface,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: Column(
                    children: [
                      ListTile(
                        contentPadding: const EdgeInsets.fromLTRB(
                          16,
                          10,
                          10,
                          2,
                        ),
                        title: Text(item.title),
                        subtitle: Text(
                          item.description,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        trailing: const Icon(Icons.chevron_right_rounded),
                        onTap: () => _openExperience(item),
                      ),
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 0, 10, 8),
                        child: Row(
                          children: [
                            if (item.status.toUpperCase() == 'APPROVED')
                              TextButton(
                                onPressed: () => _showReviewScores(item),
                                style: TextButton.styleFrom(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 6,
                                    vertical: 6,
                                  ),
                                  minimumSize: Size.zero,
                                  tapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                ),
                                child: const Text('查看评分'),
                              ),
                            const Spacer(),
                            Text(
                              _status(item),
                              style: Theme.of(context).textTheme.bodySmall
                                  ?.copyWith(
                                    color: statusStyle.foreground,
                                    fontWeight: FontWeight.w700,
                                  ),
                            ),
                            if (item.status.toUpperCase() == 'REJECTED') ...[
                              const SizedBox(width: 18),
                              TextButton.icon(
                                onPressed: () => _deleteExperience(item),
                                style: TextButton.styleFrom(
                                  foregroundColor: Theme.of(
                                    context,
                                  ).colorScheme.error,
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 6,
                                    vertical: 6,
                                  ),
                                  minimumSize: Size.zero,
                                  tapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                ),
                                icon: const Icon(
                                  Icons.delete_outline_rounded,
                                  size: 18,
                                ),
                                label: const Text('删除'),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }),
        ],
      ),
    );
  }

  String _status(CertificationRecord item) =>
      switch (item.status.toUpperCase()) {
        'PENDING' => '审核中',
        'APPROVED' => '已通过审核',
        'REJECTED' => '已被驳回',
        _ => item.status,
      };
}

class _ReviewScoreRow extends StatelessWidget {
  const _ReviewScoreRow({required this.label, required this.score});

  final String label;
  final int? score;

  Color _scoreColor(BuildContext context) {
    final value = score;
    if (value == null) return Theme.of(context).colorScheme.onSurfaceVariant;
    if (value < 40) return const Color(0xFFD84A43);
    if (value < 70) return const Color(0xFFD39420);
    return const Color(0xFF2E9461);
  }

  @override
  Widget build(BuildContext context) {
    final color = _scoreColor(context);
    final value = score;
    return Row(
      children: [
        Expanded(
          child: Text(label, style: Theme.of(context).textTheme.bodyMedium),
        ),
        Text(
          value == null ? '暂未评分' : '$value分',
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: color,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }
}
