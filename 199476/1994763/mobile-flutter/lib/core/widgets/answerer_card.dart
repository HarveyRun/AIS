import 'package:flutter/material.dart';

import '../../data/models/answerer_models.dart';
import 'app_avatar.dart';

class AnswererCard extends StatelessWidget {
  const AnswererCard({
    super.key,
    required this.answerer,
    required this.onTap,
    this.flat = false,
  });

  final Answerer answerer;
  final VoidCallback onTap;
  final bool flat;

  @override
  Widget build(BuildContext context) {
    final experience = answerer.experiences.isEmpty
        ? '暂无经历'
        : answerer.experiences.map((item) => item.title).take(1).join();
    final theme = Theme.of(context);
    final dark = theme.brightness == Brightness.dark;
    final background = theme.colorScheme.surface;

    return Material(
      color: background,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(flat ? 18 : 20),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: EdgeInsets.fromLTRB(
            flat ? 18 : 16,
            flat ? 18 : 15,
            flat ? 18 : 16,
            flat ? 18 : 16,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  AppAvatar(
                    url: answerer.avatarUrl,
                    name: answerer.displayName,
                    radius: 24,
                    verified: true,
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
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontSize: 16,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(height: 5),
                        Text(
                          'UID ${answerer.uid}',
                          style: theme.textTheme.bodySmall?.copyWith(
                            fontSize: 10,
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Icon(
                    Icons.chevron_right_rounded,
                    size: 20,
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  SizedBox(
                    width: 48,
                    child: Text(
                      '主职',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: const Color(0xFFC73F36),
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  Expanded(
                    child: Text(
                      answerer.mainJob,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurface,
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  if (answerer.mainJobYears > 0)
                    Text(
                      '${answerer.mainJobYears}年经验',
                      style: theme.textTheme.bodySmall?.copyWith(fontSize: 10),
                    ),
                ],
              ),
              if (flat)
                const SizedBox(height: 15)
              else
                Padding(
                  padding: const EdgeInsets.only(left: 48, top: 10, bottom: 11),
                  child: Divider(
                    height: 1,
                    color: theme.colorScheme.outlineVariant,
                  ),
                ),
              Row(
                children: [
                  SizedBox(
                    width: 48,
                    child: Text(
                      '亲身经历',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                        fontSize: 10,
                      ),
                    ),
                  ),
                  Flexible(
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 9,
                        vertical: 5,
                      ),
                      decoration: BoxDecoration(
                        color: dark
                            ? theme.colorScheme.surfaceContainerHigh
                            : const Color(0xFFEEF4F0),
                        borderRadius: BorderRadius.circular(7),
                      ),
                      child: Text(
                        experience,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.bodySmall?.copyWith(
                          fontSize: 10,
                          color: dark
                              ? const Color(0xFFA7C2B3)
                              : const Color(0xFF60766B),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
