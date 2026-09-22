import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shixianwen_mobile/data/models/certification_models.dart';
import 'package:shixianwen_mobile/features/home/experience_filter_overlay.dart';

void main() {
  const categories = [
    ExperienceCategoryOption(
      id: 1,
      parentId: null,
      name: '住房家居',
      recommendationGroup: false,
      targetCategoryId: null,
    ),
    ExperienceCategoryOption(
      id: 2,
      parentId: 1,
      name: '新房装修',
      recommendationGroup: false,
      targetCategoryId: null,
    ),
    ExperienceCategoryOption(
      id: 3,
      parentId: null,
      name: '职业职场',
      recommendationGroup: false,
      targetCategoryId: null,
    ),
    ExperienceCategoryOption(
      id: 4,
      parentId: 3,
      name: '求职面试',
      recommendationGroup: false,
      targetCategoryId: null,
    ),
  ];

  testWidgets(
    'parent-only category and cross-group sort are applied together',
    (tester) async {
      tester.view.physicalSize = const Size(360, 800);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      ExperienceFilterResult? result;
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => TextButton(
                onPressed: () async {
                  result = await showExperienceFilterOverlay(
                    context,
                    categories: categories,
                    categoryId: null,
                    sortBy: null,
                    sortDirection: null,
                  );
                },
                child: const Text('打开筛选'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('打开筛选'));
      await tester.pumpAndSettle();
      expect(find.text('按分类'), findsOneWidget);
      expect(find.byType(PopupMenuButton<int>), findsNWidgets(2));
      expect(find.text('按分数'), findsOneWidget);
      expect(find.text('按点赞'), findsOneWidget);
      expect(tester.takeException(), isNull);

      await tester.tap(find.byType(PopupMenuButton<int>).first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('住房家居').last);
      await tester.pumpAndSettle();
      await tester.tap(find.text('从高到低').first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('从低到高').last);
      await tester.pumpAndSettle();
      await tester.tap(find.text('查看结果'));
      await tester.pumpAndSettle();

      expect(result?.categoryId, 1);
      expect(result?.sortBy, 'LIKE_COUNT');
      expect(result?.sortDirection, 'ASC');
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('second-level category can be selected', (tester) async {
    ExperienceFilterResult? result;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => TextButton(
              onPressed: () async {
                result = await showExperienceFilterOverlay(
                  context,
                  categories: categories,
                  categoryId: null,
                  sortBy: null,
                  sortDirection: null,
                );
              },
              child: const Text('打开筛选'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('打开筛选'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(PopupMenuButton<int>).first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('住房家居').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byType(PopupMenuButton<int>).last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('新房装修').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('查看结果'));
    await tester.pumpAndSettle();

    expect(result?.categoryId, 2);
  });

  testWidgets('reset clears preselected category and sort', (tester) async {
    ExperienceFilterResult? result;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => TextButton(
              onPressed: () async {
                result = await showExperienceFilterOverlay(
                  context,
                  categories: categories,
                  categoryId: 2,
                  sortBy: 'REFERENCE_INDEX',
                  sortDirection: 'DESC',
                );
              },
              child: const Text('打开筛选'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('打开筛选'));
    await tester.pumpAndSettle();
    expect(find.text('住房家居'), findsOneWidget);
    expect(find.text('新房装修'), findsOneWidget);
    await tester.tap(find.text('重置'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('查看结果'));
    await tester.pumpAndSettle();

    expect(result?.categoryId, isNull);
    expect(result?.sortBy, isNull);
    expect(result?.sortDirection, isNull);
  });

  testWidgets('closing discards changes', (tester) async {
    ExperienceFilterResult? result;
    var completed = false;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => TextButton(
              onPressed: () async {
                result = await showExperienceFilterOverlay(
                  context,
                  categories: categories,
                  categoryId: null,
                  sortBy: null,
                  sortDirection: null,
                );
                completed = true;
              },
              child: const Text('打开筛选'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('打开筛选'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('从高到低').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('关闭'));
    await tester.pumpAndSettle();

    expect(completed, isTrue);
    expect(result, isNull);
  });

  testWidgets('switching parent clears old child selection', (tester) async {
    ExperienceFilterResult? result;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => TextButton(
              onPressed: () async {
                result = await showExperienceFilterOverlay(
                  context,
                  categories: categories,
                  categoryId: 2,
                  sortBy: null,
                  sortDirection: null,
                );
              },
              child: const Text('打开筛选'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('打开筛选'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(PopupMenuButton<int>).first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('职业职场').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('查看结果'));
    await tester.pumpAndSettle();

    expect(result?.categoryId, 3);
    expect(result?.categoryLabel, '职业职场');
  });
}
