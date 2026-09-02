import 'dart:async';

import 'package:flutter/material.dart';

import '../../data/models/answerer_models.dart';

class ExperienceTooltipTag extends StatefulWidget {
  const ExperienceTooltipTag({
    super.key,
    required this.experience,
    this.backgroundColor,
    this.textColor,
    this.padding = const EdgeInsets.symmetric(horizontal: 9, vertical: 7),
    this.borderRadius = 8,
    this.textStyle,
  });

  final AnswererExperience experience;
  final Color? backgroundColor;
  final Color? textColor;
  final EdgeInsetsGeometry padding;
  final double borderRadius;
  final TextStyle? textStyle;

  @override
  State<ExperienceTooltipTag> createState() => _ExperienceTooltipTagState();
}

class _ExperienceTooltipTagState extends State<ExperienceTooltipTag> {
  final LayerLink _layerLink = LayerLink();
  OverlayEntry? _overlayEntry;
  Timer? _hideTimer;

  @override
  void dispose() {
    _hideTooltip();
    super.dispose();
  }

  void _toggleTooltip() {
    if (_overlayEntry == null) {
      _showTooltip();
    } else {
      _hideTooltip();
    }
  }

  void _showTooltip() {
    final overlay = Overlay.of(context, rootOverlay: true);
    final theme = Theme.of(context);
    final description = widget.experience.description.trim().isEmpty
        ? '暂无经历简介'
        : widget.experience.description.trim();
    final tooltipColor = theme.colorScheme.inverseSurface;

    _overlayEntry = OverlayEntry(
      builder: (overlayContext) => Stack(
        children: [
          Positioned.fill(
            child: GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: _hideTooltip,
              child: const SizedBox.expand(),
            ),
          ),
          CompositedTransformFollower(
            link: _layerLink,
            showWhenUnlinked: false,
            targetAnchor: Alignment.topCenter,
            followerAnchor: Alignment.bottomCenter,
            offset: const Offset(0, -5),
            child: Material(
              color: Colors.transparent,
              child: GestureDetector(
                onTap: _hideTooltip,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      constraints: const BoxConstraints(maxWidth: 280),
                      margin: const EdgeInsets.symmetric(horizontal: 18),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 13,
                        vertical: 10,
                      ),
                      decoration: BoxDecoration(
                        color: tooltipColor,
                        borderRadius: BorderRadius.circular(10),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: .16),
                            blurRadius: 12,
                            offset: const Offset(0, 5),
                          ),
                        ],
                      ),
                      child: Text(
                        description,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onInverseSurface,
                          height: 1.45,
                        ),
                      ),
                    ),
                    CustomPaint(
                      size: const Size(16, 8),
                      painter: _TooltipArrowPainter(color: tooltipColor),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
    overlay.insert(_overlayEntry!);
    _hideTimer = Timer(const Duration(seconds: 5), _hideTooltip);
  }

  void _hideTooltip() {
    _hideTimer?.cancel();
    _hideTimer = null;
    _overlayEntry?.remove();
    _overlayEntry = null;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return CompositedTransformTarget(
      link: _layerLink,
      child: Semantics(
        button: true,
        label: '查看${widget.experience.title}的经历简介',
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: _toggleTooltip,
          child: Container(
            padding: widget.padding,
            decoration: BoxDecoration(
              color:
                  widget.backgroundColor ??
                  theme.colorScheme.surfaceContainerHigh,
              borderRadius: BorderRadius.circular(widget.borderRadius),
            ),
            child: Text(
              widget.experience.title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style:
                  widget.textStyle ??
                  theme.textTheme.bodySmall?.copyWith(color: widget.textColor),
            ),
          ),
        ),
      ),
    );
  }
}

class _TooltipArrowPainter extends CustomPainter {
  const _TooltipArrowPainter({required this.color});

  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final path = Path()
      ..moveTo(0, 0)
      ..lineTo(size.width, 0)
      ..lineTo(size.width / 2, size.height)
      ..close();
    canvas.drawPath(path, Paint()..color = color);
  }

  @override
  bool shouldRepaint(covariant _TooltipArrowPainter oldDelegate) {
    return oldDelegate.color != color;
  }
}
