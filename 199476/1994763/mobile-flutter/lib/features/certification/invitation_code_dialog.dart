import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class InvitationCodeDialog extends StatefulWidget {
  const InvitationCodeDialog({required this.rewardAmount, super.key});

  final double rewardAmount;

  @override
  State<InvitationCodeDialog> createState() => _InvitationCodeDialogState();
}

class _InvitationCodeDialogState extends State<InvitationCodeDialog> {
  final _codeController = TextEditingController();
  final _nameController = TextEditingController();

  @override
  void dispose() {
    _codeController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  void _submit() {
    final code = _codeController.text.trim();
    final realName = _nameController.text.trim();
    if (!RegExp(r'^\d{7}$').hasMatch(code) || !_validName(realName)) {
      setState(() {});
      return;
    }
    Navigator.pop(
      context,
      InvitationCodeInput(code: code, inviterRealName: realName),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final reward = _formatAmount(widget.rewardAmount);
    final valid =
        RegExp(r'^\d{7}$').hasMatch(_codeController.text.trim()) &&
        _validName(_nameController.text.trim());

    return Dialog(
      insetAnimationDuration: Duration.zero,
      insetPadding: const EdgeInsets.symmetric(horizontal: 22, vertical: 30),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 10, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Align(
                alignment: Alignment.centerRight,
                child: IconButton(
                  onPressed: () => Navigator.pop(context),
                  tooltip: '关闭',
                  visualDensity: VisualDensity.compact,
                  icon: const Icon(Icons.close_rounded),
                ),
              ),
              Text(
                '填写邀请码',
                style: Theme.of(
                  context,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 14),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(15),
                decoration: BoxDecoration(
                  color: scheme.primary.withValues(alpha: .08),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const _NoticeLine(text: '邀请码就是对方的 UID'),
                    const _NoticeLine(text: '填写确认后无法更改'),
                    const _NoticeLine(text: '你和对方均需通过实名和岗位认证'),
                    _NoticeLine(text: '每成功邀请 1 人得 $reward 元红包，人数不限'),
                  ],
                ),
              ),
              const SizedBox(height: 18),
              TextField(
                controller: _codeController,
                autofocus: true,
                keyboardType: TextInputType.number,
                textInputAction: TextInputAction.done,
                inputFormatters: [
                  FilteringTextInputFormatter.digitsOnly,
                  LengthLimitingTextInputFormatter(7),
                ],
                onChanged: (_) => setState(() {}),
                onSubmitted: (_) {
                  if (valid) _submit();
                },
                decoration: const InputDecoration(
                  hintText: '请输入对方的7位 UID',
                  counterText: '',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _nameController,
                keyboardType: TextInputType.name,
                textInputAction: TextInputAction.done,
                inputFormatters: [LengthLimitingTextInputFormatter(30)],
                onChanged: (_) => setState(() {}),
                onSubmitted: (_) {
                  if (valid) _submit();
                },
                decoration: const InputDecoration(
                  hintText: '请输入对方真实姓名',
                  counterText: '',
                ),
              ),
              const SizedBox(height: 18),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: valid ? _submit : null,
                  child: const Text('确认填写'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class InvitationCodeInput {
  const InvitationCodeInput({
    required this.code,
    required this.inviterRealName,
  });

  final String code;
  final String inviterRealName;
}

class _NoticeLine extends StatelessWidget {
  const _NoticeLine({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 5,
            height: 5,
            margin: const EdgeInsets.only(top: 7, right: 9),
            decoration: BoxDecoration(
              color: scheme.primary,
              shape: BoxShape.circle,
            ),
          ),
          Expanded(
            child: Text(
              text,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                height: 1.45,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

String _formatAmount(double value) {
  if (value == value.roundToDouble()) return value.toInt().toString();
  return value
      .toStringAsFixed(2)
      .replaceFirst(RegExp(r'0+$'), '')
      .replaceFirst(RegExp(r'\.$'), '');
}

bool _validName(String value) {
  return value.length >= 2 &&
      value.length <= 30 &&
      RegExp(r"^[A-Za-z\u4e00-\u9fa5·•.\- '’]+$").hasMatch(value);
}
