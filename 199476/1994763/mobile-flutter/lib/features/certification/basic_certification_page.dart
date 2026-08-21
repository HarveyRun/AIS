import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/network/realtime_service.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';
import '../../data/models/invitation_models.dart';
import 'certification_notice_card.dart';
import 'invitation_code_dialog.dart';

class BasicCertificationPage extends ConsumerStatefulWidget {
  const BasicCertificationPage({super.key});
  @override
  ConsumerState<BasicCertificationPage> createState() =>
      _BasicCertificationPageState();
}

class _BasicCertificationPageState extends ConsumerState<BasicCertificationPage>
    with WidgetsBindingObserver {
  List<CertificationRecord> _items = const [];
  InvitationCampaignStatus? _invitation;
  JobCertificationAppointment? _jobAppointment;
  StreamSubscription<RealtimeEvent>? _realtimeSubscription;
  bool _loading = true;
  bool _requesting = false;
  bool _openingInvitation = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _realtimeSubscription = ref.read(realtimeProvider).events.listen((event) {
      final certificationType = event.payload['type']?.toString();
      final certificationUpdated =
          event.type == 'CERTIFICATION_UPDATED' &&
          (certificationType == 'IDENTITY' || certificationType == 'MAIN_JOB');
      final appointmentUpdated =
          event.type == 'NOTIFICATION_CREATED' &&
          event.payload['targetPath']?.toString() ==
              '/profile/certifications/basic';
      if (certificationUpdated || appointmentUpdated) {
        unawaited(_load(silent: true));
      }
    });
    _load();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_load(silent: true));
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _realtimeSubscription?.cancel();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    if (_requesting) return;
    _requesting = true;
    if (!silent && mounted) setState(() => _loading = true);
    try {
      final repository = ref.read(repositoryProvider);
      final results = await Future.wait<Object?>([
        repository.certifications(),
        repository.invitationCampaignStatus(),
        repository.currentJobCertificationAppointment(),
      ]);
      if (mounted) {
        setState(() {
          _items = results[0] as List<CertificationRecord>;
          _invitation = results[1] as InvitationCampaignStatus;
          _jobAppointment = results[2] as JobCertificationAppointment?;
        });
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      _requesting = false;
      if (!silent && mounted) setState(() => _loading = false);
    }
  }

  CertificationRecord? _find(String type) {
    for (final item in _items.reversed) {
      if (item.type == type) return item;
    }
    return null;
  }

  bool get _basicCertificationCompleted =>
      _find('IDENTITY')?.approved == true &&
      _find('MAIN_JOB')?.approved == true;

  Future<void> _open(String type) async {
    final record = _find(type);
    await context.push(
      '/profile/certifications/basic/$type/apply',
      extra: record,
    );
    await _load();
  }

  Future<void> _openInvitation() async {
    final invitation = _invitation;
    if (invitation == null || !invitation.active || _openingInvitation) return;
    if (invitation.submitted) {
      AppMessage.show(context, '邀请码已填写，确认后无法更改');
      return;
    }

    final continueToFill = await showDialog<bool>(
      context: context,
      builder: (context) =>
          InvitationRulesDialog(rewardAmount: invitation.rewardAmount),
    );
    if (continueToFill != true || !mounted) return;

    setState(() => _openingInvitation = true);
    try {
      final repository = ref.read(repositoryProvider);
      final latest = await repository.invitationCampaignStatus();
      if (!mounted) return;
      setState(() => _invitation = latest);

      if (!latest.active) {
        AppMessage.show(context, '活动已结束');
        return;
      }
      if (latest.submitted) {
        AppMessage.show(context, '邀请码已填写，确认后无法更改');
        return;
      }
      if (!latest.eligible) {
        AppMessage.show(context, '完成实名认证和岗位认证后才可以填写邀请码');
        return;
      }

      final input = await showDialog<InvitationCodeInput>(
        context: context,
        builder: (context) => const InvitationCodeDialog(),
      );
      if (input == null || !mounted) return;

      final result = await ref
          .read(repositoryProvider)
          .bindInvitationCode(input.code, input.inviterRealName);
      if (!mounted) return;
      setState(() => _invitation = result);
      AppMessage.show(
        context,
        '已提交审核，通过后对方将获得${_formatAmount(result.rewardAmount)}元红包',
      );
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _openingInvitation = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      centerTitle: false,
      titleSpacing: 0,
      title: const Text('基础认证'),
      actions: [
        if (_invitation?.active == true)
          TextButton(
            onPressed: _loading || _openingInvitation ? null : _openInvitation,
            child: Text(
              _invitation?.submitted == true
                  ? _invitationStatusLabel(_invitation!.status)
                  : '填写邀请码',
            ),
          ),
        const SizedBox(width: 4),
      ],
    ),
    body: _loading
        ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
        : ListView(
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
            children: [
              if (!_basicCertificationCompleted) ...[
                const CertificationNoticeCard(
                  tone: CertificationNoticeTone.warning,
                  label: '审核要求',
                  items: [
                    CertificationNoticeItem(value: '年满25周岁', label: '年龄'),
                    CertificationNoticeItem(value: '5年以上', label: '岗位经验'),
                  ],
                ),
                const SizedBox(height: 12),
              ],
              Card(
                child: Column(
                  children: [
                    _BasicRow(
                      icon: Icons.contact_page_outlined,
                      title: '实名认证',
                      subtitle: '身份证正面、反面和手持身份证',
                      record: _find('IDENTITY'),
                      onTap: () => _open('IDENTITY'),
                    ),
                    const SizedBox(height: 10),
                    _BasicRow(
                      icon: Icons.work_outline_rounded,
                      title: '我的岗位',
                      subtitle: '支持线上或线下两种方式',
                      record: _find('MAIN_JOB'),
                      statusOverride:
                          _jobAppointment?.status.toUpperCase() == 'BOOKED'
                          ? 'BOOKED'
                          : null,
                      onTap: () => _open('MAIN_JOB'),
                    ),
                  ],
                ),
              ),
            ],
          ),
  );
}

String _formatAmount(double value) {
  if (value == value.roundToDouble()) return value.toInt().toString();
  return value
      .toStringAsFixed(2)
      .replaceFirst(RegExp(r'0+$'), '')
      .replaceFirst(RegExp(r'\.$'), '');
}

String _invitationStatusLabel(String status) {
  return switch (status.toUpperCase()) {
    'PENDING' => '邀请审核中',
    'APPROVED' => '邀请已通过',
    'REJECTED' => '邀请未通过',
    _ => '已填写邀请码',
  };
}

class _BasicRow extends StatelessWidget {
  const _BasicRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.record,
    required this.onTap,
    this.statusOverride,
  });
  final IconData icon;
  final String title;
  final String subtitle;
  final CertificationRecord? record;
  final VoidCallback onTap;
  final String? statusOverride;
  @override
  Widget build(BuildContext context) {
    final rawStatus = statusOverride ?? record?.status ?? '';
    final style = appStatusStyle(context, rawStatus);
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 7),
      leading: Container(
        width: 42,
        height: 42,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Icon(icon, color: Theme.of(context).colorScheme.primary),
      ),
      title: Row(children: [Text(title)]),
      subtitle: Text(subtitle),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            _status(record, statusOverride),
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: style.foreground,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(width: 6),
          const Icon(Icons.chevron_right_rounded, size: 19),
        ],
      ),
      onTap: onTap,
    );
  }

  String _status(CertificationRecord? item, String? override) =>
      switch ((override ?? item?.status ?? '').toUpperCase()) {
        'BOOKED' => '已预约',
        'PENDING' => '审核中',
        'APPROVED' => '已认证',
        'REJECTED' => '未通过',
        _ => '未认证',
      };
}
