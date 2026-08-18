import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';

class ExperienceCertificationPage extends ConsumerStatefulWidget {
  const ExperienceCertificationPage({super.key});
  @override
  ConsumerState<ExperienceCertificationPage> createState() =>
      _ExperienceCertificationPageState();
}

class _ExperienceCertificationPageState
    extends ConsumerState<ExperienceCertificationPage> {
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
      if (mounted) {
        setState(
          () => _items = items
              .where(
                (item) =>
                    item.category == 'EXPERIENCE' || item.type == 'EXPERIENCE',
              )
              .toList(),
        );
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _add() async {
    try {
      final eligibility = await ref
          .read(repositoryProvider)
          .answererEligibility();
      final eligible =
          eligibility['basicInformationApproved'] == true ||
          (eligibility['identityApproved'] == true &&
              eligibility['jobApproved'] == true);
      if (!eligible) {
        if (mounted) {
          AppMessage.show(
            context,
            eligibility['message']?.toString() ?? '完成基础信息认证后才能添加亲身经历',
          );
        }
        return;
      }
      if (!mounted) return;
      await context.push('/profile/certifications/experiences/new');
      await _load();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('亲身经历'),
      actions: [
        IconButton(
          onPressed: _add,
          icon: const Icon(Icons.add_rounded),
          tooltip: '添加经历',
        ),
      ],
    ),
    body: _loading
        ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
        : RefreshIndicator(
            onRefresh: _load,
            child: _items.isEmpty
                ? ListView(
                    padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
                    children: [
                      const SizedBox(height: 150),
                      const Icon(Icons.route_outlined, size: 44),
                      const SizedBox(height: 12),
                      const Center(child: Text('还没有亲身经历')),
                      const SizedBox(height: 18),
                      Center(
                        child: FilledButton.tonalIcon(
                          onPressed: _add,
                          icon: const Icon(Icons.add_rounded),
                          label: const Text('添加经历'),
                        ),
                      ),
                    ],
                  )
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
                    itemCount: _items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final item = _items[index];
                      final statusStyle = appStatusStyle(context, item.status);
                      return Material(
                        color: Theme.of(context).colorScheme.surface,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                        clipBehavior: Clip.antiAlias,
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 10,
                          ),
                          leading: Container(
                            width: 42,
                            height: 42,
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: Theme.of(
                                context,
                              ).colorScheme.surfaceContainerHighest,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Icon(
                              Icons.route_outlined,
                              color: Theme.of(context).colorScheme.primary,
                            ),
                          ),
                          title: Text(item.title),
                          subtitle: Text(
                            item.description,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                          trailing: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                _status(item),
                                style: Theme.of(context).textTheme.bodySmall
                                    ?.copyWith(
                                      color: statusStyle.foreground,
                                      fontWeight: FontWeight.w700,
                                    ),
                              ),
                              const Icon(Icons.chevron_right_rounded),
                            ],
                          ),
                          onTap: () async {
                            await context.push(
                              '/profile/certifications/experiences/${item.id}',
                            );
                            await _load();
                          },
                        ),
                      );
                    },
                  ),
          ),
  );
  String _status(CertificationRecord item) =>
      switch (item.status.toUpperCase()) {
        'PENDING' => '审核中',
        'APPROVED' => '已认证',
        'REJECTED' => '退回修改',
        _ => item.status,
      };
}
