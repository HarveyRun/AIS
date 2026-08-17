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
      if (mounted) setState(() {});
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final people = _selectedJob.isEmpty
        ? _people
        : _people.where((person) => person.mainJob == _selectedJob).toList();
    return Scaffold(
      appBar: AppBar(title: const Text('找人')),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.only(bottom: 32),
                children: [
                  Container(
                    margin: const EdgeInsets.fromLTRB(17, 4, 17, 0),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surfaceContainer,
                      borderRadius: BorderRadius.circular(18),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.all(18),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _experience ? '你想了解' : '你想做',
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
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: Text(
                        '可能会问到',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: _matter!.jobs
                            .map(
                              (job) => FilterChip(
                                label: Text(job.name),
                                selected: _selectedJob == job.name,
                                onSelected: (_) => setState(
                                  () => _selectedJob == job.name
                                      ? _selectedJob = ''
                                      : _selectedJob = job.name,
                                ),
                              ),
                            )
                            .toList(),
                      ),
                    ),
                  ],
                  const SizedBox(height: 20),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
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
                    AnswererCard(
                      answerer: people[index],
                      onTap: () =>
                          context.push('/answerers/${people[index].uid}'),
                    ),
                    if (index != people.length - 1) const SizedBox(height: 12),
                  ],
                  if (people.isEmpty)
                    const Padding(
                      padding: EdgeInsets.only(top: 60),
                      child: Center(child: Text('暂无可交流的人')),
                    ),
                ],
              ),
            ),
    );
  }
}
