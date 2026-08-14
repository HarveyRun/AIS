import 'package:flutter/material.dart';

class FaqPage extends StatelessWidget {
  const FaqPage({super.key});

  static const _items = [
    ('提现何时能到账？', '提现申请提交后，预计1～3个工作日到账。\n到账时间以银行实际处理进度为准。'),
    (
      '违规处理方式？',
      '违规共分为6个等级：\n0级：1次\n1级：3次\n2级：5次\n3级：10次\n4级：50次\n5级：100次\n\n对应次数扣完后，账号将被永久封禁。',
    ),
  ];

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('常见问题')),
    body: ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: _items.length,
      separatorBuilder: (_, _) => const SizedBox(height: 9),
      itemBuilder: (context, index) => Card(
        child: ExpansionTile(
          tilePadding: const EdgeInsets.symmetric(horizontal: 15),
          childrenPadding: const EdgeInsets.fromLTRB(15, 0, 15, 15),
          title: Text(
            _items[index].$1,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          expandedCrossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: double.infinity,
              child: Text(
                _items[index].$2,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ),
          ],
        ),
      ),
    ),
  );
}
