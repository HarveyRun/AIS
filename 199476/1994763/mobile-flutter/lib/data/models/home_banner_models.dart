class HomeBannerItem {
  const HomeBannerItem({
    required this.id,
    required this.displayMode,
    required this.labelText,
    required this.title,
    required this.description,
    required this.imageUrl,
  });

  factory HomeBannerItem.fromJson(Map<String, dynamic> json) {
    return HomeBannerItem(
      id: (json['id'] as num?)?.toInt() ?? 0,
      displayMode: json['displayMode']?.toString() ?? 'TEXT_ONLY',
      labelText: json['labelText']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      imageUrl: json['imageUrl']?.toString() ?? '',
    );
  }

  final int id;
  final String displayMode;
  final String labelText;
  final String title;
  final String description;
  final String imageUrl;

  bool get showsImage => displayMode != 'TEXT_ONLY' && imageUrl.isNotEmpty;
  bool get showsText => displayMode != 'IMAGE_ONLY';
}
