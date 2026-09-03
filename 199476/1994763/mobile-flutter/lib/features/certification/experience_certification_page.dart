import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';
import 'experience_form_page.dart';

class ExperienceCertificationPage extends ConsumerStatefulWidget {
  const ExperienceCertificationPage({super.key});
  @override
  ConsumerState<ExperienceCertificationPage> createState() =>
      _ExperienceCertificationPageState();
}

class _ExperienceCertificationPageState
    extends ConsumerState<ExperienceCertificationPage>
    with SingleTickerProviderStateMixin {
  late final TabController _tabs;
  List<CertificationRecord> _items = const [];
  bool _loading = true;
  bool _identityApproved = false;
  @override
  void initState() {
    super.initState();
    _tabs = TabController(length: 2, vsync: this);
    _load();
  }

  @override
  void dispose() {
    _tabs.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final repository = ref.read(repositoryProvider);
      final items = await repository.certifications();
      if (mounted) {
        setState(() {
          _identityApproved = items.any(
            (item) => item.type == 'IDENTITY' && item.approved && item.enabled,
          );
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
      final businessType = await _selectBusinessType();
      if (businessType == null || !mounted) return;
      if (businessType == ExperienceBusinessType.publicWelfare) {
        await context.push(
          '/profile/certifications/experiences/public-welfare/new',
        );
        await _load();
        return;
      }
      final identityApproved = records.any(
        (record) =>
            record.type == 'IDENTITY' &&
            record.status.toUpperCase() == 'APPROVED' &&
            record.enabled,
      );
      if (!identityApproved) {
        if (!mounted) return;
        final goToIdentity = await _showIdentityRequiredDialog();
        if (goToIdentity == true && mounted) {
          CertificationRecord? identityRecord;
          for (final record in records) {
            if (record.type == 'IDENTITY') {
              identityRecord = record;
              break;
            }
          }
          if (!mounted) return;
          await context.push(
            '/profile/certifications/basic/IDENTITY/apply',
            extra: identityRecord,
          );
          await _load();
        }
        return;
      }
      await context.push('/profile/certifications/experiences/monetized/new');
      await _load();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<ExperienceBusinessType?> _selectBusinessType() =>
      showModalBottomSheet<ExperienceBusinessType>(
        context: context,
        showDragHandle: true,
        builder: (context) => SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 18),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('发布经历', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 14),
                _BusinessTypeOption(
                  icon: Icons.volunteer_activism_rounded,
                  title: '公益分享',
                  subtitle: '无需实名认证、审核简单，但无法变现',
                  color: const Color(0xFF27855A),
                  onTap: () => Navigator.pop(
                    context,
                    ExperienceBusinessType.publicWelfare,
                  ),
                ),
                const SizedBox(height: 10),
                _BusinessTypeOption(
                  icon: Icons.workspace_premium_rounded,
                  title: '干货变现',
                  subtitle: '本人真实经历、审核严格，可设定变现金额',
                  color: Theme.of(context).colorScheme.primary,
                  onTap: () =>
                      Navigator.pop(context, ExperienceBusinessType.monetized),
                ),
              ],
            ),
          ),
        ),
      );

  Future<void> _upgrade(CertificationRecord item) async {
    try {
      final records = await ref.read(repositoryProvider).certifications();
      final identityApproved = records.any(
        (record) =>
            record.type == 'IDENTITY' && record.approved && record.enabled,
      );
      if (!identityApproved) {
        if (!mounted) return;
        final goToIdentity = await _showIdentityRequiredDialog();
        if (goToIdentity == true && mounted) {
          final identityRecord = records
              .where((record) => record.type == 'IDENTITY')
              .firstOrNull;
          await context.push(
            '/profile/certifications/basic/IDENTITY/apply',
            extra: identityRecord,
          );
          await _load();
        }
        return;
      }
      if (!mounted) return;
      await context.push(
        '/profile/certifications/experiences/monetized/new?upgradeSourceId=${item.id}',
      );
      await _load();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<bool?> _showIdentityRequiredDialog() => showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text(
        '需实名认证',
        style: TextStyle(
          color: Colors.black,
          fontWeight: FontWeight.w700,
          fontSize: 22,
        ),
      ),
      content: const Text('需先完成实名认证，且年龄须满 25 周岁。'),
      actions: [
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('去实名认证'),
          ),
        ),
      ],
    ),
  );

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
              if (_identityApproved)
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
            tabs: const [
              Tab(text: '干货变现'),
              Tab(text: '公益分享'),
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
              _buildExperienceList(
                _items.where((item) => item.isMonetized).toList(),
                emptyText: '还没有干货变现经历',
              ),
              _buildExperienceList(
                _items.where((item) => item.isPublicWelfare).toList(),
                emptyText: '还没有公益分享经历',
              ),
            ],
          ),
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
              final canUpgrade =
                  item.isPublicWelfare &&
                  item.approved &&
                  !_items.any(
                    (candidate) => candidate.upgradeSourceId == item.id,
                  );
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
                  child: ListTile(
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 10,
                    ),
                    leading: Container(
                      width: 42,
                      height: 42,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: Theme.of(
                          context,
                        ).colorScheme.surfaceContainerHighest,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(
                        Icons.route_outlined,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                    ),
                    title: Text(item.title),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          item.description.isEmpty ? '视频详述' : item.description,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (canUpgrade) ...[
                          const SizedBox(height: 4),
                          TextButton(
                            onPressed: () => _upgrade(item),
                            style: TextButton.styleFrom(
                              padding: EdgeInsets.zero,
                              minimumSize: const Size(0, 32),
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                            ),
                            child: const Text('升级为干货变现'),
                          ),
                        ],
                      ],
                    ),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          _status(item),
                          style: Theme.of(context).textTheme.bodySmall
                              ?.copyWith(
                                color: statusStyle.foreground,
                                fontWeight: FontWeight.w700,
                              ),
                        ),
                        const Icon(Icons.chevron_right_rounded),
                      ],
                    ),
                    onTap: () async {
                      await context.push(
                        item.isPublicWelfare
                            ? '/profile/certifications/experiences/public-welfare/${item.id}'
                            : '/profile/certifications/experiences/monetized/${item.id}',
                      );
                      await _load();
                    },
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
        'APPROVED' => item.isPublicWelfare ? '已发布' : '已认证',
        'REJECTED' => '退回修改',
        _ => item.status,
      };
}

class _BusinessTypeOption extends StatelessWidget {
  const _BusinessTypeOption({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.color,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Material(
    color: Color.alphaBlend(
      color.withValues(alpha: .07),
      Theme.of(context).colorScheme.surface,
    ),
    borderRadius: BorderRadius.circular(16),
    clipBehavior: Clip.antiAlias,
    child: InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(icon, color: color, size: 26),
            const SizedBox(width: 13),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
                ],
              ),
            ),
            const Icon(Icons.chevron_right_rounded),
          ],
        ),
      ),
    ),
  );
}
