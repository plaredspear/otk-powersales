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

/// 취소된 요청 여부 판별 — 취소는 정상 흐름이므로 에러 로그/상태 오염에서 제외하는 데 쓴다.
bool isRequestCancelled(Object error) {
  return error is DioException && CancelToken.isCancel(error);
}
