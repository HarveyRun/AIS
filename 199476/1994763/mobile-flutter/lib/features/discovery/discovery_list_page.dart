import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/discovery_models.dart';

class DiscoveryListPage extends ConsumerStatefulWidget {
  const DiscoveryListPage({super.key, required this.type});
  final String type;

  @override
  ConsumerState<DiscoveryListPage> createState() => _DiscoveryListPageState();
}

class _DiscoveryListPageState extends ConsumerState<DiscoveryListPage> {
  final _searchController = TextEditingController();
  List<DiscoverySearchItem> _all = const [];
  bool _loading = true;

  bool get _experiences => widget.type == 'experiences';

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final repository = ref.read(repositoryProvider);
      final items = _experiences
          ? await repository.searchExperiences('')
          : await repository.searchMatters('');
      if (mounted) setState(() => _all = items);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final keyword = _searchController.text.trim().toLowerCase();
    final visible = keyword.isEmpty
        ? _all
        : _all
              .where(
                (item) => '${item.categoryName} ${item.title}'
                    .toLowerCase()
                    .contains(keyword),
              )
              .toList();
    final groups = <int, List<DiscoverySearchItem>>{};
    for (final item in visible) {
      groups.putIfAbsent(item.categoryId, () => []).add(item);
    }
    return Scaffold(
      appBar: AppBar(title: Text(_experiences ? '按经历找人' : '按事情找人')),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
                children: [
                  TextField(
                    controller: _searchController,
                    onChanged: (_) => setState(() {}),
                    decoration: const InputDecoration(
                      prefixIcon: Icon(Icons.search_rounded),
                      hintText: '搜索关键词',
                    ),
                  ),
                  const SizedBox(height: 18),
                  for (final entry in groups.entries) ...[
                    Padding(
                      padding: const EdgeInsets.only(bottom: 9),
                      child: Text(
                        entry.value.first.categoryName,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ),
                    Material(
                      color: Theme.of(context).colorScheme.surface,
                      borderRadius: BorderRadius.circular(16),
                      clipBehavior: Clip.antiAlias,
                      child: Column(
                        children: [
                          for (
                            var index = 0;
                            index < entry.value.length;
                            index++
                          ) ...[
                            _DiscoveryRow(
                              title: entry.value[index].title,
                              onPressed: () => context.push(
                                '/discover/${widget.type}/${entry.value[index].id}/results?title=${Uri.encodeQueryComponent(entry.value[index].title)}',
                              ),
                            ),
                            if (index != entry.value.length - 1)
                              const Divider(
                                height: 1,
                                indent: 14,
                                endIndent: 14,
                              ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                  ],
                  if (groups.isEmpty)
                    const Padding(
                      padding: EdgeInsets.only(top: 90),
                      child: Center(child: Text('暂无数据')),
                    ),
                ],
              ),
            ),
    );
  }
}

class _DiscoveryRow extends StatelessWidget {
  const _DiscoveryRow({required this.title, required this.onPressed});

  final String title;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      minTileHeight: 48,
      contentPadding: const EdgeInsets.symmetric(horizontal: 14),
      title: Text(title, style: Theme.of(context).textTheme.bodyLarge),
      trailing: Icon(
        Icons.chevron_right_rounded,
        size: 19,
        color: Theme.of(context).textTheme.bodySmall?.color,
      ),
      onTap: onPressed,
    );
  }
}
