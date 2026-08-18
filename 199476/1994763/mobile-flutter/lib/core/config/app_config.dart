abstract final class AppConfig {
  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api',
  );

  static Uri resolveResource(String? path) {
    if (path == null || path.trim().isEmpty) {
      return Uri();
    }
    final value = path.trim();
    final parsed = Uri.tryParse(value);
    if (parsed != null && parsed.hasScheme) {
      return parsed;
    }
    final apiUri = Uri.parse(apiBaseUrl);
    final relative = Uri.parse(value);
    return apiUri.replace(
      path: relative.path.startsWith('/')
          ? relative.path
          : '/${relative.path}',
      query: relative.hasQuery ? relative.query : null,
    );
  }

  static Uri realtimeUri(String ticket) {
    final apiUri = Uri.parse(apiBaseUrl);
    return apiUri.replace(
      scheme: apiUri.scheme == 'https' ? 'wss' : 'ws',
      path: '${apiUri.path}/realtime/ws',
      queryParameters: {'ticket': ticket},
    );
  }
}
