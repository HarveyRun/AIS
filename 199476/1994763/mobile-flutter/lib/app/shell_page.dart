import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'providers.dart';

class AppShellPage extends ConsumerStatefulWidget {
  const AppShellPage({super.key, required this.child, required this.location});

  final Widget child;
  final String location;

  @override
  ConsumerState<AppShellPage> createState() => _AppShellPageState();
}

class _AppShellPageState extends ConsumerState<AppShellPage> {
  void _unfocusAfterEnteringHome() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      FocusManager.instance.primaryFocus?.unfocus();
    });
  }

  @override
  void initState() {
    super.initState();
    if (widget.location == '/home') {
      _unfocusAfterEnteringHome();
    }
  }

  @override
  void didUpdateWidget(covariant AppShellPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.location != widget.location && widget.location == '/home') {
      _unfocusAfterEnteringHome();
    }
  }

  int get _selectedIndex {
    if (widget.location.startsWith('/inquiries')) return 1;
    if (widget.location.startsWith('/profile')) return 2;
    return 0;
  }

  bool get _showBottomNavigation =>
      !widget.location.startsWith('/profile/certifications/experiences') &&
      !widget.location.startsWith('/profile/settings') &&
      !widget.location.startsWith('/answerers/');

  @override
  Widget build(BuildContext context) {
    final unread = ref.watch(inquiryUnreadCountProvider);
    return Scaffold(
      body: widget.child,
      bottomNavigationBar: _showBottomNavigation
          ? _AppBottomNavigation(selectedIndex: _selectedIndex, unread: unread)
          : null,
    );
  }
}

class _AppBottomNavigation extends StatelessWidget {
  const _AppBottomNavigation({
    required this.selectedIndex,
    required this.unread,
  });

  final int selectedIndex;
  final int unread;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final dark = theme.brightness == Brightness.dark;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: dark ? const Color(0xFF121315) : Colors.white,
        border: Border(
          top: BorderSide(
            color: dark ? const Color(0xFF303238) : const Color(0xFFF0E8E1),
          ),
        ),
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: 58,
          child: Row(
            children: [
              _NavigationItem(
                label: '首页',
                icon: Icons.home_outlined,
                selectedIcon: Icons.home_rounded,
                selected: selectedIndex == 0,
                onTap: () {
                  FocusManager.instance.primaryFocus?.unfocus();
                  context.go('/home');
                },
              ),
              _NavigationItem(
                label: '我的询问',
                icon: Icons.chat_bubble_outline_rounded,
                selectedIcon: Icons.chat_bubble_rounded,
                selected: selectedIndex == 1,
                badge: unread,
                onTap: () {
                  FocusManager.instance.primaryFocus?.unfocus();
                  context.go('/inquiries');
                },
              ),
              _NavigationItem(
                label: '我的',
                icon: Icons.person_outline_rounded,
                selectedIcon: Icons.person_rounded,
                selected: selectedIndex == 2,
                onTap: () {
                  FocusManager.instance.primaryFocus?.unfocus();
                  context.go('/profile');
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NavigationItem extends StatelessWidget {
  const _NavigationItem({
    required this.label,
    required this.icon,
    required this.selectedIcon,
    required this.selected,
    required this.onTap,
    this.badge = 0,
  });

  final String label;
  final IconData icon;
  final IconData selectedIcon;
  final bool selected;
  final VoidCallback onTap;
  final int badge;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = selected
        ? theme.colorScheme.primary
        : theme.textTheme.bodySmall?.color;
    return Expanded(
      child: InkResponse(
        onTap: onTap,
        radius: 28,
        child: Padding(
          padding: const EdgeInsets.only(top: 7, bottom: 5),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Badge(
                isLabelVisible: badge > 0,
                backgroundColor: theme.colorScheme.primary,
                label: Text(
                  badge > 99 ? '99+' : '$badge',
                  style: const TextStyle(fontSize: 9, color: Colors.white),
                ),
                child: Icon(
                  selected ? selectedIcon : icon,
                  size: 21,
                  color: color,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                label,
                style: TextStyle(
                  fontSize: 9,
                  height: 1,
                  fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
                  color: color,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
