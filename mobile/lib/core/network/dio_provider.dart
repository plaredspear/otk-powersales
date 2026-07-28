import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/auth_interceptor.dart';
import '../../presentation/providers/auth_provider.dart';
import '../config/app_config.dart';
import '../services/push_notification_service.dart';
import 'request_cancel_controller.dart';

/// 앱 전역 Dio HTTP Client Provider
///
/// AuthInterceptor를 포함한 단일 Dio 인스턴스를 제공합니다.
/// - baseUrl: [AppConfig.baseUrl] (`--dart-define-from-file`로 주입)
/// - 토큰 자동 첨부
/// - 401 토큰 갱신
/// - 403 GPS 동의 처리
final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: AppConfig.baseUrl,
    connectTimeout: const Duration(seconds: 10),
    // 주문취소(SD03051) 등 SAP 경유 API 는 백엔드→SAP 처리에만 8~10초+ 걸려,
    // 10초 수신 타임아웃은 서버가 정상 처리 중인데도 클라이언트가 먼저 끊어 "응답 지연" 에러를
    // 냈다. 백엔드 SAP read-timeout(30초)보다 여유 있게 잡아 서버 처리 완료를 기다린다.
    receiveTimeout: const Duration(seconds: 35),
  ));

  // 생명주기 취소 토큰을 모든 요청에 자동 첨부 — AuthInterceptor 보다 먼저 등록해
  // 토큰 갱신 등 내부 재요청에도 취소가 전파되도록 한다.
  requestCancelController.attachTo(dio);

  final localDataSource = ref.watch(authLocalDataSourceProvider);
  final interceptor = AuthInterceptor(
    localDataSource: localDataSource,
    dio: dio,
    // 강제 로그아웃(세션 만료/단말 회수)은 access token 이 이미 무효라 서버 FCM 해제 API 를
    // 부를 수 없다. 그대로 두면 서버에 남은 토큰으로 로그아웃된 단말에 이전 사용자의 알림이
    // 계속 도착하므로, 단말에서 토큰을 폐기해 수신을 끊는다.
    // (푸시 서비스는 dio 에 의존하지 않으므로 Provider 순환 참조가 없다.)
    onForceLogout: ref.watch(pushNotificationServiceProvider).deleteToken,
  );
  dio.interceptors.add(interceptor);

  return dio;
});
