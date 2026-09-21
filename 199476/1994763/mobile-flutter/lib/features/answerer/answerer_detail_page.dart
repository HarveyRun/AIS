import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/answerer_models.dart';
import '../../data/models/certification_models.dart';
import '../../data/models/app_global_settings.dart';
import '../certification/material_viewer.dart';
import '../certification/experience_additional_info_page.dart';

class AnswererDetailPage extends ConsumerStatefulWidget {
  const AnswererDetailPage({
    super.key,
    required this.uid,
    this.experienceCertificationId,
  });
  final String uid;
  final int? experienceCertificationId;

  @override
  ConsumerState<AnswererDetailPage> createState() => _AnswererDetailPageState();
}

class _AnswererDetailPageState extends ConsumerState<AnswererDetailPage> {
  static const bool _showInquiryFlowBeforeQuestion = false;

  Answerer? _answerer;
  bool _loading = true;
  bool _likeSubmitting = false;
  AppGlobalSettings? _settings;

  AnswererExperience? get _selectedExperience {
    final experiences = _answerer?.experiences ?? const <AnswererExperience>[];
    for (final item in experiences) {
      if (item.certificationId == widget.experienceCertificationId) return item;
    }
    return experiences.firstOrNull;
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final result = await Future.wait([
        ref.read(repositoryProvider).answerer(widget.uid),
        ref.read(repositoryProvider).appGlobalSettings(),
      ]);
      final person = result[0] as Answerer;
      final settings = result[1] as AppGlobalSettings;
      if (mounted) {
        setState(() {
          _answerer = person;
          _settings = settings;
        });
        unawaited(
          ref
              .read(analyticsProvider)
              .track(
                'profile_view',
                properties: {
                  'answerer_user_id': person.id,
                  'answerer_uid': person.uid,
                  'experience_names': person.experiences
                      .map((item) => item.title)
                      .toList(),
                },
              ),
        );
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _ask() async {
    final person = _answerer!;
    if (_selectedExperience?.canInquire != true) return;
    var depositRequired = false;
    await ref
        .read(analyticsProvider)
        .track(
          'inquiry_start',
          properties: {
            'answerer_user_id': person.id,
            'answerer_uid': person.uid,
            'experience_names': person.experiences
                .map((item) => item.title)
                .toList(),
          },
        );
    try {
      final existing = await ref.read(repositoryProvider).inquiries();
      depositRequired =
          existing
              .where(
                (item) =>
                    !item.isIncoming && item.status.toUpperCase() == 'PENDING',
              )
              .length >=
          (_settings?.freePendingInquiryLimit ?? 3);
      for (final inquiry in existing) {
        if (!inquiry.isIncoming &&
            inquiry.otherUserId == person.id &&
            const [
              'PENDING',
              'ACTIVE',
              'TEXT_LIMIT_REACHED',
            ].contains(inquiry.status.toUpperCase())) {
          if (mounted) context.push('/chat/${inquiry.id}');
          return;
        }
      }
      final communicating = existing.any(
        (item) => !item.isIncoming && item.canChat,
      );
      if (communicating) {
        if (mounted) {
          AppMessage.show(context, '你已有一条正在交流的询问，结束后才能发起新的询问');
        }
        return;
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
      return;
    }
    final experience = _selectedExperience;
    if (!mounted || experience?.certificationId == null) return;
    if (_showInquiryFlowBeforeQuestion) {
      final understood = await _showInquiryFlow(person.inquiryHourlyRate);
      if (!mounted || !understood) return;
    }
    final question = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (context) => _InquiryInfoSheet(
        depositRequired: depositRequired,
        freePendingLimit: _settings?.freePendingInquiryLimit ?? 3,
        depositAmount: _settings?.inquiryDepositAmount ?? 2,
      ),
    );
    if (!mounted || question == null) return;
    try {
      final created = await ref
          .read(repositoryProvider)
          .createInquiry(
            answererId: person.id,
            sourceExperienceCertificationId: experience!.certificationId!,
            question: question,
          );
      if (mounted) context.push('/chat/${created.id}');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  Future<void> _toggleLike() async {
    final experience = _selectedExperience;
    final certificationId = experience?.certificationId;
    if (experience == null || certificationId == null || _likeSubmitting) {
      return;
    }
    setState(() => _likeSubmitting = true);
    try {
      final result = await ref
          .read(repositoryProvider)
          .setExperienceLike(
            uid: widget.uid,
            certificationId: certificationId,
            liked: !experience.likedByCurrentUser,
          );
      if (!mounted) return;
      final current = _answerer;
      if (current == null) return;
      setState(() {
        _answerer = current.copyWith(
          experiences: current.experiences
              .map(
                (item) => item.certificationId == certificationId
                    ? item.copyWith(
                        likeCount: result.likeCount,
                        likedByCurrentUser: result.liked,
                      )
                    : item,
              )
              .toList(growable: false),
        );
      });
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _likeSubmitting = false);
    }
  }

  Future<bool> _showInquiryFlow(int hourlyRate) async {
    final settings = _settings;
    final initialMessages = settings?.initialTextMessageLimit ?? 50;
    return await showDialog<bool>(
          context: context,
          builder: (dialogContext) => AlertDialog(
            insetPadding: const EdgeInsets.symmetric(
              horizontal: 30,
              vertical: 24,
            ),
            title: Row(
              children: [
                const Expanded(
                  child: Text(
                    '询问前请确认',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                  ),
                ),
                IconButton(
                  tooltip: '关闭',
                  onPressed: () => Navigator.pop(dialogContext, false),
                  icon: const Icon(Icons.close_rounded),
                ),
              ],
            ),
            content: SizedBox(
              width: double.maxFinite,
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _FlowRule(
                      title: '文字交流',
                      content: '$initialMessages条免费文字消息。',
                    ),
                    const SizedBox(height: 10),
                    _FlowRule(title: '语音通话', content: '支持语音通话，按时长计费。'),
                  ],
                ),
              ),
            ),
            actions: [
              FilledButton(
                onPressed: () => Navigator.pop(dialogContext, true),
                child: const Text('开始询问'),
              ),
            ],
          ),
        ) ??
        false;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('经历详情')),
      body: _loading
          ? const SizedBox.shrink()
          : _answerer == null
          ? const Center(child: Text('档案不存在'))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 92),
                children: [
                  _WebAnswererOverview(
                    answerer: _answerer!,
                    preferredExperienceCertificationId:
                        widget.experienceCertificationId,
                    likeSubmitting: _likeSubmitting,
                    onToggleLike: _toggleLike,
                  ),
                ],
              ),
            ),
      bottomNavigationBar: _selectedExperience?.canInquire != true
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(10, 7, 10, 9),
              child: FilledButton.icon(
                onPressed: _ask,
                icon: const Icon(Icons.chat_bubble_outline_rounded),
                label: const Text('询问'),
              ),
            ),
    );
  }
}

class _FlowRule extends StatelessWidget {
  const _FlowRule({required this.title, required this.content});

  final String title;
  final String content;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleSmall),
          const SizedBox(height: 4),
          Text(
            content,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(height: 1.45),
          ),
        ],
      ),
    );
  }
}

class _InquiryInfoSheet extends StatefulWidget {
  const _InquiryInfoSheet({
    required this.depositRequired,
    required this.freePendingLimit,
    required this.depositAmount,
  });

  final bool depositRequired;
  final int freePendingLimit;
  final double depositAmount;

  @override
  State<_InquiryInfoSheet> createState() => _InquiryInfoSheetState();
}

class _InquiryInfoSheetState extends State<_InquiryInfoSheet> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _submit() {
    final value = _controller.text.trim();
    if (value.isEmpty) return;
    Navigator.of(context).pop(value);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        18,
        10,
        18,
        MediaQuery.viewInsetsOf(context).bottom + 16,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  '简单说说你的情况',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
              IconButton(
                onPressed: () => Navigator.of(context).pop(),
                icon: const Icon(Icons.close_rounded),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            '对方会先看到这段内容，再决定是否接受询问。',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          if (widget.depositRequired) ...[
            const SizedBox(height: 12),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 11),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.tertiaryContainer,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                '你已有${widget.freePendingLimit}条等待对方处理的询问。本次将暂存${widget.depositAmount.toStringAsFixed(widget.depositAmount % 1 == 0 ? 0 : 2)}元押金，对方拒绝或本次询问结束后，会自动退回账户余额。',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.onTertiaryContainer,
                  height: 1.45,
                ),
              ),
            ),
          ],
          const SizedBox(height: 16),
          TextField(
            controller: _controller,
            autofocus: true,
            minLines: 5,
            maxLines: 7,
            maxLength: 200,
            inputFormatters: AppInputFormatters.description(200),
            textInputAction: TextInputAction.done,
            onChanged: (_) => setState(() {}),
            onSubmitted: (_) => _submit(),
            decoration: const InputDecoration(
              hintText:
                  '请简单写清：你正在经历什么、你是什么身份、事情进行到哪一步、目前最关键的情况。\n\n例如：我是房主，准备装修一套新房，目前正在选择装修方式和装修公司，主要拿不准全包还是半包。',
            ),
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _controller.text.trim().isEmpty ? null : _submit,
              child: const Text('发起询问'),
            ),
          ),
        ],
      ),
    );
  }
}

// Kept temporarily while the Web-matched profile layout is rolled out.
// ignore: unused_element
class _AnswererOverview extends StatelessWidget {
  const _AnswererOverview({required this.answerer});

  final Answerer answerer;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Theme.of(context).colorScheme.surface,
      borderRadius: BorderRadius.circular(20),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                AppAvatar(
                  url: answerer.avatarUrl,
                  name: answerer.displayName,
                  radius: 30,
                ),
                const SizedBox(width: 13),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        answerer.displayName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'UID ${answerer.uid}',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.verified_rounded,
                      size: 14,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      '已核实',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Theme.of(context).colorScheme.primary,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const Divider(height: 1, indent: 16, endIndent: 16),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Row(
              children: [
                SizedBox(
                  width: 72,
                  child: Text(
                    '我的岗位',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
                Expanded(
                  child: Text(
                    answerer.mainJob,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ],
            ),
          ),
          const Divider(height: 1, indent: 16, endIndent: 16),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('亲身经历', style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(height: 10),
                if (answerer.experiences.isEmpty)
                  Text('暂无经历', style: Theme.of(context).textTheme.bodyMedium)
                else
                  ...answerer.experiences.map(
                    (item) => Padding(
                      padding: const EdgeInsets.only(bottom: 9),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Padding(
                            padding: const EdgeInsets.only(top: 7),
                            child: Container(
                              width: 5,
                              height: 5,
                              decoration: BoxDecoration(
                                color: Theme.of(context).colorScheme.primary,
                                shape: BoxShape.circle,
                              ),
                            ),
                          ),
                          const SizedBox(width: 9),
                          Expanded(
                            child: Text(
                              item.title,
                              style: Theme.of(context).textTheme.bodyMedium,
                            ),
                          ),
                        ],
                      ),
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

class _WebAnswererOverview extends StatelessWidget {
  const _WebAnswererOverview({
    required this.answerer,
    required this.likeSubmitting,
    required this.onToggleLike,
    this.preferredExperienceCertificationId,
  });

  final Answerer answerer;
  final bool likeSubmitting;
  final VoidCallback onToggleLike;
  final int? preferredExperienceCertificationId;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    AnswererExperience? experience;
    for (final item in answerer.experiences) {
      if (item.certificationId == preferredExperienceCertificationId) {
        experience = item;
        break;
      }
    }
    experience ??= answerer.experiences.firstOrNull;
    final publicPhotos =
        experience?.materials
            .where((item) => item.kind.toUpperCase() == 'IMAGE')
            .toList(growable: false) ??
        const <CertificationMaterial>[];
    final publicVideos =
        experience?.materials
            .where((item) => item.kind.toUpperCase() == 'VIDEO')
            .toList(growable: false) ??
        const <CertificationMaterial>[];
    final publicAudios =
        experience?.materials
            .where((item) => item.kind.toUpperCase() == 'AUDIO')
            .toList(growable: false) ??
        const <CertificationMaterial>[];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: theme.colorScheme.surface,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            children: [
              AppAvatar(
                url: answerer.avatarUrl,
                name: answerer.displayName,
                radius: 24,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      answerer.displayName,
                      style: theme.textTheme.titleMedium,
                    ),
                    const SizedBox(height: 3),
                    Text(
                      'UID ${answerer.uid}',
                      style: theme.textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _ProfileFactBox(
          title: experience?.title ?? '暂无经历',
          metadata: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Expanded(
                child: _ReferenceIndexIndicator(
                  value: experience?.referenceIndex,
                ),
              ),
              if (experience != null)
                _ExperienceLikeButton(
                  liked: experience.likedByCurrentUser,
                  count: experience.likeCount,
                  loading: likeSubmitting,
                  onTap: onToggleLike,
                ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (experience != null && experience.description.isNotEmpty)
                Text(
                  experience.description,
                  style: theme.textTheme.bodyMedium?.copyWith(height: 1.55),
                ),
            ],
          ),
        ),
        if (experience != null && !experience.additionalInfo.isEmpty) ...[
          const SizedBox(height: 12),
          ExperienceAdditionalInfoCard(value: experience.additionalInfo),
        ],
        if (publicPhotos.isNotEmpty ||
            publicVideos.isNotEmpty ||
            publicAudios.isNotEmpty) ...[
          const SizedBox(height: 12),
          _PublicMediaBox(
            photos: publicPhotos,
            videos: publicVideos,
            audios: publicAudios,
          ),
        ],
      ],
    );
  }
}

class _ExperienceLikeButton extends StatelessWidget {
  const _ExperienceLikeButton({
    required this.liked,
    required this.count,
    required this.loading,
    required this.onTap,
  });

  final bool liked;
  final int count;
  final bool loading;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final activeColor = theme.colorScheme.primary;
    final idleColor = theme.colorScheme.onSurfaceVariant;
    return Semantics(
      button: true,
      selected: liked,
      label: liked ? '取消点赞，当前$count次点赞' : '点赞，当前$count次点赞',
      child: Tooltip(
        message: liked ? '取消点赞' : '点赞',
        child: InkResponse(
          onTap: loading ? null : onTap,
          radius: 25,
          child: AnimatedOpacity(
            duration: const Duration(milliseconds: 160),
            opacity: loading ? 0.45 : 1,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 8),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 180),
                    transitionBuilder: (child, animation) =>
                        ScaleTransition(scale: animation, child: child),
                    child: Icon(
                      liked
                          ? Icons.thumb_up_alt_rounded
                          : Icons.thumb_up_alt_outlined,
                      key: ValueKey(liked),
                      size: 22,
                      color: liked ? activeColor : idleColor,
                    ),
                  ),
                  const SizedBox(width: 5),
                  Text(
                    '$count',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: liked ? activeColor : idleColor,
                      fontWeight: liked ? FontWeight.w700 : FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ReferenceIndexIndicator extends StatelessWidget {
  const _ReferenceIndexIndicator({required this.value});

  final int? value;

  Future<void> _showExplanation(BuildContext context) {
    return showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        insetPadding: const EdgeInsets.symmetric(horizontal: 30, vertical: 24),
        titlePadding: const EdgeInsets.fromLTRB(22, 18, 10, 0),
        contentPadding: const EdgeInsets.fromLTRB(22, 16, 22, 24),
        title: Row(
          children: [
            Expanded(
              child: Text(
                '参考指数说明',
                style: Theme.of(
                  dialogContext,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
              ),
            ),
            IconButton(
              tooltip: '关闭',
              onPressed: () => Navigator.of(dialogContext).pop(),
              icon: const Icon(Icons.close_rounded),
            ),
          ],
        ),
        content: const SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '参考指数系平台在审核经历时，基于多维度综合评估计算得出。\n\n'
                '分值越高，表示这段经历当前可参考的信息越充分。',
                style: TextStyle(height: 1.65),
              ),
              SizedBox(height: 18),
              Text(
                '该指数仅用于辅助了解内容，不代表平台对实际结果作出保证。',
                style: TextStyle(
                  color: Color(0xFFD13B32),
                  fontWeight: FontWeight.w700,
                  height: 1.5,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _levelText(int? normalized) {
    if (normalized == null) return '暂未评分';
    if (normalized < 40) return '信息较少';
    if (normalized < 70) return '信息一般';
    return '信息较充分';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final normalized = value?.clamp(0, 100);
    return Semantics(
      label: normalized == null ? '暂未评分' : '参考指数 $normalized',
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          _ReferenceScoreCircle(value: normalized),
          const SizedBox(width: 8),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    '参考指数',
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                      height: 1,
                    ),
                  ),
                  const SizedBox(width: 2),
                  Tooltip(
                    message: '查看指数说明',
                    child: InkResponse(
                      onTap: () => _showExplanation(context),
                      radius: 11,
                      child: Padding(
                        padding: const EdgeInsets.all(2),
                        child: Icon(
                          Icons.help_outline_rounded,
                          size: 16,
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 2),
              Text(
                _levelText(normalized),
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                  height: 1,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ReferenceScoreCircle extends StatelessWidget {
  const _ReferenceScoreCircle({required this.value});

  final int? value;

  Color _scoreColor(Color fallback) {
    final score = value;
    if (score == null) return fallback;
    const red = Color(0xFFD84A43);
    const yellow = Color(0xFFD39420);
    const green = Color(0xFF2E9461);
    if (score <= 50) {
      return Color.lerp(red, yellow, score / 50)!;
    }
    return Color.lerp(yellow, green, (score - 50) / 50)!;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scoreColor = _scoreColor(theme.colorScheme.outline);
    return SizedBox.square(
      dimension: 40,
      child: Stack(
        alignment: Alignment.center,
        children: [
          CustomPaint(
            size: const Size.square(40),
            painter: _ReferenceRingPainter(enabled: value != null),
          ),
          Container(
            width: 32,
            height: 32,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: theme.colorScheme.surface,
            ),
            child: Text(
              value?.toString() ?? '—',
              style: theme.textTheme.labelLarge?.copyWith(
                height: 1,
                fontWeight: FontWeight.w800,
                color: scoreColor,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ReferenceRingPainter extends CustomPainter {
  const _ReferenceRingPainter({required this.enabled});

  final bool enabled;

  @override
  void paint(Canvas canvas, Size size) {
    final bounds = Offset.zero & size;
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3.5
      ..isAntiAlias = true
      ..shader = enabled
          ? const SweepGradient(
              colors: [
                Color(0xFFD84A43),
                Color(0xFFE3AA32),
                Color(0xFF2E9461),
                Color(0xFFD84A43),
              ],
              stops: [0, 0.34, 0.67, 1],
            ).createShader(bounds)
          : null
      ..color = enabled ? Colors.white : const Color(0xFFD1CCC8);
    canvas.drawCircle(
      Offset(size.width / 2, size.height / 2),
      size.width / 2 - paint.strokeWidth / 2,
      paint,
    );
  }

  @override
  bool shouldRepaint(covariant _ReferenceRingPainter oldDelegate) {
    return oldDelegate.enabled != enabled;
  }
}

class _PublicMediaBox extends StatelessWidget {
  const _PublicMediaBox({
    required this.photos,
    required this.videos,
    required this.audios,
  });

  final List<CertificationMaterial> photos;
  final List<CertificationMaterial> videos;
  final List<CertificationMaterial> audios;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '证明资料',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 8),
          if (photos.isNotEmpty)
            _PublicMediaEntry(
              icon: Icons.photo_library_outlined,
              label: '用户照片',
              count: photos.length,
              onTap: () => openPhotoGallery(context, photos),
            ),
          if (videos.isNotEmpty)
            _PublicMediaEntry(
              icon: Icons.play_circle_outline_rounded,
              label: '用户录像',
              count: videos.length,
              onTap: () => openMaterialList(context, '用户录像', videos),
            ),
          if (audios.isNotEmpty)
            _PublicMediaEntry(
              icon: Icons.graphic_eq_rounded,
              label: '用户音频',
              count: audios.length,
              onTap: () => openMaterialList(context, '用户音频', audios),
            ),
        ],
      ),
    );
  }
}

class _PublicMediaEntry extends StatelessWidget {
  const _PublicMediaEntry({
    required this.icon,
    required this.label,
    required this.count,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final int count;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListTile(
      contentPadding: EdgeInsets.zero,
      minVerticalPadding: 4,
      leading: Icon(icon, color: theme.colorScheme.primary),
      title: Text(label),
      subtitle: Text('$count 项内容'),
      trailing: const Icon(Icons.chevron_right_rounded),
      onTap: onTap,
    );
  }
}

class _ProfileFactBox extends StatelessWidget {
  const _ProfileFactBox({
    required this.title,
    required this.metadata,
    required this.child,
  });

  final String title;
  final Widget metadata;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        borderRadius: BorderRadius.circular(19),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          metadata,
          const SizedBox(height: 14),
          child,
        ],
      ),
    );
  }
}
