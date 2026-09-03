import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class InvitationRewardDialog extends StatefulWidget {
  const InvitationRewardDialog({super.key});

  @override
  State<InvitationRewardDialog> createState() =>
      _InvitationRewardDialogState();
}

class _InvitationRewardDialogState extends State<InvitationRewardDialog> {
  final _uidController = TextEditingController();
  final _phoneController = TextEditingController();

  @override
  void dispose() {
    _uidController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final valid =
        RegExp(r'^\d{7}$').hasMatch(_uidController.text.trim()) &&
        RegExp(r'^1[3-9]\d{9}$').hasMatch(_phoneController.text.trim());

    return Dialog(
      insetPadding: const EdgeInsets.symmetric(horizontal: 22, vertical: 24),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420),
        child: SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(
            20,
            10,
            20,
            20 + MediaQuery.viewInsetsOf(context).bottom,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      '邀请得奖金',
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(context),
                    tooltip: '关闭',
                    visualDensity: VisualDensity.compact,
                    icon: const Icon(Icons.close_rounded),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: scheme.primary.withValues(alpha: .07),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _RuleLine(text: '邀请码就是受邀好友的 UID'),
                    _RuleLine(text: '领取时还需填写好友的注册手机号'),
                    _RuleLine(text: '好友有审核通过的公益分享，奖励 2 元'),
                    _RuleLine(text: '好友有审核通过的干货变现，奖励 5 元'),
                    _RuleLine(text: '同一个 UID 只能领取一次，最高奖励 7 元'),
                  ],
                ),
              ),
              const SizedBox(height: 18),
              Text(
                '受邀好友的 UID',
                style: Theme.of(
                  context,
                ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: _uidController,
                autofocus: true,
                keyboardType: TextInputType.number,
                textInputAction: TextInputAction.done,
                inputFormatters: [
                  FilteringTextInputFormatter.digitsOnly,
                  LengthLimitingTextInputFormatter(7),
                ],
                onChanged: (_) => setState(() {}),
                onSubmitted: (_) {
                  FocusScope.of(context).nextFocus();
                },
                decoration: const InputDecoration(
                  hintText: '请输入7位 UID',
                  counterText: '',
                ),
              ),
              const SizedBox(height: 14),
              Text(
                '受邀好友的注册手机号',
                style: Theme.of(
                  context,
                ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                textInputAction: TextInputAction.done,
                inputFormatters: [
                  FilteringTextInputFormatter.digitsOnly,
                  LengthLimitingTextInputFormatter(11),
                ],
                onChanged: (_) => setState(() {}),
                onSubmitted: (_) {
                  if (valid) {
                    Navigator.pop(
                      context,
                      InvitationRewardInput(
                        uid: _uidController.text.trim(),
                        phone: _phoneController.text.trim(),
                      ),
                    );
                  }
                },
                decoration: const InputDecoration(
                  hintText: '请输入11位手机号',
                  counterText: '',
                ),
              ),
              const SizedBox(height: 18),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: valid
                      ? () => Navigator.pop(
                          context,
                          InvitationRewardInput(
                            uid: _uidController.text.trim(),
                            phone: _phoneController.text.trim(),
                          ),
                        )
                      : null,
                  child: const Text('领取奖金'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class InvitationRewardInput {
  const InvitationRewardInput({required this.uid, required this.phone});

  final String uid;
  final String phone;
}

class _RuleLine extends StatelessWidget {
  const _RuleLine({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
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
              color: Theme.of(context).colorScheme.primary,
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
