import 'package:flutter/material.dart';

import '../../core/input/app_input_formatters.dart';

Future<Map<String, dynamic>?> showInquiryEvaluationSheet(BuildContext context) {
  return showModalBottomSheet<Map<String, dynamic>>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (_) => const _EvaluationSheet(),
  );
}

Future<Map<String, String>?> showInquiryReviewSheet(BuildContext context) {
  return showModalBottomSheet<Map<String, String>>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (_) => const _ReviewSheet(),
  );
}

class _EvaluationSheet extends StatefulWidget {
  const _EvaluationSheet();
  @override
  State<_EvaluationSheet> createState() => _EvaluationSheetState();
}

class _EvaluationSheetState extends State<_EvaluationSheet> {
  final _comment = TextEditingController();
  final _levels = List<int>.filled(6, 1);
  final _tags = <String>{};

  static const _dimensions = [
    '是否回答了你的问题',
    '内容是否具体',
    '是否符合对方的真实经历',
    '对你是否有实际帮助',
    '交流态度是否合适',
    '以后是否愿意再问对方',
  ];
  static const _negativeTags = {
    'OFF_TOPIC': '答非所问',
    'TOO_GENERAL': '过于空泛',
    'NO_RESPONSE': '没有有效回应',
    'SUSPECTED_FABRICATION': '疑似虚构',
    'INAPPROPRIATE_LANGUAGE': '言语不当',
    'OFF_PLATFORM_PAYMENT': '引导平台外付款',
    'ADVERTISEMENT': '广告推销',
  };

  @override
  void dispose() {
    _comment.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        18,
        8,
        18,
        MediaQuery.viewInsetsOf(context).bottom + 18,
      ),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const _Handle(),
            Row(
              children: [
                Expanded(
                  child: Text(
                    '评价本次交流',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                IconButton(
                  onPressed: () => Navigator.pop(context),
                  tooltip: '关闭',
                  icon: const Icon(Icons.close_rounded),
                ),
              ],
            ),
            const SizedBox(height: 18),
            for (var index = 0; index < _dimensions.length; index++) ...[
              Text(_dimensions[index]),
              const SizedBox(height: 7),
              SegmentedButton<int>(
                showSelectedIcon: false,
                segments: const [
                  ButtonSegment(value: 0, label: Text('不符合')),
                  ButtonSegment(value: 1, label: Text('一般')),
                  ButtonSegment(value: 2, label: Text('符合')),
                ],
                selected: {_levels[index]},
                style: ButtonStyle(
                  backgroundColor: WidgetStateProperty.resolveWith(
                    (states) => states.contains(WidgetState.selected)
                        ? Theme.of(context).colorScheme.primary
                        : Theme.of(context).colorScheme.surfaceContainer,
                  ),
                  foregroundColor: WidgetStateProperty.resolveWith(
                    (states) => states.contains(WidgetState.selected)
                        ? Theme.of(context).colorScheme.onPrimary
                        : Theme.of(context).colorScheme.onSurface,
                  ),
                  side: const WidgetStatePropertyAll(BorderSide.none),
                ),
                onSelectionChanged: (value) =>
                    setState(() => _levels[index] = value.first),
              ),
              const SizedBox(height: 14),
            ],
            const Text('存在的问题（可多选）'),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 6,
              children: _negativeTags.entries
                  .map((entry) {
                    final selected = _tags.contains(entry.key);
                    return FilterChip(
                      label: Text(entry.value),
                      selected: selected,
                      showCheckmark: false,
                      backgroundColor: Theme.of(
                        context,
                      ).colorScheme.surfaceContainer,
                      selectedColor: Theme.of(context).colorScheme.primary,
                      labelStyle: TextStyle(
                        color: selected
                            ? Theme.of(context).colorScheme.onPrimary
                            : Theme.of(context).colorScheme.onSurface,
                      ),
                      side: BorderSide.none,
                      onSelected: (selected) => setState(
                        () => selected
                            ? _tags.add(entry.key)
                            : _tags.remove(entry.key),
                      ),
                    );
                  })
                  .toList(growable: false),
            ),
            const SizedBox(height: 14),
            TextField(
              controller: _comment,
              minLines: 3,
              maxLines: 5,
              inputFormatters: AppInputFormatters.description(300),
              decoration: const InputDecoration(hintText: '补充说明（选填）'),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => Navigator.pop(context, {
                  'answeredLevel': _levels[0],
                  'specificLevel': _levels[1],
                  'matchedLevel': _levels[2],
                  'usefulLevel': _levels[3],
                  'communicationLevel': _levels[4],
                  'askAgainLevel': _levels[5],
                  'negativeTags': _tags.toList(growable: false),
                  'comment': _comment.text.trim(),
                }),
                child: const Text('提交评价'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ReviewSheet extends StatefulWidget {
  const _ReviewSheet();
  @override
  State<_ReviewSheet> createState() => _ReviewSheetState();
}

class _ReviewSheetState extends State<_ReviewSheet> {
  final _description = TextEditingController();
  String _reason = 'NO_EFFECTIVE_ANSWER';
  static const _reasons = {
    'NO_EFFECTIVE_ANSWER': '没有提供有效回答',
    'CLEARLY_OFF_TOPIC': '明显答非所问',
    'SUSPECTED_FABRICATION': '疑似虚构经历或身份',
    'HARASSMENT': '交流中存在骚扰',
    'OFF_PLATFORM_PAYMENT': '引导平台外付款',
    'OTHER': '其他问题',
  };
  @override
  void dispose() {
    _description.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        18,
        8,
        18,
        MediaQuery.viewInsetsOf(context).bottom + 18,
      ),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const _Handle(),
            Text('申请质量复核', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text(
              '平台会核对本次交流内容。处理期间资金继续冻结，最终只会全额退款或正常结算。',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 18),
            DropdownButtonFormField<String>(
              value: _reason,
              items: _reasons.entries
                  .map(
                    (item) => DropdownMenuItem(
                      value: item.key,
                      child: Text(item.value),
                    ),
                  )
                  .toList(growable: false),
              onChanged: (value) => setState(() => _reason = value ?? _reason),
              decoration: const InputDecoration(labelText: '问题类型'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _description,
              minLines: 4,
              maxLines: 7,
              inputFormatters: AppInputFormatters.description(500),
              decoration: const InputDecoration(hintText: '具体说明回答存在什么问题'),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () {
                  if (_description.text.trim().isEmpty) return;
                  Navigator.pop(context, {
                    'reasonCode': _reason,
                    'description': _description.text.trim(),
                  });
                },
                child: const Text('提交复核'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Handle extends StatelessWidget {
  const _Handle();
  @override
  Widget build(BuildContext context) => Center(
    child: Container(
      width: 36,
      height: 4,
      margin: const EdgeInsets.only(bottom: 18),
      decoration: BoxDecoration(
        color: Theme.of(context).dividerColor,
        borderRadius: BorderRadius.circular(4),
      ),
    ),
  );
}
