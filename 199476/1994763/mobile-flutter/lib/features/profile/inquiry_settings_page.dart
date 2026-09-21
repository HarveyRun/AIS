import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/app_global_settings.dart';

class InquirySettingsPage extends ConsumerStatefulWidget {
  const InquirySettingsPage({super.key});

  @override
  ConsumerState<InquirySettingsPage> createState() =>
      _InquirySettingsPageState();
}

class _InquirySettingsPageState extends ConsumerState<InquirySettingsPage> {
  bool _loading = true;
  AppGlobalSettings? _settings;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final values = await Future.wait([
        ref.read(authControllerProvider).refreshUser(),
        ref.read(repositoryProvider).appGlobalSettings(),
      ]);
      _settings = values[1] as AppGlobalSettings;
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<bool> _editHourlyRate({bool showSaved = true}) async {
    final user = ref.read(authControllerProvider).user;
    if (user == null) return false;
    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (context) =>
          _HourlyRateSheet(
            hourlyRate: user.inquiryHourlyRate,
            minimum: _settings?.hourlyRateMin ?? 1,
            maximum: _settings?.hourlyRateMax ?? 5000,
          ),
    );
    if (saved == true && mounted && showSaved) {
      AppMessage.show(context, '每小时费用已保存');
    }
    return saved == true;
  }

  Future<void> _handleHourlyRateTap() async {
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
      await _editHourlyRate();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _toggleAccepting(bool value) async {
    try {
      final latest = await ref.read(authControllerProvider).refreshUser();
      if (!mounted) return;
      if (value && latest.inquiryPriceUpdatedAt == null) {
        final saved = await _editHourlyRate(showSaved: false);
        if (!saved || !mounted) return;
      }
      final updated = await ref
          .read(repositoryProvider)
          .setAcceptingInquiries(value);
      ref.read(authControllerProvider).replaceUser(updated);
      if (mounted) AppMessage.show(context, value ? '已开始接受新询问' : '已暂停接受新询问');
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
                          title: const Text('每小时费用'),
                          subtitle: Text(
                            user.inquiryPriceUpdatedAt == null
                                ? '尚未设置 · 每3个月可调整一次'
                                : '¥${user.inquiryHourlyRate}/小时 · 每3个月可调整一次',
                          ),
                          trailing: const Icon(Icons.chevron_right_rounded),
                          onTap: _handleHourlyRateTap,
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
                                ? '其他用户可以向你发起询问'
                                : '暂停后不会收到新的询问',
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
                      '语音通话接通后按实际通话时间计费。接受状态每6小时可切换一次。',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}

class _HourlyRateSheet extends ConsumerStatefulWidget {
  const _HourlyRateSheet({required this.hourlyRate,required this.minimum,required this.maximum});

  final int hourlyRate;
  final int minimum;
  final int maximum;

  @override
  ConsumerState<_HourlyRateSheet> createState() => _HourlyRateSheetState();
}

class _HourlyRateSheetState extends ConsumerState<_HourlyRateSheet> {
  late final TextEditingController _rate;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _rate = TextEditingController(text: '${widget.hourlyRate}');
  }

  @override
  void dispose() {
    _rate.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final hourlyRate = int.tryParse(_rate.text);
    if (hourlyRate == null || hourlyRate < widget.minimum || hourlyRate > widget.maximum) {
      AppMessage.show(context, '每小时费用须在${widget.minimum}—${widget.maximum}元之间');
      return;
    }
    setState(() => _saving = true);
    try {
      final updated = await ref
          .read(repositoryProvider)
          .setInquiryHourlyRate(hourlyRate);
      ref.read(authControllerProvider).replaceUser(updated);
      if (mounted) Navigator.pop(context, true);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
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
                    '设置每小时费用',
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
              '此费用用于语音通话的时长计费。',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 18),
            TextField(
              controller: _rate,
              keyboardType: TextInputType.number,
              inputFormatters: AppInputFormatters.positiveInteger(max: widget.maximum),
              decoration: InputDecoration(
                prefixText: '¥ ',
                suffixText: '/小时',
                hintText: '${widget.minimum}—${widget.maximum}',
              ),
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
}
