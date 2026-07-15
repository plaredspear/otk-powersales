import 'package:dio/dio.dart';

/// API 에러 응답에서 사용자 친화적 메시지를 추출합니다.
///
/// DioException인 경우 서버 응답의 `error.message`를 우선 사용하고,
/// 그 외에는 Exception 접두사를 제거한 문자열을 반환합니다.
String extractErrorMessage(dynamic e) {
  if (e is DioException) {
    final data = e.response?.data;
    if (data is Map<String, dynamic>) {
      final error = data['error'];
      if (error is Map<String, dynamic>) {
        final message = error['message'];
        if (message is String && message.isNotEmpty) {
          return message;
        }
      }
      final topMessage = data['message'];
      if (topMessage is String && topMessage.isNotEmpty) {
        return topMessage;
      }
    }
    // 서버 응답 본문이 없는 네트워크/타임아웃 계열 — e.toString() 대신 사용자 친화 메시지.
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return '서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.';
      case DioExceptionType.connectionError:
        return '네트워크 연결을 확인해주세요.';
      case DioExceptionType.badResponse:
        // 여기 도달 = 응답 body 에서 서버 표준 error.message / message 파싱이 모두 실패한 상태.
        // CloudFront/ALB/nginx 등 중간 게이트웨이가 백엔드보다 먼저 끊으면 502/503/504 를
        // HTML 에러 페이지로 내려주는데(우리 error 포맷 아님), 그대로 두면 마지막 fallback 의
        // raw Dio 문자열이 사용자에게 노출된다. 5xx 는 게이트웨이 지연/장애로 간주해 친화 메시지로.
        final status = e.response?.statusCode ?? 0;
        if (status >= 500) {
          return '서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.';
        }
        break;
      default:
        break;
    }
  }
  return e.toString().replaceFirst('Exception: ', '');
}

/// 서버 응답 없이 발생한 네트워크/타임아웃 계열 오류 여부.
///
/// 이 경우 서버 `error.code` 가 없으므로, 코드 기반 매핑 대신
/// [extractErrorMessage] 의 친화 메시지를 그대로 노출해야 한다.
bool isNetworkError(dynamic e) {
  if (e is! DioException) return false;
  if (e.response != null) return false;
  switch (e.type) {
    case DioExceptionType.connectionTimeout:
    case DioExceptionType.sendTimeout:
    case DioExceptionType.receiveTimeout:
    case DioExceptionType.connectionError:
      return true;
    default:
      return false;
  }
}

/// "요청 결과를 알 수 없는" 미확정(inconclusive) 오류 여부.
///
/// 클라이언트/게이트웨이가 백엔드 최종 응답을 받기 전에 끊긴 경우다:
///  - timeout 계열(connection/send/receive) + connectionError → 응답 자체 미수신
///  - 게이트웨이 5xx(502/503/504) → CloudFront/ALB/nginx 가 백엔드보다 먼저 끊음
///
/// 이때 서버(백엔드/SAP)는 요청을 마저 처리했을 수 있으므로 "실패" 로 단정하면
/// 안 된다. 특히 주문 취소처럼 부수효과(SAP 전송/DB 반영)가 있는 요청은,
/// 이 경우 실패 확정 대신 **최신 상태 재조회**로 실제 반영 여부를 확인해야
/// 중복 전송/오표시를 막는다. (서버 error.code 가 있는 정상 4xx/도메인 오류는
/// 결과가 확정된 것이므로 여기서 제외한다.)
bool isInconclusiveError(dynamic e) {
  if (e is! DioException) return false;
  // 백엔드가 error.code 를 담아 응답했다면 결과가 확정된 것 — 미확정 아님.
  if (extractErrorCode(e) != null) return false;
  switch (e.type) {
    case DioExceptionType.connectionTimeout:
    case DioExceptionType.sendTimeout:
    case DioExceptionType.receiveTimeout:
    case DioExceptionType.connectionError:
      return true;
    case DioExceptionType.badResponse:
      final status = e.response?.statusCode ?? 0;
      return status >= 500;
    default:
      return false;
  }
}

/// API 에러 응답에서 에러 코드(`error.code`)를 추출합니다.
///
/// DioException 의 서버 응답에서 `error.code` 를 반환하고,
/// 없으면 null 을 반환합니다.
String? extractErrorCode(dynamic e) {
  if (e is DioException) {
    final data = e.response?.data;
    if (data is Map<String, dynamic>) {
      final error = data['error'];
      if (error is Map<String, dynamic>) {
        final code = error['code'];
        if (code is String && code.isNotEmpty) {
          return code;
        }
      }
    }
  }
  return null;
}
