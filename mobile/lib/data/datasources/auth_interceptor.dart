import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';

import '../../app_router.dart';
import '../../core/navigation/navigator_key.dart';
import '../../core/network/request_cancel_controller.dart';
import '../../core/session/session_reset_controller.dart';
import '../../core/utils/error_utils.dart';
import 'auth_local_datasource.dart';
import 'token_refresh_coordinator.dart';

/// 인증 Dio Interceptor
///
/// 모든 API 요청에 대해:
/// 1. 요청 전: Authorization 헤더에 access_token 자동 첨부
/// 2. 401 응답: refresh_token으로 토큰 갱신 → 원래 요청 재시도
/// 3. 403 GPS_CONSENT_REQUIRED: GPS 동의 화면으로 네비게이션
class AuthInterceptor extends Interceptor {
  final AuthLocalDataSource _localDataSource;
  final Dio _dio;

  /// 401 → 토큰 갱신 후 재시도된 요청 표식 (무한 루프 방지)
  static const String _retriedKey = '__auth_retried__';

  /// 이 인스턴스가 이미 강제 로그아웃을 수행했는지 (중복 실행 방지).
  ///
  /// 한 번의 세션 만료가 두 경로로 이어질 수 있다: refresh 요청 자체의 401 에서 한 번,
  /// 그 실패가 갱신 결과 null 로 환원돼 원요청 쪽에서 또 한 번. 동시 요청이 여럿이면
  /// 그 수만큼 반복된다. 세션 리셋(ProviderScope 재생성)과 단말 정리 훅이 그때마다
  /// 중복 실행되므로 첫 1회로 고정한다. 강제 로그아웃 후에는 새 ProviderScope 가 새
  /// 인터셉터를 만들므로, 다음 세션의 강제 로그아웃은 정상 동작한다.
  bool _forcedLogout = false;

  /// 명시적 자동 로그인(auth_provider.tryAutoLogin)의 refresh 요청 표식.
  ///
  /// 이 표식이 붙은 요청의 401 은 인터셉터가 가로채 _forceLogout(세션 재생성)하지
  /// 않고 그대로 호출측에 전파한다. 호출측이 토큰 정리 + 로그인 전환을 단독으로
  /// 수행하므로, 인터셉터까지 로그인 전환을 일으키면 로그인 화면이 두 번 쌓인다.
  static const String skipAuthLogoutExtraKey = '__skip_auth_logout__';

  /// 강제 로그아웃(세션 만료 / 단말 회수) 시 실행할 정리 훅.
  ///
  /// 이 시점의 access token 은 이미 무효라 서버 API(FCM 토큰 해제 등)를 호출할 수 없다.
  /// 단말 로컬에서만 가능한 정리를 주입받아 수행한다([dioProvider] 가 FCM 토큰 폐기를 연결).
  final Future<void> Function()? _onForceLogout;

  AuthInterceptor({
    required AuthLocalDataSource localDataSource,
    required Dio dio,
    Future<void> Function()? onForceLogout,
  })  : _localDataSource = localDataSource,
        _dio = dio,
        _onForceLogout = onForceLogout;

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
    final errorCode = _errorCode(err.response?.data);

    // 단말 회수/교체(DEVICE_REVOKED): 다른 기기에서 로그인되어 현재 단말이 차단됨.
    // refresh 토큰도 서버에서 무효화됐으므로 갱신 시도 없이 즉시 강제 로그아웃.
    if (errorCode == 'DEVICE_REVOKED') {
      await _forceLogout(reason: LogoutReason.deviceRevoked);
      return handler.next(err);
    }

    // 서버 데이터 정비로 발급시각 컷오프 이전 토큰이 일괄 무효화됨(SESSION_INVALIDATED).
    // refresh 토큰도 같은 컷오프에 걸리므로 갱신 시도 없이 즉시 강제 로그아웃한다.
    if (errorCode == 'SESSION_INVALIDATED') {
      await _forceLogout(reason: LogoutReason.sessionInvalidated);
      return handler.next(err);
    }

    // refresh 요청 자체의 401 = 세션 만료. 다만 [TokenRefreshCoordinator] 경유 회전에는
    // skip 표식이 붙어 onError 앞단에서 이미 전파되므로 여기 도달하지 않는다 — 표식 없이
    // refresh 를 직접 호출하는 경로가 생겼을 때를 위한 방어다.
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
      final outcome = await _refreshAccessToken();
      final newToken = outcome.accessToken;
      if (newToken == null) {
        // 갱신을 대기하던 동시 요청이 그 사이 취소됐으면(백그라운드 전환) 로그아웃하지
        // 않는다. 갱신 주도 요청의 취소는 _refreshAccessToken 이 rethrow 해 아래
        // catch 에서 처리되고, 대기 요청은 여기서 자신의 취소 여부로 가드한다.
        if (err.requestOptions.cancelToken?.isCancelled == true) {
          return handler.next(err);
        }
        // 서버에 닿지 못한 실패(네트워크/타임아웃/5xx)는 세션 만료가 아니다. 토큰을 보존한
        // 채 원 에러만 전파해, 통신이 잠깐 끊겼을 뿐인 사용자가 로그아웃되지 않게 한다.
        if (outcome.transient) {
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
      // 재시도가 네트워크 오류/5xx 로 실패한 것도 세션 만료가 아니다 — 갱신은 이미 성공해
      // 새 토큰이 저장돼 있으므로, 지우지 말고 다음 요청에서 그대로 쓰게 한다.
      if (!isSessionInvalidError(e)) {
        return handler.next(err);
      }
      await _forceLogout(reason: LogoutReason.sessionExpired);
      handler.next(err);
    }
  }

  /// 토큰 갱신.
  ///
  /// 실제 회전은 [TokenRefreshCoordinator] 가 수행한다 — 인터셉터 안의 동시 401 뿐 아니라
  /// 콜드스타트 세션 복원(`AuthNotifier.tryAutoLogin`)과도 회전을 공유해야 하기 때문이다.
  /// 인터셉터 자체 락으로는 그 교차 경로를 막을 수 없어, 같은 refresh token 이 두 번 나가
  /// 재사용 탐지(family revoke)로 세션이 끊기는 사고가 났다.
  Future<_RefreshOutcome> _refreshAccessToken() async {
    final refreshToken = await _localDataSource.getRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      return const _RefreshOutcome.sessionInvalid();
    }

    try {
      final data = await TokenRefreshCoordinator.instance.refresh(
        dio: _dio,
        refreshToken: refreshToken,
      );

      final newAccessToken = data['accessToken'] as String;
      // 서버는 refresh 마다 refresh token 을 회전(이전 token 즉시 폐기 + 재사용 탐지)하며
      // 응답에 항상 새 refresh token 을 포함한다(TokenResponse.refreshToken 은 non-null).
      // 회전된 값을 조건부가 아니라 무조건 저장해야 다음 refresh 에서 옛 token 을 보내
      // family revoke 로 강제 로그아웃되지 않는다. 자동 로그인 경로(AuthTokenModel.fromJson)
      // 와 동일하게 필수 파싱하여, 계약이 깨지면 조용한 저장 누락 대신 즉시 드러나게 한다.
      final newRefreshToken = data['refreshToken'] as String;
      await _localDataSource.saveAccessToken(newAccessToken);
      await _localDataSource.saveRefreshToken(newRefreshToken);

      return _RefreshOutcome.success(newAccessToken);
    } catch (e) {
      // 갱신 요청이 취소된 경우 — 실패로 환원하면 호출자가 _forceLogout 으로 빠진다.
      // 취소는 실패가 아니므로 예외를 전파해 _handle401 의 취소 가드가 토큰을 보존하게 한다.
      if (isRequestCancelled(e)) {
        rethrow;
      }
      // 401 만 세션 무효 확정. 그 외(네트워크/타임아웃/5xx/파싱)는 서버 판정이 아니므로
      // 토큰을 보존해야 한다 — 여기서 로그아웃시키면 통신 불안정이 곧 강제 재로그인이 된다.
      return isSessionInvalidError(e)
          ? const _RefreshOutcome.sessionInvalid()
          : const _RefreshOutcome.transientFailure();
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

  /// 강제 로그아웃: 토큰 클리어 + 단말 정리 훅 + 전역 상태 초기화(로그인 화면 이동)
  ///
  /// 루트 ProviderScope 를 재생성해 모든 Provider(도메인 캐시 포함)를 폐기하므로,
  /// 토큰 만료로 로그아웃된 뒤 다른 계정으로 로그인해도 잔여 데이터가 노출되지 않는다.
  ///
  /// 정리 훅(FCM 토큰 폐기)은 네트워크를 탈 수 있으므로 await 하지 않는다 — 로그인 화면
  /// 전환이 그만큼 지연되면 안 되고, 훅은 자체적으로 실패를 흡수한다.
  Future<void> _forceLogout({LogoutReason? reason}) async {
    if (_forcedLogout) return;
    _forcedLogout = true;
    await _localDataSource.clearTokens();
    // 폐기된 세션의 회전 캐시를 프로세스에 남겨 두지 않는다.
    TokenRefreshCoordinator.instance.reset();
    unawaited(_onForceLogout?.call() ?? Future<void>.value());
    SessionResetController.instance.requestReset(reason: reason);
  }
}

/// 토큰 갱신 시도 결과.
///
/// 실패를 한 가지로 뭉뚱그리면(구 구현의 `null` 반환) 네트워크 장애까지 세션 만료로 처리돼
/// 강제 로그아웃된다. "서버가 401 로 무효를 확정" 과 "서버에 닿지 못함" 을 구분해,
/// 후자는 토큰을 보존한 채 원 에러만 전파한다.
class _RefreshOutcome {
  /// 갱신된 access token. 실패면 null.
  final String? accessToken;

  /// 세션 무효가 아니라 일시적 실패(네트워크/타임아웃/5xx)인지.
  final bool transient;

  const _RefreshOutcome.success(String this.accessToken) : transient = false;

  /// 서버가 401 로 세션 무효를 확정 — 재로그인 필요.
  const _RefreshOutcome.sessionInvalid()
      : accessToken = null,
        transient = false;

  /// 서버 판정 없이 실패 — 토큰 보존.
  const _RefreshOutcome.transientFailure()
      : accessToken = null,
        transient = true;
}
