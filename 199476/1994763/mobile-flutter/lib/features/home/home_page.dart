import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/widgets/answerer_card.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/answerer_models.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  final _searchController = TextEditingController();
  final _scrollController = ScrollController();
  final _pageController = PageController();
  final List<Answerer> _items = [];
  Timer? _bannerTimer;
  Timer? _searchTimer;
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 0;
  int _banner = 0;

  static const _banners = [
    ('大多数人都会遇到', '买房、装修，是生活里绕不开的一环', '先问问做过和经历过的人，别稀里糊涂花钱'),
    ('上班总会遇到点糟心事', '离职、裁员，遇到容易慌？', '找经历过的人聊聊，心里就有底了'),
    ('家庭的担子', '照顾老人、孩子成长，没人天生就会', '问问过来人，听听日常里管用的经验'),
  ];

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _load(reset: true);
    _refreshNoticeCount();
    _bannerTimer = Timer.periodic(const Duration(seconds: 10), (_) {
      if (!mounted || !_pageController.hasClients) return;
      _banner = (_banner + 1) % _banners.length;
      _pageController.animateToPage(
        _banner,
        duration: const Duration(milliseconds: 350),
        curve: Curves.easeOutCubic,
      );
    });
  }

  @override
  void dispose() {
    _bannerTimer?.cancel();
    _searchTimer?.cancel();
    _searchController.dispose();
    _scrollController.dispose();
    _pageController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.extentAfter < 220) _load();
  }

  Future<void> _refreshNoticeCount() async {
    try {
      final count = await ref
          .read(repositoryProvider)
          .notificationUnreadCount();
      ref.read(notificationCountProvider.notifier).state = count;
    } catch (_) {}
  }

  Future<void> _load({bool reset = false}) async {
    if (reset) {
      _page = 0;
      _hasMore = true;
      setState(() => _loading = true);
    } else {
      if (!_hasMore || _loadingMore) return;
      setState(() => _loadingMore = true);
    }
    try {
      final result = await ref
          .read(repositoryProvider)
          .answerers(
            page: _page,
            size: 10,
            keyword: _searchController.text.trim(),
          );
      if (!mounted) return;
      setState(() {
        if (reset) _items.clear();
        _items.addAll(result.items);
        _hasMore = result.hasMore;
        if (result.hasMore) _page++;
      });
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
          _loadingMore = false;
        });
      }
    }
  }

  void _search(String _) {
    _searchTimer?.cancel();
    _searchTimer = Timer(
      const Duration(milliseconds: 350),
      () => _load(reset: true),
    );
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user!;
    final noticeCount = ref.watch(notificationCountProvider);
    return SafeArea(
      bottom: false,
      child: RefreshIndicator(
        onRefresh: () async {
          await Future.wait([_load(reset: true), _refreshNoticeCount()]);
        },
        child: CustomScrollView(
          controller: _scrollController,
          slivers: [
            SliverAppBar(
              pinned: true,
              toolbarHeight: 60,
              titleSpacing: 16,
              automaticallyImplyLeading: false,
              title: Row(
                children: [
                  GestureDetector(
                    onTap: () => context.go('/profile'),
                    child: AppAvatar(
                      url: user.avatarUrl,
                      name: user.displayName,
                      radius: 19,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: SizedBox(
                      height: 38,
                      child: TextField(
                        controller: _searchController,
                        onChanged: _search,
                        textInputAction: TextInputAction.search,
                        decoration: InputDecoration(
                          hintText: '搜索主职',
                          prefixIcon: const Icon(
                            Icons.search_rounded,
                            size: 20,
                          ),
                          suffixIcon: _searchController.text.isEmpty
                              ? null
                              : IconButton(
                                  onPressed: () {
                                    _searchController.clear();
                                    _load(reset: true);
                                    setState(() {});
                                  },
                                  icon: const Icon(
                                    Icons.close_rounded,
                                    size: 18,
                                  ),
                                ),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  IconButton(
                    onPressed: () => context.push('/notices'),
                    icon: Badge(
                      isLabelVisible: noticeCount > 0,
                      label: Text(noticeCount > 99 ? '99+' : '$noticeCount'),
                      child: const Icon(Icons.mail_outline_rounded),
                    ),
                  ),
                ],
              ),
            ),
            SliverToBoxAdapter(
              child: _Banner(
                controller: _pageController,
                banners: _banners,
                active: _banner,
                onChanged: (value) => setState(() => _banner = value),
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.only(top: 10),
                child: _DiscoveryHub(
                  onMatters: () => context.push('/discover/matters'),
                  onExperiences: () => context.push('/discover/experiences'),
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 24, 16, 12),
              sliver: SliverToBoxAdapter(
                child: Text(
                  '可以帮你的人',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
            ),
            if (_loading)
              const SliverFillRemaining(
                hasScrollBody: false,
                child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
              )
            else if (_items.isEmpty)
              const SliverFillRemaining(
                hasScrollBody: false,
                child: Center(child: Text('没有找到相关的人')),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.only(bottom: 20),
                sliver: SliverList.separated(
                  itemCount: _items.length,
                  itemBuilder: (context, index) => AnswererCard(
                    answerer: _items[index],
                    onTap: () =>
                        context.push('/answerers/${_items[index].uid}'),
                  ),
                  separatorBuilder: (_, _) =>
                      const Divider(height: 1, indent: 77),
                ),
              ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.only(bottom: 24),
                child: Center(
                  child: _loadingMore
                      ? const SizedBox.square(
                          dimension: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : Text(
                          _hasMore ? '继续下滑，看看更多人' : '已经到底啦',
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Banner extends StatelessWidget {
  const _Banner({
    required this.controller,
    required this.banners,
    required this.active,
    required this.onChanged,
  });
  final PageController controller;
  final List<(String, String, String)> banners;
  final int active;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 138,
      child: Stack(
        children: [
          PageView.builder(
            controller: controller,
            onPageChanged: onChanged,
            itemCount: banners.length,
            itemBuilder: (context, index) {
              final item = banners[index];
              return Container(
                margin: const EdgeInsets.fromLTRB(16, 8, 16, 4),
                padding: const EdgeInsets.fromLTRB(18, 15, 18, 15),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primary,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Stack(
                  children: [
                    Positioned(
                      right: -4,
                      top: -22,
                      child: Text(
                        '问',
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: .09),
                          fontSize: 104,
                          height: 1,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          item.$1,
                          style: const TextStyle(
                            color: Color(0xE6FFFFFF),
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                            letterSpacing: .5,
                          ),
                        ),
                        const SizedBox(height: 7),
                        Text(
                          item.$2,
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(
                                color: Colors.white,
                                fontSize: 17,
                                height: 1.25,
                                fontWeight: FontWeight.w800,
                              ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 5),
                        Text(
                          item.$3,
                          style: const TextStyle(
                            color: Color(0xD9FFFFFF),
                            fontSize: 11,
                            height: 1.35,
                          ),
                          maxLines: 2,
                        ),
                      ],
                    ),
                  ],
                ),
              );
            },
          ),
          Positioned(
            left: 36,
            bottom: 13,
            child: Row(
              children: List.generate(
                banners.length,
                (index) => AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  width: active == index ? 18 : 5,
                  height: 5,
                  margin: const EdgeInsets.only(right: 5),
                  decoration: BoxDecoration(
                    color: active == index
                        ? Colors.white
                        : Colors.white.withValues(alpha: .35),
                    borderRadius: BorderRadius.circular(3),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _DiscoveryHub extends StatelessWidget {
  const _DiscoveryHub({required this.onMatters, required this.onExperiences});

  final VoidCallback onMatters;
  final VoidCallback onExperiences;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Theme.of(context).colorScheme.surface,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: Row(
          children: [
            Expanded(
              child: _DiscoveryEntry(
                icon: Icons.search_rounded,
                title: '按事情',
                subtitle: '看看应该问谁',
                onTap: onMatters,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _DiscoveryEntry(
                icon: Icons.route_outlined,
                title: '按经历',
                subtitle: '找经历过的人',
                onTap: onExperiences,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DiscoveryEntry extends StatelessWidget {
  const _DiscoveryEntry({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 2, vertical: 10),
        child: Row(
          children: [
            Icon(icon, color: Theme.of(context).colorScheme.primary, size: 23),
            const SizedBox(width: 11),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: Theme.of(context).textTheme.bodySmall,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
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
