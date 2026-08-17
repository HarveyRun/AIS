import 'package:flutter/material.dart';

import '../../data/models/answerer_models.dart';
import 'app_avatar.dart';

class AnswererCard extends StatelessWidget {
  const AnswererCard({super.key, required this.answerer, required this.onTap});

  final Answerer answerer;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final experience = answerer.experiences.isEmpty
        ? '暂未填写亲身经历'
        : answerer.experiences.map((item) => item.title).take(1).join();
    final theme = Theme.of(context);
    final dark = theme.brightness == Brightness.dark;
    final border = dark ? theme.colorScheme.outline : const Color(0xFFE7E1DC);
    final background = dark
        ? theme.colorScheme.surface
        : const Color(0xFFFFFEFD);

    return Material(
      color: background,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(color: border),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 15, 16, 16),
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
                            color: const Color(0xFF918C87),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Icon(
                    Icons.chevron_right_rounded,
                    size: 20,
                    color: Color(0xFF9D9792),
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
              const Padding(
                padding: EdgeInsets.only(left: 48, top: 10, bottom: 11),
                child: Divider(height: 1, color: Color(0xFFEAE5E1)),
              ),
              Row(
                children: [
                  SizedBox(
                    width: 48,
                    child: Text(
                      '亲身经历',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: const Color(0xFF827B75),
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
                          color: const Color(0xFF60766B),
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
