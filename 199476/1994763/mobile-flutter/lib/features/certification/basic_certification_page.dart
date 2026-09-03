import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/network/realtime_service.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';

class BasicCertificationPage extends ConsumerStatefulWidget {
  const BasicCertificationPage({super.key});

  @override
  ConsumerState<BasicCertificationPage> createState() =>
      _BasicCertificationPageState();
}

class _BasicCertificationPageState extends ConsumerState<BasicCertificationPage>
    with WidgetsBindingObserver {
  List<CertificationRecord> _items = const [];
  StreamSubscription<RealtimeEvent>? _subscription;
  bool _loading = true;
  bool _requesting = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _subscription = ref.read(realtimeProvider).events.listen((event) {
      if (event.type == 'CERTIFICATION_UPDATED' &&
          event.payload['type']?.toString() == 'IDENTITY') {
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
    _subscription?.cancel();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    if (_requesting) return;
    _requesting = true;
    if (!silent && mounted) setState(() => _loading = true);
    try {
      final items = await ref
          .read(repositoryProvider)
          .certifications(showLoading: !silent);
      if (mounted) setState(() => _items = items);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      _requesting = false;
      if (!silent && mounted) setState(() => _loading = false);
    }
  }

  CertificationRecord? get _identity {
    for (final item in _items.reversed) {
      if (item.type == 'IDENTITY') return item;
    }
    return null;
  }

  Future<void> _openIdentity() async {
    await context.push(
      '/profile/certifications/basic/IDENTITY/apply',
      extra: _identity,
    );
    await _load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('实名认证')),
    body: _loading
        ? const SizedBox.shrink()
        : ListView(
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
            children: [
              Card(
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 9,
                  ),
                  leading: Container(
                    width: 42,
                    height: 42,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(
                      Icons.contact_page_outlined,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                  title: const Text('实名认证'),
                  subtitle: const Text('身份证正面、反面和手持身份证'),
                  trailing: _CertificationStatus(record: _identity),
                  onTap: _openIdentity,
                ),
              ),
            ],
          ),
  );
}

class _CertificationStatus extends StatelessWidget {
  const _CertificationStatus({required this.record});
  final CertificationRecord? record;

  @override
  Widget build(BuildContext context) {
    final status = record?.status ?? '';
    final style = appStatusStyle(context, status);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          switch (status.toUpperCase()) {
            'PENDING' => '审核中',
            'APPROVED' => '已通过',
            'REJECTED' => '未通过',
            _ => '去认证',
          },
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: style.foreground,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(width: 2),
        const Icon(Icons.chevron_right_rounded, size: 20),
      ],
    );
  }
}
