import 'package:flutter/material.dart';

class JobCertificationNoticeDialog extends StatelessWidget {
  const JobCertificationNoticeDialog({super.key});

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final scheme = Theme.of(context).colorScheme;
    final success = dark ? const Color(0xFF7EC497) : const Color(0xFF43855A);
    final successSurface = Color.alphaBlend(
      success.withValues(alpha: dark ? .15 : .09),
      scheme.surface,
    );

    return Dialog(
      backgroundColor: successSurface,
      insetPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * .82,
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 10, 20, 18),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Align(
                alignment: Alignment.centerRight,
                child: IconButton(
                  onPressed: () => Navigator.pop(context, false),
                  tooltip: '关闭',
                  visualDensity: VisualDensity.compact,
                  icon: Icon(Icons.close_rounded, color: success),
                ),
              ),
              const Flexible(
                child: SingleChildScrollView(
                  child: _JobCertificationNoticeContent(),
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  style: FilledButton.styleFrom(backgroundColor: success),
                  onPressed: () => Navigator.pop(context, true),
                  child: const Text('继续认证'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _JobCertificationNoticeContent extends StatelessWidget {
  const _JobCertificationNoticeContent();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final normal = TextStyle(
      color: scheme.onSurfaceVariant,
      fontSize: 11,
      height: 1.65,
    );
    final strong = normal.copyWith(
      color: scheme.onSurface,
      fontWeight: FontWeight.w700,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text.rich(
          TextSpan(
            style: normal,
            children: [
              const TextSpan(text: '平台'),
              TextSpan(text: '不要求必须提交某一种证明材料', style: strong),
              const TextSpan(text: '。您可以根据自己的实际情况，提交能够证明相关职业经历的材料。'),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Text('为了更容易通过审核，建议优先提交以下材料：', style: normal),
        const SizedBox(height: 10),
        _RecommendationItem(
          number: '1',
          children: [
            TextSpan(text: '优先推荐', style: strong),
            const TextSpan(text: '：劳动合同、在职证明、离职证明等，能够直接体现'),
            TextSpan(text: '岗位/职业 + 任职时间', style: strong),
            const TextSpan(text: '的材料。'),
          ],
        ),
        const SizedBox(height: 10),
        _RecommendationItem(
          number: '2',
          children: [
            TextSpan(text: '其次推荐', style: strong),
            const TextSpan(text: '：社保、个税、公积金、工资流水等，能够证明'),
            TextSpan(text: '工作单位 + 工作时间', style: strong),
            const TextSpan(text: '的材料，并搭配其他能够证明岗位的材料。'),
          ],
        ),
        const SizedBox(height: 10),
        _RecommendationItem(
          number: '3',
          children: [
            TextSpan(text: '辅助材料', style: strong),
            const TextSpan(
              text: '：Offer、工作证、企业邮箱、项目记录、工作成果、原公司或相关人员证明等，可用于补充职业和任职时间信息。',
            ),
          ],
        ),
        const SizedBox(height: 12),
        Text('材料可以组合提交，不要求覆盖全部工作经历，也不要求提供每一份工作合同。', style: strong),
        const SizedBox(height: 12),
        Text.rich(
          TextSpan(
            style: normal,
            children: [
              const TextSpan(text: '相关职业累计经历达到 '),
              TextSpan(text: '5 年及以上', style: strong),
              const TextSpan(text: '，即可满足职业年限审核要求。审核通过后，平台将根据您提交的有效材料展示您的'),
              TextSpan(text: '真实职业工龄', style: strong),
              const TextSpan(text: '；提交的经历越完整、可验证年限越长，越有助于提升您的合作机会。'),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Text('如部分过往经历已经无法提供材料，也无需担心，优先提交您目前能够找到、证明力较强的材料即可。', style: normal),
      ],
    );
  }
}

class _RecommendationItem extends StatelessWidget {
  const _RecommendationItem({required this.number, required this.children});

  final String number;
  final List<InlineSpan> children;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final scheme = Theme.of(context).colorScheme;
    final success = dark ? const Color(0xFF7EC497) : const Color(0xFF43855A);
    final normal = TextStyle(
      color: scheme.onSurfaceVariant,
      fontSize: 11,
      height: 1.65,
    );

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 22,
          height: 22,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: success.withValues(alpha: .14),
            shape: BoxShape.circle,
          ),
          child: Text(
            number,
            style: TextStyle(
              color: success,
              fontSize: 10,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        const SizedBox(width: 9),
        Expanded(
          child: Text.rich(TextSpan(style: normal, children: children)),
        ),
      ],
    );
  }
}
