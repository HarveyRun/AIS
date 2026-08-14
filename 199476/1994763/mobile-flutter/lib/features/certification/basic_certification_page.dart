import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';

class BasicCertificationPage extends ConsumerStatefulWidget {
  const BasicCertificationPage({super.key});
  @override
  ConsumerState<BasicCertificationPage> createState() =>
      _BasicCertificationPageState();
}

class _BasicCertificationPageState
    extends ConsumerState<BasicCertificationPage> {
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
      final items = await ref.read(repositoryProvider).certifications();
      if (mounted) setState(() => _items = items);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  CertificationRecord? _find(String type) {
    for (final item in _items) {
      if (item.type == type) return item;
    }
    return null;
  }

  Future<void> _open(String type) async {
    await context.push(
      '/profile/certifications/basic/$type/apply',
      extra: _find(type),
    );
    await _load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('基础信息')),
    body: _loading
        ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
        : ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Card(
                child: Column(
                  children: [
                    _BasicRow(
                      title: '实名认证',
                      subtitle: '身份证正面、反面和手持身份证',
                      record: _find('IDENTITY'),
                      onTap: () => _open('IDENTITY'),
                    ),
                    const Divider(height: 1, indent: 16, endIndent: 16),
                    _BasicRow(
                      title: '我的岗位',
                      subtitle: '通过现场录像或照片核实',
                      record: _find('MAIN_JOB'),
                      onTap: () => _open('MAIN_JOB'),
                    ),
                  ],
                ),
              ),
            ],
          ),
  );
}

class _BasicRow extends StatelessWidget {
  const _BasicRow({
    required this.title,
    required this.subtitle,
    required this.record,
    required this.onTap,
  });
  final String title;
  final String subtitle;
  final CertificationRecord? record;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 7),
    title: Row(children: [Text(title)]),
    subtitle: Text(subtitle),
    trailing: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(_status(record), style: Theme.of(context).textTheme.bodySmall),
        const Icon(Icons.chevron_right_rounded),
      ],
    ),
    onTap: onTap,
  );
  String _status(CertificationRecord? item) =>
      switch (item?.status.toUpperCase()) {
        'PENDING' => '审核中',
        'APPROVED' => '已认证',
        'REJECTED' => '退回修改',
        _ => '未认证',
      };
}
