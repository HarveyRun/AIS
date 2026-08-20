import 'dart:async';
import 'dart:math';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/config/app_config.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/answerer_card.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/answerer_models.dart';
import '../../data/models/home_banner_models.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  final _random = Random();
  final _searchController = TextEditingController();
  final _scrollController = ScrollController();
  final _pageController = PageController();
  final List<Answerer> _items = [];
  List<HomeBannerItem> _banners = const [];
  Timer? _bannerTimer;
  Timer? _searchTimer;
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 0;
  int _banner = 0;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _load(reset: true);
    _loadBanners();
    _refreshNoticeCount();
  }

  void _restartBannerTimer() {
    _bannerTimer?.cancel();
    if (_banners.length < 2) return;
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

  Future<void> _loadBanners() async {
    try {
      final banners = await ref.read(repositoryProvider).homeBanners();
      if (!mounted) return;
      setState(() {
        _banners = banners;
        _banner = banners.isEmpty ? 0 : min(_banner, banners.length - 1);
      });
      if (_pageController.hasClients && banners.isNotEmpty) {
        _pageController.jumpToPage(_banner);
      }
      _restartBannerTimer();
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _banners = const [];
        _banner = 0;
      });
      _restartBannerTimer();
    }
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

  void _openRandomDiscovery() {
    context.push(
      _random.nextBool() ? '/discover/matters' : '/discover/experiences',
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user;
    if (user == null) return const SizedBox.shrink();
    final noticeCount = ref.watch(notificationCountProvider);
    final theme = Theme.of(context);
    return ColoredBox(
      color: theme.scaffoldBackgroundColor,
      child: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          onRefresh: () async {
            await Future.wait([
              _load(reset: true),
              _loadBanners(),
              _refreshNoticeCount(),
            ]);
          },
          child: CustomScrollView(
            controller: _scrollController,
            slivers: [
              SliverAppBar(
                pinned: true,
                toolbarHeight: 66,
                titleSpacing: 10,
                backgroundColor: theme.colorScheme.surface,
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
                          inputFormatters: AppInputFormatters.search,
                          textInputAction: TextInputAction.search,
                          decoration: InputDecoration(
                            hintText: '搜索主职',
                            filled: true,
                            fillColor: theme.colorScheme.surfaceContainer,
                            contentPadding: EdgeInsets.zero,
                            prefixIcon: const Icon(
                              Icons.search_rounded,
                              size: 19,
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
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(20),
                              borderSide: BorderSide.none,
                            ),
                            enabledBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(20),
                              borderSide: BorderSide.none,
                            ),
                            focusedBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(20),
                              borderSide: BorderSide.none,
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
              if (_banners.isNotEmpty)
                SliverToBoxAdapter(
                  child: _Banner(
                    controller: _pageController,
                    banners: _banners,
                    active: _banner,
                    onChanged: (value) => setState(() => _banner = value),
                    onTap: _openRandomDiscovery,
                  ),
                ),
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: _DiscoveryHub(
                    onMatters: () => context.push('/discover/matters'),
                    onExperiences: () => context.push('/discover/experiences'),
                  ),
                ),
              ),
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(10, 24, 10, 10),
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
                  child: Center(
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                )
              else if (_items.isEmpty)
                const SliverFillRemaining(
                  hasScrollBody: false,
                  child: Center(child: Text('没有找到相关的人')),
                )
              else
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(10, 0, 10, 20),
                  sliver: SliverList.separated(
                    itemCount: _items.length,
                    itemBuilder: (context, index) => AnswererCard(
                      answerer: _items[index],
                      flat: true,
                      onTap: () =>
                          context.push('/answerers/${_items[index].uid}'),
                    ),
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
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
    required this.onTap,
  });
  final PageController controller;
  final List<HomeBannerItem> banners;
  final int active;
  final ValueChanged<int> onChanged;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SizedBox(
      height: 174,
      child: Stack(
        children: [
          PageView.builder(
            controller: controller,
            onPageChanged: onChanged,
            itemCount: banners.length,
            itemBuilder: (context, index) {
              final item = banners[index];
              return Semantics(
                button: true,
                label: '随机查看按事情或按经历找人',
                child: GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: onTap,
                  child: Container(
                    margin: const EdgeInsets.fromLTRB(10, 14, 10, 4),
                    clipBehavior: Clip.antiAlias,
                    decoration: BoxDecoration(
                      color: theme.colorScheme.surface,
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: _BannerContent(item: item),
                  ),
                ),
              );
            },
          ),
          if (banners.length > 1)
            Positioned(
              left: 35,
              bottom: 16,
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
                          ? const Color(0xFFD7473E)
                          : theme.colorScheme.outlineVariant,
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

class _BannerContent extends StatelessWidget {
  const _BannerContent({required this.item});

  final HomeBannerItem item;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final imageUrl = AppConfig.resolveImage(item.imageUrl).toString();

    if (item.displayMode == 'IMAGE_ONLY') {
      return _BannerImage(imageUrl: imageUrl);
    }

    if (item.displayMode == 'IMAGE_TEXT') {
      return Stack(
        fit: StackFit.expand,
        children: [
          _BannerImage(imageUrl: imageUrl),
          const DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.centerLeft,
                end: Alignment.centerRight,
                colors: [
                  Color(0xC9000000),
                  Color(0x59000000),
                  Colors.transparent,
                ],
                stops: [0, .62, 1],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 18, 120, 16),
            child: _BannerText(item: item, onImage: true),
          ),
        ],
      );
    }

    return Stack(
      children: [
        Positioned.fill(
          child: CustomPaint(
            painter: _BannerDecorationPainter(
              color: theme.brightness == Brightness.dark
                  ? theme.colorScheme.primary.withValues(alpha: .13)
                  : const Color(0xFFF5E9E7),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(18, 18, 18, 16),
          child: _BannerText(item: item),
        ),
      ],
    );
  }
}

class _BannerImage extends StatelessWidget {
  const _BannerImage({required this.imageUrl});

  final String imageUrl;

  @override
  Widget build(BuildContext context) {
    if (imageUrl.isEmpty) {
      return ColoredBox(color: Theme.of(context).colorScheme.surfaceContainer);
    }
    return CachedNetworkImage(
      imageUrl: imageUrl,
      fit: BoxFit.cover,
      placeholder: (_, _) =>
          ColoredBox(color: Theme.of(context).colorScheme.surfaceContainer),
      errorWidget: (_, _, _) => ColoredBox(
        color: Theme.of(context).colorScheme.surfaceContainer,
        child: Icon(
          Icons.image_not_supported_outlined,
          color: Theme.of(context).colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }
}

class _BannerText extends StatelessWidget {
  const _BannerText({required this.item, this.onImage = false});

  final HomeBannerItem item;
  final bool onImage;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final foreground = onImage ? Colors.white : theme.colorScheme.onSurface;
    final secondary = onImage
        ? Colors.white.withValues(alpha: .82)
        : theme.colorScheme.onSurfaceVariant;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (item.labelText.isNotEmpty) ...[
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
            decoration: BoxDecoration(
              color: onImage
                  ? Colors.black.withValues(alpha: .35)
                  : const Color(0xFFB86F5D),
              borderRadius: BorderRadius.circular(7),
            ),
            child: Text(
              '✣  ${item.labelText}',
              style: const TextStyle(
                color: Colors.white,
                fontSize: 10,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          const SizedBox(height: 12),
        ],
        Text(
          item.title,
          style: theme.textTheme.titleMedium?.copyWith(
            color: foreground,
            fontSize: 20,
            height: 1.32,
            fontWeight: FontWeight.w800,
          ),
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        if (item.description.isNotEmpty) ...[
          const SizedBox(height: 7),
          Text(
            item.description,
            style: TextStyle(color: secondary, fontSize: 10, height: 1.35),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ],
    );
  }
}

class _DiscoveryHub extends StatelessWidget {
  const _DiscoveryHub({required this.onMatters, required this.onExperiences});

  final VoidCallback onMatters;
  final VoidCallback onExperiences;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 10),
      child: Row(
        children: [
          Expanded(
            child: _DiscoveryEntry(
              icon: Icons.search_rounded,
              title: '按事情',
              subtitle: '先看看应该问哪些人',
              borderColor: const Color(0xFFE8D2CA),
              iconColor: const Color(0xFFD9473E),
              onTap: onMatters,
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: _DiscoveryEntry(
              icon: Icons.route_outlined,
              title: '按经历',
              subtitle: '找经历过的人',
              borderColor: const Color(0xFFCEE2D8),
              iconColor: const Color(0xFF4E8E70),
              onTap: onExperiences,
            ),
          ),
        ],
      ),
    );
  }
}

class _DiscoveryEntry extends StatelessWidget {
  const _DiscoveryEntry({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.borderColor,
    required this.iconColor,
    required this.onTap,
  });
  final IconData icon;
  final String title;
  final String subtitle;
  final Color borderColor;
  final Color iconColor;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final dark = theme.brightness == Brightness.dark;
    return Material(
      color: dark
          ? iconColor.withValues(alpha: .12)
          : borderColor.withValues(alpha: .22),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 13),
          child: Row(
            children: [
              Icon(icon, color: iconColor, size: 20),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
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
              Icon(
                Icons.chevron_right_rounded,
                size: 17,
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BannerDecorationPainter extends CustomPainter {
  const _BannerDecorationPainter({required this.color});

  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 18;
    canvas.drawCircle(Offset(size.width - 18, 42), 83, paint);
  }

  @override
  bool shouldRepaint(covariant _BannerDecorationPainter oldDelegate) =>
      oldDelegate.color != color;
}
