import 'package:dio/dio.dart';

/// 진행 중인 HTTP 요청을 앱 생명주기에 맞춰 일괄 취소하는 컨트롤러.
///
/// dio 는 한 번 떠난 요청을 명시적으로 취소하지 않으면 timeout(connect/receive)
/// 까지 살아 있다. 앱이 백그라운드/종료로 전환될 때 진행 중 요청을 끊어,
/// 종료 직전 느린 외부 요청이 매달려 있다가 재개 시 stale 응답이 상태를
/// 뒤늦게 덮어쓰는 문제를 방지한다.
///
/// [token] 은 모든 요청에 인터셉터로 자동 첨부된다([attachTo]).
/// [cancelAll] 호출 시 진행 중 요청이 모두 cancel 되고, 이후 요청을 위해
/// 새 [CancelToken] 으로 교체된다.
class RequestCancelController {
  CancelToken _token = CancelToken();

  /// 현재 활성 취소 토큰.
  CancelToken get token => _token;

  /// 진행 중인 모든 요청을 취소하고 토큰을 새로 교체한다.
  void cancelAll([String reason = 'app lifecycle']) {
    if (!_token.isCancelled) {
      _token.cancel(reason);
    }
    _token = CancelToken();
  }

  /// dio 의 모든 요청에 현재 취소 토큰을 자동 첨부한다.
  ///
  /// 호출 측이 명시적으로 `cancelToken` 을 지정한 요청은 그대로 존중한다.
  void attachTo(Dio dio) {
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        options.cancelToken ??= _token;
        handler.next(options);
      },
    ));
  }
}

/// 앱 전역 단일 인스턴스. main 의 lifecycle 옵저버에서 [cancelAll] 을 호출한다.
final requestCancelController = RequestCancelController();

/// 생명주기 일괄 취소([RequestCancelController.cancelAll])에서 **제외**되는 전용 취소 토큰.
///
/// "서버에서는 이미 반영됐는데 응답만 못 받으면 복구 불가" 인 요청에 붙인다. 현재 유일한
/// 대상은 refresh token 회전이다 — 서버는 refresh token 을 1회용으로 소비(회전)하므로,
/// 백그라운드 전환으로 응답을 놓치면 단말에는 **이미 폐기된 옛 token** 만 남는다. 다음 갱신에서
/// 그 token 을 보내면 서버가 재사용(탈취)으로 판정해 token family 전체를 무효화하고, 사용자는
/// 아무 잘못 없이 강제 로그아웃된다. 이 토큰은 절대 cancel 하지 않으므로, 앱이 백그라운드로
/// 가도 회전 응답을 끝까지 수신해 새 token 페어를 저장할 수 있다.
///
/// 일반 조회 요청에는 쓰지 말 것 — 취소되지 않아 timeout 까지 매달린다.
final CancelToken lifecycleExemptCancelToken = CancelToken();

/// 취소된 요청 여부 판별 — 취소는 정상 흐름이므로 에러 로그/상태 오염에서 제외하는 데 쓴다.
bool isRequestCancelled(Object error) {
  return error is DioException && CancelToken.isCancel(error);
}
