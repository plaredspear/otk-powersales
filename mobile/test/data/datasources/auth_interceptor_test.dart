import 'dart:async';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/session/session_reset_controller.dart';
import 'package:mobile/data/datasources/auth_interceptor.dart';
import 'package:mobile/data/datasources/auth_local_datasource.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
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

      await dio.get('/api/v1/mobile/sales/summary');

      expect(capturedRequests, hasLength(1));
      expect(
        capturedRequests.first.headers['Authorization'],
        'Bearer $_mockAccessToken',
      );
    });

    test('login 엔드포인트에는 Authorization 헤더 미첨부', () async {
      fakeLocalDataSource.accessToken = _mockAccessToken;

      await dio.post('/api/v1/mobile/auth/login', data: {
        'employeeCode': '20010585',
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

      await dio.post('/api/v1/mobile/auth/refresh', data: {
        'refreshToken': 'mockRefresh',
      });

      expect(capturedRequests, hasLength(1));
      expect(
        capturedRequests.first.headers['Authorization'],
        isNull,
      );
    });

    test('토큰이 없으면 Authorization 헤더 미첨부', () async {
      fakeLocalDataSource.accessToken = null;

      await dio.get('/api/v1/mobile/sales/summary');

      expect(capturedRequests, hasLength(1));
      expect(
        capturedRequests.first.headers['Authorization'],
        isNull,
      );
    });

    test('토큰이 빈 문자열이면 Authorization 헤더 미첨부', () async {
      fakeLocalDataSource.accessToken = '';

      await dio.get('/api/v1/mobile/sales/summary');

      expect(capturedRequests, hasLength(1));
      expect(
        capturedRequests.first.headers['Authorization'],
        isNull,
      );
    });
  });

  group('_handle403', () {
    Dio createDioWith403({
      required String body,
      String contentType = 'application/json; charset=utf-8',
    }) {
      final d = Dio(BaseOptions(baseUrl: 'https://api.test.com'));
      final localDS = FakeAuthLocalDataSource();
      d.interceptors.add(
        AuthInterceptor(localDataSource: localDS, dio: d),
      );
      d.httpClientAdapter = _Mock403Adapter(
        body: body,
        contentType: contentType,
      );
      return d;
    }

    test('Map 응답에서 GPS_CONSENT_REQUIRED 감지 시 cancel DioException 반환',
        () async {
      final d = createDioWith403(
        body:
            '{"success":false,"data":null,"error":{"code":"GPS_CONSENT_REQUIRED","message":"GPS 사용 동의가 필요합니다"}}',
      );

      try {
        await d.get('/api/v1/mobile/schedule/monthly');
        fail('Expected DioException');
      } on DioException catch (e) {
        expect(e.type, DioExceptionType.cancel);
        expect(e.message, '');
      }
    });

    test('String JSON 응답에서 GPS_CONSENT_REQUIRED 감지 시 cancel DioException 반환',
        () async {
      final d = createDioWith403(
        body:
            '{"success":false,"data":null,"error":{"code":"GPS_CONSENT_REQUIRED","message":"GPS 사용 동의가 필요합니다"}}',
        contentType: 'text/plain',
      );

      try {
        await d.get('/api/v1/mobile/schedule/monthly');
        fail('Expected DioException');
      } on DioException catch (e) {
        expect(e.type, DioExceptionType.cancel);
      }
    });

    test('GPS 외 403 에러는 일반 에러로 전달', () async {
      final d = createDioWith403(
        body:
            '{"success":false,"data":null,"error":{"code":"ACCESS_DENIED","message":"접근 권한이 없습니다"}}',
      );

      try {
        await d.get('/api/v1/admin/users');
        fail('Expected DioException');
      } on DioException catch (e) {
        expect(e.type, DioExceptionType.badResponse);
        expect(e.response?.statusCode, 403);
      }
    });

    test('파싱 불가능한 String 응답은 일반 403 에러로 전달', () async {
      final d = createDioWith403(
        body: 'not json',
        contentType: 'text/plain',
      );

      try {
        await d.get('/api/v1/mobile/schedule/monthly');
        fail('Expected DioException');
      } on DioException catch (e) {
        expect(e.type, DioExceptionType.badResponse);
      }
    });
  });

  group('_handle401 무한 루프 방지', () {
    test('갱신 성공 후 재시도도 401이면 1회만 갱신하고 에러 전파 (무한 루프 X)', () async {
      final adapter = _Mock401ExceptRefreshAdapter();
      final d = Dio(BaseOptions(baseUrl: 'https://api.test.com'));
      final localDS = FakeAuthLocalDataSource()
        ..accessToken = 'oldToken'
        ..refreshToken = 'refreshTok';
      d.interceptors.add(AuthInterceptor(localDataSource: localDS, dio: d));
      d.httpClientAdapter = adapter;

      try {
        await d.get('/api/v1/mobile/education/posts');
        fail('Expected DioException');
      } on DioException catch (e) {
        expect(e.response?.statusCode, 401);
      }

      // 토큰 갱신은 정확히 1회만 수행되어야 한다 (무한 갱신 루프가 아님).
      expect(adapter.refreshCount, 1);
      // 보호 자원 요청은 최초 1회 + 재시도 1회 = 2회에서 멈춰야 한다.
      expect(adapter.protectedCount, 2);
      // 강제 로그아웃으로 토큰이 제거되어야 한다.
      expect(localDS.accessToken, isNull);
    });
  });

  group('로그인 이력 없음 (신규 설치)', () {
    test('refresh token 이 없으면 401 을 세션 만료로 처리하지 않고 그대로 전파한다', () async {
      // 첫 설치 후 로그인 전: access/refresh token 이 모두 없다.
      final d = Dio(BaseOptions(baseUrl: 'https://api.test.com'));
      final localDS = FakeAuthLocalDataSource();
      d.interceptors.add(AuthInterceptor(localDataSource: localDS, dio: d));
      d.httpClientAdapter = _Always401Adapter();

      // 직전 테스트가 남긴 사유가 있으면 소비해 초기화.
      SessionResetController.instance.consumeReason();

      try {
        await d.get('/api/v1/mobile/education/posts');
        fail('Expected DioException');
      } on DioException catch (e) {
        // 401 이 강제 로그아웃 없이 그대로 전파되어야 한다.
        expect(e.response?.statusCode, 401);
      }

      // 만료할 세션이 없으므로 "세션 만료" 강제 로그아웃 사유가 설정되면 안 된다.
      expect(SessionResetController.instance.consumeReason(), isNull);
    });
  });

  group('취소된 요청 가드 (백그라운드 전환)', () {
    test('취소 에러는 토큰 갱신/로그아웃 없이 그대로 전파한다(토큰 보존)', () async {
      // 응답 대신 즉시 취소 에러를 방출하는 어댑터 — 백그라운드 cancelAll 을 모사.
      final d = Dio(BaseOptions(baseUrl: 'https://api.test.com'));
      final localDS = FakeAuthLocalDataSource()
        ..accessToken = 'liveToken'
        ..refreshToken = 'refreshTok';
      d.interceptors.add(AuthInterceptor(localDataSource: localDS, dio: d));
      d.httpClientAdapter = _CancelAdapter();

      try {
        await d.get('/api/v1/mobile/sales/summary');
        fail('Expected DioException');
      } on DioException catch (e) {
        // 취소 에러가 그대로 전파되어야 한다.
        expect(e.type, DioExceptionType.cancel);
      }

      // 취소는 인증 실패가 아니므로 토큰이 보존되어야 한다(강제 로그아웃 X).
      expect(localDS.accessToken, 'liveToken');
      expect(localDS.refreshToken, 'refreshTok');
    });

    test('401 갱신 도중 요청이 취소되면 로그아웃하지 않고 토큰을 보존한다', () async {
      // 보호 자원은 401, refresh 시도는 취소 에러를 던지는 어댑터.
      final d = Dio(BaseOptions(baseUrl: 'https://api.test.com'));
      final localDS = FakeAuthLocalDataSource()
        ..accessToken = 'oldToken'
        ..refreshToken = 'refreshTok';
      d.interceptors.add(AuthInterceptor(localDataSource: localDS, dio: d));
      d.httpClientAdapter = _Cancel401RefreshAdapter();

      try {
        await d.get('/api/v1/mobile/education/posts');
        fail('Expected DioException');
      } on DioException catch (_) {
        // 에러 타입은 구현 상세이므로 단정하지 않는다 — 핵심은 토큰 보존이다.
      }

      // refresh 가 취소된 것은 인증 실패가 아니므로 토큰을 지우면 안 된다.
      expect(localDS.accessToken, 'oldToken');
      expect(localDS.refreshToken, 'refreshTok');
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
  String? savedEmployeeNumber;
  bool rememberEmployeeNumber = false;

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
  Future<void> saveEmployeeNumber(String employeeCode) async {
    savedEmployeeNumber = employeeCode;
    rememberEmployeeNumber = true;
  }

  @override
  Future<String?> getSavedEmployeeNumber() async {
    if (!rememberEmployeeNumber) return null;
    return savedEmployeeNumber;
  }

  @override
  Future<void> clearSavedEmployeeNumber() async {
    savedEmployeeNumber = null;
    rememberEmployeeNumber = false;
  }

  @override
  Future<bool> isRememberEmployeeNumberEnabled() async {
    return rememberEmployeeNumber;
  }

  // AuthLocalDataSource는 class이므로 noSuchMethod로 미구현 메서드 처리 불필요
  // 모든 public 메서드를 위에서 구현했습니다.
}

// --- Mock 403 Adapter ---

/// 모든 요청에 403 응답을 반환하는 테스트용 HttpClientAdapter
class _Mock403Adapter implements HttpClientAdapter {
  final String body;
  final String contentType;

  _Mock403Adapter({
    required this.body,
    this.contentType = 'application/json; charset=utf-8',
  });

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    return ResponseBody.fromString(body, 403, headers: {
      'content-type': [contentType],
    });
  }

  @override
  void close({bool force = false}) {}
}

/// /auth/refresh 는 200(새 토큰)으로, 그 외 보호 자원은 항상 401로 응답하는 어댑터.
/// "갱신은 성공하지만 서버가 계속 401을 주는" 무한 루프 시나리오 재현용.
class _Mock401ExceptRefreshAdapter implements HttpClientAdapter {
  int refreshCount = 0;
  int protectedCount = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    if (options.path.contains('/auth/refresh')) {
      refreshCount++;
      return ResponseBody.fromString(
        '{"success":true,"data":{"accessToken":"newToken"}}',
        200,
        headers: {
          'content-type': ['application/json; charset=utf-8'],
        },
      );
    }
    protectedCount++;
    return ResponseBody.fromString(
      '{"success":false,"data":null,"error":{"code":"UNAUTHORIZED","message":"인증이 필요합니다"}}',
      401,
      headers: {
        'content-type': ['application/json; charset=utf-8'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

/// 모든 요청에 401 로 응답하는 어댑터. 로그인 전(무인증) 요청이 인증 필요 엔드포인트에서
/// 401 을 받는 상황을 모사한다.
class _Always401Adapter implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    return ResponseBody.fromString(
      '{"success":false,"data":null,"error":{"code":"UNAUTHORIZED","message":"인증이 필요합니다"}}',
      401,
      headers: {
        'content-type': ['application/json; charset=utf-8'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

/// 모든 요청을 취소 에러(DioExceptionType.cancel)로 종결하는 어댑터.
/// 백그라운드 전환 시 cancelAll() 로 진행 중 요청이 취소되는 상황을 모사한다.
class _CancelAdapter implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    throw DioException.requestCancelled(
      requestOptions: options,
      reason: 'app lifecycle',
    );
  }

  @override
  void close({bool force = false}) {}
}

/// 보호 자원은 401, /auth/refresh 시도는 취소 에러로 종결하는 어댑터.
/// 401 → 토큰 갱신 진행 중에 백그라운드 전환으로 갱신이 취소되는 상황을 모사한다.
class _Cancel401RefreshAdapter implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    if (options.path.contains('/auth/refresh')) {
      throw DioException.requestCancelled(
        requestOptions: options,
        reason: 'app lifecycle',
      );
    }
    return ResponseBody.fromString(
      '{"success":false,"data":null,"error":{"code":"UNAUTHORIZED","message":"인증이 필요합니다"}}',
      401,
      headers: {
        'content-type': ['application/json; charset=utf-8'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

// --- Test Data ---

const _mockAccessToken = 'testAccessTokenAbc123';
