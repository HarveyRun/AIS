import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/widgets/answerer_card.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/answerer_models.dart';
import '../../data/models/discovery_models.dart';

class DiscoveryResultsPage extends ConsumerStatefulWidget {
  const DiscoveryResultsPage({
    super.key,
    required this.type,
    required this.id,
    required this.title,
  });
  final String type;
  final int id;
  final String title;

  @override
  ConsumerState<DiscoveryResultsPage> createState() =>
      _DiscoveryResultsPageState();
}

class _DiscoveryResultsPageState extends ConsumerState<DiscoveryResultsPage> {
  bool _loading = true;
  List<Answerer> _people = const [];
  DiscoveryMatter? _matter;
  String _selectedJob = '';

  bool get _experience => widget.type == 'experiences';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final repository = ref.read(repositoryProvider);
      if (_experience) {
        _people = await repository.answerersByExperience(widget.id);
      } else {
        final results = await Future.wait([
          repository.discoveryMatter(widget.id),
          repository.answerersByMatter(widget.id),
        ]);
        _matter = results[0] as DiscoveryMatter;
        _people = results[1] as List<Answerer>;
      }
      if (mounted) {
        setState(() {});
        unawaited(
          ref
              .read(analyticsProvider)
              .track(
                _people.isEmpty ? 'people_result_empty' : 'people_result_view',
                properties: {
                  'content_id': widget.id,
                  'content_name': _matter?.title ?? widget.title,
                  'content_type': _experience ? 'EXPERIENCE' : 'MATTER',
                  'result_count': _people.length,
                },
              ),
        );
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _showJobDescriptions() async {
    final jobs = _matter?.jobs ?? const <DiscoveryJob>[];
    if (jobs.isEmpty) return;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (context) => _JobDescriptionsSheet(jobs: jobs),
    );
  }

  @override
  Widget build(BuildContext context) {
    final people = _selectedJob.isEmpty
        ? _people
        : _people.where((person) => person.mainJob == _selectedJob).toList();
    return Scaffold(
      appBar: AppBar(title: const Text('找人')),
      body: _loading
          ? const SizedBox.shrink()
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.only(bottom: 32),
                children: [
                  Container(
                    margin: const EdgeInsets.fromLTRB(10, 8, 10, 0),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surface,
                      borderRadius: BorderRadius.circular(18),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _experience ? '你想了解' : '你想问',
                            style: TextStyle(
                              color: Theme.of(context).colorScheme.primary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(height: 5),
                          Text(
                            _matter?.title ?? widget.title,
                            style: Theme.of(context).textTheme.titleLarge,
                          ),
                          const SizedBox(height: 5),
                          Text(
                            _experience
                                ? '这些人提交过相关经历证明，可以找他们聊聊。'
                                : '这些人的岗位与这件事有关，可以找他们聊聊。',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (!_experience && (_matter?.jobs.isNotEmpty ?? false)) ...[
                    const SizedBox(height: 18),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 10),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              '可能会问到',
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                          ),
                          TextButton(
                            onPressed: _showJobDescriptions,
                            style: TextButton.styleFrom(
                              minimumSize: const Size(0, 32),
                              padding: const EdgeInsets.symmetric(
                                horizontal: 2,
                                vertical: 4,
                              ),
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                            ),
                            child: const Text('岗位说明'),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 8),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 10),
                      child: Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: _matter!.jobs
                            .map(
                              (job) => FilterChip(
                                label: Text(job.name),
                                selected: _selectedJob == job.name,
                                showCheckmark: false,
                                side: BorderSide.none,
                                selectedColor: Theme.of(
                                  context,
                                ).colorScheme.primary,
                                backgroundColor: Theme.of(
                                  context,
                                ).colorScheme.surface,
                                labelStyle: TextStyle(
                                  color: _selectedJob == job.name
                                      ? Theme.of(context).colorScheme.onPrimary
                                      : Theme.of(context).colorScheme.onSurface,
                                ),
                                onSelected: (_) {
                                  final selecting = _selectedJob != job.name;
                                  setState(
                                    () => _selectedJob = selecting
                                        ? job.name
                                        : '',
                                  );
                                  if (selecting) {
                                    unawaited(
                                      ref
                                          .read(analyticsProvider)
                                          .track(
                                            'job_filter_select',
                                            properties: {
                                              'job_id': job.id,
                                              'job_name': job.name,
                                              'matter_id': widget.id,
                                            },
                                          ),
                                    );
                                  }
                                },
                              ),
                            )
                            .toList(),
                      ),
                    ),
                  ],
                  const SizedBox(height: 20),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            '找到这些人',
                            style: Theme.of(context).textTheme.titleLarge,
                          ),
                        ),
                        Text(
                          '${people.length}人',
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 9),
                  for (var index = 0; index < people.length; index++) ...[
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 10),
                      child: AnswererCard(
                        answerer: people[index],
                        flat: true,
                        onTap: () {
                          final person = people[index];
                          unawaited(
                            ref
                                .read(analyticsProvider)
                                .track(
                                  'answerer_card_click',
                                  properties: {
                                    'answerer_user_id': person.id,
                                    'answerer_uid': person.uid,
                                    'job_name': person.mainJob,
                                    'position': index + 1,
                                    'source': _experience
                                        ? 'experience'
                                        : 'matter',
                                    'content_id': widget.id,
                                  },
                                ),
                          );
                          context.push('/answerers/${person.uid}');
                        },
                      ),
                    ),
                    if (index != people.length - 1) const SizedBox(height: 12),
                  ],
                  if (people.isEmpty)
                    const Padding(
                      padding: EdgeInsets.fromLTRB(10, 56, 10, 24),
                      child: Center(child: Text('暂无可交流的人')),
                    ),
                ],
              ),
            ),
    );
  }
}

class _JobDescriptionsSheet extends StatelessWidget {
  const _JobDescriptionsSheet({required this.jobs});

  final List<DiscoveryJob> jobs;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final maximumHeight = MediaQuery.sizeOf(context).height * 0.72;

    return ConstrainedBox(
      constraints: BoxConstraints(maxHeight: maximumHeight),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 10),
          Center(
            child: Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: theme.colorScheme.outlineVariant,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 18, 10, 10),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    '岗位说明',
                    style: theme.textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                IconButton(
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close_rounded),
                  tooltip: '关闭',
                ),
              ],
            ),
          ),
          Flexible(
            child: ListView.separated(
              padding: const EdgeInsets.fromLTRB(18, 4, 18, 24),
              itemCount: jobs.length,
              separatorBuilder: (_, _) => const SizedBox(height: 18),
              itemBuilder: (context, index) {
                final job = jobs[index];
                final cardColor = theme.brightness == Brightness.dark
                    ? theme.colorScheme.surfaceContainerHigh
                    : const Color(0xFFFFF3D6);
                return Container(
                  padding: const EdgeInsets.all(15),
                  decoration: BoxDecoration(
                    color: cardColor,
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: _JobDescription(job: job),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _JobDescription extends StatelessWidget {
  const _JobDescription({required this.job});

  final DiscoveryJob job;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final mainWork = job.mainWork.isNotEmpty ? job.mainWork : job.description;
    final sections = <({String label, String value})>[
      if (job.roleDescription.isNotEmpty)
        (label: '在这件事里', value: job.roleDescription),
      if (mainWork.isNotEmpty) (label: '主要工作', value: mainWork),
      if (job.canHelpWith.isNotEmpty) (label: '可以帮你判断', value: job.canHelpWith),
      if (job.notResponsibleFor.isNotEmpty)
        (label: '一般不处理', value: job.notResponsibleFor),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          job.name,
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        if (job.mainWork.isNotEmpty && job.description.isNotEmpty) ...[
          const SizedBox(height: 6),
          Text(
            job.description,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
              height: 1.5,
            ),
          ),
        ],
        if (sections.isEmpty) ...[
          const SizedBox(height: 6),
          Text(
            '暂无岗位说明',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
        for (final section in sections) ...[
          const SizedBox(height: 12),
          Text(
            section.label,
            style: theme.textTheme.labelMedium?.copyWith(
              color: theme.colorScheme.primary,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 3),
          Text(
            section.value,
            style: theme.textTheme.bodyMedium?.copyWith(height: 1.55),
          ),
        ],
      ],
    );
  }
}
