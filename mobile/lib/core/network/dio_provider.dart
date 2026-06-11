import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/auth_interceptor.dart';
import '../../presentation/providers/auth_provider.dart';
import '../config/app_config.dart';
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
    receiveTimeout: const Duration(seconds: 10),
  ));

  // 생명주기 취소 토큰을 모든 요청에 자동 첨부 — AuthInterceptor 보다 먼저 등록해
  // 토큰 갱신 등 내부 재요청에도 취소가 전파되도록 한다.
  requestCancelController.attachTo(dio);

  final localDataSource = ref.watch(authLocalDataSourceProvider);
  final interceptor = AuthInterceptor(
    localDataSource: localDataSource,
    dio: dio,
  );
  dio.interceptors.add(interceptor);

  return dio;
});
