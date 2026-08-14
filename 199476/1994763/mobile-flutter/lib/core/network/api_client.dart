import 'dart:async';

import 'package:dio/dio.dart';

import '../config/app_config.dart';
import '../storage/app_storage.dart';
import 'api_exception.dart';

class ApiClient {
  ApiClient(this._storage)
    : _dio = Dio(
        BaseOptions(
          baseUrl: AppConfig.apiBaseUrl,
          connectTimeout: const Duration(seconds: 12),
          receiveTimeout: const Duration(seconds: 30),
          sendTimeout: const Duration(seconds: 30),
          headers: const {'Accept': 'application/json'},
        ),
      ) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _storage.readToken();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
        onError: (error, handler) async {
          if (error.response?.statusCode == 401) {
            await _storage.deleteToken();
            _unauthorizedController.add(null);
          }
          handler.next(error);
        },
      ),
    );
  }

  final Dio _dio;
  final AppStorage _storage;
  final StreamController<void> _unauthorizedController =
      StreamController<void>.broadcast();
  final Map<String, Future<Object?>> _inFlight = {};

  Stream<void> get unauthorizedEvents => _unauthorizedController.stream;

  Future<T> get<T>(String path, {Map<String, dynamic>? query}) {
    return _request<T>('GET', path, query: query);
  }

  Future<T> post<T>(String path, {Object? data, Map<String, dynamic>? query}) {
    return _request<T>('POST', path, data: data, query: query);
  }

  Future<T> put<T>(String path, {Object? data}) {
    return _request<T>('PUT', path, data: data);
  }

  Future<T> patch<T>(String path, {Object? data}) {
    return _request<T>('PATCH', path, data: data);
  }

  Future<T> delete<T>(String path) {
    return _request<T>('DELETE', path);
  }

  Future<T> _request<T>(
    String method,
    String path, {
    Object? data,
    Map<String, dynamic>? query,
  }) {
    final key = '$method:$path:${query ?? const {}}:${_dataKey(data)}';
    final running = _inFlight[key];
    if (running != null) {
      return running.then((value) => value as T);
    }

    final request = _perform<T>(method, path, data: data, query: query);
    _inFlight[key] = request;
    request.whenComplete(() => _inFlight.remove(key));
    return request;
  }

  Future<T> _perform<T>(
    String method,
    String path, {
    Object? data,
    Map<String, dynamic>? query,
  }) async {
    try {
      final response = await _dio.request<Object?>(
        path,
        data: data,
        queryParameters: query,
        options: Options(method: method),
      );
      final body = response.data;
      if (body is Map<String, dynamic>) {
        if (body['success'] == false) {
          throw ApiException(
            body['message']?.toString() ?? '服务暂时不可用，请稍后重试',
            statusCode: response.statusCode,
          );
        }
        return body['data'] as T;
      }
      return body as T;
    } on ApiException {
      rethrow;
    } on DioException catch (error) {
      final responseBody = error.response?.data;
      final message = responseBody is Map<String, dynamic>
          ? responseBody['message']?.toString()
          : null;
      throw ApiException(
        message ?? _networkMessage(error),
        statusCode: error.response?.statusCode,
      );
    } catch (_) {
      throw const ApiException('数据处理失败，请稍后重试');
    }
  }

  String _dataKey(Object? data) {
    if (data is FormData) {
      return '${data.fields}:${data.files.map((item) => item.value.filename)}';
    }
    return data?.toString() ?? '';
  }

  String _networkMessage(DioException error) {
    return switch (error.type) {
      DioExceptionType.connectionTimeout ||
      DioExceptionType.sendTimeout ||
      DioExceptionType.receiveTimeout => '连接超时，请检查网络后重试',
      DioExceptionType.connectionError => '无法连接服务器，请检查网络',
      _ => '服务暂时不可用，请稍后重试',
    };
  }

  Future<void> dispose() async {
    await _unauthorizedController.close();
    _dio.close(force: true);
  }
}
