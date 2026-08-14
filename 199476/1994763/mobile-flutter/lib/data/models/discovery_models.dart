class DiscoveryCategory {
  const DiscoveryCategory({
    required this.code,
    required this.name,
    required this.subcategories,
  });

  factory DiscoveryCategory.fromJson(Map<String, dynamic> json) {
    return DiscoveryCategory(
      code: json['code']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      subcategories: (json['subcategories'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(DiscoverySubcategory.fromJson)
          .toList(growable: false),
    );
  }

  final String code;
  final String name;
  final List<DiscoverySubcategory> subcategories;
}

class DiscoverySubcategory {
  const DiscoverySubcategory({
    required this.id,
    required this.name,
    required this.matters,
    required this.experiences,
  });

  factory DiscoverySubcategory.fromJson(Map<String, dynamic> json) {
    return DiscoverySubcategory(
      id: _int(json['id']),
      name: json['name']?.toString() ?? '',
      matters: (json['matters'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(DiscoveryMatter.fromJson)
          .toList(growable: false),
      experiences: (json['experiences'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(DiscoveryExperience.fromJson)
          .toList(growable: false),
    );
  }

  final int id;
  final String name;
  final List<DiscoveryMatter> matters;
  final List<DiscoveryExperience> experiences;
}

class DiscoveryJob {
  const DiscoveryJob({
    required this.id,
    required this.name,
    required this.answererCount,
  });

  factory DiscoveryJob.fromJson(Map<String, dynamic> json) {
    return DiscoveryJob(
      id: _int(json['id']),
      name: json['name']?.toString() ?? '',
      answererCount: _int(json['answererCount']),
    );
  }

  final int id;
  final String name;
  final int answererCount;
}

class DiscoveryMatter {
  const DiscoveryMatter({
    required this.id,
    required this.title,
    required this.jobs,
  });

  factory DiscoveryMatter.fromJson(Map<String, dynamic> json) {
    return DiscoveryMatter(
      id: _int(json['id']),
      title: json['title']?.toString() ?? '',
      jobs: (json['jobs'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(DiscoveryJob.fromJson)
          .toList(growable: false),
    );
  }

  final int id;
  final String title;
  final List<DiscoveryJob> jobs;
}

class DiscoveryExperience {
  const DiscoveryExperience({
    required this.id,
    required this.title,
    required this.answererCount,
  });

  factory DiscoveryExperience.fromJson(Map<String, dynamic> json) {
    return DiscoveryExperience(
      id: _int(json['id']),
      title: (json['title'] ?? json['name'])?.toString() ?? '',
      answererCount: _int(json['answererCount']),
    );
  }

  final int id;
  final String title;
  final int answererCount;
}

class DiscoverySearchItem {
  const DiscoverySearchItem({
    required this.id,
    required this.title,
    required this.categoryId,
    required this.categoryName,
    required this.answererCount,
  });

  factory DiscoverySearchItem.fromJson(Map<String, dynamic> json) {
    return DiscoverySearchItem(
      id: _int(json['id']),
      title: (json['title'] ?? json['name'])?.toString() ?? '',
      categoryId: _int(json['categoryId']),
      categoryName: json['categoryName']?.toString() ?? '',
      answererCount: _int(json['answererCount']),
    );
  }

  final int id;
  final String title;
  final int categoryId;
  final String categoryName;
  final int answererCount;
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
