import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';

import '../../app_router.dart';
import '../../core/navigation/navigator_key.dart';
import 'auth_local_datasource.dart';

/// 인증 Dio Interceptor
///
/// 모든 API 요청에 대해:
/// 1. 요청 전: Authorization 헤더에 access_token 자동 첨부
/// 2. 401 응답: refresh_token으로 토큰 갱신 → 원래 요청 재시도
/// 3. 403 GPS_CONSENT_REQUIRED: GPS 동의 화면으로 네비게이션
class AuthInterceptor extends Interceptor {
  final AuthLocalDataSource _localDataSource;
  final Dio _dio;

  /// 토큰 갱신 중복 방지용
  bool _isRefreshing = false;
  Completer<String?>? _refreshCompleter;

  AuthInterceptor({
    required AuthLocalDataSource localDataSource,
    required Dio dio,
  })  : _localDataSource = localDataSource,
        _dio = dio;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    // auth 엔드포인트(login, refresh)는 토큰 첨부 불필요
    final path = options.path;
    if (path.contains('/auth/login') || path.contains('/auth/refresh')) {
      return handler.next(options);
    }

    final accessToken = await _localDataSource.getAccessToken();
    if (accessToken != null && accessToken.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $accessToken';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    final response = err.response;
    if (response == null) {
      return handler.next(err);
    }

    if (response.statusCode == 401) {
      await _handle401(err, handler);
    } else if (response.statusCode == 403) {
      _handle403(err, handler);
    } else {
      handler.next(err);
    }
  }

  /// 401 처리: 토큰 갱신 → 원래 요청 재시도
  Future<void> _handle401(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    // 이미 refresh 요청 자체가 401이면 로그아웃
    if (err.requestOptions.path.contains('/auth/refresh')) {
      await _forceLogout();
      return handler.next(err);
    }

    try {
      final newToken = await _refreshAccessToken();
      if (newToken == null) {
        await _forceLogout();
        return handler.next(err);
      }

      // 원래 요청에 새 토큰 설정하고 재시도
      final options = err.requestOptions;
      options.headers['Authorization'] = 'Bearer $newToken';
      final retryResponse = await _dio.fetch(options);
      handler.resolve(retryResponse);
    } catch (_) {
      await _forceLogout();
      handler.next(err);
    }
  }

  /// 토큰 갱신 (동시 요청 시 1회만 수행)
  Future<String?> _refreshAccessToken() async {
    if (_isRefreshing) {
      // 다른 요청이 이미 갱신 중 → 완료 대기
      return _refreshCompleter?.future;
    }

    _isRefreshing = true;
    _refreshCompleter = Completer<String?>();

    try {
      final refreshToken = await _localDataSource.getRefreshToken();
      if (refreshToken == null || refreshToken.isEmpty) {
        _refreshCompleter!.complete(null);
        return null;
      }

      final response = await _dio.post(
        '/api/v1/auth/refresh',
        data: {'refresh_token': refreshToken},
      );

      final data = response.data['data'] as Map<String, dynamic>;
      final newAccessToken = data['access_token'] as String;
      await _localDataSource.saveAccessToken(newAccessToken);

      // refresh_token도 갱신되면 저장
      if (data.containsKey('refresh_token')) {
        await _localDataSource.saveRefreshToken(
          data['refresh_token'] as String,
        );
      }

      _refreshCompleter!.complete(newAccessToken);
      return newAccessToken;
    } catch (e) {
      _refreshCompleter!.complete(null);
      return null;
    } finally {
      _isRefreshing = false;
    }
  }

  /// 403 처리: GPS_CONSENT_REQUIRED이면 GPS 동의 화면으로 이동
  void _handle403(DioException err, ErrorInterceptorHandler handler) {
    final data = err.response?.data;

    // 응답 body를 Map으로 변환 (Map 직접 수신 또는 String JSON 디코딩)
    Map<String, dynamic>? parsed;
    if (data is Map<String, dynamic>) {
      parsed = data;
    } else if (data is String) {
      try {
        final decoded = jsonDecode(data);
        if (decoded is Map<String, dynamic>) {
          parsed = decoded;
        }
      } catch (_) {
        // JSON 파싱 실패 → 일반 403 처리
      }
    }

    if (parsed != null) {
      final error = parsed['error'];
      final code = error is Map<String, dynamic> ? error['code'] : null;
      if (code == 'GPS_CONSENT_REQUIRED') {
        _navigateToGpsConsent();
        // cancel 타입으로 교체하여 UI에서 에러 표시 억제
        handler.reject(
          DioException(
            requestOptions: err.requestOptions,
            type: DioExceptionType.cancel,
            message: '',
          ),
        );
        return;
      }
    }
    // GPS 외 403 에러는 일반 에러로 전달
    handler.next(err);
  }

  /// GPS 동의 화면으로 네비게이션
  void _navigateToGpsConsent() {
    final navigator = navigatorKey.currentState;
    if (navigator != null) {
      navigator.pushNamed(AppRouter.gpsConsent);
    }
  }

  /// 강제 로그아웃: 토큰 클리어 + 로그인 화면 이동
  Future<void> _forceLogout() async {
    await _localDataSource.clearTokens();
    final navigator = navigatorKey.currentState;
    if (navigator != null) {
      navigator.pushNamedAndRemoveUntil(
        AppRouter.login,
        (route) => false,
      );
    }
  }
}
