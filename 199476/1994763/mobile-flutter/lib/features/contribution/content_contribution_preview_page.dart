import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/content_contribution_models.dart';

class ContentContributionPreviewPage extends ConsumerStatefulWidget {
  const ContentContributionPreviewPage({
    required this.matterName,
    required this.jobs,
    super.key,
  });

  final String matterName;
  final List<ContentContributionDraftJob> jobs;

  @override
  ConsumerState<ContentContributionPreviewPage> createState() =>
      _ContentContributionPreviewPageState();
}

class _ContentContributionPreviewPageState
    extends ConsumerState<ContentContributionPreviewPage> {
  bool _submitting = false;

  Future<void> _submit() async {
    if (_submitting) return;
    FocusManager.instance.primaryFocus?.unfocus();
    setState(() => _submitting = true);
    try {
      await ref
          .read(repositoryProvider)
          .submitContentContribution(
            matterName: widget.matterName,
            jobs: widget.jobs,
          );
      if (!mounted) return;
      AppMessage.show(context, '候选内容已提交，等待平台审核');
      context.go('/content-contributions');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('提交预览')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 110),
        children: [
          const _FinalReviewWarning(),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(17),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('事情名称', style: theme.textTheme.bodySmall),
                  const SizedBox(height: 6),
                  Text(widget.matterName, style: theme.textTheme.titleLarge),
                ],
              ),
            ),
          ),
          const SizedBox(height: 18),
          Text(
            '可能参与的岗位（${widget.jobs.length}）',
            style: theme.textTheme.titleLarge,
          ),
          const SizedBox(height: 9),
          ...List.generate(
            widget.jobs.length,
            (index) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      CircleAvatar(
                        radius: 16,
                        backgroundColor: theme.colorScheme.primaryContainer,
                        child: Text('${index + 1}'),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              widget.jobs[index].jobName,
                              style: theme.textTheme.titleMedium,
                            ),
                            const SizedBox(height: 5),
                            Text(widget.jobs[index].responsibility),
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
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(10, 8, 10, 12),
        child: FilledButton(
          onPressed: _submitting ? null : _submit,
          child: const Text('确认内容并提交'),
        ),
      ),
    );
  }
}

class _FinalReviewWarning extends StatelessWidget {
  const _FinalReviewWarning();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final dark = theme.brightness == Brightness.dark;
    final color = dark ? const Color(0xFFE0A24A) : const Color(0xFFB36B18);
    final background = Color.alphaBlend(
      color.withValues(alpha: dark ? .14 : .09),
      theme.colorScheme.surface,
    );

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(15, 13, 15, 14),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.warning_amber_rounded, size: 20, color: color),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '提交后无法修改',
                  style: theme.textTheme.titleSmall?.copyWith(
                    color: color,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '提交后不支持编辑或撤回',
                  style: theme.textTheme.bodySmall?.copyWith(
                    height: 1.5,
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
