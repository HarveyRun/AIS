import 'package:flutter/material.dart';

import '../../core/widgets/app_message.dart';

enum EndedInquiryAction { complaint, block }

Future<EndedInquiryAction?> showEndedInquiryActions(BuildContext context) {
  return showDialog<EndedInquiryAction>(
    context: context,
    builder: (context) => AlertDialog(
      titlePadding: const EdgeInsets.fromLTRB(24, 14, 10, 8),
      title: Row(
        children: [
          const Expanded(child: Text('请选择操作')),
          IconButton(
            onPressed: () => Navigator.pop(context),
            tooltip: '关闭',
            icon: const Icon(Icons.close_rounded),
          ),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Expanded(
                  child: _ActionItem(
                    icon: Icons.report_outlined,
                    label: '投诉',
                    onTap: () =>
                        Navigator.pop(context, EndedInquiryAction.complaint),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _ActionItem(
                    icon: Icons.block_rounded,
                    label: '拉黑',
                    onTap: () =>
                        Navigator.pop(context, EndedInquiryAction.block),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    ),
  );
}

class _ActionItem extends StatelessWidget {
  const _ActionItem({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Theme.of(context).colorScheme.surfaceContainer,
      borderRadius: BorderRadius.circular(12),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, color: Theme.of(context).colorScheme.onSurfaceVariant),
              const SizedBox(height: 7),
              Text(label),
            ],
          ),
        ),
      ),
    );
  }
}

Future<({String category, String content})?> showInquiryComplaintDialog(
  BuildContext context,
) {
  return showDialog<({String category, String content})>(
    context: context,
    barrierDismissible: false,
    builder: (_) => const _InquiryComplaintDialog(),
  );
}

Future<bool> showBlockInquiryConfirmation(
  BuildContext context,
  String otherName,
) async {
  final confirmed = await showDialog<bool>(
    context: context,
    barrierDismissible: false,
    builder: (context) => AlertDialog(
      titlePadding: const EdgeInsets.fromLTRB(24, 14, 10, 8),
      title: Row(
        children: [
          const Expanded(child: Text('确认拉黑')),
          IconButton(
            onPressed: () => Navigator.pop(context, false),
            tooltip: '关闭',
            icon: const Icon(Icons.close_rounded),
          ),
        ],
      ),
      content: Text(
        '拉黑${otherName.isEmpty ? '对方' : otherName}后，双方将无法再向对方发起询问。拉黑后不能恢复。',
      ),
      actions: [
        FilledButton(
          onPressed: () => Navigator.pop(context, true),
          child: const Text('确认拉黑'),
        ),
      ],
    ),
  );
  return confirmed == true;
}

class _InquiryComplaintDialog extends StatefulWidget {
  const _InquiryComplaintDialog();

  @override
  State<_InquiryComplaintDialog> createState() =>
      _InquiryComplaintDialogState();
}

class _InquiryComplaintDialogState extends State<_InquiryComplaintDialog> {
  static const _categories = ['服务态度问题', '虚假信息', '骚扰或不当言论', '其他问题'];

  final _contentController = TextEditingController();
  String? _category;

  @override
  void dispose() {
    _contentController.dispose();
    super.dispose();
  }

  void _submit() {
    final content = _contentController.text.trim();
    if (_category == null) {
      AppMessage.show(context, '请选择投诉类型');
      return;
    }
    if (content.isEmpty) {
      AppMessage.show(context, '请说明具体情况');
      return;
    }
    Navigator.pop(context, (category: _category!, content: content));
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      child: AlertDialog(
        insetPadding: const EdgeInsets.symmetric(horizontal: 24),
        titlePadding: const EdgeInsets.fromLTRB(24, 14, 10, 8),
        title: Row(
          children: [
            const Expanded(child: Text('确认投诉')),
            IconButton(
              onPressed: () => Navigator.pop(context),
              tooltip: '关闭',
              icon: const Icon(Icons.close_rounded),
            ),
          ],
        ),
        content: SingleChildScrollView(
          child: SizedBox(
            width: double.maxFinite,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _categories
                      .map((category) {
                        final selected = _category == category;
                        return ChoiceChip(
                          label: Text(category),
                          selected: selected,
                          showCheckmark: false,
                          onSelected: (_) =>
                              setState(() => _category = category),
                        );
                      })
                      .toList(growable: false),
                ),
                const SizedBox(height: 14),
                TextField(
                  controller: _contentController,
                  maxLength: 500,
                  minLines: 3,
                  maxLines: 5,
                  decoration: const InputDecoration(
                    hintText: '请说明具体情况',
                    counterText: '',
                  ),
                ),
              ],
            ),
          ),
        ),
        actions: [FilledButton(onPressed: _submit, child: const Text('确认投诉'))],
      ),
    );
  }
}
