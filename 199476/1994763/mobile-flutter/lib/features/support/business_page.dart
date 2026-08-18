import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app/providers.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/app_message.dart';

class BusinessPage extends ConsumerStatefulWidget {
  const BusinessPage({super.key});
  @override
  ConsumerState<BusinessPage> createState() => _BusinessPageState();
}

class _BusinessPageState extends ConsumerState<BusinessPage> {
  final _contact = TextEditingController();
  final _content = TextEditingController();
  bool _sending = false;
  @override
  void dispose() {
    _contact.dispose();
    _content.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_contact.text.trim().isEmpty || _content.text.trim().isEmpty) {
      AppMessage.show(context, '请把信息填写完整');
      return;
    }
    setState(() => _sending = true);
    try {
      await ref.read(repositoryProvider).submitBusinessCooperation({
        'contact': _contact.text.trim(),
        'content': _content.text.trim(),
      });
      if (mounted) AppMessage.show(context, '已经提交');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('商务合作')),
    body: ListView(
      padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
      children: [
        Text('联系方式', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 7),
        TextField(
          controller: _contact,
          inputFormatters: AppInputFormatters.description(50),
          decoration: InputDecoration(
            hintText: '请填写手机号、微信或邮箱',
            fillColor: Theme.of(context).colorScheme.surfaceContainerHighest,
          ),
        ),
        const SizedBox(height: 18),
        Text('合作内容', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 7),
        TextField(
          controller: _content,
          inputFormatters: AppInputFormatters.description(500),
          minLines: 6,
          maxLines: 10,
          decoration: InputDecoration(
            hintText: '请简单说明合作想法',
            fillColor: Theme.of(context).colorScheme.surfaceContainerHighest,
          ),
        ),
        const SizedBox(height: 18),
        FilledButton(
          onPressed: _sending ? null : _submit,
          child: const Text('提交'),
        ),
      ],
    ),
  );
}
