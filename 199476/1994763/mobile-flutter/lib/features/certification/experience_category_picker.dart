import 'package:flutter/material.dart';

import '../../data/models/certification_models.dart';

typedef ExperienceCategorySelection = ({
  ExperienceCategoryOption parent,
  ExperienceCategoryOption child,
});

Future<ExperienceCategorySelection?> showExperienceCategoryPicker(
  BuildContext context, {
  required List<ExperienceCategoryOption> categories,
  int? selectedParentId,
  int? selectedChildId,
}) => showModalBottomSheet<ExperienceCategorySelection>(
  context: context,
  isScrollControlled: true,
  useSafeArea: true,
  backgroundColor: Theme.of(context).colorScheme.surface,
  shape: const RoundedRectangleBorder(
    borderRadius: BorderRadius.vertical(top: Radius.circular(18)),
  ),
  builder: (_) => _CategorySheet(
    categories: categories,
    selectedParentId: selectedParentId,
    selectedChildId: selectedChildId,
  ),
);

class _CategorySheet extends StatefulWidget {
  const _CategorySheet({
    required this.categories,
    required this.selectedParentId,
    required this.selectedChildId,
  });

  final List<ExperienceCategoryOption> categories;
  final int? selectedParentId;
  final int? selectedChildId;

  @override
  State<_CategorySheet> createState() => _CategorySheetState();
}

class _CategorySheetState extends State<_CategorySheet> {
  late int? _activeParentId;
  late int? _selectedChildId;

  @override
  void initState() {
    super.initState();
    final parents = widget.categories.where((item) => item.parentId == null);
    _activeParentId = widget.selectedParentId ??
        (parents.isEmpty ? null : parents.first.id);
    _selectedChildId = widget.selectedChildId;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final parents = widget.categories
        .where((item) => item.parentId == null)
        .toList(growable: false);
    final children = widget.categories
        .where((item) => item.parentId == _activeParentId)
        .toList(growable: false);
    final activeParent = parents
        .where((item) => item.id == _activeParentId)
        .firstOrNull;
    final selectedChild = children
        .where((item) => (item.targetCategoryId ?? item.id) == _selectedChildId || item.id == _selectedChildId)
        .firstOrNull;
    final canonicalChild = selectedChild == null ? null :
        widget.categories.where((item) => item.id == (selectedChild.targetCategoryId ?? selectedChild.id)).firstOrNull;
    final canonicalParent = canonicalChild == null ? null :
        widget.categories.where((item) => item.id == canonicalChild.parentId).firstOrNull;

    return SizedBox(
      height: MediaQuery.sizeOf(context).height * .68,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 12, 10, 12),
            child: Row(
              children: [
                Expanded(
                  child: Text('选择分类', style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  )),
                ),
                IconButton(
                  tooltip: '关闭',
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.close_rounded),
                ),
              ],
            ),
          ),
          Expanded(
            child: Row(
              children: [
                SizedBox(
                  width: 132,
                  child: ColoredBox(
                    color: colors.surfaceContainerLow,
                    child: ListView.builder(
                      itemCount: parents.length,
                      itemBuilder: (context, index) {
                        final parent = parents[index];
                        final selected = parent.id == _activeParentId;
                        return InkWell(
                          onTap: () => setState(() {
                            if (_activeParentId != parent.id) {
                              _activeParentId = parent.id;
                              _selectedChildId = null;
                            }
                          }),
                          child: Padding(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16, vertical: 17,
                            ),
                            child: Text(
                              parent.name,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: theme.textTheme.bodyMedium?.copyWith(
                                color: selected ? colors.primary : colors.onSurface,
                                fontWeight: selected ? FontWeight.w700 : FontWeight.w400,
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.only(top: 4),
                    itemCount: children.length,
                    itemBuilder: (context, index) {
                      final child = children[index];
                      final selected = child.id == _selectedChildId ||
                          (child.targetCategoryId ?? child.id) == _selectedChildId;
                      return InkWell(
                        onTap: () => setState(() => _selectedChildId = child.targetCategoryId ?? child.id),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 18, vertical: 17,
                          ),
                          child: Row(
                            children: [
                              Expanded(child: Text(
                                child.name,
                                style: theme.textTheme.bodyMedium?.copyWith(
                                  color: selected ? colors.primary : colors.onSurface,
                                  fontWeight: FontWeight.w400,
                                ),
                              )),
                              if (selected) Icon(
                                Icons.check_rounded,
                                color: colors.primary,
                                size: 20,
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
          SafeArea(
            top: false,
            minimum: const EdgeInsets.fromLTRB(16, 10, 16, 12),
            child: SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: activeParent == null || canonicalChild == null || canonicalParent == null
                    ? null
                    : () => Navigator.pop(context, (
                        parent: canonicalParent,
                        child: canonicalChild,
                      )),
                child: const Text('确定'),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
