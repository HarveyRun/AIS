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
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: Text(_experiences ? '按经历找人' : '按事情找人'),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(17, 8, 17, 32),
                children: [
                  TextField(
                    controller: _searchController,
                    onChanged: (_) => setState(() {}),
                    decoration: InputDecoration(
                      filled: true,
                      fillColor: const Color(0xFFF5F2EF),
                      prefixIcon: const Icon(
                        Icons.search_rounded,
                        size: 19,
                        color: Color(0xFF8D8884),
                      ),
                      hintText: '搜索关键词',
                      hintStyle: const TextStyle(
                        color: Color(0xFF8D8884),
                        fontSize: 12,
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  for (final entry in groups.entries) ...[
                    Padding(
                      padding: const EdgeInsets.only(left: 2, bottom: 9),
                      child: Text(
                        entry.value.first.categoryName,
                        style: Theme.of(context).textTheme.titleMedium
                            ?.copyWith(
                              fontSize: 14,
                              fontWeight: FontWeight.w700,
                            ),
                      ),
                    ),
                    LayoutBuilder(
                      builder: (context, constraints) {
                        const spacing = 10.0;
                        final itemWidth = (constraints.maxWidth - spacing) / 2;
                        return Wrap(
                          spacing: spacing,
                          runSpacing: 10,
                          children: [
                            for (final item in entry.value)
                              SizedBox(
                                width: itemWidth,
                                child: _DiscoveryRow(
                                  title: item.title,
                                  onPressed: () => context.push(
                                    '/discover/${widget.type}/${item.id}/results?title=${Uri.encodeQueryComponent(item.title)}',
                                  ),
                                ),
                              ),
                          ],
                        );
                      },
                    ),
                    const SizedBox(height: 22),
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
    return Material(
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(13),
        side: const BorderSide(color: Color(0xFFE7D9D1)),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onPressed,
        child: SizedBox(
          height: 44,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: const Color(0xFF675C56),
                  fontSize: 12,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
