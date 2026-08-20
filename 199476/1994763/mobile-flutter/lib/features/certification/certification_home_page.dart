import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';

class CertificationHomePage extends ConsumerStatefulWidget {
  const CertificationHomePage({super.key});
  @override
  ConsumerState<CertificationHomePage> createState() =>
      _CertificationHomePageState();
}

class _CertificationHomePageState extends ConsumerState<CertificationHomePage> {
  List<CertificationRecord> _items = const [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final itemsRequest = ref.read(repositoryProvider).certifications();
      final userRequest = ref.read(authControllerProvider).refreshUser();
      final items = await itemsRequest;
      await userRequest;
      if (mounted) setState(() => _items = items);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<bool> _editPriceRange({bool showSuccessMessage = true}) async {
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
    if (saved == true && mounted && showSuccessMessage) {
      AppMessage.show(context, '可接受金额已保存');
    }
    return saved == true;
  }

  bool get _joined {
    final identity = _items.any(
      (item) => item.type == 'IDENTITY' && item.approved && item.enabled,
    );
    final job = _items.any(
      (item) => item.type == 'MAIN_JOB' && item.approved && item.enabled,
    );
    return identity && job;
  }

  Future<void> _toggleAccepting(bool value) async {
    try {
      final latest = await ref.read(authControllerProvider).refreshUser();
      if (!mounted) return;
      if (latest.answererStatus.toUpperCase() != 'APPROVED' && !_joined) {
        AppMessage.show(context, '完成基础信息认证后才能接受询问');
        return;
      }
      if (value && latest.inquiryPriceUpdatedAt == null) {
        final saved = await _editPriceRange(showSuccessMessage: false);
        if (!saved || !mounted) return;
      }
      final updated = await ref
          .read(repositoryProvider)
          .setAcceptingInquiries(value);
      ref.read(authControllerProvider).replaceUser(updated);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user;
    final accepting = user?.acceptingInquiries ?? false;
    return Scaffold(
      appBar: AppBar(title: Text(_joined ? '答主信息' : '成为答主')),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 30),
                children: [
                  Container(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 18),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [Color(0xFF3A3030), Color(0xFF241F20)],
                      ),
                      borderRadius: BorderRadius.circular(22),
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 46,
                          height: 46,
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: .08),
                            borderRadius: BorderRadius.circular(14),
                          ),
                          child: const Icon(
                            Icons.record_voice_over_outlined,
                            color: Color(0xFFE7C88F),
                          ),
                        ),
                        const SizedBox(width: 13),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _joined ? '管理你的答主信息' : '完成认证，帮人帮己',
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 16,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              const SizedBox(height: 5),
                              Text(
                                _joined ? '设置是否接受新的询问' : '认证真实身份和工作信息',
                                style: const TextStyle(
                                  color: Color(0xFFBEB7B4),
                                  fontSize: 10,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    _joined ? '答主设置' : '完成以下认证',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 8),
                  Card(
                    child: Column(
                      children: [
                        if (_joined) ...[
                          SwitchListTile(
                            contentPadding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 7,
                            ),
                            secondary: const Icon(
                              Icons.chat_bubble_outline_rounded,
                            ),
                            title: const Text('接受新询问'),
                            subtitle: Text(
                              accepting
                                  ? '其他人可以向你发起询问 · 每6小时可切换一次'
                                  : '暂停后不会收到新的询问 · 每6小时可切换一次',
                            ),
                            value: accepting,
                            onChanged: _toggleAccepting,
                          ),
                          const SizedBox(height: 4),
                          ListTile(
                            contentPadding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 7,
                            ),
                            leading: const Icon(Icons.payments_outlined),
                            title: const Text('可接受金额'),
                            subtitle: Text(
                              user?.inquiryPriceUpdatedAt == null
                                  ? '尚未设置 · 每3个月可调整一次'
                                  : '¥${user?.inquiryPriceMin ?? 1}—¥${user?.inquiryPriceMax ?? 5000} · 每3个月可调整一次',
                            ),
                            trailing: const Icon(Icons.chevron_right_rounded),
                            onTap: _editPriceRange,
                          ),
                          const SizedBox(height: 4),
                        ],
                        ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 7,
                          ),
                          leading: const Icon(Icons.badge_outlined),
                          title: const Text('基础信息'),
                          subtitle: const Text('身份信息和我的岗位'),
                          trailing: const Icon(Icons.chevron_right_rounded),
                          onTap: () =>
                              context.push('/profile/certifications/basic'),
                        ),
                      ],
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
              '别人发起询问时，金额需要在这个范围内。',
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
                child: _saving
                    ? const SizedBox.square(
                        dimension: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Text('保存'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
