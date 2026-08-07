import 'package:dio/dio.dart';

import '../../core/network/request_cancel_controller.dart';
import '../../core/services/app_version_fields.dart';
import 'auth_interceptor.dart';

/// refresh token 회전을 **앱 전역에서 단일화**하는 코디네이터.
///
/// ## 왜 필요한가 — 회전은 1회용이라 중복 실행이 곧 강제 로그아웃이다
///
/// 서버는 refresh token 을 원자적으로 소비(consume)하고 새 페어를 발급한다. 같은 token 으로
/// 두 번째 요청이 오면 **재사용(탈취) 판정 → token family 전체 무효화** 이므로, 그 사용자는
/// 이후 어떤 refresh 도 통과하지 못하고 재로그인해야 한다.
///
/// 갱신 진입점은 두 갈래다:
///  - 콜드스타트 세션 복원([AuthApiDataSource.refreshToken] ← `AuthNotifier.tryAutoLogin`)
///  - 사용 중 401 자동 갱신([AuthInterceptor])
///
/// 이 둘은 서로 다른 계층이라 각자 락을 걸어 봐야 소용이 없다. 앱 시작 직후 세션 복원이
/// 진행 중인데 다른 화면의 요청이 401 을 받으면 **같은 refresh token 으로 회전이 두 번**
/// 나가고, 진 쪽이 곧바로 탈취로 판정되어 방금 복원한 세션이 끊긴다. 그래서 두 진입점 모두
/// 본 코디네이터를 통과시켜 진행 중인 회전 1건을 공유하게 한다.
///
/// ## 두 겹의 보호
///
/// 1. **in-flight 공유**: 회전이 진행 중이면 새 요청을 보내지 않고 그 결과를 함께 받는다.
/// 2. **직전 소비 토큰 매핑**: in-flight 가 끝난 직후, 그 사이 옛 token 을 읽어 둔 호출자가
///    뒤늦게 도착할 수 있다. 이미 소비한 token 으로 들어온 요청은 **보내지 않고** 그 token 이
///    회전된 결과를 그대로 돌려준다 — 보내 봐야 재사용 판정으로 세션만 잃는다.
///
/// 응답은 [lifecycleExemptCancelToken] 으로 보내 백그라운드 전환에도 끝까지 수신한다
/// (취소되면 서버만 회전을 반영한 채 단말이 새 token 을 잃어 같은 사고가 난다).
class TokenRefreshCoordinator {
  TokenRefreshCoordinator._();

  /// 프로세스 싱글턴 — 세션 리셋(ProviderScope 재생성)으로 인터셉터/데이터소스가 새로 만들어져도
  /// 회전 단일화는 유지되어야 하므로 Provider 가 아닌 전역 인스턴스로 둔다.
  static final TokenRefreshCoordinator instance = TokenRefreshCoordinator._();

  static const String refreshPath = '/api/v1/mobile/auth/refresh';

  /// 진행 중인 회전 (없으면 null).
  Future<Map<String, dynamic>>? _inFlight;

  /// 직전 회전에서 **소비된** refresh token 과 그 결과.
  String? _consumedToken;
  Map<String, dynamic>? _consumedResult;

  /// refresh token 회전. 진행 중이거나 이미 소비된 요청은 실제 HTTP 를 보내지 않고 결과를 공유한다.
  ///
  /// 반환값은 서버 응답의 `data` (accessToken / refreshToken / expiresIn). 호출측이 각자
  /// 저장하는데, 값이 동일하므로 중복 저장은 멱등이다.
  Future<Map<String, dynamic>> refresh({
    required Dio dio,
    required String refreshToken,
  }) {
    final inFlight = _inFlight;
    if (inFlight != null) return inFlight;

    // 이 token 은 방금 다른 경로가 회전시켜 이미 서버에서 폐기됐다 — 보내면 재사용 판정이다.
    final consumed = _consumedResult;
    if (consumed != null && refreshToken == _consumedToken) {
      return Future.value(consumed);
    }

    return _inFlight = _perform(dio, refreshToken);
  }

  /// 로그아웃 시 캐시 정리 — 폐기된 세션의 token 을 프로세스에 남겨 두지 않는다.
  void reset() {
    _consumedToken = null;
    _consumedResult = null;
  }

  Future<Map<String, dynamic>> _perform(Dio dio, String refreshToken) async {
    try {
      final response = await dio.post(
        refreshPath,
        data: {
          'refreshToken': refreshToken,
          // 현재 사용 중인 앱 버전 보고 (서버가 사용자별 현재 버전 기록).
          ...await appVersionFields(),
        },
        // 백그라운드 전환으로 취소되면 서버만 회전을 반영하고 단말은 새 token 을 잃는다.
        cancelToken: lifecycleExemptCancelToken,
        // refresh 의 401 은 호출측이 판단한다 — 인터셉터가 여기서 강제 로그아웃까지 하면
        // 호출측 처리와 겹쳐 로그인 화면이 두 번 쌓인다.
        options: Options(
          extra: {AuthInterceptor.skipAuthLogoutExtraKey: true},
        ),
      );

      final data = response.data['data'];
      if (data is! Map<String, dynamic>) {
        throw DioException(
          requestOptions: response.requestOptions,
          response: response,
          type: DioExceptionType.badResponse,
          message: 'refresh 응답에 토큰 data 가 없습니다',
        );
      }
      // 이 token 은 서버에서 소비됐다 — 뒤늦게 같은 값으로 들어오는 호출자에게 결과를 돌려준다.
      _consumedToken = refreshToken;
      _consumedResult = data;
      return data;
    } finally {
      _inFlight = null;
    }
  }
}
