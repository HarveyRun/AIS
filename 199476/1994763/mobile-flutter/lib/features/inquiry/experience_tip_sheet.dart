import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app/providers.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/app_global_settings.dart';

Future<int?> showExperienceTipSheet(
  BuildContext context, {
  required int certificationId,
  required String experienceTitle,
}) {
  return showModalBottomSheet<int>(
    context: context,
    isScrollControlled: true,
    builder: (context) => _ExperienceTipSheet(
      certificationId: certificationId,
      experienceTitle: experienceTitle,
    ),
  );
}

class _ExperienceTipSheet extends ConsumerStatefulWidget {
  const _ExperienceTipSheet({
    required this.certificationId,
    required this.experienceTitle,
  });

  final int certificationId;
  final String experienceTitle;

  @override
  ConsumerState<_ExperienceTipSheet> createState() =>
      _ExperienceTipSheetState();
}

class _ExperienceTipSheetState extends ConsumerState<_ExperienceTipSheet> {
  AppGlobalSettings? _settings;
  final _customAmount = TextEditingController();
  final _requestIds = <int, String>{};
  int? _selectedAmount = 1;
  bool _customSelected = false;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    ref
        .read(repositoryProvider)
        .appGlobalSettings()
        .then((value) {
          if (mounted) setState(() => _settings = value);
        })
        .catchError((_) {});
  }

  @override
  void dispose() {
    _customAmount.dispose();
    super.dispose();
  }

  void _selectAmount(int amount) {
    if (_submitting) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _selectedAmount = amount;
      _customSelected = false;
    });
  }

  void _selectCustom() {
    if (_submitting) return;
    setState(() {
      _selectedAmount = null;
      _customSelected = true;
    });
  }

  int? _currentAmount() {
    if (!_customSelected) return _selectedAmount;
    return int.tryParse(_customAmount.text.trim());
  }

  String _requestIdFor(int amount) {
    return _requestIds.putIfAbsent(amount, () {
      final suffix = Random.secure().nextInt(1 << 32).toRadixString(16);
      return 'tip_${DateTime.now().microsecondsSinceEpoch}_$suffix';
    });
  }

  Future<void> _submit() async {
    final amount = _currentAmount();
    final maximum = _settings?.tipMaxAmount ?? 5000;
    if (_settings?.experienceTipEnabled == false) {
      AppMessage.show(context, '打赏功能暂时不可用');
      return;
    }
    if (amount == null || amount < 1 || amount > maximum) {
      AppMessage.show(context, '请输入1—$maximum之间的整数金额');
      return;
    }
    setState(() => _submitting = true);
    try {
      await ref
          .read(repositoryProvider)
          .tipExperience(
            certificationId: widget.certificationId,
            amount: amount,
            requestId: _requestIdFor(amount),
          );
      if (mounted) Navigator.pop(context, amount);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final amounts = _settings?.tipPresetAmounts ?? const <int>[1, 3, 5, 18, 68];
    final maximum = _settings?.tipMaxAmount ?? 5000;
    final feeRate = theme.platform == TargetPlatform.iOS
        ? (_settings?.iosLaborFeeRate ?? .05)
        : (_settings?.androidLaborFeeRate ?? .05);
    final feePercent = feeRate * 100;
    final width = (MediaQuery.sizeOf(context).width - 60) / 3;
    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        18,
        20,
        MediaQuery.viewInsetsOf(context).bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  '打赏',
                  style: theme.textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              IconButton(
                onPressed: _submitting ? null : () => Navigator.pop(context),
                tooltip: '关闭',
                icon: const Icon(Icons.close_rounded),
              ),
            ],
          ),
          Text(
            widget.experienceTitle,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 18),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              for (final amount in amounts)
                _TipAmountItem(
                  width: width,
                  label: '¥$amount',
                  selected: !_customSelected && _selectedAmount == amount,
                  onTap: () => _selectAmount(amount),
                ),
              _TipAmountItem(
                width: width,
                label: '自定义',
                selected: _customSelected,
                onTap: _selectCustom,
              ),
            ],
          ),
          if (_customSelected) ...[
            const SizedBox(height: 12),
            TextField(
              controller: _customAmount,
              autofocus: true,
              keyboardType: TextInputType.number,
              inputFormatters: AppInputFormatters.positiveInteger(max: maximum),
              decoration: const InputDecoration(
                prefixText: '¥ ',
                hintText: '请输入打赏金额',
              ),
            ),
          ],
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _submitting ? null : _submit,
              child: const Text('确认打赏'),
            ),
          ),
        ],
      ),
    );
  }
}

class _TipAmountItem extends StatelessWidget {
  const _TipAmountItem({
    required this.width,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final double width;
  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Material(
      color: selected ? colors.primary : colors.surfaceContainerHighest,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: SizedBox(
          width: width,
          height: 48,
          child: Center(
            child: Text(
              label,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                color: selected ? colors.onPrimary : colors.onSurface,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
