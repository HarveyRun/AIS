import 'package:flutter/material.dart';

import '../../data/models/certification_models.dart';

class ExperienceFilterResult {
  const ExperienceFilterResult({
    required this.categoryId,
    required this.categoryLabel,
    required this.sortBy,
    required this.sortDirection,
  });

  final int? categoryId;
  final String categoryLabel;
  final String? sortBy;
  final String? sortDirection;
}

Future<ExperienceFilterResult?> showExperienceFilterOverlay(
  BuildContext context, {
  required List<ExperienceCategoryOption> categories,
  required int? categoryId,
  required String? sortBy,
  required String? sortDirection,
}) => showGeneralDialog<ExperienceFilterResult>(
  context: context,
  barrierDismissible: false,
  barrierColor: Colors.transparent,
  transitionDuration: const Duration(milliseconds: 160),
  pageBuilder: (routeContext, _, __) => _ExperienceFilterOverlay(
    categories: categories,
    initialCategoryId: categoryId,
    initialSortBy: sortBy,
    initialSortDirection: sortDirection,
  ),
  transitionBuilder: (context, animation, _, child) =>
      FadeTransition(opacity: animation, child: child),
);

class _ExperienceFilterOverlay extends StatefulWidget {
  const _ExperienceFilterOverlay({
    required this.categories,
    required this.initialCategoryId,
    required this.initialSortBy,
    required this.initialSortDirection,
  });

  final List<ExperienceCategoryOption> categories;
  final int? initialCategoryId;
  final String? initialSortBy;
  final String? initialSortDirection;

  @override
  State<_ExperienceFilterOverlay> createState() =>
      _ExperienceFilterOverlayState();
}

class _ExperienceFilterOverlayState extends State<_ExperienceFilterOverlay> {
  int? _categoryId;
  int? _activeParentId;
  int _sortIndex = 0;

  static const _sorts = <(String, String?, String?)>[
    ('默认排序', null, null),
    ('内容综合分高到低', 'REFERENCE_INDEX', 'DESC'),
    ('内容综合分低到高', 'REFERENCE_INDEX', 'ASC'),
    ('点赞高到低', 'LIKE_COUNT', 'DESC'),
    ('点赞低到高', 'LIKE_COUNT', 'ASC'),
  ];

  @override
  void initState() {
    super.initState();
    final selected = widget.categories
        .where((item) => item.id == widget.initialCategoryId)
        .firstOrNull;
    _categoryId = selected?.id;
    _activeParentId = selected?.parentId ?? selected?.id;
    final index = _sorts.indexWhere(
      (item) =>
          item.$2 == widget.initialSortBy &&
          item.$3 == widget.initialSortDirection,
    );
    _sortIndex = index < 0 ? 0 : index;
  }

  void _apply() {
    final selected = widget.categories
        .where((item) => item.id == _categoryId)
        .firstOrNull;
    final parent = widget.categories
        .where((item) => item.id == selected?.parentId)
        .firstOrNull;
    final label = selected == null
        ? '全部分类'
        : parent == null
        ? selected.name
        : '${parent.name} / ${selected.name}';
    final sort = _sorts[_sortIndex];
    Navigator.pop(
      context,
      ExperienceFilterResult(
        categoryId: _categoryId,
        categoryLabel: label,
        sortBy: sort.$2,
        sortDirection: sort.$3,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final light = theme.brightness == Brightness.light;
    final pageBackground = light ? Colors.white : colors.surface;
    final controlBackground = light
        ? const Color(0xFFF2F2F2)
        : colors.surfaceContainerLow;
    final parents = widget.categories
        .where((item) => item.parentId == null)
        .toList();
    final children = widget.categories
        .where((item) => item.parentId == _activeParentId)
        .toList();

    return Material(
      color: pageBackground,
      child: SafeArea(
        child: Scaffold(
          backgroundColor: pageBackground,
          appBar: AppBar(
            backgroundColor: pageBackground,
            centerTitle: true,
            title: const Text('筛选经历'),
            leading: IconButton(
              tooltip: '关闭',
              onPressed: () => Navigator.pop(context),
              icon: const Icon(Icons.close_rounded),
            ),
          ),
          body: ListView(
            padding: const EdgeInsets.fromLTRB(10, 14, 10, 20),
            children: [
              Text(
                '按分类',
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: _categoryDropdown(
                      value: _activeParentId ?? 0,
                      options: [
                        const MapEntry(0, '全部'),
                        for (final parent in parents)
                          MapEntry(parent.id, parent.name),
                      ],
                      onChanged: (value) => setState(() {
                        _activeParentId = value == 0 ? null : value;
                        _categoryId = _activeParentId;
                      }),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _categoryDropdown(
                      value:
                          _activeParentId == null ||
                              _categoryId == _activeParentId
                          ? 0
                          : _categoryId ?? 0,
                      options: [
                        MapEntry(0, _activeParentId == null ? '二级分类' : '全部'),
                        for (final child in children)
                          MapEntry(child.id, child.name),
                      ],
                      onChanged: _activeParentId == null
                          ? null
                          : (value) => setState(() {
                              _categoryId = value == 0
                                  ? _activeParentId
                                  : value;
                            }),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              _sortRow('按分数', lowIndex: 2, highIndex: 1),
              const SizedBox(height: 22),
              _sortRow('按点赞', lowIndex: 4, highIndex: 3),
            ],
          ),
          bottomNavigationBar: SafeArea(
            top: false,
            minimum: const EdgeInsets.fromLTRB(10, 8, 10, 12),
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(
                      backgroundColor: controlBackground,
                    ),
                    onPressed: () => setState(() {
                      _categoryId = null;
                      _activeParentId = null;
                      _sortIndex = 0;
                    }),
                    child: const Text('重置'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  flex: 2,
                  child: FilledButton(
                    onPressed: _apply,
                    child: const Text('查看结果'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _categoryDropdown({
    required int value,
    required List<MapEntry<int, String>> options,
    required ValueChanged<int?>? onChanged,
  }) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final controlBackground = theme.brightness == Brightness.light
        ? const Color(0xFFF2F2F2)
        : colors.surfaceContainerLow;
    final current =
        options.where((item) => item.key == value).firstOrNull ?? options.first;
    return LayoutBuilder(
      builder: (context, constraints) => PopupMenuButton<int>(
        enabled: onChanged != null,
        initialValue: value,
        position: PopupMenuPosition.under,
        offset: const Offset(0, 4),
        padding: EdgeInsets.zero,
        tooltip: '选择分类',
        color: theme.brightness == Brightness.light
            ? Colors.white
            : colors.surface,
        elevation: 5,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        constraints: BoxConstraints(
          minWidth: constraints.maxWidth,
          maxWidth: constraints.maxWidth,
          maxHeight: 240,
        ),
        onSelected: onChanged,
        itemBuilder: (context) => [
          for (final item in options)
            PopupMenuItem<int>(
              value: item.key,
              height: 44,
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      item.value,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: item.key == value
                            ? colors.primary
                            : colors.onSurface,
                      ),
                    ),
                  ),
                  if (item.key == value)
                    Icon(Icons.check_rounded, size: 16, color: colors.primary),
                ],
              ),
            ),
        ],
        child: Container(
          height: 44,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          decoration: BoxDecoration(
            color: controlBackground,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  current.value,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: onChanged == null
                        ? colors.onSurfaceVariant
                        : colors.onSurface,
                  ),
                ),
              ),
              Icon(
                Icons.keyboard_arrow_down_rounded,
                size: 20,
                color: onChanged == null
                    ? colors.onSurfaceVariant
                    : colors.onSurface,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _sortRow(
    String title, {
    required int lowIndex,
    required int highIndex,
  }) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: theme.textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(child: _sortButton('从低到高', lowIndex)),
            const SizedBox(width: 10),
            Expanded(child: _sortButton('从高到低', highIndex)),
          ],
        ),
      ],
    );
  }

  Widget _sortButton(String title, int index) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final controlBackground = theme.brightness == Brightness.light
        ? const Color(0xFFF2F2F2)
        : colors.surfaceContainerLow;
    final selected = _sortIndex == index;
    return Material(
      color: selected ? colors.primary : controlBackground,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: () => setState(() => _sortIndex = selected ? 0 : index),
        child: SizedBox(
          height: 44,
          child: Center(
            child: Text(
              title,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: selected ? colors.onPrimary : colors.onSurface,
                fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
