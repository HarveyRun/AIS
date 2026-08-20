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
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final items = await ref.read(repositoryProvider).certifications();
      if (mounted) {
        setState(
          () => _items = items
              .where(
                (item) =>
                    item.category == 'EXPERIENCE' || item.type == 'EXPERIENCE',
              )
              .toList(),
        );
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _add() async {
    try {
      final eligibility = await ref
          .read(repositoryProvider)
          .answererEligibility();
      final eligible = eligibility['identityApproved'] == true;
      if (!eligible) {
        if (mounted) {
          AppMessage.show(context, '完成实名认证后才能添加亲身经历');
        }
        return;
      }
      if (!mounted) return;
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (context) => const _ExperienceNoticeDialog(),
      );
      if (confirmed != true || !mounted) return;
      await context.push('/profile/certifications/experiences/new');
      await _load();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('亲身经历'),
      actions: [
        IconButton(
          onPressed: _add,
          icon: const Icon(Icons.add_rounded),
          tooltip: '添加经历',
        ),
      ],
    ),
    body: _loading
        ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
        : RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
              children: [
                if (_items.isEmpty) ...[
                  const SizedBox(height: 86),
                  const Icon(
                    Icons.route_outlined,
                    size: 32,
                    color: Color(0xFF9A9A9A),
                  ),
                  const SizedBox(height: 12),
                  const Center(child: Text('还没有亲身经历')),
                  const SizedBox(height: 18),
                  Center(
                    child: FilledButton.tonalIcon(
                      onPressed: _add,
                      icon: const Icon(Icons.add_rounded),
                      label: const Text('添加经历'),
                    ),
                  ),
                ] else
                  ..._items.indexed.map((entry) {
                    final index = entry.$1;
                    final item = entry.$2;
                    final statusStyle = appStatusStyle(context, item.status);
                    return Padding(
                      padding: EdgeInsets.only(
                        bottom: index == _items.length - 1 ? 0 : 10,
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
                          subtitle: Text(
                            item.description,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
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
                              '/profile/certifications/experiences/${item.id}',
                            );
                            await _load();
                          },
                        ),
                      ),
                    );
                  }),
              ],
            ),
          ),
  );
  String _status(CertificationRecord item) =>
      switch (item.status.toUpperCase()) {
        'PENDING' => '审核中',
        'APPROVED' => '已认证',
        'REJECTED' => '退回修改',
        _ => item.status,
      };
}

class _ExperienceNoticeDialog extends StatelessWidget {
  const _ExperienceNoticeDialog();

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final scheme = Theme.of(context).colorScheme;
    final warning = dark ? const Color(0xFFE0A24A) : const Color(0xFFB36B18);
    final warningSurface = Color.alphaBlend(
      warning.withValues(alpha: dark ? .14 : .08),
      scheme.surface,
    );

    return AlertDialog(
      backgroundColor: warningSurface,
      titlePadding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
      contentPadding: const EdgeInsets.fromLTRB(20, 16, 20, 6),
      actionsPadding: const EdgeInsets.fromLTRB(20, 10, 20, 18),
      title: Row(
        children: [
          Expanded(
            child: Text(
              '温馨提示',
              style: TextStyle(color: warning, fontWeight: FontWeight.w700),
            ),
          ),
          IconButton(
            onPressed: () => Navigator.pop(context, false),
            tooltip: '关闭',
            visualDensity: VisualDensity.compact,
            icon: Icon(Icons.close_rounded, color: warning),
          ),
        ],
      ),
      content: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * .55,
        ),
        child: SingleChildScrollView(child: const _ExperienceNoticeContent()),
      ),
      actions: [
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            style: FilledButton.styleFrom(backgroundColor: warning),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('继续添加'),
          ),
        ),
      ],
    );
  }
}

class _ExperienceNoticeContent extends StatelessWidget {
  const _ExperienceNoticeContent();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final normal = TextStyle(
      color: scheme.onSurfaceVariant,
      fontSize: 11,
      height: 1.65,
    );
    final strong = normal.copyWith(
      color: scheme.onSurface,
      fontWeight: FontWeight.w700,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text.rich(
          TextSpan(
            style: normal,
            children: [
              const TextSpan(text: '平台'),
              TextSpan(text: '不强制要求提交能够完整证明经历真实性的材料', style: strong),
              const TextSpan(text: '，但必须提供能够证明'),
              TextSpan(text: '该段经历与您本人直接相关的材料或信息。', style: strong),
            ],
          ),
        ),
        const SizedBox(height: 11),
        Text.rich(
          TextSpan(
            style: normal,
            children: [
              const TextSpan(text: '平台会对内容进行基础审核，包括'),
              TextSpan(text: '时间、人物、事件经过及前后逻辑是否合理', style: strong),
              const TextSpan(text: '，对于明显矛盾、不符合常识或疑似虚构的内容，可能无法通过审核。'),
            ],
          ),
        ),
        const SizedBox(height: 11),
        Text.rich(
          TextSpan(
            style: normal,
            children: [
              const TextSpan(text: '请务必以'),
              TextSpan(text: '您本人亲身经历、亲自参与或亲自面对的事情', style: strong),
              const TextSpan(
                text: '为核心进行分享。家人、亲戚、朋友的经历，以及听说、转述、道听途说的内容，均不作为有效经历采纳。',
              ),
            ],
          ),
        ),
        const SizedBox(height: 11),
        Text('不要求完整证明，但必须是真实发生在您本人身上的经历。', style: strong),
      ],
    );
  }
}
