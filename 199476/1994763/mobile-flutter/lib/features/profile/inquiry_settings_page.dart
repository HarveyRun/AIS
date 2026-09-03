import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/app_message.dart';

class InquirySettingsPage extends ConsumerStatefulWidget {
  const InquirySettingsPage({super.key});

  @override
  ConsumerState<InquirySettingsPage> createState() =>
      _InquirySettingsPageState();
}

class _InquirySettingsPageState extends ConsumerState<InquirySettingsPage> {
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      await ref.read(authControllerProvider).refreshUser();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<bool> _editPriceRange({bool showSaved = true}) async {
    final user = ref.read(authControllerProvider).user;
    if (user == null) return false;
    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _InquiryPriceRangeSheet(
        minimum: user.inquiryPriceMin,
        maximum: user.inquiryPriceMax,
      ),
    );
    if (saved == true && mounted && showSaved) {
      AppMessage.show(context, '可接受金额已保存');
    }
    return saved == true;
  }

  Future<void> _handlePriceRangeTap() async {
    try {
      final latest = await ref.read(authControllerProvider).refreshUser();
      if (!mounted) return;
      final updatedAt = latest.inquiryPriceUpdatedAt;
      if (updatedAt != null) {
        final nextAdjustment = _addMonths(updatedAt, 3);
        if (nextAdjustment.isAfter(DateTime.now())) {
          AppMessage.show(
            context,
            '每3个月可调整一次，下次可在${DateFormat('yyyy-MM-dd HH:mm').format(nextAdjustment)}调整',
          );
          return;
        }
      }
      await _editPriceRange();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _toggleAccepting(bool value) async {
    try {
      final latest = await ref.read(authControllerProvider).refreshUser();
      if (!mounted) return;
      if (value && latest.inquiryPriceUpdatedAt == null) {
        final saved = await _editPriceRange(showSaved: false);
        if (!saved || !mounted) return;
      }
      final updated = await ref
          .read(repositoryProvider)
          .setAcceptingInquiries(value);
      ref.read(authControllerProvider).replaceUser(updated);
      if (mounted) {
        AppMessage.show(context, value ? '已开始接受新询问' : '已暂停接受新询问');
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  DateTime _addMonths(DateTime source, int months) {
    final zeroBasedMonth = source.month - 1 + months;
    final year = source.year + zeroBasedMonth ~/ 12;
    final month = zeroBasedMonth % 12 + 1;
    final lastDay = DateTime(year, month + 1, 0).day;
    final day = source.day > lastDay ? lastDay : source.day;
    return DateTime(
      year,
      month,
      day,
      source.hour,
      source.minute,
      source.second,
      source.millisecond,
      source.microsecond,
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user;
    return Scaffold(
      appBar: AppBar(title: const Text('询问设置')),
      body: _loading || user == null
          ? const SizedBox.shrink()
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 30),
                children: [
                  Material(
                    color: Theme.of(context).colorScheme.surface,
                    borderRadius: BorderRadius.circular(18),
                    clipBehavior: Clip.antiAlias,
                    child: Column(
                      children: [
                        ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 8,
                          ),
                          leading: const Icon(Icons.payments_outlined),
                          title: const Text('可接受金额'),
                          subtitle: Text(
                            user.inquiryPriceUpdatedAt == null
                                ? '尚未设置 · 每3个月可调整一次'
                                : '¥${user.inquiryPriceMin}—¥${user.inquiryPriceMax} · 每3个月可调整一次',
                          ),
                          trailing: const Icon(Icons.chevron_right_rounded),
                          onTap: _handlePriceRangeTap,
                        ),
                        SwitchListTile(
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 8,
                          ),
                          secondary: const Icon(
                            Icons.chat_bubble_outline_rounded,
                          ),
                          title: const Text('接受新询问'),
                          subtitle: Text(
                            user.acceptingInquiries
                                ? '其他用户可以向你发起付费询问'
                                : '暂停后不会收到新的付费询问',
                          ),
                          value: user.acceptingInquiries,
                          onChanged: _toggleAccepting,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 10),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    child: Text(
                      '金额每3个月可调整一次；接受状态每6小时可切换一次。',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}

class _InquiryPriceRangeSheet extends ConsumerStatefulWidget {
  const _InquiryPriceRangeSheet({required this.minimum, required this.maximum});

  final int minimum;
  final int maximum;

  @override
  ConsumerState<_InquiryPriceRangeSheet> createState() =>
      _InquiryPriceRangeSheetState();
}

class _InquiryPriceRangeSheetState
    extends ConsumerState<_InquiryPriceRangeSheet> {
  late final TextEditingController _minimum;
  late final TextEditingController _maximum;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _minimum = TextEditingController(text: '${widget.minimum}');
    _maximum = TextEditingController(text: '${widget.maximum}');
  }

  @override
  void dispose() {
    _minimum.dispose();
    _maximum.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final minimum = int.tryParse(_minimum.text);
    final maximum = int.tryParse(_maximum.text);
    if (minimum == null || maximum == null) {
      AppMessage.show(context, '请填写最低和最高金额');
      return;
    }
    if (minimum < 1 || maximum > 5000 || minimum > maximum) {
      AppMessage.show(context, '最低金额不能高于最高金额，且须在1—5000元之间');
      return;
    }
    setState(() => _saving = true);
    try {
      final updated = await ref
          .read(repositoryProvider)
          .setInquiryPriceRange(minimum: minimum, maximum: maximum);
      ref.read(authControllerProvider).replaceUser(updated);
      if (mounted) Navigator.pop(context, true);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        20,
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
                  '设置可接受金额',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.close_rounded),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            '其他用户发起询问时，可在这个范围内填写金额。',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 18),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: TextField(
                  controller: _minimum,
                  keyboardType: TextInputType.number,
                  inputFormatters: AppInputFormatters.positiveInteger(
                    max: 5000,
                  ),
                  decoration: const InputDecoration(
                    labelText: '最低金额',
                    prefixText: '¥ ',
                  ),
                ),
              ),
              const Padding(
                padding: EdgeInsets.fromLTRB(12, 17, 12, 0),
                child: Text('至'),
              ),
              Expanded(
                child: TextField(
                  controller: _maximum,
                  keyboardType: TextInputType.number,
                  inputFormatters: AppInputFormatters.positiveInteger(
                    max: 5000,
                  ),
                  decoration: const InputDecoration(
                    labelText: '最高金额',
                    prefixText: '¥ ',
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _saving ? null : _save,
              child: const Text('保存'),
            ),
          ),
        ],
      ),
    ),
  );
}
