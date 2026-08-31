import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/content_contribution_models.dart';
import 'content_contribution_preview_page.dart';

class ContentContributionFormPage extends ConsumerStatefulWidget {
  const ContentContributionFormPage({super.key});

  @override
  ConsumerState<ContentContributionFormPage> createState() =>
      _ContentContributionFormPageState();
}

class _ContentContributionFormPageState
    extends ConsumerState<ContentContributionFormPage> {
  final _matter = TextEditingController();
  final List<_JobEditor> _jobs = [_JobEditor()];

  @override
  void dispose() {
    _matter.dispose();
    for (final job in _jobs) {
      job.dispose();
    }
    super.dispose();
  }

  void _addJob() {
    if (_jobs.length >= 10) {
      AppMessage.show(context, '每条内容最多填写10个岗位');
      return;
    }
    setState(() => _jobs.add(_JobEditor()));
  }

  void _removeJob(int index) {
    if (_jobs.length == 1) {
      AppMessage.show(context, '至少需要填写1个岗位');
      return;
    }
    final removed = _jobs.removeAt(index);
    removed.dispose();
    setState(() {});
  }

  List<ContentContributionDraftJob>? _validate() {
    final matter = _matter.text.trim();
    if (matter.length < 2 || matter.length > 10) {
      AppMessage.show(context, '请填写2～10个字的事情名称');
      return null;
    }
    final result = <ContentContributionDraftJob>[];
    final names = <String>{};
    for (var index = 0; index < _jobs.length; index++) {
      final name = _jobs[index].name.text.trim();
      final responsibility = _jobs[index].responsibility.text.trim();
      if (name.length < 2) {
        AppMessage.show(context, '请填写第${index + 1}个岗位名称');
        return null;
      }
      if (responsibility.length < 5) {
        AppMessage.show(context, '请具体说明第${index + 1}个岗位主要负责什么');
        return null;
      }
      final key = name.replaceAll(RegExp(r'\s+'), '').toLowerCase();
      if (!names.add(key)) {
        AppMessage.show(context, '同一条内容中不能重复填写相同岗位');
        return null;
      }
      result.add(
        ContentContributionDraftJob(
          jobName: name,
          responsibility: responsibility,
        ),
      );
    }
    return result;
  }

  Future<void> _preview() async {
    final jobs = _validate();
    if (jobs == null) return;
    FocusManager.instance.primaryFocus?.unfocus();
    await ref
        .read(analyticsProvider)
        .track(
          'content_contribution_preview',
          properties: {'job_count': jobs.length},
        );
    if (!mounted) return;
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => ContentContributionPreviewPage(
          matterName: _matter.text.trim(),
          jobs: jobs,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('提交候选内容')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 110),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('事情名称', style: theme.textTheme.titleMedium),
                  const SizedBox(height: 5),
                  Text('一件多数普通人会遇到的事情', style: theme.textTheme.bodySmall),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _matter,
                    maxLength: 10,
                    inputFormatters: [LengthLimitingTextInputFormatter(10)],
                    textInputAction: TextInputAction.next,
                    decoration: const InputDecoration(
                      hintText: '请输入',
                      counterText: '',
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 18),
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('可能参与的岗位', style: theme.textTheme.titleLarge),
                    const SizedBox(height: 3),
                    Text('至少1个，最多10个', style: theme.textTheme.bodySmall),
                  ],
                ),
              ),
              TextButton.icon(
                onPressed: _jobs.length >= 10 ? null : _addJob,
                icon: const Icon(Icons.add_rounded),
                label: const Text('添加岗位'),
              ),
            ],
          ),
          const SizedBox(height: 8),
          ...List.generate(
            _jobs.length,
            (index) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _JobCard(
                index: index,
                editor: _jobs[index],
                canRemove: _jobs.length > 1,
                onRemove: () => _removeJob(index),
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(10, 8, 10, 12),
        child: FilledButton(onPressed: _preview, child: const Text('预览并确认')),
      ),
    );
  }
}

class _JobCard extends StatelessWidget {
  const _JobCard({
    required this.index,
    required this.editor,
    required this.canRemove,
    required this.onRemove,
  });

  final int index;
  final _JobEditor editor;
  final bool canRemove;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    '岗位 ${index + 1}',
                    style: theme.textTheme.titleMedium,
                  ),
                ),
                if (canRemove)
                  IconButton(
                    tooltip: '删除岗位',
                    onPressed: onRemove,
                    icon: const Icon(Icons.delete_outline_rounded),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            TextField(
              controller: editor.name,
              maxLength: 40,
              inputFormatters: [LengthLimitingTextInputFormatter(40)],
              textInputAction: TextInputAction.next,
              decoration: const InputDecoration(
                hintText: '岗位名称，例如：水暖工',
                counterText: '',
              ),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: editor.responsibility,
              minLines: 3,
              maxLines: 5,
              maxLength: 300,
              inputFormatters: [LengthLimitingTextInputFormatter(300)],
              decoration: const InputDecoration(
                hintText: '这个岗位在这件事中主要负责什么',
                counterText: '',
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _JobEditor {
  final name = TextEditingController();
  final responsibility = TextEditingController();

  void dispose() {
    name.dispose();
    responsibility.dispose();
  }
}
