import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/auth_interceptor.dart';
import 'auth_provider.dart';

/// 앱 전역 Dio HTTP Client Provider
///
/// AuthInterceptor를 포함한 단일 Dio 인스턴스를 제공합니다.
/// - 토큰 자동 첨부
/// - 401 토큰 갱신
/// - 403 GPS 동의 처리
final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: 'https://api.example.com', // TODO: 실제 API URL로 변경
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
  ));

  final localDataSource = ref.watch(authLocalDataSourceProvider);
  final interceptor = AuthInterceptor(
    localDataSource: localDataSource,
    dio: dio,
  );
  dio.interceptors.add(interceptor);

  return dio;
});
