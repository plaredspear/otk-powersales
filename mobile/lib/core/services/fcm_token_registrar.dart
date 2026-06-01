import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:logger/logger.dart';

import '../../data/datasources/fcm_token_api_datasource.dart';
import '../network/dio_provider.dart';
import 'push_notification_service.dart';

/// FCM 토큰을 서버에 등록/해제하는 코디네이터.
///
/// 인증 흐름(로그인 성공/로그아웃)과 FCM 토큰 갱신(onTokenRefresh)에서 호출된다.
/// 푸시는 부가 기능이므로 네트워크 실패는 조용히 무시(로깅)하여 인증 흐름을 막지 않는다.
class FcmTokenRegistrar {
  FcmTokenRegistrar({
    required PushNotificationService push,
    required FcmTokenApiDataSource api,
    Logger? logger,
  })  : _push = push,
        _api = api,
        _logger = logger ?? Logger();

  final PushNotificationService _push;
  final FcmTokenApiDataSource _api;
  final Logger _logger;

  /// 현재 디바이스 토큰을 조회해 서버에 등록한다 (로그인/자동로그인 성공 후).
  Future<void> registerCurrentToken() async {
    final token = await _push.getToken();
    if (token == null) return;
    await registerToken(token);
  }

  /// 지정 토큰을 서버에 등록한다 (onTokenRefresh).
  Future<void> registerToken(String token) async {
    try {
      await _api.register(token);
    } catch (e) {
      _logger.w('FCM 토큰 등록 실패(무시): $e');
    }
  }

  /// 서버에서 토큰을 해제한다 (로그아웃 — access token 이 유효한 시점에 호출).
  Future<void> unregister() async {
    try {
      await _api.unregister();
    } catch (e) {
      _logger.w('FCM 토큰 해제 실패(무시): $e');
    }
  }
}

/// FcmTokenApiDataSource Provider
final fcmTokenApiDataSourceProvider = Provider<FcmTokenApiDataSource>((ref) {
  return FcmTokenApiDataSource(ref.watch(dioProvider));
});

/// FcmTokenRegistrar Provider
final fcmTokenRegistrarProvider = Provider<FcmTokenRegistrar>((ref) {
  return FcmTokenRegistrar(
    push: ref.watch(pushNotificationServiceProvider),
    api: ref.watch(fcmTokenApiDataSourceProvider),
  );
});
