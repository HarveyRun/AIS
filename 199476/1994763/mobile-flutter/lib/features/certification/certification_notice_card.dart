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
    this.marker,
  });

  final String text;
  final List<String> emphasis;
  final String? marker;
}

class CertificationNoticeCard extends StatelessWidget {
  const CertificationNoticeCard({
    super.key,
    required this.tone,
    this.label = '审核标准',
    this.title,
    this.description,
    this.items = const [],
    this.paragraphs = const [],
    this.footer,
    this.onClose,
    this.prominentLabel = false,
  });

  final String? title;
  final String label;
  final String? description;
  final List<CertificationNoticeItem> items;
  final List<CertificationNoticeParagraph> paragraphs;
  final CertificationNoticeTone tone;
  final String? footer;
  final VoidCallback? onClose;
  final bool prominentLabel;

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
                      Icon(
                        _toneIcon,
                        size: prominentLabel ? 21 : 16,
                        color: toneColor,
                      ),
                      SizedBox(width: prominentLabel ? 8 : 6),
                      Text(
                        label,
                        style: TextStyle(
                          color: toneColor,
                          fontSize: prominentLabel ? 18 : 11,
                          fontWeight: prominentLabel
                              ? FontWeight.w700
                              : FontWeight.w600,
                        ),
                      ),
                      if (onClose != null) ...[
                        const Spacer(),
                        IconButton(
                          onPressed: onClose,
                          tooltip: '关闭',
                          visualDensity: VisualDensity.compact,
                          constraints: const BoxConstraints.tightFor(
                            width: 32,
                            height: 32,
                          ),
                          padding: EdgeInsets.zero,
                          icon: Icon(
                            Icons.close_rounded,
                            size: 20,
                            color: scheme.onSurfaceVariant,
                          ),
                        ),
                      ],
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
    final theme = Theme.of(context);
    final scheme = Theme.of(context).colorScheme;
    final hasMarker = paragraph.marker != null;
    final normalStyle = TextStyle(
      color: hasMarker ? scheme.onSurface : scheme.onSurfaceVariant,
      fontSize: hasMarker ? 13 : 11,
      height: hasMarker ? 1.6 : 1.65,
    );
    final emphasisStyle = normalStyle.copyWith(
      color: scheme.onSurface,
      fontWeight: FontWeight.w700,
    );

    final content = Text.rich(
      TextSpan(children: _buildSpans(normalStyle, emphasisStyle)),
    );
    if (!hasMarker) return content;

    final markerColor = Theme.of(context).brightness == Brightness.dark
        ? const Color(0xFFE0A24A)
        : const Color(0xFFB36B18);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 22,
          height: 22,
          margin: const EdgeInsets.only(top: 1),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: markerColor.withValues(alpha: .12),
            shape: BoxShape.circle,
          ),
          child: Text(
            paragraph.marker!,
            style: theme.textTheme.labelSmall?.copyWith(
              color: markerColor,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(child: content),
      ],
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
