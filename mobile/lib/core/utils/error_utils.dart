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
  }
  return e.toString().replaceFirst('Exception: ', '');
}
