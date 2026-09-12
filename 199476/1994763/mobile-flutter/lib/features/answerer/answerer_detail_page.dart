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
  Answerer? _answerer;
  bool _loading = true;
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
        setState(() { _answerer = person; _settings = settings; });
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
              'TEXT_ENDED',
              'PAID_ACTIVE',
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
    final understood = await _showInquiryFlow(person.inquiryHourlyRate);
    if (!mounted || !understood) return;
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

  Future<bool> _showInquiryFlow(int hourlyRate) async {
    final settings = _settings;
    final initialMessages = settings?.initialTextMessageLimit ?? 50;
    final messageLength = settings?.textMessageMaxLength ?? 100;
    final rewardMessages = settings?.voiceRewardMessages ?? 50;
    final rewardMinutes = ((settings?.voiceRewardSeconds ?? 300) / 60).ceil();
    final maxDays = settings?.inquiryMaxDurationDays ?? 20;
    return await showDialog<bool>(
          context: context,
          builder: (dialogContext) => AlertDialog(
            title: const Text('询问前请确认'),
            content: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _FlowRule(
                    title: '先文字交流',
                    content: '对方接受后，双方各有$initialMessages条免费文字消息，每条最多$messageLength字；每完成一次不少于$rewardMinutes分钟的正常语音通话，双方各增加$rewardMessages条。',
                  ),
                  _FlowRule(
                    title: '语音通话',
                    content: '对方当前设置 ¥$hourlyRate/小时，接通后按实际通话时间计费。',
                  ),
                  const _FlowRule(
                    title: '退款与结算',
                    content:
                        '对方未接听或通话未接通，冻结金额退回余额；通话接通后，按实际通话时间结算。',
                  ),
                  _FlowRule(
                    title: '最长交流期限',
                    content: '对方接受询问后，整次询问最多保留$maxDays天，到期将自动结束。',
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(dialogContext, false),
                child: const Text('暂不询问'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(dialogContext, true),
                child: const Text('已了解，继续'),
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
  const _InquiryInfoSheet({required this.depositRequired,required this.freePendingLimit,required this.depositAmount});

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
    this.preferredExperienceCertificationId,
  });

  final Answerer answerer;
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
  const _ProfileFactBox({required this.title, required this.child});

  final String title;
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
          const SizedBox(height: 14),
          child,
        ],
      ),
    );
  }
}
