import 'package:flutter/material.dart';

import '../../data/models/answerer_models.dart';
import '../../features/certification/material_viewer.dart';
import 'app_avatar.dart';
import 'experience_tooltip_tag.dart';

class AnswererCard extends StatelessWidget {
  const AnswererCard({
    super.key,
    required this.answerer,
    required this.onTap,
    this.experience,
    this.flat = false,
  });
  final Answerer answerer;
  final AnswererExperience? experience;
  final VoidCallback onTap;
  final bool flat;

  @override
  Widget build(BuildContext context) => Material(
    color: Theme.of(context).colorScheme.surface,
    borderRadius: BorderRadius.circular(18),
    clipBehavior: Clip.antiAlias,
    child: InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 20),
        child: experience == null
            ? _LegacyContent(answerer: answerer)
            : _ExperienceContent(answerer: answerer, experience: experience!),
      ),
    ),
  );
}

class _ExperienceContent extends StatelessWidget {
  const _ExperienceContent({required this.answerer, required this.experience});
  final Answerer answerer;
  final AnswererExperience experience;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final detailVideos = experience.materials
        .where((item) => item.kind.toUpperCase() == 'DETAIL_VIDEO')
        .toList(growable: false);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          experience.title,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.titleLarge?.copyWith(
            fontSize: 17,
            height: 1.35,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            AppAvatar(
              url: answerer.avatarUrl,
              name: answerer.displayName,
              radius: 15,
              verified: answerer.identityVerified,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                answerer.displayName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ),
        if (experience.description.trim().isNotEmpty) ...[
          const SizedBox(height: 16),
          Text(
            experience.description,
            maxLines: 5,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.bodyMedium?.copyWith(
              height: 1.55,
              color: theme.colorScheme.onSurface.withValues(alpha: 0.78),
            ),
          ),
        ],
        const SizedBox(height: 20),
        Row(
          children: [
            if (detailVideos.isNotEmpty)
              InkWell(
                onTap: () => openMaterial(context, detailVideos.first),
                borderRadius: BorderRadius.circular(8),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 5),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.play_circle_fill_rounded,
                        size: 22,
                        color: theme.colorScheme.primary,
                      ),
                      const SizedBox(width: 7),
                      Text(
                        '查看详述录像',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.primary,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            const Spacer(),
            Text(
              '查看详情',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.primary,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _LegacyContent extends StatelessWidget {
  const _LegacyContent({required this.answerer});
  final Answerer answerer;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            AppAvatar(
              url: answerer.avatarUrl,
              name: answerer.displayName,
              radius: 24,
              verified: answerer.identityVerified,
            ),
            const SizedBox(width: 13),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    answerer.displayName,
                    style: theme.textTheme.titleMedium,
                  ),
                  Text('UID ${answerer.uid}', style: theme.textTheme.bodySmall),
                ],
              ),
            ),
            const Icon(Icons.chevron_right_rounded),
          ],
        ),
        const SizedBox(height: 14),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: answerer.experiences
                .map(
                  (item) => Padding(
                    padding: const EdgeInsets.only(right: 6),
                    child: ExperienceTooltipTag(experience: item),
                  ),
                )
                .toList(),
          ),
        ),
      ],
    );
  }
}
