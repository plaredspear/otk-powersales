import 'package:dio/dio.dart';

/// FCM 디바이스 토큰 등록/해제 API 데이터소스.
///
/// 백엔드: `POST/DELETE /api/v1/mobile/fcm-token` (인증 필요 — AuthInterceptor 가 토큰 첨부).
class FcmTokenApiDataSource {
  final Dio _dio;

  FcmTokenApiDataSource(this._dio);

  /// 현재 디바이스 토큰을 인증 사용자 계정에 등록/갱신한다.
  Future<void> register(String token) async {
    await _dio.post(
      '/api/v1/mobile/fcm-token',
      data: {'token': token},
    );
  }

  /// 인증 사용자 계정의 FCM 토큰을 해제한다 (로그아웃).
  Future<void> unregister() async {
    await _dio.delete('/api/v1/mobile/fcm-token');
  }
}
