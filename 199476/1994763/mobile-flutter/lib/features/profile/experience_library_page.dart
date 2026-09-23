import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/widgets/answerer_card.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/answerer_models.dart';

enum ExperienceLibraryType { favorites, recent }

class ExperienceLibraryPage extends ConsumerStatefulWidget {
  const ExperienceLibraryPage({super.key, required this.type});

  final ExperienceLibraryType type;

  @override
  ConsumerState<ExperienceLibraryPage> createState() =>
      _ExperienceLibraryPageState();
}

class _ExperienceLibraryPageState extends ConsumerState<ExperienceLibraryPage> {
  final _controller = ScrollController();
  final List<ExperienceLibraryItem> _items = [];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 0;

  bool get _recent => widget.type == ExperienceLibraryType.recent;

  @override
  void initState() {
    super.initState();
    _controller.addListener(_onScroll);
    _load(reset: true);
  }

  void _onScroll() {
    if (_controller.hasClients && _controller.position.extentAfter < 180) {
      _load();
    }
  }

  Future<void> _load({bool reset = false}) async {
    if (reset) {
      _page = 0;
      _hasMore = true;
      if (mounted) setState(() => _loading = true);
    } else {
      if (!_hasMore || _loadingMore || _loading) return;
      setState(() => _loadingMore = true);
    }
    try {
      final result = await ref
          .read(repositoryProvider)
          .experienceLibrary(
            type: _recent ? 'RECENT' : 'FAVORITES',
            page: _page,
            size: 20,
          );
      if (!mounted) return;
      setState(() {
        if (reset) _items.clear();
        _items.addAll(result.items);
        _hasMore = result.hasMore;
        if (_hasMore) _page++;
      });
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
          _loadingMore = false;
        });
      }
    }
  }

  Future<void> _removeFavorite(ExperienceLibraryItem item) async {
    final experience = item.answerer.experiences.firstOrNull;
    final certificationId = experience?.certificationId;
    if (certificationId == null) return;
    try {
      await ref
          .read(repositoryProvider)
          .setExperienceFavorite(
            uid: item.answerer.uid,
            certificationId: certificationId,
            favorited: false,
          );
      if (!mounted) return;
      setState(() => _items.remove(item));
      AppMessage.show(context, '已取消收藏');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _removeRecent(ExperienceLibraryItem item) async {
    final certificationId =
        item.answerer.experiences.firstOrNull?.certificationId;
    if (certificationId == null) return;
    try {
      await ref
          .read(repositoryProvider)
          .deleteRecentExperience(certificationId);
      if (!mounted) return;
      setState(() => _items.remove(item));
      AppMessage.show(context, '浏览记录已删除');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _clearRecent() async {
    if (_items.isEmpty) return;
    final confirmed = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => AlertDialog(
        titlePadding: const EdgeInsets.fromLTRB(24, 14, 8, 0),
        title: Row(
          children: [
            const Expanded(child: Text('清空浏览记录？')),
            IconButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              tooltip: '关闭',
              icon: const Icon(Icons.close_rounded),
            ),
          ],
        ),
        content: const Text('清空后无法恢复。'),
        actions: [
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('确认清空'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      await ref.read(repositoryProvider).clearRecentExperiences();
      if (!mounted) return;
      setState(() {
        _items.clear();
        _hasMore = false;
      });
      AppMessage.show(context, '浏览记录已清空');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(_recent ? '最近浏览' : '我的收藏'),
      actions: [
        if (_recent)
          IconButton(
            onPressed: _items.isEmpty ? null : _clearRecent,
            tooltip: '一键清空',
            icon: const Icon(Icons.cleaning_services_outlined),
          ),
      ],
    ),
    body: _loading
        ? const SizedBox.shrink()
        : _items.isEmpty
        ? _emptyView()
        : _content(),
  );

  Widget _emptyView() => RefreshIndicator(
    onRefresh: () => _load(reset: true),
    child: ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: [
        SizedBox(height: MediaQuery.sizeOf(context).height * .28),
        Icon(
          _recent ? Icons.history_rounded : Icons.bookmark_border_rounded,
          size: 32,
          color: const Color(0xFF9A9A9A),
        ),
        const SizedBox(height: 12),
        Center(child: Text(_recent ? '暂无浏览记录' : '暂无收藏的经历')),
      ],
    ),
  );

  Widget _content() {
    final rows = <({String? header, ExperienceLibraryItem? item})>[];
    String? previousHeader;
    for (final item in _items) {
      if (_recent) {
        final header = _dateHeader(item.occurredAt);
        if (header != previousHeader) {
          rows.add((header: header, item: null));
          previousHeader = header;
        }
      }
      rows.add((header: null, item: item));
    }
    return RefreshIndicator(
      onRefresh: () => _load(reset: true),
      child: ListView.builder(
        controller: _controller,
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(10, 12, 10, 28),
        itemCount: rows.length + (_loadingMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index == rows.length) {
            return const Padding(
              padding: EdgeInsets.all(16),
              child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
            );
          }
          final row = rows[index];
          if (row.header != null) return _timelineHeader(row.header!);
          final item = row.item!;
          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: _libraryCard(item),
          );
        },
      ),
    );
  }

  Widget _timelineHeader(String label) => Padding(
    padding: const EdgeInsets.fromLTRB(6, 6, 6, 10),
    child: Row(
      children: [
        Container(
          width: 7,
          height: 7,
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.primary,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 8),
        Text(
          label,
          style: Theme.of(
            context,
          ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
        ),
      ],
    ),
  );

  Widget _libraryCard(ExperienceLibraryItem item) {
    final answerer = item.answerer;
    final experience = answerer.experiences.first;
    final colors = Theme.of(context).colorScheme;
    return AnswererCard(
      answerer: answerer,
      experience: experience,
      flat: true,
      showDetailsRow: false,
      onTap: () async {
        await context.push(
          '/answerers/${answerer.uid}?experienceId=${experience.certificationId}',
        );
        if (mounted) await _load(reset: true);
      },
      footer: Container(
        padding: const EdgeInsets.fromLTRB(18, 0, 10, 10),
        child: Row(
          children: [
            if (_recent)
              Text(
                _timeLabel(item.occurredAt),
                style: Theme.of(
                  context,
                ).textTheme.bodySmall?.copyWith(color: colors.onSurfaceVariant),
              ),
            const Spacer(),
            TextButton(
              onPressed: () =>
                  _recent ? _removeRecent(item) : _removeFavorite(item),
              style: TextButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                minimumSize: const Size(0, 34),
              ),
              child: Text(_recent ? '删除记录' : '取消收藏'),
            ),
          ],
        ),
      ),
    );
  }

  String _dateHeader(DateTime? value) {
    if (value == null) return '更早';
    final local = value.toLocal();
    final today = DateTime.now();
    final date = DateTime(local.year, local.month, local.day);
    final todayDate = DateTime(today.year, today.month, today.day);
    final difference = todayDate.difference(date).inDays;
    if (difference == 0) return '今天';
    if (difference == 1) return '昨天';
    return local.year == today.year
        ? DateFormat('M月d日').format(local)
        : DateFormat('yyyy年M月d日').format(local);
  }

  String _timeLabel(DateTime? value) =>
      value == null ? '' : DateFormat('HH:mm').format(value.toLocal());
}
