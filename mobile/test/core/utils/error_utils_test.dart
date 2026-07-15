import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/utils/error_utils.dart';

/// [extractErrorMessage] / [isNetworkError] / [extractErrorCode] 단위 테스트.
///
/// 핵심 회귀 방어: CloudFront/ALB/nginx 등 중간 게이트웨이가 백엔드보다 먼저 끊어
/// 502/503/504 를 HTML 에러 페이지로 내려줄 때(서버 표준 error 포맷 아님),
/// raw Dio 문자열이 사용자에게 노출되지 않고 친화 메시지로 대체되는지 검증한다.
void main() {
  final requestOptions = RequestOptions(path: '/api/v1/mobile/me/order-requests/1/cancel');

  DioException dioWith({
    required DioExceptionType type,
    Response<dynamic>? response,
  }) {
    return DioException(
      requestOptions: requestOptions,
      type: type,
      response: response,
    );
  }

  Response<dynamic> responseWith(int statusCode, dynamic data) {
    return Response<dynamic>(
      requestOptions: requestOptions,
      statusCode: statusCode,
      data: data,
    );
  }

  group('extractErrorMessage', () {
    test('서버 표준 error.message 를 우선 추출한다', () {
      final e = dioWith(
        type: DioExceptionType.badResponse,
        response: responseWith(502, {
          'error': {'code': 'ORD_CANCEL_SAP_FAILED', 'message': 'SAP 송신에 실패했습니다'},
        }),
      );
      expect(extractErrorMessage(e), 'SAP 송신에 실패했습니다');
    });

    test('error 가 없으면 top-level message 를 추출한다', () {
      final e = dioWith(
        type: DioExceptionType.badResponse,
        response: responseWith(400, {'message': '잘못된 요청입니다'}),
      );
      expect(extractErrorMessage(e), '잘못된 요청입니다');
    });

    test('5xx HTML 에러 페이지(표준 포맷 아님)는 친화 메시지로 대체한다', () {
      // CloudFront/nginx 504 게이트웨이 타임아웃 시 HTML body — error.message 파싱 실패 케이스.
      final e = dioWith(
        type: DioExceptionType.badResponse,
        response: responseWith(504, '<html><body>504 Gateway Time-out</body></html>'),
      );
      expect(
        extractErrorMessage(e),
        '서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.',
      );
    });

    test('502/503 도 5xx 로 친화 메시지 대체', () {
      for (final status in [502, 503]) {
        final e = dioWith(
          type: DioExceptionType.badResponse,
          response: responseWith(status, '<html>error</html>'),
        );
        expect(
          extractErrorMessage(e),
          '서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.',
          reason: 'status=$status',
        );
      }
    });

    test('4xx 인데 표준 포맷이 아니면 (5xx 아님) 기존 fallback 유지', () {
      // 4xx 는 게이트웨이 지연이 아니라 클라이언트 오류라 친화 대체 대상이 아니다.
      final e = dioWith(
        type: DioExceptionType.badResponse,
        response: responseWith(404, '<html>Not Found</html>'),
      );
      // 마지막 fallback(e.toString()) 로 떨어진다 — 5xx 특례에 걸리지 않음을 확인.
      expect(
        extractErrorMessage(e),
        isNot('서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.'),
      );
    });

    test('receiveTimeout 은 지연 친화 메시지', () {
      final e = dioWith(type: DioExceptionType.receiveTimeout);
      expect(
        extractErrorMessage(e),
        '서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.',
      );
    });

    test('connectionError 는 네트워크 친화 메시지', () {
      final e = dioWith(type: DioExceptionType.connectionError);
      expect(extractErrorMessage(e), '네트워크 연결을 확인해주세요.');
    });
  });

  group('extractErrorCode', () {
    test('error.code 를 추출한다', () {
      final e = dioWith(
        type: DioExceptionType.badResponse,
        response: responseWith(502, {
          'error': {'code': 'ORD_CANCEL_SAP_FAILED', 'message': 'x'},
        }),
      );
      expect(extractErrorCode(e), 'ORD_CANCEL_SAP_FAILED');
    });

    test('HTML 5xx 응답은 code 가 없어 null', () {
      final e = dioWith(
        type: DioExceptionType.badResponse,
        response: responseWith(504, '<html>504</html>'),
      );
      expect(extractErrorCode(e), isNull);
    });
  });

  group('isInconclusiveError', () {
    test('receiveTimeout 은 미확정', () {
      expect(isInconclusiveError(dioWith(type: DioExceptionType.receiveTimeout)), isTrue);
    });

    test('connectionError 는 미확정', () {
      expect(isInconclusiveError(dioWith(type: DioExceptionType.connectionError)), isTrue);
    });

    test('게이트웨이 5xx(HTML, code 없음)는 미확정', () {
      for (final status in [502, 503, 504]) {
        final e = dioWith(
          type: DioExceptionType.badResponse,
          response: responseWith(status, '<html>$status</html>'),
        );
        expect(isInconclusiveError(e), isTrue, reason: 'status=$status');
      }
    });

    test('백엔드 error.code 가 있는 5xx 는 확정(미확정 아님)', () {
      // 백엔드가 명시적 에러를 응답 = 결과 확정. 재조회 대상 아님.
      final e = dioWith(
        type: DioExceptionType.badResponse,
        response: responseWith(502, {
          'error': {'code': 'ORD_CANCEL_SAP_FAILED', 'message': 'x'},
        }),
      );
      expect(isInconclusiveError(e), isFalse);
    });

    test('4xx 는 미확정 아님 (클라이언트 오류 = 결과 확정)', () {
      final e = dioWith(
        type: DioExceptionType.badResponse,
        response: responseWith(400, {'error': {'code': 'X', 'message': 'y'}}),
      );
      expect(isInconclusiveError(e), isFalse);
    });

    test('DioException 아닌 것은 미확정 아님', () {
      expect(isInconclusiveError(Exception('boom')), isFalse);
    });
  });

  group('isNetworkError', () {
    test('response 있는 5xx 는 네트워크 에러로 보지 않는다 (기존 동작 유지)', () {
      final e = dioWith(
        type: DioExceptionType.badResponse,
        response: responseWith(504, '<html>504</html>'),
      );
      expect(isNetworkError(e), isFalse);
    });

    test('response 없는 timeout 은 네트워크 에러', () {
      final e = dioWith(type: DioExceptionType.receiveTimeout);
      expect(isNetworkError(e), isTrue);
    });
  });
}
