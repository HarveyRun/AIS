import 'package:flutter/material.dart';

enum CertificationNoticeTone { success, warning, error, info }

class CertificationNoticeItem {
  const CertificationNoticeItem({required this.value, required this.label});

  final String value;
  final String label;
}

class CertificationNoticeParagraph {
  const CertificationNoticeParagraph({
    required this.text,
    this.emphasis = const [],
  });

  final String text;
  final List<String> emphasis;
}

class CertificationNoticeCard extends StatelessWidget {
  const CertificationNoticeCard({
    super.key,
    required this.tone,
    this.title,
    this.description,
    this.items = const [],
    this.paragraphs = const [],
    this.footer,
  });

  final String? title;
  final String? description;
  final List<CertificationNoticeItem> items;
  final List<CertificationNoticeParagraph> paragraphs;
  final CertificationNoticeTone tone;
  final String? footer;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final scheme = Theme.of(context).colorScheme;
    final toneColor = _toneColor(dark);
    final backgroundColor = Color.alphaBlend(
      toneColor.withValues(alpha: dark ? .14 : .08),
      scheme.surface,
    );

    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: DecoratedBox(
        decoration: BoxDecoration(color: backgroundColor),
        child: Stack(
          children: [
            Positioned(
              right: -24,
              top: -34,
              child: Container(
                width: 118,
                height: 118,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: toneColor.withValues(alpha: .07),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 17, 18, 18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(_toneIcon, size: 16, color: toneColor),
                      const SizedBox(width: 6),
                      Text(
                        '温馨提示',
                        style: TextStyle(
                          color: toneColor,
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                  if (title != null) ...[
                    const SizedBox(height: 13),
                    Text(
                      title!,
                      style: TextStyle(
                        color: scheme.onSurface,
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        height: 1.25,
                      ),
                    ),
                  ],
                  if (description != null) ...[
                    const SizedBox(height: 5),
                    Text(
                      description!,
                      style: TextStyle(
                        color: scheme.onSurfaceVariant,
                        fontSize: 10,
                        height: 1.45,
                      ),
                    ),
                  ],
                  if (items.isNotEmpty) ...[
                    const SizedBox(height: 17),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        for (var index = 0; index < items.length; index++) ...[
                          if (index > 0)
                            Container(
                              width: 1,
                              height: 39,
                              margin: const EdgeInsets.symmetric(
                                horizontal: 18,
                              ),
                              color: toneColor.withValues(alpha: .16),
                            ),
                          Expanded(child: _NoticeItem(item: items[index])),
                        ],
                      ],
                    ),
                  ],
                  if (paragraphs.isNotEmpty) ...[
                    const SizedBox(height: 13),
                    for (var index = 0; index < paragraphs.length; index++) ...[
                      _NoticeParagraph(paragraph: paragraphs[index]),
                      if (index < paragraphs.length - 1)
                        const SizedBox(height: 11),
                    ],
                  ],
                  if (footer != null) ...[
                    const SizedBox(height: 15),
                    Text(
                      footer!,
                      style: TextStyle(
                        color: scheme.onSurfaceVariant,
                        fontSize: 10,
                        height: 1.45,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Color _toneColor(bool dark) => switch (tone) {
    CertificationNoticeTone.success =>
      dark ? const Color(0xFF7EC497) : const Color(0xFF43855A),
    CertificationNoticeTone.warning =>
      dark ? const Color(0xFFE0A24A) : const Color(0xFFB36B18),
    CertificationNoticeTone.error =>
      dark ? const Color(0xFFE57B74) : const Color(0xFFBE3D35),
    CertificationNoticeTone.info =>
      dark ? const Color(0xFF76A9E0) : const Color(0xFF3979B9),
  };

  IconData get _toneIcon => switch (tone) {
    CertificationNoticeTone.success => Icons.check_circle_outline_rounded,
    CertificationNoticeTone.warning => Icons.warning_amber_rounded,
    CertificationNoticeTone.error => Icons.error_outline_rounded,
    CertificationNoticeTone.info => Icons.info_outline_rounded,
  };
}

class _NoticeParagraph extends StatelessWidget {
  const _NoticeParagraph({required this.paragraph});

  final CertificationNoticeParagraph paragraph;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final normalStyle = TextStyle(
      color: scheme.onSurfaceVariant,
      fontSize: 11,
      height: 1.65,
    );
    final emphasisStyle = normalStyle.copyWith(
      color: scheme.onSurface,
      fontWeight: FontWeight.w700,
    );

    return Text.rich(
      TextSpan(children: _buildSpans(normalStyle, emphasisStyle)),
    );
  }

  List<TextSpan> _buildSpans(TextStyle normal, TextStyle emphasis) {
    if (paragraph.emphasis.isEmpty) {
      return [TextSpan(text: paragraph.text, style: normal)];
    }

    final spans = <TextSpan>[];
    var remaining = paragraph.text;
    while (remaining.isNotEmpty) {
      String? nearest;
      var nearestIndex = remaining.length;
      for (final candidate in paragraph.emphasis) {
        final index = remaining.indexOf(candidate);
        if (index >= 0 && index < nearestIndex) {
          nearest = candidate;
          nearestIndex = index;
        }
      }

      if (nearest == null) {
        spans.add(TextSpan(text: remaining, style: normal));
        break;
      }
      if (nearestIndex > 0) {
        spans.add(
          TextSpan(text: remaining.substring(0, nearestIndex), style: normal),
        );
      }
      spans.add(TextSpan(text: nearest, style: emphasis));
      remaining = remaining.substring(nearestIndex + nearest.length);
    }
    return spans;
  }
}

class _NoticeItem extends StatelessWidget {
  const _NoticeItem({required this.item});

  final CertificationNoticeItem item;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          item.value,
          style: TextStyle(
            color: scheme.onSurface,
            fontSize: 16,
            fontWeight: FontWeight.w700,
            height: 1.15,
          ),
        ),
        const SizedBox(height: 5),
        Text(
          item.label,
          style: TextStyle(
            color: scheme.onSurfaceVariant,
            fontSize: 10,
            height: 1.3,
          ),
        ),
      ],
    );
  }
}
