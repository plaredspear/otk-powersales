import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';

import '../../app_router.dart';
import '../../core/navigation/navigator_key.dart';
import '../../core/network/request_cancel_controller.dart';
import '../../core/services/app_version_fields.dart';
import '../../core/session/session_reset_controller.dart';
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

  /// 401 → 토큰 갱신 후 재시도된 요청 표식 (무한 루프 방지)
  static const String _retriedKey = '__auth_retried__';

  /// 명시적 자동 로그인(auth_provider.tryAutoLogin)의 refresh 요청 표식.
  ///
  /// 이 표식이 붙은 요청의 401 은 인터셉터가 가로채 _forceLogout(세션 재생성)하지
  /// 않고 그대로 호출측에 전파한다. 호출측이 토큰 정리 + 로그인 전환을 단독으로
  /// 수행하므로, 인터셉터까지 로그인 전환을 일으키면 로그인 화면이 두 번 쌓인다.
  static const String skipAuthLogoutExtraKey = '__skip_auth_logout__';

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
    // 앱 백그라운드/종료 전환으로 취소된 요청은 인증 실패가 아니다.
    // 토큰 갱신/강제 로그아웃 대상으로 처리하면 안 되므로(원치 않는 로그아웃 방지)
    // 토큰을 보존한 채 취소 에러를 그대로 전파한다. (auth_provider 와 동일 정책)
    if (isRequestCancelled(err)) {
      return handler.next(err);
    }

    final response = err.response;
    if (response == null) {
      return handler.next(err);
    }

    // 로그인 요청의 에러(예: 401 자격증명 오류)는 인터셉터가 가로채면 안 된다.
    // 401을 토큰 갱신 대상으로 처리하면 refresh 토큰이 없어 _forceLogout() →
    // 로그인 화면 재진입(슬라이드 전환)이 발생한다. 로그인 화면이 직접 에러를
    // 표시하도록 그대로 전달한다. (login 은 토큰 갱신/강제 로그아웃 대상이 아님)
    if (err.requestOptions.path.contains('/auth/login')) {
      return handler.next(err);
    }

    // 명시적 자동 로그인의 refresh 401 은 호출측(tryAutoLogin)이 토큰 정리 + 로그인
    // 전환을 직접 수행한다. 인터셉터가 추가로 _forceLogout(세션 재생성)까지 하면
    // 호출측의 로그인 전환과 겹쳐 로그인 화면이 두 번 쌓인다 — 그대로 전파한다.
    if (err.requestOptions.extra[skipAuthLogoutExtraKey] == true) {
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
    // 단말 회수/교체(DEVICE_REVOKED): 다른 기기에서 로그인되어 현재 단말이 차단됨.
    // refresh 토큰도 서버에서 무효화됐으므로 갱신 시도 없이 즉시 강제 로그아웃.
    if (_errorCode(err.response?.data) == 'DEVICE_REVOKED') {
      await _forceLogout(reason: LogoutReason.deviceRevoked);
      return handler.next(err);
    }

    // 이미 refresh 요청 자체가 401이면 로그아웃
    if (err.requestOptions.path.contains('/auth/refresh')) {
      await _forceLogout(reason: LogoutReason.sessionExpired);
      return handler.next(err);
    }

    // 이미 한 번 토큰 갱신 후 재시도된 요청이 또 401이면 무한 갱신→재시도 루프가 된다.
    // (갱신은 성공하지만 서버가 계속 401을 주는 케이스) → 더 갱신하지 않고 로그아웃.
    if (err.requestOptions.extra[_retriedKey] == true) {
      await _forceLogout(reason: LogoutReason.sessionExpired);
      return handler.next(err);
    }

    // 저장된 refresh token 이 없으면 복원/만료할 "세션" 자체가 없다 — 로그인 이력이 없는
    // 무인증 상태(예: 첫 설치 후 로그인 전)에서 인증 필요 엔드포인트가 401을 준 경우다.
    // 이 401 을 세션 만료로 오인해 _forceLogout(세션 만료 안내)하지 않고 그대로 전파한다.
    // (전파된 에러는 호출측이 자체 처리하며, 화면은 이미 로그인 화면에 있다.)
    final refreshToken = await _localDataSource.getRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      return handler.next(err);
    }

    try {
      final newToken = await _refreshAccessToken();
      if (newToken == null) {
        // 갱신을 대기하던 동시 요청이 그 사이 취소됐으면(백그라운드 전환) 로그아웃하지
        // 않는다. 갱신 주도 요청의 취소는 _refreshAccessToken 이 rethrow 해 아래
        // catch 에서 처리되고, 대기 요청은 여기서 자신의 취소 여부로 가드한다.
        if (err.requestOptions.cancelToken?.isCancelled == true) {
          return handler.next(err);
        }
        await _forceLogout(reason: LogoutReason.sessionExpired);
        return handler.next(err);
      }

      // 원래 요청에 새 토큰 설정하고 재시도 (재시도 표식으로 루프 차단)
      final options = err.requestOptions;
      options.headers['Authorization'] = 'Bearer $newToken';
      options.extra[_retriedKey] = true;
      final retryResponse = await _dio.fetch(options);
      handler.resolve(retryResponse);
    } catch (e) {
      // 재시도가 백그라운드 전환으로 취소된 경우 — 인증 실패가 아니므로 로그아웃하지
      // 않고 토큰을 보존한다. 재개 후 재요청 시 정상 갱신/재시도가 가능하다.
      if (isRequestCancelled(e)) {
        return handler.next(err);
      }
      await _forceLogout(reason: LogoutReason.sessionExpired);
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
        '/api/v1/mobile/auth/refresh',
        data: {
          'refreshToken': refreshToken,
          // 현재 사용 중인 앱 버전 보고 (자동 리프레시로 현재 버전 최신화).
          ...await appVersionFields(),
        },
      );

      final data = response.data['data'] as Map<String, dynamic>;
      final newAccessToken = data['accessToken'] as String;
      await _localDataSource.saveAccessToken(newAccessToken);

      // refresh_token도 갱신되면 저장
      if (data.containsKey('refreshToken')) {
        await _localDataSource.saveRefreshToken(
          data['refreshToken'] as String,
        );
      }

      _refreshCompleter!.complete(newAccessToken);
      return newAccessToken;
    } catch (e) {
      // 갱신 요청 자체가 취소된 경우(백그라운드 전환) — null(인증 실패)로 환원하면
      // 호출자가 _forceLogout 으로 빠진다. 취소는 실패가 아니므로 예외를 전파해
      // _handle401 의 취소 가드가 토큰을 보존하도록 한다. 대기 중인 동시 요청은
      // null 로 완료시켜 각자 자신의 취소 경로를 타게 한다.
      _refreshCompleter!.complete(null);
      if (isRequestCancelled(e)) {
        rethrow;
      }
      return null;
    } finally {
      _isRefreshing = false;
    }
  }

  /// 응답 body에서 error.code 추출 (Map 직접 수신 또는 String JSON 디코딩).
  String? _errorCode(dynamic data) {
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
        // JSON 파싱 실패 → null
      }
    }
    if (parsed == null) return null;
    final error = parsed['error'];
    return error is Map<String, dynamic> ? error['code'] as String? : null;
  }

  /// 403 처리: GPS_CONSENT_REQUIRED이면 GPS 동의 화면으로 이동
  void _handle403(DioException err, ErrorInterceptorHandler handler) {
    final code = _errorCode(err.response?.data);

    if (code != null) {
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
      if (code == 'AUTH_PASSWORD_CHANGE_REQUIRED') {
        // Spec #584: 강제 변경 미완료 사원이 화이트리스트 외 호출 시 자동 라우팅.
        _navigateToPasswordChangeRequired();
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
    // 기타 403 에러는 일반 에러로 전달
    handler.next(err);
  }

  /// GPS 동의 화면으로 네비게이션
  void _navigateToGpsConsent() {
    final navigator = navigatorKey.currentState;
    if (navigator != null) {
      navigator.pushNamed(AppRouter.gpsConsent);
    }
  }

  /// 강제 비밀번호 변경 화면으로 네비게이션 (Spec #584).
  void _navigateToPasswordChangeRequired() {
    final navigator = navigatorKey.currentState;
    if (navigator != null) {
      navigator.pushNamedAndRemoveUntil(
        AppRouter.changePassword,
        (route) => false,
      );
    }
  }

  /// 강제 로그아웃: 토큰 클리어 + 전역 상태 초기화(로그인 화면 이동)
  ///
  /// 루트 ProviderScope 를 재생성해 모든 Provider(도메인 캐시 포함)를 폐기하므로,
  /// 토큰 만료로 로그아웃된 뒤 다른 계정으로 로그인해도 잔여 데이터가 노출되지 않는다.
  Future<void> _forceLogout({LogoutReason? reason}) async {
    await _localDataSource.clearTokens();
    SessionResetController.instance.requestReset(reason: reason);
  }
}
