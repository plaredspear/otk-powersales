import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/config/app_config.dart';
import 'package:mobile/core/network/dio_provider.dart';
import 'package:mobile/data/datasources/auth_local_datasource.dart';
import 'package:mobile/presentation/providers/auth_provider.dart';

/// Fake AuthLocalDataSource (테스트용)
class FakeAuthLocalDataSource implements AuthLocalDataSource {
  @override
  Future<String?> getAccessToken() async => null;

  @override
  Future<String?> getRefreshToken() async => null;

  @override
  Future<void> saveAccessToken(String token) async {}

  @override
  Future<void> saveRefreshToken(String token) async {}

  @override
  Future<void> clearTokens() async {}

  @override
  Future<void> saveEmployeeNumber(String employeeCode) async {}

  @override
  Future<String?> getSavedEmployeeNumber() async => null;

  @override
  Future<void> setAutoLogin(bool enabled) async {}

  @override
  Future<bool> isAutoLoginEnabled() async => false;

  @override
  Future<String> getDeviceId() async => 'fake-device-id';

  @override
  Future<void> clearSavedEmployeeNumber() async {}

  @override
  Future<bool> isRememberEmployeeNumberEnabled() async => false;
}

void main() {
  group('dioProvider', () {
    test('baseUrl이 AppConfig.baseUrl을 사용해야 한다', () {
      final container = ProviderContainer(
        overrides: [
          authLocalDataSourceProvider.overrideWithValue(
            FakeAuthLocalDataSource(),
          ),
        ],
      );
      addTearDown(container.dispose);

      final dio = container.read(dioProvider);

      expect(dio, isA<Dio>());
      expect(dio.options.baseUrl, AppConfig.baseUrl);
    });

    test('connectTimeout이 10초여야 한다', () {
      final container = ProviderContainer(
        overrides: [
          authLocalDataSourceProvider.overrideWithValue(
            FakeAuthLocalDataSource(),
          ),
        ],
      );
      addTearDown(container.dispose);

      final dio = container.read(dioProvider);

      expect(dio.options.connectTimeout, const Duration(seconds: 10));
    });

    test('receiveTimeout이 35초여야 한다', () {
      final container = ProviderContainer(
        overrides: [
          authLocalDataSourceProvider.overrideWithValue(
            FakeAuthLocalDataSource(),
          ),
        ],
      );
      addTearDown(container.dispose);

      final dio = container.read(dioProvider);

      // SAP 경유 API(주문취소 등) 처리 대기 위해 lib에서 35초로 상향(dio_provider.dart 주석 참조).
      expect(dio.options.receiveTimeout, const Duration(seconds: 35));
    });
  });
}
