import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../config/app_config.dart';

class AppAvatar extends StatelessWidget {
  const AppAvatar({
    super.key,
    this.url,
    this.name = '',
    this.radius = 24,
    this.verified = false,
  });

  final String? url;
  final String name;
  final double radius;
  final bool verified;

  @override
  Widget build(BuildContext context) {
    final resolved = AppConfig.resolveImage(url);
    return Stack(
      clipBehavior: Clip.none,
      children: [
        CircleAvatar(
          radius: radius,
          backgroundColor: Theme.of(
            context,
          ).colorScheme.surfaceContainerHighest,
          backgroundImage: resolved.hasScheme
              ? CachedNetworkImageProvider(resolved.toString())
              : null,
          child: !resolved.hasScheme
              ? Icon(
                  Icons.person_rounded,
                  size: radius * 1.08,
                  color: Theme.of(context).hintColor,
                )
              : null,
        ),
        if (verified)
          Positioned(
            right: -1,
            bottom: -1,
            child: Container(
              width: radius * .72,
              height: radius * .72,
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primary,
                shape: BoxShape.circle,
                border: Border.all(
                  color: Theme.of(context).scaffoldBackgroundColor,
                  width: 2,
                ),
              ),
              child: Icon(
                Icons.check_rounded,
                size: radius * .46,
                color: Colors.white,
              ),
            ),
          ),
      ],
    );
  }
}
