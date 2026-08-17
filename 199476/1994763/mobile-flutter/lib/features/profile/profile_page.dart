import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';

class ProfilePage extends ConsumerStatefulWidget {
  const ProfilePage({super.key});

  @override
  ConsumerState<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends ConsumerState<ProfilePage> {
  List<CertificationRecord> _certifications = const [];

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  Future<void> _refresh() async {
    try {
      final results = await Future.wait([
        ref.read(authControllerProvider).refreshUser(),
        ref.read(repositoryProvider).certifications(),
        ref.read(repositoryProvider).customerServiceUnreadCount(),
      ]);
      if (!mounted) return;
      setState(() => _certifications = results[1] as List<CertificationRecord>);
      ref.read(customerServiceUnreadProvider.notifier).state =
          results[2] as int;
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  bool get _joined {
    final required = _certifications.where((item) => item.required).toList();
    return required.isNotEmpty && required.every((item) => item.approved);
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user!;
    final theme = ref.watch(themeControllerProvider);
    final customerUnread = ref.watch(customerServiceUnreadProvider);
    return Scaffold(
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _refresh,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(17, 8, 17, 30),
            children: [
              Row(
                children: [
                  Text('我的', style: Theme.of(context).textTheme.headlineMedium),
                  const Spacer(),
                  IconButton(
                    onPressed: () => ref.read(themeControllerProvider).toggle(),
                    tooltip: theme.dark ? '切换为明亮主题' : '切换为黑暗主题',
                    icon: Icon(
                      theme.dark
                          ? Icons.light_mode_outlined
                          : Icons.dark_mode_outlined,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _ProfileOverview(
                avatarUrl: user.avatarUrl,
                displayName: user.displayName,
                uid: user.uid,
                verified: _certifications.any(
                  (item) =>
                      (item.type == 'IDENTITY' || item.type.contains('实名')) &&
                      item.approved,
                ),
                answererTitle: _joined ? '答主信息' : '成为答主',
                onSettings: () => context.push('/profile/settings'),
                onAnswerer: () => context.push('/profile/certifications'),
                onWallet: () => context.push('/profile/wallet'),
              ),
              const SizedBox(height: 20),
              Text('更多服务', style: Theme.of(context).textTheme.labelMedium),
              const SizedBox(height: 9),
              Material(
                color: Colors.transparent,
                borderRadius: BorderRadius.circular(16),
                clipBehavior: Clip.antiAlias,
                child: Column(
                  children: [
                    _MenuItem(
                      icon: Icons.help_outline_rounded,
                      title: '常见问题',
                      onTap: () => context.push('/profile/faq'),
                    ),
                    _MenuItem(
                      icon: Icons.headset_mic_outlined,
                      title: '在线客服',
                      badge: customerUnread,
                      onTap: () => context.push('/profile/customer-service'),
                    ),
                    _MenuItem(
                      icon: Icons.feedback_outlined,
                      title: '投诉与反馈',
                      onTap: () => context.push('/profile/feedback'),
                    ),
                    _MenuItem(
                      icon: Icons.handshake_outlined,
                      title: '商务合作',
                      onTap: () => context.push('/profile/business'),
                    ),
                    _MenuItem(
                      icon: Icons.logout_rounded,
                      title: '退出登录',
                      onTap: () async {
                        await ref.read(authControllerProvider).logout();
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              Center(
                child: Text(
                  '每个人，都有能帮上别人的地方。',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ProfileOverview extends StatelessWidget {
  const _ProfileOverview({
    required this.avatarUrl,
    required this.displayName,
    required this.uid,
    required this.verified,
    required this.answererTitle,
    required this.onSettings,
    required this.onAnswerer,
    required this.onWallet,
  });

  final String avatarUrl;
  final String displayName;
  final String uid;
  final bool verified;
  final String answererTitle;
  final VoidCallback onSettings;
  final VoidCallback onAnswerer;
  final VoidCallback onWallet;

  @override
  Widget build(BuildContext context) {
    const gold = Color(0xFFE7C88F);
    return ClipRRect(
      borderRadius: BorderRadius.circular(24),
      child: DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF3A3030), Color(0xFF241F20)],
          ),
        ),
        child: Stack(
          children: [
            Positioned(
              right: -34,
              top: -48,
              child: Container(
                width: 140,
                height: 140,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Theme.of(
                    context,
                  ).colorScheme.primary.withValues(alpha: .16),
                ),
              ),
            ),
            Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(18, 18, 12, 16),
                  child: Row(
                    children: [
                      AppAvatar(
                        url: avatarUrl,
                        name: displayName,
                        radius: 28,
                        verified: verified,
                      ),
                      const SizedBox(width: 13),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              displayName,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 17,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'UID $uid',
                              style: const TextStyle(
                                color: Color(0xFFBEB7B4),
                                fontSize: 11,
                              ),
                            ),
                          ],
                        ),
                      ),
                      IconButton(
                        onPressed: onSettings,
                        tooltip: '账号设置',
                        visualDensity: VisualDensity.compact,
                        style: IconButton.styleFrom(
                          foregroundColor: Colors.white,
                          backgroundColor: Colors.white.withValues(alpha: .08),
                        ),
                        icon: const Icon(Icons.settings_outlined, size: 20),
                      ),
                    ],
                  ),
                ),
                Divider(height: 1, color: Colors.white.withValues(alpha: .09)),
                Row(
                  children: [
                    Expanded(
                      child: _OverviewEntry(
                        icon: Icons.record_voice_over_outlined,
                        title: answererTitle,
                        color: gold,
                        onTap: onAnswerer,
                      ),
                    ),
                    Container(
                      width: 1,
                      height: 32,
                      color: Colors.white.withValues(alpha: .09),
                    ),
                    Expanded(
                      child: _OverviewEntry(
                        icon: Icons.account_balance_wallet_outlined,
                        title: '账户余额',
                        color: gold,
                        onTap: onWallet,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _OverviewEntry extends StatelessWidget {
  const _OverviewEntry({
    required this.icon,
    required this.title,
    required this.color,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 20, color: color),
            const SizedBox(width: 8),
            Flexible(
              child: Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MenuItem extends StatelessWidget {
  const _MenuItem({
    required this.icon,
    required this.title,
    required this.onTap,
    this.badge = 0,
  });
  final IconData icon;
  final String title;
  final VoidCallback onTap;
  final int badge;
  @override
  Widget build(BuildContext context) => ListTile(
    minTileHeight: 52,
    contentPadding: const EdgeInsets.symmetric(horizontal: 16),
    leading: Container(
      width: 31,
      height: 31,
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Icon(icon, size: 17, color: Theme.of(context).colorScheme.primary),
    ),
    title: Text(title, style: Theme.of(context).textTheme.bodyLarge),
    trailing: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (badge > 0) Badge(label: Text(badge > 99 ? '99+' : '$badge')),
        const SizedBox(width: 4),
        Icon(
          Icons.chevron_right_rounded,
          size: 19,
          color: Theme.of(context).textTheme.bodySmall?.color,
        ),
      ],
    ),
    onTap: onTap,
  );
}
