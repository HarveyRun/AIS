class HomeBannerItem {
  const HomeBannerItem({
    required this.id,
    required this.displayMode,
    required this.actionType,
    required this.labelText,
    required this.title,
    required this.description,
    required this.imageUrl,
  });

  factory HomeBannerItem.fromJson(Map<String, dynamic> json) {
    return HomeBannerItem(
      id: (json['id'] as num?)?.toInt() ?? 0,
      displayMode: json['displayMode']?.toString() ?? 'TEXT_ONLY',
      actionType: json['actionType']?.toString() ?? 'NONE',
      labelText: json['labelText']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      imageUrl: json['imageUrl']?.toString() ?? '',
    );
  }

  final int id;
  final String displayMode;
  final String actionType;
  final String labelText;
  final String title;
  final String description;
  final String imageUrl;

  bool get showsImage => displayMode != 'TEXT_ONLY' && imageUrl.isNotEmpty;
  bool get showsText => displayMode != 'IMAGE_ONLY';
  bool get opensPlatformIntroduction => actionType == 'PLATFORM_INTRODUCTION';
  bool get opensActivityRules =>
      const {'FIRST_EXPERIENCE_REWARD'}.contains(actionType);

  String get targetPath {
    return switch (actionType) {
      'MY_EXPERIENCES' => '/profile/certifications/experiences',
      _ => '',
    };
  }

  bool get canOpen =>
      opensPlatformIntroduction || opensActivityRules || targetPath.isNotEmpty;
}

class HomeBannerAvailability {
  const HomeBannerAvailability({
    required this.available,
    required this.state,
    required this.message,
    this.banner,
  });

  factory HomeBannerAvailability.fromJson(Map<String, dynamic> json) {
    final rawBanner = json['banner'];
    return HomeBannerAvailability(
      available: json['available'] == true,
      state: json['state']?.toString() ?? 'OFFLINE',
      message: json['message']?.toString() ?? '',
      banner: rawBanner is Map
          ? HomeBannerItem.fromJson(Map<String, dynamic>.from(rawBanner))
          : null,
    );
  }

  final bool available;
  final String state;
  final String message;
  final HomeBannerItem? banner;
}
