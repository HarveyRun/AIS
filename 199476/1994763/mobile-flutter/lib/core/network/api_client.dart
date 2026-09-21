import 'dart:async';
import 'dart:io';

import 'package:dio/dio.dart';

import '../config/app_config.dart';
import '../storage/app_storage.dart';
import 'api_exception.dart';
import 'request_loading_controller.dart';

class ApiClient {
  ApiClient(this._storage, this._loading)
    : _dio = Dio(
        BaseOptions(
          baseUrl: AppConfig.apiBaseUrl,
          connectTimeout: const Duration(seconds: 12),
          receiveTimeout: const Duration(seconds: 30),
          sendTimeout: const Duration(seconds: 30),
          headers: {
            'Accept': 'application/json',
            'X-Client-Platform': Platform.isIOS ? 'ios' : 'android',
          },
        ),
      ) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _storage.readToken();
          options.headers['X-Device-Id'] = await _storage
              .readOrCreateDeviceId();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
        onError: (error, handler) async {
          final responseBody = error.response?.data;
          final responseData = responseBody is Map
              ? responseBody['data']
              : null;
          final isAccountPenalty =
              error.response?.statusCode == 403 &&
              responseData is Map &&
              responseData['type'] == 'ACCOUNT_PENALTY';
          if (isAccountPenalty) {
            await _storage.deleteToken();
            _accountPenaltyController.add(
              AccountPenaltyNotice.fromJson(
                Map<String, dynamic>.from(responseData),
              ),
            );
          } else if (error.response?.statusCode == 401) {
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
  final RequestLoadingController _loading;
  final StreamController<void> _unauthorizedController =
      StreamController<void>.broadcast();
  final StreamController<AccountPenaltyNotice> _accountPenaltyController =
      StreamController<AccountPenaltyNotice>.broadcast();
  final Map<String, Future<Object?>> _inFlight = {};

  Stream<void> get unauthorizedEvents => _unauthorizedController.stream;

  Stream<AccountPenaltyNotice> get accountPenaltyEvents =>
      _accountPenaltyController.stream;

  Future<T> get<T>(
    String path, {
    Map<String, dynamic>? query,
    bool showLoading = true,
  }) {
    return _request<T>('GET', path, query: query, showLoading: showLoading);
  }

  Future<T> post<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? query,
    bool showLoading = true,
    Duration? sendTimeout,
    Duration? receiveTimeout,
  }) {
    return _request<T>(
      'POST',
      path,
      data: data,
      query: query,
      showLoading: showLoading,
      sendTimeout: sendTimeout,
      receiveTimeout: receiveTimeout,
    );
  }

  Future<T> put<T>(String path, {Object? data, bool showLoading = true}) {
    return _request<T>('PUT', path, data: data, showLoading: showLoading);
  }

  Future<T> patch<T>(String path, {Object? data, bool showLoading = true}) {
    return _request<T>('PATCH', path, data: data, showLoading: showLoading);
  }

  Future<T> delete<T>(String path, {bool showLoading = true}) {
    return _request<T>('DELETE', path, showLoading: showLoading);
  }

  Future<T> _request<T>(
    String method,
    String path, {
    Object? data,
    Map<String, dynamic>? query,
    bool showLoading = true,
    Duration? sendTimeout,
    Duration? receiveTimeout,
  }) {
    final key = '$method:$path:${query ?? const {}}:${_dataKey(data)}';
    final running = _inFlight[key];
    if (running != null) {
      return running.then((value) => value as T);
    }

    final request = _perform<T>(
      method,
      path,
      data: data,
      query: query,
      showLoading: showLoading,
      sendTimeout: sendTimeout,
      receiveTimeout: receiveTimeout,
    );
    _inFlight[key] = request;
    request.whenComplete(() => _inFlight.remove(key));
    return request;
  }

  Future<T> _perform<T>(
    String method,
    String path, {
    Object? data,
    Map<String, dynamic>? query,
    required bool showLoading,
    Duration? sendTimeout,
    Duration? receiveTimeout,
  }) async {
    if (showLoading) _loading.begin();
    try {
      final response = await _dio.request<Object?>(
        path,
        data: data,
        queryParameters: query,
        options: Options(
          method: method,
          sendTimeout: sendTimeout,
          receiveTimeout: receiveTimeout,
        ),
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
    } finally {
      if (showLoading) _loading.end();
    }
  }

  String _dataKey(Object? data) {
    if (data is FormData) {
      return '${data.fields}:${data.files.map((item) => item.value.filename)}';
    }
    return data?.toString() ?? '';
  }

  String _networkMessage(DioException error) {
    if (error.response?.statusCode == 413) {
      return '上传内容过大，请压缩后重试';
    }
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
    await _accountPenaltyController.close();
    _dio.close(force: true);
  }
}

class AccountPenaltyNotice {
  const AccountPenaltyNotice({
    required this.reason,
    required this.permanent,
    this.banUntil,
  });

  factory AccountPenaltyNotice.fromJson(Map<String, dynamic> json) {
    return AccountPenaltyNotice(
      reason: json['reason']?.toString() ?? '违反平台规则',
      permanent: json['permanent'] == true,
      banUntil: DateTime.tryParse(json['banUntil']?.toString() ?? ''),
    );
  }

  final String reason;
  final bool permanent;
  final DateTime? banUntil;
}
