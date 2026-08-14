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
        : answerer.experiences.map((item) => item.title).take(2).join('、');

    return Material(
      color: Theme.of(context).colorScheme.surface,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
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
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            answerer.mainJob,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                        ),
                        if (answerer.mainJobYears > 0)
                          Text(
                            '${answerer.mainJobYears}年',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                      ],
                    ),
                    const SizedBox(height: 2),
                    Text(
                      answerer.displayName == 'UID ${answerer.uid}'
                          ? 'UID ${answerer.uid}'
                          : '${answerer.displayName} · UID ${answerer.uid}',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Text(
                          '经历',
                          style: Theme.of(context).textTheme.bodySmall
                              ?.copyWith(
                                color: Theme.of(context).colorScheme.primary,
                                fontWeight: FontWeight.w600,
                              ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            experience,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Icon(
                Icons.chevron_right_rounded,
                size: 20,
                color: Theme.of(context).textTheme.bodySmall?.color,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
