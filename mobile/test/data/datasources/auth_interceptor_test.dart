import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/auth_interceptor.dart';
import 'package:mobile/data/datasources/auth_local_datasource.dart';

void main() {
  late FakeAuthLocalDataSource fakeLocalDataSource;
  late Dio dio;
  late AuthInterceptor interceptor;
  late List<RequestOptions> capturedRequests;

  setUp(() {
    fakeLocalDataSource = FakeAuthLocalDataSource();
    dio = Dio(BaseOptions(baseUrl: 'https://api.test.com'));

    interceptor = AuthInterceptor(
      localDataSource: fakeLocalDataSource,
      dio: dio,
    );

    capturedRequests = [];

    // AuthInterceptor를 먼저 추가
    dio.interceptors.add(interceptor);

    // 요청 캡처 + 응답 시뮬레이션 인터셉터 (AuthInterceptor 뒤에 추가)
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          capturedRequests.add(options);
          // 요청을 가로채서 성공 응답 반환 (실제 네트워크 호출 방지)
          handler.resolve(Response(
            requestOptions: options,
            statusCode: 200,
            data: {'status': 'ok'},
          ));
        },
      ),
    );
  });

  group('onRequest', () {
    test('요청에 Authorization 헤더 자동 첨부', () async {
      fakeLocalDataSource.accessToken = _mockAccessToken;

      await dio.get('/api/v1/sales/summary');

      expect(capturedRequests, hasLength(1));
      expect(
        capturedRequests.first.headers['Authorization'],
        'Bearer $_mockAccessToken',
      );
    });

    test('login 엔드포인트에는 Authorization 헤더 미첨부', () async {
      fakeLocalDataSource.accessToken = _mockAccessToken;

      await dio.post('/api/v1/auth/login', data: {
        'employee_id': '20010585',
        'password': 'test1234',
      });

      expect(capturedRequests, hasLength(1));
      expect(
        capturedRequests.first.headers['Authorization'],
        isNull,
      );
    });

    test('refresh 엔드포인트에는 Authorization 헤더 미첨부', () async {
      fakeLocalDataSource.accessToken = _mockAccessToken;

      await dio.post('/api/v1/auth/refresh', data: {
        'refresh_token': 'mock_refresh',
      });

      expect(capturedRequests, hasLength(1));
      expect(
        capturedRequests.first.headers['Authorization'],
        isNull,
      );
    });

    test('토큰이 없으면 Authorization 헤더 미첨부', () async {
      fakeLocalDataSource.accessToken = null;

      await dio.get('/api/v1/sales/summary');

      expect(capturedRequests, hasLength(1));
      expect(
        capturedRequests.first.headers['Authorization'],
        isNull,
      );
    });

    test('토큰이 빈 문자열이면 Authorization 헤더 미첨부', () async {
      fakeLocalDataSource.accessToken = '';

      await dio.get('/api/v1/sales/summary');

      expect(capturedRequests, hasLength(1));
      expect(
        capturedRequests.first.headers['Authorization'],
        isNull,
      );
    });
  });
}

// --- Fakes ---

/// AuthLocalDataSource의 Fake 구현
///
/// SecureStorage/Hive 없이 메모리에서 토큰을 관리합니다.
class FakeAuthLocalDataSource implements AuthLocalDataSource {
  String? accessToken;
  String? refreshToken;
  bool autoLogin = false;
  String? savedEmployeeId;
  bool rememberEmployeeId = false;

  @override
  Future<void> saveAccessToken(String token) async {
    accessToken = token;
  }

  @override
  Future<void> saveRefreshToken(String token) async {
    refreshToken = token;
  }

  @override
  Future<String?> getAccessToken() async {
    return accessToken;
  }

  @override
  Future<String?> getRefreshToken() async {
    return refreshToken;
  }

  @override
  Future<void> clearTokens() async {
    accessToken = null;
    refreshToken = null;
  }

  @override
  Future<void> setAutoLogin(bool enabled) async {
    autoLogin = enabled;
  }

  @override
  Future<bool> isAutoLoginEnabled() async {
    return autoLogin;
  }

  @override
  Future<String> getDeviceId() async {
    return 'fake-device-id';
  }

  @override
  Future<void> saveEmployeeId(String employeeId) async {
    savedEmployeeId = employeeId;
    rememberEmployeeId = true;
  }

  @override
  Future<String?> getSavedEmployeeId() async {
    if (!rememberEmployeeId) return null;
    return savedEmployeeId;
  }

  @override
  Future<void> clearSavedEmployeeId() async {
    savedEmployeeId = null;
    rememberEmployeeId = false;
  }

  @override
  Future<bool> isRememberEmployeeIdEnabled() async {
    return rememberEmployeeId;
  }

  // AuthLocalDataSource는 class이므로 noSuchMethod로 미구현 메서드 처리 불필요
  // 모든 public 메서드를 위에서 구현했습니다.
}

// --- Test Data ---

const _mockAccessToken = 'test_access_token_abc123';
