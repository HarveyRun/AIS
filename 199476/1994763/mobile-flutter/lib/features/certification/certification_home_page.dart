import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
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
      final items = await ref.read(repositoryProvider).certifications();
      if (mounted) setState(() => _items = items);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
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
    final accepting =
        ref.watch(authControllerProvider).user?.acceptingInquiries ?? false;
    return Scaffold(
      appBar: AppBar(title: Text(_joined ? '答主信息' : '成为答主')),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 30),
                children: [
                  Text(
                    _joined ? '答主设置' : '完成以下认证',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 10),
                  Card(
                    child: Column(
                      children: [
                        if (_joined) ...[
                          SwitchListTile(
                            secondary: const Icon(
                              Icons.chat_bubble_outline_rounded,
                            ),
                            title: const Text('接受新询问'),
                            subtitle: Text(
                              accepting ? '其他人可以向你发起询问' : '暂停后不会收到新的询问',
                            ),
                            value: accepting,
                            onChanged: _toggleAccepting,
                          ),
                          const Divider(height: 1, indent: 56),
                        ],
                        ListTile(
                          leading: const Icon(Icons.badge_outlined),
                          title: const Text('基础信息'),
                          subtitle: const Text('身份信息和我的岗位'),
                          trailing: const Icon(Icons.chevron_right_rounded),
                          onTap: () =>
                              context.push('/profile/certifications/basic'),
                        ),
                        const Divider(height: 1, indent: 56),
                        ListTile(
                          leading: const Icon(Icons.route_outlined),
                          title: const Text('亲身经历'),
                          subtitle: const Text('亲自经历过的事情'),
                          trailing: const Icon(Icons.chevron_right_rounded),
                          onTap: () => context.push(
                            '/profile/certifications/experiences',
                          ),
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
