import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/request_cancel_controller.dart';

void main() {
  group('RequestCancelController', () {
    test('attachTo 는 cancelToken 미지정 요청에 컨트롤러 토큰을 자동 첨부한다', () async {
      final controller = RequestCancelController();
      final dio = Dio();
      controller.attachTo(dio);

      RequestOptions? captured;
      // attachTo 인터셉터 다음에 캡처용 인터셉터를 두어 첨부 결과를 확인한다.
      dio.interceptors.add(InterceptorsWrapper(
        onRequest: (options, handler) {
          captured = options;
          // 실제 네트워크로 나가지 않도록 즉시 reject(취소로 종결).
          handler.reject(
            DioException.requestCancelled(
              requestOptions: options,
              reason: 'test',
            ),
            true,
          );
        },
      ));

      // 인터셉터 체인은 비동기로 실행되므로 완료를 기다린다(reject 로 종결).
      try {
        await dio.get<void>('/any');
      } catch (_) {}

      expect(captured, isNotNull);
      expect(captured!.cancelToken, same(controller.token));
    });

    test('attachTo 는 호출 측이 지정한 cancelToken 을 덮어쓰지 않는다', () async {
      final controller = RequestCancelController();
      final dio = Dio();
      controller.attachTo(dio);

      final explicit = CancelToken();
      RequestOptions? captured;
      dio.interceptors.add(InterceptorsWrapper(
        onRequest: (options, handler) {
          captured = options;
          handler.reject(
            DioException.requestCancelled(
              requestOptions: options,
              reason: 'test',
            ),
            true,
          );
        },
      ));

      try {
        await dio.get<void>('/any', cancelToken: explicit);
      } catch (_) {}

      expect(captured!.cancelToken, same(explicit));
      expect(captured!.cancelToken, isNot(same(controller.token)));
    });

    test('cancelAll 은 진행 중 토큰을 취소하고 새 토큰으로 교체한다', () {
      final controller = RequestCancelController();
      final before = controller.token;

      controller.cancelAll('lifecycle');

      expect(before.isCancelled, isTrue);
      // 교체된 새 토큰은 취소되지 않은 상태여야 재개 후 요청이 정상 동작한다.
      expect(controller.token, isNot(same(before)));
      expect(controller.token.isCancelled, isFalse);
    });

    test('cancelAll 을 두 번 호출해도 안전하다(idempotent)', () {
      final controller = RequestCancelController();
      controller.cancelAll();
      final mid = controller.token;
      controller.cancelAll();

      expect(mid.isCancelled, isTrue);
      expect(controller.token.isCancelled, isFalse);
    });
  });

  group('isRequestCancelled', () {
    test('취소된 DioException 은 true', () {
      final token = CancelToken();
      token.cancel('x');
      final err = DioException.requestCancelled(
        requestOptions: RequestOptions(path: '/'),
        reason: 'x',
      );
      expect(isRequestCancelled(err), isTrue);
    });

    test('일반 DioException 은 false', () {
      final err = DioException(
        requestOptions: RequestOptions(path: '/'),
        type: DioExceptionType.connectionTimeout,
      );
      expect(isRequestCancelled(err), isFalse);
    });

    test('DioException 이 아닌 에러는 false', () {
      expect(isRequestCancelled(Exception('boom')), isFalse);
    });
  });
}
