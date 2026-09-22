import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';

class ExperienceAdditionalInfoPage extends StatefulWidget {
  const ExperienceAdditionalInfoPage({super.key, required this.initialValue});

  final ExperienceAdditionalInfo initialValue;

  @override
  State<ExperienceAdditionalInfoPage> createState() =>
      _ExperienceAdditionalInfoPageState();
}

class _ExperienceAdditionalInfoPageState
    extends State<ExperienceAdditionalInfoPage> {
  static const _ageRanges = [
    '0～5岁',
    '6～14岁',
    '15～23岁',
    '24～33岁',
    '34～54岁',
    '55岁及以上',
  ];
  static const _educations = [
    '小学及以下',
    '初中',
    '高中/中专',
    '大专',
    '本科',
    '硕士',
    '博士及以上',
  ];

  late final TextEditingController _location;
  late final TextEditingController _startDate;
  late final TextEditingController _endDate;
  late final TextEditingController _count;
  late final TextEditingController _role;
  late final TextEditingController _job;
  late String _ageRange;
  late String _education;

  @override
  void initState() {
    super.initState();
    final value = widget.initialValue;
    _location = TextEditingController(text: value.location);
    _startDate = TextEditingController(text: value.startDate);
    _endDate = TextEditingController(text: value.endDate);
    _count = TextEditingController(text: value.count?.toString() ?? '');
    _role = TextEditingController(text: value.role);
    _job = TextEditingController(text: value.job);
    _ageRange = value.ageRange;
    _education = value.education;
  }

  @override
  void dispose() {
    _location.dispose();
    _startDate.dispose();
    _endDate.dispose();
    _count.dispose();
    _role.dispose();
    _job.dispose();
    super.dispose();
  }

  void _save() {
    final count = int.tryParse(_count.text);
    if (_count.text.isNotEmpty && (count == null || count < 1)) {
      AppMessage.show(context, '已经历的次数不能小于1次');
      return;
    }
    Navigator.pop(
      context,
      ExperienceAdditionalInfo(
        categoryId: widget.initialValue.categoryId,
        categoryParentId: widget.initialValue.categoryParentId,
        categoryName: widget.initialValue.categoryName,
        categoryParentName: widget.initialValue.categoryParentName,
        location: _location.text.trim(),
        startDate: _startDate.text.trim(),
        endDate: _endDate.text.trim(),
        count: count,
        role: _role.text.trim(),
        ageRange: _ageRange,
        education: _education,
        job: _job.text.trim(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('补充更多信息')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 110),
        children: [
          Container(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 18),
            decoration: BoxDecoration(
              color: theme.colorScheme.surface,
              borderRadius: BorderRadius.circular(18),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('以下内容均为选填', style: theme.textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(
                  '填写与你经历发生时相符的信息，方便他人判断是否适合向你询问。',
                  style: theme.textTheme.bodySmall?.copyWith(height: 1.45),
                ),
                const SizedBox(height: 20),
                _TextInfoField(
                  number: 1,
                  label: '发生地点',
                  controller: _location,
                  hintText: '例如：北京市朝阳区',
                  maxLength: 20,
                ),
                _TextInfoField(
                  number: 2,
                  label: '开始时间',
                  controller: _startDate,
                  hintText: '例如：2024年3月2日',
                  maxLength: 20,
                ),
                _TextInfoField(
                  number: 3,
                  label: '结束时间',
                  controller: _endDate,
                  hintText: '例如：2024年3月2日',
                  maxLength: 20,
                ),
                _TextInfoField(
                  number: 4,
                  label: '已经历的次数',
                  controller: _count,
                  hintText: '请输入次数',
                  suffixText: '次',
                  maxLength: 2,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
                _TextInfoField(
                  number: 5,
                  label: '本人当时的身份',
                  controller: _role,
                  hintText: '例如：房主、租客、员工',
                  maxLength: 7,
                ),
                _SelectInfoField(
                  number: 6,
                  label: '当时年龄段',
                  value: _ageRange,
                  options: _ageRanges,
                  onChanged: (value) => setState(() => _ageRange = value),
                ),
                _SelectInfoField(
                  number: 7,
                  label: '当时学历',
                  value: _education,
                  options: _educations,
                  onChanged: (value) => setState(() => _education = value),
                ),
                _TextInfoField(
                  number: 8,
                  label: '当时职业',
                  controller: _job,
                  hintText: '例如：销售、学生、个体经营者',
                  maxLength: 12,
                  bottomSpacing: 0,
                ),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(10, 8, 10, 12),
        child: FilledButton(onPressed: _save, child: const Text('保存')),
      ),
    );
  }
}

class _TextInfoField extends StatelessWidget {
  const _TextInfoField({
    required this.number,
    required this.label,
    required this.controller,
    required this.hintText,
    required this.maxLength,
    this.suffixText,
    this.keyboardType,
    this.inputFormatters,
    this.bottomSpacing = 18,
  });

  final int number;
  final String label;
  final TextEditingController controller;
  final String hintText;
  final int maxLength;
  final String? suffixText;
  final TextInputType? keyboardType;
  final List<TextInputFormatter>? inputFormatters;
  final double bottomSpacing;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: bottomSpacing),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _FieldTitle(number: number, label: label),
          const SizedBox(height: 7),
          TextField(
            controller: controller,
            maxLength: maxLength,
            keyboardType: keyboardType,
            inputFormatters:
                inputFormatters ?? AppInputFormatters.description(maxLength),
            decoration: InputDecoration(
              hintText: hintText,
              suffixText: suffixText,
              counterText: '',
            ),
          ),
        ],
      ),
    );
  }
}

class _SelectInfoField extends StatelessWidget {
  const _SelectInfoField({
    required this.number,
    required this.label,
    required this.value,
    required this.options,
    required this.onChanged,
  });

  final int number;
  final String label;
  final String value;
  final List<String> options;
  final ValueChanged<String> onChanged;

  Future<void> _select(BuildContext context) async {
    final selected = await showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: ListView(
          shrinkWrap: true,
          padding: const EdgeInsets.fromLTRB(10, 0, 10, 12),
          children: [
            ListTile(
              title: const Text('不填写'),
              trailing: value.isEmpty
                  ? Icon(
                      Icons.check_rounded,
                      color: Theme.of(sheetContext).colorScheme.primary,
                    )
                  : null,
              onTap: () => Navigator.pop(sheetContext, ''),
            ),
            for (final option in options)
              ListTile(
                title: Text(option),
                trailing: value == option
                    ? Icon(
                        Icons.check_rounded,
                        color: Theme.of(sheetContext).colorScheme.primary,
                      )
                    : null,
                onTap: () => Navigator.pop(sheetContext, option),
              ),
          ],
        ),
      ),
    );
    if (selected != null) onChanged(selected);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _FieldTitle(number: number, label: label),
          const SizedBox(height: 7),
          InkWell(
            borderRadius: BorderRadius.circular(12),
            onTap: () => _select(context),
            child: InputDecorator(
              decoration: const InputDecoration(
                suffixIcon: Icon(Icons.keyboard_arrow_down_rounded),
              ),
              child: Text(
                value.isEmpty ? '请选择' : value,
                style: value.isEmpty
                    ? Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: Theme.of(context).hintColor,
                      )
                    : Theme.of(context).textTheme.bodyMedium,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _FieldTitle extends StatelessWidget {
  const _FieldTitle({required this.number, required this.label});

  final int number;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Text(
      '$number. $label',
      style: Theme.of(context).textTheme.titleMedium,
    );
  }
}

class ExperienceAdditionalInfoCard extends StatelessWidget {
  const ExperienceAdditionalInfoCard({
    super.key,
    required this.value,
    this.title = '更多信息',
    this.showCategory = true,
  });

  final ExperienceAdditionalInfo value;
  final String title;
  final bool showCategory;

  @override
  Widget build(BuildContext context) {
    final items = <(String, String)>[
      if (showCategory && value.categoryName.isNotEmpty)
        ('经历分类', '${value.categoryParentName} / ${value.categoryName}'),
      if (value.location.isNotEmpty) ('发生地点', value.location),
      if (value.startDate.isNotEmpty) ('开始时间', value.startDate),
      if (value.endDate.isNotEmpty) ('结束时间', value.endDate),
      if (value.count != null) ('已经历的次数', '${value.count}次'),
      if (value.role.isNotEmpty) ('本人当时的身份', value.role),
      if (value.ageRange.isNotEmpty) ('当时年龄段', value.ageRange),
      if (value.education.isNotEmpty) ('当时学历', value.education),
      if (value.job.isNotEmpty) ('当时职业', value.job),
    ];
    if (items.isEmpty) return const SizedBox.shrink();
    final theme = Theme.of(context);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 8),
          for (final item in items)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: 116,
                    child: Text(item.$1, style: theme.textTheme.bodySmall),
                  ),
                  Expanded(
                    child: Text(
                      item.$2,
                      textAlign: TextAlign.right,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
