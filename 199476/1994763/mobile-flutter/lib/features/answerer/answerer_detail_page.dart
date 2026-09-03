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
import '../certification/material_viewer.dart';

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
      final person = await ref.read(repositoryProvider).answerer(widget.uid);
      if (mounted) {
        setState(() => _answerer = person);
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
      for (final inquiry in existing) {
        if (!inquiry.isIncoming &&
            inquiry.otherUserId == person.id &&
            const [
              'PENDING',
              'ACTIVE',
              'AWAITING_CONFIRMATION',
              'DISPUTED',
            ].contains(inquiry.status.toUpperCase())) {
          if (mounted) context.push('/chat/${inquiry.id}');
          return;
        }
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
      return;
    }
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _InquirySheet(
        answerer: person,
        preferredExperienceCertificationId: widget.experienceCertificationId,
      ),
    );
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
      bottomNavigationBar:
          _answerer == null || _selectedExperience?.canInquire != true
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
                  verified: answerer.identityVerified,
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
    final detailVideos =
        experience?.materials
            .where((item) => item.kind.toUpperCase() == 'DETAIL_VIDEO')
            .toList(growable: false) ??
        const <CertificationMaterial>[];
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
                verified: answerer.identityVerified,
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
              if (detailVideos.isNotEmpty) ...[
                if (experience != null && experience.description.isNotEmpty)
                  const SizedBox(height: 16),
                _DetailMaterial(
                  material: detailVideos.first,
                  onTap: () => openMaterial(context, detailVideos.first),
                  label: '详述录像',
                ),
              ],
            ],
          ),
        ),
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

class _DetailMaterial extends StatelessWidget {
  const _DetailMaterial({
    required this.material,
    required this.onTap,
    this.label,
  });
  final CertificationMaterial material;
  final VoidCallback onTap;
  final String? label;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: Container(
          width: (MediaQuery.sizeOf(context).width - 62) / 2,
          height: 112,
          color: Theme.of(context).colorScheme.surfaceContainerHigh,
          child: Stack(
            alignment: Alignment.center,
            children: [
              const Icon(Icons.play_circle_fill_rounded, size: 40),
              if (label != null)
                Positioned(
                  left: 10,
                  bottom: 8,
                  child: Text(
                    label!,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      fontWeight: FontWeight.w700,
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

class _InquirySheet extends ConsumerStatefulWidget {
  const _InquirySheet({
    required this.answerer,
    this.preferredExperienceCertificationId,
  });
  final Answerer answerer;
  final int? preferredExperienceCertificationId;
  @override
  ConsumerState<_InquirySheet> createState() => _InquirySheetState();
}

class _InquirySheetState extends ConsumerState<_InquirySheet> {
  final _amount = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (widget.answerer.experiences.isEmpty) {
      AppMessage.show(context, '对方暂无可询问的亲身经历');
      return;
    }
    final selectedExperience = widget.answerer.experiences.firstWhere(
      (item) =>
          item.certificationId == widget.preferredExperienceCertificationId,
      orElse: () => widget.answerer.experiences.first,
    );
    if (selectedExperience.certificationId == null) {
      AppMessage.show(context, '所选亲身经历暂不可询问');
      return;
    }
    if (!selectedExperience.canInquire) {
      AppMessage.show(context, '这段经历暂不可询问');
      return;
    }
    final amount = int.tryParse(_amount.text.trim());
    if (amount == null ||
        amount < widget.answerer.inquiryPriceMin ||
        amount > widget.answerer.inquiryPriceMax) {
      AppMessage.show(
        context,
        '请输入¥${widget.answerer.inquiryPriceMin}—¥${widget.answerer.inquiryPriceMax}之间的金额',
      );
      return;
    }
    setState(() => _submitting = true);
    try {
      await ref
          .read(analyticsProvider)
          .track(
            'inquiry_submit_click',
            properties: {
              'answerer_user_id': widget.answerer.id,
              'experience_id': selectedExperience.certificationId,
              'experience_name': selectedExperience.title,
              'amount_bucket': _amountBucket(amount),
            },
          );
      final created = await ref
          .read(repositoryProvider)
          .createInquiry(
            answererId: widget.answerer.id,
            sourceExperienceCertificationId:
                selectedExperience.certificationId!,
            amount: amount,
          );
      if (!mounted) return;
      Navigator.pop(context);
      context.push('/chat/${created.id}');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  String _amountBucket(int amount) {
    if (amount <= 20) return '1-20';
    if (amount <= 50) return '21-50';
    if (amount <= 100) return '51-100';
    if (amount <= 300) return '101-300';
    if (amount <= 1000) return '301-1000';
    return '1001-5000';
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        20,
        20,
        MediaQuery.viewInsetsOf(context).bottom + 22,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('发起询问', style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 4),
                    Text(
                      '发起后可先发送一条消息，对方回复后即可继续交流',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.close_rounded),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Text('你打算给多少钱', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 3),
          Text(
            '对方可接受 ¥${widget.answerer.inquiryPriceMin}—¥${widget.answerer.inquiryPriceMax}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _amount,
            keyboardType: TextInputType.number,
            inputFormatters: AppInputFormatters.positiveInteger(max: 5000),
            decoration: InputDecoration(
              prefixText: '¥ ',
              hintText:
                  '请输入${widget.answerer.inquiryPriceMin}—${widget.answerer.inquiryPriceMax}',
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '发起后金额暂时冻结，对方未接受会自动退回。',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 18),
          FilledButton(
            onPressed: _submitting ? null : _submit,
            child: const Text('确认发起'),
          ),
        ],
      ),
    );
  }
}
