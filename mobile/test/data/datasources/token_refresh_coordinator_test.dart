import 'dart:async';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/network/request_cancel_controller.dart';
import 'package:mobile/data/datasources/token_refresh_coordinator.dart';

/// refresh token 회전 단일화 회귀 테스트.
///
/// 서버는 refresh token 을 1회용으로 소비하므로, 같은 token 이 두 번 나가면 재사용(탈취)
/// 판정으로 token family 전체가 무효화되어 사용자가 강제 로그아웃된다. 코디네이터가
/// (1) 진행 중 회전을 공유하고, (2) 이미 소비된 token 의 재요청을 막고,
/// (3) 백그라운드 전환(생명주기 일괄 취소)에도 회전 응답을 지키는지 검증한다.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    // 코디네이터는 프로세스 싱글턴이라 테스트 간 소비 캐시가 남는다.
    TokenRefreshCoordinator.instance.reset();
  });

  Dio buildDio(HttpClientAdapter adapter, {bool attachLifecycle = false}) {
    final dio = Dio(BaseOptions(baseUrl: 'https://api.test.com'));
    if (attachLifecycle) requestCancelController.attachTo(dio);
    dio.httpClientAdapter = adapter;
    return dio;
  }

  test('동시 회전 요청은 HTTP 를 1회만 보내고 결과를 공유한다', () async {
    final adapter = _GatedRefreshAdapter();
    final dio = buildDio(adapter);

    final first = TokenRefreshCoordinator.instance
        .refresh(dio: dio, refreshToken: 'R1');
    final second = TokenRefreshCoordinator.instance
        .refresh(dio: dio, refreshToken: 'R1');

    adapter.release();
    final results = await Future.wait([first, second]);

    // 두 번 나갔다면 진 쪽이 서버에서 재사용 판정 → family revoke → 강제 로그아웃이다.
    expect(adapter.callCount, 1);
    expect(results[0]['refreshToken'], 'R2');
    expect(results[1]['refreshToken'], 'R2');
  });

  test('이미 소비된 refresh token 으로 다시 요청하면 HTTP 없이 직전 회전 결과를 돌려준다',
      () async {
    final adapter = _GatedRefreshAdapter()..release();
    final dio = buildDio(adapter);

    final first = await TokenRefreshCoordinator.instance
        .refresh(dio: dio, refreshToken: 'R1');
    // 회전이 끝난 뒤, 그 전에 옛 token 을 읽어 둔 다른 경로가 뒤늦게 도착한 상황.
    final late = await TokenRefreshCoordinator.instance
        .refresh(dio: dio, refreshToken: 'R1');

    // 보내 봐야 재사용 판정으로 세션만 잃는다 — 요청은 여전히 1회여야 한다.
    expect(adapter.callCount, 1);
    expect(late['accessToken'], first['accessToken']);
    expect(late['refreshToken'], 'R2');
  });

  test('다른 refresh token 이면 정상적으로 새 회전을 수행한다', () async {
    final adapter = _GatedRefreshAdapter()..release();
    final dio = buildDio(adapter);

    await TokenRefreshCoordinator.instance.refresh(dio: dio, refreshToken: 'R1');
    await TokenRefreshCoordinator.instance.refresh(dio: dio, refreshToken: 'R2');

    expect(adapter.callCount, 2);
  });

  test('reset() 이후에는 같은 token 이어도 다시 회전을 시도한다', () async {
    final adapter = _GatedRefreshAdapter()..release();
    final dio = buildDio(adapter);

    await TokenRefreshCoordinator.instance.refresh(dio: dio, refreshToken: 'R1');
    TokenRefreshCoordinator.instance.reset();
    await TokenRefreshCoordinator.instance.refresh(dio: dio, refreshToken: 'R1');

    expect(adapter.callCount, 2);
  });

  test('회전 요청은 생명주기 일괄 취소(백그라운드 전환)에 취소되지 않는다', () async {
    final adapter = _GatedRefreshAdapter();
    final dio = buildDio(adapter, attachLifecycle: true);

    final pending = TokenRefreshCoordinator.instance
        .refresh(dio: dio, refreshToken: 'R1');

    // 앱이 백그라운드로 전환되어 진행 중 요청이 일괄 취소되는 상황.
    requestCancelController.cancelAll('test: app lifecycle');
    adapter.release();

    // 취소되면 서버만 회전을 반영하고 단말은 새 token 을 잃어 다음 실행에서
    // 재사용 판정 → 강제 로그아웃이 된다. 끝까지 수신해야 한다.
    final result = await pending;
    expect(result['refreshToken'], 'R2');
  });

  test('회전 실패는 in-flight 를 해제해 다음 시도를 막지 않는다', () async {
    final failing = _FailingRefreshAdapter();
    final dio = buildDio(failing);

    await expectLater(
      TokenRefreshCoordinator.instance.refresh(dio: dio, refreshToken: 'R1'),
      throwsA(isA<DioException>()),
    );

    // 실패한 token 은 소비되지 않았으므로 재시도가 가능해야 한다.
    await expectLater(
      TokenRefreshCoordinator.instance.refresh(dio: dio, refreshToken: 'R1'),
      throwsA(isA<DioException>()),
    );
    expect(failing.callCount, 2);
  });
}

/// `/auth/refresh` 를 R1 → R2 로 회전시키는 어댑터. [release] 전까지 응답을 붙잡아
/// "회전 진행 중" 상태를 재현한다.
class _GatedRefreshAdapter implements HttpClientAdapter {
  final Completer<void> _gate = Completer<void>();
  int callCount = 0;

  void release() {
    if (!_gate.isCompleted) _gate.complete();
  }

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    callCount++;
    await _gate.future;
    return ResponseBody.fromString(
      '{"success":true,"data":{"accessToken":"A2","refreshToken":"R2","expiresIn":3600}}',
      200,
      headers: {
        'content-type': ['application/json; charset=utf-8'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

/// 네트워크 오류로 회전이 실패하는 어댑터.
class _FailingRefreshAdapter implements HttpClientAdapter {
  int callCount = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    callCount++;
    throw DioException.connectionError(
      requestOptions: options,
      reason: 'network down',
    );
  }

  @override
  void close({bool force = false}) {}
}
