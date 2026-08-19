class AppUpdateInfo {
  const AppUpdateInfo({
    required this.hasUpdate,
    required this.forceUpdate,
    required this.latestVersionName,
    required this.latestVersionCode,
    required this.minimumSupportedVersionCode,
    required this.title,
    required this.updateContent,
    required this.downloadUrl,
  });

  factory AppUpdateInfo.fromJson(Map<String, dynamic> json) {
    return AppUpdateInfo(
      hasUpdate: json['hasUpdate'] == true,
      forceUpdate: json['forceUpdate'] == true,
      latestVersionName: json['latestVersionName']?.toString() ?? '',
      latestVersionCode: _int(json['latestVersionCode']),
      minimumSupportedVersionCode: _int(json['minimumSupportedVersionCode']),
      title: json['title']?.toString() ?? '发现新版本',
      updateContent: json['updateContent']?.toString() ?? '',
      downloadUrl: json['downloadUrl']?.toString() ?? '',
    );
  }

  final bool hasUpdate;
  final bool forceUpdate;
  final String latestVersionName;
  final int latestVersionCode;
  final int minimumSupportedVersionCode;
  final String title;
  final String updateContent;
  final String downloadUrl;
}

int _int(Object? value) {
  return value is num ? value.toInt() : int.tryParse('$value') ?? 0;
}
