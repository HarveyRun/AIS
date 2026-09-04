import 'package:flutter/material.dart';

Future<bool?> showHomeActivityRulesDialog(
  BuildContext context, {
  required String actionType,
}) {
  final content = _ActivityRuleContent.fromActionType(actionType);
  return showDialog<bool>(
    context: context,
    builder: (dialogContext) => _ActivityRulesDialog(content: content),
  );
}

class _ActivityRulesDialog extends StatelessWidget {
  const _ActivityRulesDialog({required this.content});

  final _ActivityRuleContent content;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    return Dialog(
      elevation: 0,
      backgroundColor: scheme.surface,
      insetPadding: const EdgeInsets.symmetric(horizontal: 22, vertical: 28),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: 420,
          maxHeight: MediaQuery.sizeOf(context).height * .76,
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 14, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Expanded(
                    child: Text(
                      '活动规则',
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w800,
                        height: 1.25,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(context, false),
                    tooltip: '关闭',
                    visualDensity: VisualDensity.compact,
                    icon: const Icon(Icons.close_rounded, size: 22),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Flexible(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      ...content.rules.indexed.map(
                        (entry) =>
                            _RuleItem(number: entry.$1 + 1, text: entry.$2),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 18),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => Navigator.pop(context, true),
                  child: Text(content.actionLabel),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RuleItem extends StatelessWidget {
  const _RuleItem({required this.number, required this.text});

  final int number;
  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 22,
            child: Text(
              '$number.',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: scheme.onSurfaceVariant,
                fontWeight: FontWeight.w600,
                height: 1.5,
              ),
            ),
          ),
          Expanded(
            child: Text(
              text,
              style: theme.textTheme.bodyMedium?.copyWith(height: 1.5),
            ),
          ),
        ],
      ),
    );
  }
}

class _ActivityRuleContent {
  const _ActivityRuleContent({
    required this.title,
    required this.rules,
    required this.actionLabel,
  });

  final String title;
  final List<String> rules;
  final String actionLabel;

  factory _ActivityRuleContent.fromActionType(String actionType) {
    return switch (actionType) {
      'FIRST_EXPERIENCE_REWARD' => const _ActivityRuleContent(
        title: '首次发布经历',
        rules: [
          '首次发布且审核通过的经历可获奖励。',
          '每账号仅限一次，以最先审核通过的经历为准。',
          '后续发布或升级经历，不重复奖励，不补差额。',
        ],
        actionLabel: '去发布',
      ),
      'INVITE_PUBLIC_EXPERIENCE' => const _ActivityRuleContent(
        title: '邀请好友分享公益经历',
        rules: ['被邀请人发布经历且通过审核后，您即可获得奖励。', '每成功邀请一位完成发布，即可获得一次奖励。', '同一被邀请人仅限奖励一次。'],
        actionLabel: '去发布',
      ),
      'INVITE_MONETIZED_EXPERIENCE' => const _ActivityRuleContent(
        title: '邀请好友分享变现经历',
        rules: ['被邀请人发布经历且通过审核后，您即可获得奖励。', '每成功邀请一位完成发布，即可获得一次奖励。', '同一被邀请人仅限奖励一次。'],
        actionLabel: '去发布',
      ),
      _ => const _ActivityRuleContent(
        title: '活动说明',
        rules: ['请按页面说明参与活动。'],
        actionLabel: '继续',
      ),
    };
  }
}
