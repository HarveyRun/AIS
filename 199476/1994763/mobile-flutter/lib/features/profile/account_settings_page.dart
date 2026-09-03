import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../app/providers.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';
import '../../data/models/user_models.dart';
import '../../data/repositories/app_repository.dart';

class AccountSettingsPage extends ConsumerStatefulWidget {
  const AccountSettingsPage({super.key});
  @override
  ConsumerState<AccountSettingsPage> createState() =>
      _AccountSettingsPageState();
}

class _AccountSettingsPageState extends ConsumerState<AccountSettingsPage> {
  late final TextEditingController _nickname;
  List<ViolationCounter>? _violationCounters;
  CertificationRecord? _identity;
  bool _editingNickname = false;
  bool _savingNickname = false;

  @override
  void initState() {
    super.initState();
    _nickname = TextEditingController(
      text: ref.read(authControllerProvider).user?.nickname ?? '',
    );
    _loadSettingsData();
  }

  Future<void> _loadSettingsData() async {
    try {
      final results = await Future.wait([
        ref.read(repositoryProvider).certifications(),
        ref.read(repositoryProvider).violationCounters(),
      ]);
      if (!mounted) return;
      final certifications = results[0] as List<CertificationRecord>;
      setState(() {
        _identity = _findIdentity(certifications);
        _violationCounters = results[1] as List<ViolationCounter>;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _violationCounters = const []);
      AppMessage.show(context, '$error');
    }
  }

  CertificationRecord? _findIdentity(List<CertificationRecord> certifications) {
    for (final item in certifications) {
      if (item.type == 'IDENTITY') return item;
    }
    return null;
  }

  Future<void> _openIdentityCertification() async {
    try {
      final certifications = await ref
          .read(repositoryProvider)
          .certifications();
      if (!mounted) return;
      final identity = _findIdentity(certifications);
      setState(() => _identity = identity);
      await context.push(
        '/profile/certifications/basic/IDENTITY/apply',
        extra: identity,
      );
      if (!mounted) return;
      final latest = await ref.read(repositoryProvider).certifications();
      if (mounted) setState(() => _identity = _findIdentity(latest));
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  void dispose() {
    _nickname.dispose();
    super.dispose();
  }

  Future<void> _changeAvatar() async {
    final file = await ImagePicker().pickImage(
      source: ImageSource.gallery,
      imageQuality: 88,
      maxWidth: 1600,
    );
    if (file == null) return;
    try {
      final user = await ref
          .read(repositoryProvider)
          .updateAvatar(UploadFile(path: file.path, name: file.name));
      ref.read(authControllerProvider).replaceUser(user);
      if (mounted) AppMessage.show(context, '头像已更新');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _saveNickname() async {
    final currentUser = ref.read(authControllerProvider).user;
    if (currentUser == null) return;
    setState(() => _savingNickname = true);
    try {
      final user = await ref
          .read(repositoryProvider)
          .updateProfile(
            nickname: _nickname.text.trim(),
            jobTitle: currentUser.jobTitle,
          );
      ref.read(authControllerProvider).replaceUser(user);
      if (mounted) {
        setState(() => _editingNickname = false);
        AppMessage.show(context, '昵称已更新');
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _savingNickname = false);
    }
  }

  Future<void> _deleteAccount() async {
    try {
      final eligibility = await ref
          .read(repositoryProvider)
          .accountDeletionEligibility();
      if (!mounted) return;

      final continueDeletion = await showModalBottomSheet<bool>(
        context: context,
        isScrollControlled: true,
        useSafeArea: true,
        backgroundColor: Colors.transparent,
        builder: (context) =>
            _AccountDeletionCheckSheet(eligibility: eligibility),
      );
      if (continueDeletion != true || !mounted) return;

      final confirmed = await showDialog<bool>(
        context: context,
        barrierDismissible: false,
        builder: (context) => AlertDialog(
          title: const Text('确认注销账号？'),
          content: const Text('注销后，当前账号及相关使用权限将被永久关闭，且无法恢复。请再次确认是否继续。'),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('暂不注销'),
            ),
            FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.error,
                foregroundColor: Theme.of(context).colorScheme.onError,
              ),
              onPressed: () => Navigator.pop(context, true),
              child: const Text('确认注销'),
            ),
          ],
        ),
      );
      if (confirmed != true || !mounted) return;

      await ref.read(authControllerProvider).deleteAccount();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user;
    if (user == null) return const SizedBox.shrink();
    return Scaffold(
      appBar: AppBar(title: const Text('账号设置')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 28),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      AppAvatar(
                        url: user.avatarUrl,
                        name: user.displayName,
                        radius: 31,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              user.displayName,
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'UID ${user.uid}',
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ],
                        ),
                      ),
                      TextButton.icon(
                        onPressed: _changeAvatar,
                        icon: const Icon(Icons.photo_camera_outlined, size: 15),
                        label: const Text('修改头像'),
                        style: TextButton.styleFrom(
                          backgroundColor: Theme.of(
                            context,
                          ).colorScheme.surfaceContainer,
                          padding: const EdgeInsets.symmetric(horizontal: 10),
                        ),
                      ),
                    ],
                  ),
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 16),
                    child: Divider(height: 1),
                  ),
                  Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '昵称',
                              style: Theme.of(context).textTheme.labelMedium,
                            ),
                            const SizedBox(height: 5),
                            Text(
                              user.displayName,
                              style: Theme.of(context).textTheme.bodyLarge,
                            ),
                          ],
                        ),
                      ),
                      TextButton.icon(
                        onPressed: _savingNickname
                            ? null
                            : () {
                                if (_editingNickname) {
                                  _nickname.text = user.nickname;
                                }
                                setState(
                                  () => _editingNickname = !_editingNickname,
                                );
                              },
                        icon: Icon(
                          _editingNickname
                              ? Icons.close_rounded
                              : Icons.edit_outlined,
                          size: 16,
                        ),
                        label: Text(_editingNickname ? '取消' : '修改'),
                      ),
                    ],
                  ),
                  if (_editingNickname) ...[
                    const SizedBox(height: 12),
                    TextField(
                      controller: _nickname,
                      autofocus: true,
                      maxLength: 12,
                      textInputAction: TextInputAction.done,
                      onSubmitted: (_) => _saveNickname(),
                      decoration: const InputDecoration(hintText: '未设置时显示 UID'),
                    ),
                    const SizedBox(height: 10),
                    Align(
                      alignment: Alignment.centerRight,
                      child: FilledButton(
                        onPressed: _savingNickname ? null : _saveNickname,
                        child: Text(_savingNickname ? '保存中…' : '保存昵称'),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          _IdentitySettingCard(
            record: _identity,
            onTap: _openIdentityCertification,
          ),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('违规次数剩余', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 10),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 11,
                    vertical: 9,
                  ),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.errorContainer,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      SizedBox(
                        width: 18,
                        height: 18,
                        child: Center(
                          child: Icon(
                            Icons.warning_amber_rounded,
                            size: 18,
                            color: Theme.of(
                              context,
                            ).colorScheme.onErrorContainer,
                          ),
                        ),
                      ),
                      const SizedBox(width: 7),
                      Expanded(
                        child: Text(
                          '任意一个等级的次数扣完，账号都将被永久封禁。',
                          style: Theme.of(context).textTheme.bodySmall
                              ?.copyWith(
                                color: Theme.of(
                                  context,
                                ).colorScheme.onErrorContainer,
                                fontWeight: FontWeight.w700,
                                height: 1.25,
                              ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
                if (_violationCounters == null)
                  const SizedBox.shrink()
                else if (_violationCounters!.isEmpty)
                  Text('暂时无法获取', style: Theme.of(context).textTheme.bodySmall)
                else
                  GridView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: _violationCounters!.length,
                    gridDelegate:
                        const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 5,
                          crossAxisSpacing: 7,
                          mainAxisSpacing: 7,
                          childAspectRatio: 0.82,
                        ),
                    itemBuilder: (context, index) {
                      final counter = _violationCounters![index];
                      final exhausted = counter.remainingCount == 0;
                      final colors = Theme.of(context).colorScheme;
                      return DecoratedBox(
                        decoration: BoxDecoration(
                          color: exhausted
                              ? colors.errorContainer
                              : colors.surfaceContainer,
                          borderRadius: BorderRadius.circular(11),
                        ),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              '${counter.level}级',
                              style: Theme.of(context).textTheme.labelSmall
                                  ?.copyWith(
                                    color: exhausted
                                        ? colors.onErrorContainer
                                        : colors.onSurfaceVariant,
                                  ),
                            ),
                            const SizedBox(height: 3),
                            Text(
                              '${counter.remainingCount}次',
                              style: Theme.of(context).textTheme.titleSmall
                                  ?.copyWith(
                                    color: exhausted
                                        ? colors.onErrorContainer
                                        : colors.onSurface,
                                    fontWeight: FontWeight.w700,
                                  ),
                            ),
                            const SizedBox(height: 3),
                            Text(
                              '已扣 ${counter.usedCount}次',
                              style: Theme.of(context).textTheme.labelSmall
                                  ?.copyWith(
                                    color: exhausted
                                        ? colors.onErrorContainer.withValues(
                                            alpha: 0.78,
                                          )
                                        : colors.onSurfaceVariant,
                                    fontWeight: FontWeight.w400,
                                  ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              children: [
                Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surfaceContainer,
                    borderRadius: BorderRadius.circular(11),
                  ),
                  child: const Icon(Icons.person_off_outlined, size: 17),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '注销账号',
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        '账号注销后无法恢复',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
                TextButton(onPressed: _deleteAccount, child: const Text('注销')),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _IdentitySettingCard extends StatelessWidget {
  const _IdentitySettingCard({required this.record, required this.onTap});

  final CertificationRecord? record;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final status = record?.status ?? '';
    final style = appStatusStyle(context, status);
    final label = switch (status.toUpperCase()) {
      'PENDING' => '审核中',
      'APPROVED' => '已认证',
      'REJECTED' => '未通过',
      _ => '去认证',
    };
    return Material(
      color: Theme.of(context).colorScheme.surface,
      borderRadius: BorderRadius.circular(16),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.surfaceContainer,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  Icons.verified_user_outlined,
                  size: 20,
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '实名认证',
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '查看和管理实名认证',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              Text(
                label,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: style.foreground,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(width: 2),
              const Icon(Icons.chevron_right_rounded, size: 20),
            ],
          ),
        ),
      ),
    );
  }
}

class _AccountDeletionCheckSheet extends StatelessWidget {
  const _AccountDeletionCheckSheet({required this.eligibility});

  final AccountDeletionEligibility eligibility;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Material(
      color: colors.surface,
      borderRadius: const BorderRadius.vertical(top: Radius.circular(22)),
      clipBehavior: Clip.antiAlias,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 12, 18, 18),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 36,
                  height: 4,
                  decoration: BoxDecoration(
                    color: colors.outlineVariant,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Text(
                '注销账号前，请先完成以下事项',
                style: Theme.of(
                  context,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 6),
              Text(
                eligibility.eligible
                    ? '当前已满足注销条件，继续后还需再次确认。'
                    : '以下事项全部完成后，才可以注销当前账号。',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: colors.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 18),
              _DeletionConditionRow(
                passed: eligibility.availableBalanceCleared,
                title: '账户可用余额已处理',
                detail: eligibility.availableBalanceCleared
                    ? '当前可用余额为 ¥0'
                    : '仍有 ¥${_formatAmount(eligibility.availableBalance)} 可用余额',
              ),
              const SizedBox(height: 10),
              _DeletionConditionRow(
                passed: eligibility.frozenBalanceCleared,
                title: '账户冻结金额已清零',
                detail: eligibility.frozenBalanceCleared
                    ? '当前冻结金额为 ¥0'
                    : '仍有 ¥${_formatAmount(eligibility.frozenBalance)} 冻结金额',
              ),
              const SizedBox(height: 10),
              _DeletionConditionRow(
                passed: eligibility.noActiveInquiries,
                title: '没有尚未结束的询问',
                detail: eligibility.noActiveInquiries
                    ? '当前没有进行中的询问'
                    : '请先等待相关询问结束',
              ),
              const SizedBox(height: 22),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => Navigator.pop(context, eligibility.eligible),
                  child: Text(eligibility.eligible ? '继续注销' : '知道了'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _DeletionConditionRow extends StatelessWidget {
  const _DeletionConditionRow({
    required this.passed,
    required this.title,
    required this.detail,
  });

  final bool passed;
  final String title;
  final String detail;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final accent = passed ? const Color(0xFF2E8B57) : colors.error;
    final background = passed
        ? const Color(0xFF2E8B57).withValues(alpha: 0.08)
        : colors.errorContainer.withValues(alpha: 0.55);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 12),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(13),
      ),
      child: Row(
        children: [
          Icon(
            passed ? Icons.check_circle_rounded : Icons.info_rounded,
            size: 21,
            color: accent,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 3),
                Text(
                  detail,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

String _formatAmount(double value) {
  if (value == value.truncateToDouble()) return value.toInt().toString();
  return value
      .toStringAsFixed(2)
      .replaceFirst(RegExp(r'0+$'), '')
      .replaceFirst(RegExp(r'\.$'), '');
}
