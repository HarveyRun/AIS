import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/support_models.dart';

class FeedbackPage extends ConsumerStatefulWidget {
  const FeedbackPage({super.key});
  @override
  ConsumerState<FeedbackPage> createState() => _FeedbackPageState();
}

class _FeedbackPageState extends ConsumerState<FeedbackPage> {
  final _content = TextEditingController();
  String _type = 'PRODUCT';
  String _complaintCategory = '服务态度问题';
  List<FeedbackRecord> _records = const [];
  bool _loadingRecords = true;
  bool _sending = false;

  static const _complaintCategories = ['服务态度问题', '虚假能力信息', '骚扰或不当言论', '其它'];

  @override
  void initState() {
    super.initState();
    _loadRecords();
  }

  @override
  void dispose() {
    _content.dispose();
    super.dispose();
  }

  Future<void> _loadRecords() async {
    try {
      final records = await ref.read(repositoryProvider).feedbackRecords();
      if (mounted) setState(() => _records = records);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loadingRecords = false);
    }
  }

  Future<void> _submit() async {
    if (_content.text.trim().isEmpty) {
      AppMessage.show(context, '请填写反馈内容');
      return;
    }
    setState(() => _sending = true);
    try {
      await ref
          .read(repositoryProvider)
          .submitFeedback(
            type: _type,
            category: _type == 'PRODUCT' ? '产品反馈' : _complaintCategory,
            content: _content.text.trim(),
          );
      _content.clear();
      await _loadRecords();
      if (mounted) {
        AppMessage.show(context, _type == 'PRODUCT' ? '反馈已提交' : '投诉已提交');
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('投诉与反馈')),
    body: ListView(
      padding: const EdgeInsets.fromLTRB(17, 8, 17, 28),
      children: [
        _FeedbackTabs(
          selected: _type,
          onChanged: (value) => setState(() => _type = value),
        ),
        const SizedBox(height: 16),
        if (_type == 'COMPLAINT') ...[
          DropdownButtonFormField<String>(
            value: _complaintCategory,
            decoration: const InputDecoration(labelText: '投诉类型'),
            items: _complaintCategories
                .map((item) => DropdownMenuItem(value: item, child: Text(item)))
                .toList(),
            onChanged: (value) {
              if (value != null) setState(() => _complaintCategory = value);
            },
          ),
          const SizedBox(height: 12),
        ],
        TextField(
          controller: _content,
          maxLength: 500,
          minLines: 5,
          maxLines: 9,
          decoration: InputDecoration(
            hintText: _type == 'PRODUCT' ? '说说你希望事先问改进什么' : '请说明发生的时间、经过和诉求',
          ),
        ),
        const SizedBox(height: 10),
        FilledButton(
          onPressed: _sending ? null : _submit,
          child: Text(_type == 'PRODUCT' ? '提交反馈' : '提交投诉'),
        ),
        const SizedBox(height: 24),
        if (_loadingRecords)
          const Center(
            child: Padding(
              padding: EdgeInsets.all(20),
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
          )
        else if (_records.isNotEmpty) ...[
          Text('我的提交', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 9),
          Card(
            child: Column(
              children: _records.take(5).map((record) {
                final last = record == _records.take(5).last;
                return Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 13,
                      ),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  record.category,
                                  style: Theme.of(
                                    context,
                                  ).textTheme.titleMedium,
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  record.content,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: Theme.of(context).textTheme.bodySmall,
                                ),
                                if (record.createdAt != null) ...[
                                  const SizedBox(height: 5),
                                  Text(
                                    DateFormat(
                                      'yyyy-MM-dd HH:mm',
                                    ).format(record.createdAt!),
                                    style: Theme.of(
                                      context,
                                    ).textTheme.bodySmall,
                                  ),
                                ],
                              ],
                            ),
                          ),
                          const SizedBox(width: 12),
                          _FeedbackStatus(status: record.status),
                        ],
                      ),
                    ),
                    if (!last) const Divider(height: 1),
                  ],
                );
              }).toList(),
            ),
          ),
        ],
      ],
    ),
  );
}

class _FeedbackTabs extends StatelessWidget {
  const _FeedbackTabs({required this.selected, required this.onChanged});

  final String selected;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 46,
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(13),
      ),
      child: Row(
        children: [
          _FeedbackTab(
            label: '产品反馈',
            selected: selected == 'PRODUCT',
            onTap: () => onChanged('PRODUCT'),
          ),
          const SizedBox(width: 5),
          _FeedbackTab(
            label: '投诉',
            selected: selected == 'COMPLAINT',
            onTap: () => onChanged('COMPLAINT'),
          ),
        ],
      ),
    );
  }
}

class _FeedbackTab extends StatelessWidget {
  const _FeedbackTab({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 160),
          height: 38,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: selected
                ? Theme.of(context).colorScheme.surface
                : Colors.transparent,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Text(
            label,
            style: TextStyle(
              fontSize: 13,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              color: selected ? Theme.of(context).colorScheme.primary : null,
            ),
          ),
        ),
      ),
    );
  }
}

class _FeedbackStatus extends StatelessWidget {
  const _FeedbackStatus({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    const labels = {'SUBMITTED': '待处理', 'PROCESSING': '处理中', 'RESOLVED': '已解决'};
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        labels[status] ?? status,
        style: Theme.of(context).textTheme.bodySmall,
      ),
    );
  }
}
