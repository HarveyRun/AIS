import 'dart:async';
import 'dart:math' show min;

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/app_route_observer.dart';
import '../../app/providers.dart';
import '../../core/config/app_config.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/answerer_card.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../core/widgets/platform_introduction_gate.dart';
import '../../data/models/answerer_models.dart';
import '../../data/models/home_banner_models.dart';
import 'home_activity_rules_dialog.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> with RouteAware {
  final _searchController = TextEditingController();
  final _searchFocusNode = FocusNode(
    skipTraversal: true,
    canRequestFocus: false,
  );
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
  int _listRequestVersion = 0;
  int _banner = 0;
  int _pageSize = 20;
  final Set<int> _seenBanners = {};
  final Set<int> _checkingBanners = {};
  ModalRoute<void>? _route;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _initializeList();
    _loadBanners();
    _refreshNoticeCount();
    _releaseSearchFocus();
  }

  Future<void> _initializeList() async {
    try {
      final settings = await ref.read(repositoryProvider).appGlobalSettings();
      _pageSize = settings.homePageSize;
    } catch (_) {}
    if (mounted) await _load(reset: true);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final route = ModalRoute.of(context);
    if (_route == route) return;
    if (_route != null) {
      appRouteObserver.unsubscribe(this);
    }
    _route = route;
    if (route != null) {
      appRouteObserver.subscribe(this, route);
    }
  }

  @override
  void didPush() {
    _releaseSearchFocus();
  }

  @override
  void didPopNext() {
    _releaseSearchFocus();
  }

  void _releaseSearchFocus() {
    _searchFocusNode.canRequestFocus = false;
    _searchFocusNode.unfocus();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _searchFocusNode.canRequestFocus = false;
      _searchFocusNode.unfocus();
      SystemChannels.textInput.invokeMethod<void>('TextInput.hide');
    });
  }

  void _restartBannerTimer() {
    _bannerTimer?.cancel();
    if (_banners.length < 2) return;
    _bannerTimer = Timer(const Duration(seconds: 10), () {
      if (!mounted || !_pageController.hasClients) return;
      _banner = (_banner + 1) % _banners.length;
      _pageController.animateToPage(
        _banner,
        duration: const Duration(milliseconds: 350),
        curve: Curves.easeOutCubic,
      );
    });
  }

  void _onBannerChanged(int value) {
    setState(() => _banner = value);
    _trackBannerImpression(value);
    _restartBannerTimer();
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
      _trackBannerImpression(_banner);
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
    appRouteObserver.unsubscribe(this);
    _bannerTimer?.cancel();
    _searchTimer?.cancel();
    _searchController.dispose();
    _searchFocusNode.dispose();
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
    late final int requestVersion;
    if (reset) {
      requestVersion = ++_listRequestVersion;
      _page = 0;
      _hasMore = true;
      setState(() => _loading = true);
    } else {
      if (!_hasMore || _loadingMore) return;
      requestVersion = _listRequestVersion;
      setState(() => _loadingMore = true);
    }
    final keyword = _searchController.text.trim();
    try {
      final result = await ref
          .read(repositoryProvider)
          .answerers(page: _page, size: _pageSize, keyword: keyword);
      if (!mounted || requestVersion != _listRequestVersion) return;
      setState(() {
        if (reset) _items.clear();
        _items.addAll(result.items);
        _hasMore = result.hasMore;
        if (result.hasMore) _page++;
      });
      if (reset && keyword.isNotEmpty) {
        unawaited(
          ref
              .read(analyticsProvider)
              .track(
                'home_search_submit',
                properties: {
                  'search_term': keyword,
                  'result_count': result.items.length,
                },
              ),
        );
      }
    } catch (error) {
      if (mounted && requestVersion == _listRequestVersion) {
        AppMessage.show(context, '$error');
      }
    } finally {
      if (mounted && requestVersion == _listRequestVersion) {
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

  void _trackBannerImpression(int index) {
    if (index < 0 || index >= _banners.length) return;
    final item = _banners[index];
    if (!_seenBanners.add(item.id)) return;
    unawaited(
      ref
          .read(analyticsProvider)
          .track(
            'banner_impression',
            properties: {
              'banner_id': item.id,
              'banner_title': item.title,
              'display_mode': item.displayMode,
              'position': index + 1,
            },
          ),
    );
  }

  void _openBanner(HomeBannerItem item) {
    if (!item.canOpen || !_checkingBanners.add(item.id)) return;
    unawaited(_verifyAndOpenBanner(item));
  }

  Future<void> _verifyAndOpenBanner(HomeBannerItem item) async {
    try {
      final result = await ref
          .read(repositoryProvider)
          .homeBannerAvailability(item.id);
      if (!mounted) return;
      if (!result.available || result.banner == null) {
        _removeUnavailableBanner(item.id);
        AppMessage.show(
          context,
          result.message.isEmpty ? '该内容当前不可用' : result.message,
        );
        return;
      }

      final current = result.banner!;
      if (!current.canOpen) {
        AppMessage.show(context, '该内容暂无更多信息');
        return;
      }
      _performBannerAction(current);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      _checkingBanners.remove(item.id);
    }
  }

  void _removeUnavailableBanner(int id) {
    final remaining = _banners.where((item) => item.id != id).toList();
    setState(() {
      _banners = remaining;
      _banner = remaining.isEmpty ? 0 : min(_banner, remaining.length - 1);
    });
    _restartBannerTimer();
    if (_pageController.hasClients && remaining.isNotEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || !_pageController.hasClients) return;
        _pageController.jumpToPage(_banner);
      });
    }
  }

  void _performBannerAction(HomeBannerItem item) {
    unawaited(
      ref
          .read(analyticsProvider)
          .track(
            'banner_click',
            properties: {
              'banner_id': item.id,
              'banner_title': item.title,
              'destination': item.actionType,
            },
          ),
    );
    if (item.opensPlatformIntroduction) {
      unawaited(showPlatformIntroductionDialog(context));
      return;
    }
    if (item.opensActivityRules) {
      unawaited(_openActivityBanner(item));
      return;
    }
    context.push(item.targetPath);
  }

  Future<void> _openActivityBanner(HomeBannerItem item) async {
    final confirmed = await showHomeActivityRulesDialog(
      context,
      actionType: item.actionType,
    );
    if (!mounted || confirmed != true) return;
    context.push('/profile/certifications/experiences');
  }

  void _openAnswerer(Answerer answerer, int position) {
    final experience = answerer.experiences.first;
    unawaited(
      ref
          .read(analyticsProvider)
          .track(
            'answerer_card_click',
            properties: {
              'answerer_user_id': answerer.id,
              'answerer_uid': answerer.uid,
              'experience_id': experience.certificationId,
              'experience_name': experience.title,
              'job_name': answerer.mainJob == '-' ? null : answerer.mainJob,
              'position': position,
              'source': 'home',
            },
          ),
    );
    context.push(
      '/answerers/${answerer.uid}?experienceId=${experience.certificationId}',
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
                      child: Container(
                        height: 38,
                        decoration: BoxDecoration(
                          color:
                              theme.inputDecorationTheme.fillColor ??
                              theme.colorScheme.surfaceContainerHighest,
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Row(
                          children: [
                            Expanded(
                              child: Padding(
                                padding: const EdgeInsets.only(left: 14),
                                child: TextField(
                                  controller: _searchController,
                                  focusNode: _searchFocusNode,
                                  autofocus: false,
                                  onTap: () {
                                    _searchFocusNode.canRequestFocus = true;
                                    _searchFocusNode.requestFocus();
                                  },
                                  onChanged: _search,
                                  inputFormatters: AppInputFormatters.search,
                                  textInputAction: TextInputAction.search,
                                  textAlignVertical: TextAlignVertical.center,
                                  style: theme.textTheme.bodyMedium?.copyWith(
                                    height: 1,
                                    fontWeight: FontWeight.w500,
                                  ),
                                  decoration: InputDecoration(
                                    hintText: '搜索经历',
                                    filled: false,
                                    isDense: true,
                                    contentPadding: EdgeInsets.zero,
                                    prefixIcon: const Icon(
                                      Icons.search_rounded,
                                      size: 18,
                                    ),
                                    prefixIconConstraints: const BoxConstraints(
                                      minWidth: 30,
                                      minHeight: 30,
                                    ),
                                    hintStyle: theme.textTheme.bodyMedium
                                        ?.copyWith(
                                          height: 1,
                                          color: theme
                                              .colorScheme
                                              .onSurfaceVariant
                                              .withValues(alpha: .95),
                                          fontWeight: FontWeight.w500,
                                        ),
                                    suffixIconConstraints: const BoxConstraints(
                                      minWidth: 34,
                                      minHeight: 34,
                                    ),
                                    suffixIcon: _searchController.text.isEmpty
                                        ? null
                                        : IconButton(
                                            padding: EdgeInsets.zero,
                                            visualDensity:
                                                VisualDensity.compact,
                                            onPressed: () {
                                              _searchTimer?.cancel();
                                              _searchController.clear();
                                              _load(reset: true);
                                              setState(() {});
                                            },
                                            icon: const Icon(
                                              Icons.close_rounded,
                                              size: 17,
                                            ),
                                          ),
                                    border: InputBorder.none,
                                    enabledBorder: InputBorder.none,
                                    focusedBorder: InputBorder.none,
                                  ),
                                ),
                              ),
                            ),
                          ],
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
                    onChanged: _onBannerChanged,
                    onInteractionStart: _restartBannerTimer,
                    onTap: _openBanner,
                  ),
                ),
              if (_loading)
                const SliverFillRemaining(
                  hasScrollBody: false,
                  child: SizedBox.shrink(),
                )
              else if (_items.isEmpty)
                const SliverFillRemaining(
                  hasScrollBody: false,
                  child: Center(child: Text('没有找到相关经历')),
                )
              else
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(10, 0, 10, 20),
                  sliver: SliverList.separated(
                    itemCount: _items.length,
                    itemBuilder: (context, index) => AnswererCard(
                      answerer: _items[index],
                      experience: _items[index].experiences.first,
                      flat: true,
                      onTap: () => _openAnswerer(_items[index], index + 1),
                    ),
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                  ),
                ),
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: Center(
                    child: _loadingMore
                        ? const SizedBox.shrink()
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
    required this.onInteractionStart,
    required this.onTap,
  });
  final PageController controller;
  final List<HomeBannerItem> banners;
  final int active;
  final ValueChanged<int> onChanged;
  final VoidCallback onInteractionStart;
  final ValueChanged<HomeBannerItem> onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SizedBox(
      height: 174,
      child: Stack(
        children: [
          NotificationListener<ScrollStartNotification>(
            onNotification: (notification) {
              if (notification.dragDetails != null) onInteractionStart();
              return false;
            },
            child: PageView.builder(
              controller: controller,
              onPageChanged: onChanged,
              itemCount: banners.length,
              itemBuilder: (context, index) {
                final item = banners[index];
                return Semantics(
                  button: item.canOpen,
                  label: item.canOpen ? '打开Banner对应页面' : 'Banner内容',
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: item.canOpen ? () => onTap(item) : null,
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
