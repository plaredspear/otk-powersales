import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/services/fcm_token_registrar.dart';
import 'package:mobile/core/services/push_notification_service.dart';
import 'package:mobile/data/datasources/fcm_token_api_datasource.dart';

void main() {
  late _FakePush push;
  late _FakeApi api;
  late FcmTokenRegistrar registrar;

  setUp(() {
    push = _FakePush();
    api = _FakeApi();
    registrar = FcmTokenRegistrar(push: push, api: api);
  });

  group('registerCurrentToken', () {
    test('토큰이 있으면 해당 토큰으로 서버 등록을 호출한다', () async {
      push.tokenToReturn = 'fcm-abc';

      await registrar.registerCurrentToken();

      expect(api.registeredTokens, ['fcm-abc']);
    });

    test('토큰이 null 이면 서버 등록을 호출하지 않는다', () async {
      push.tokenToReturn = null;

      await registrar.registerCurrentToken();

      expect(api.registerAttempts, 0);
    });
  });

  group('registerToken', () {
    test('지정 토큰으로 서버 등록을 호출한다', () async {
      await registrar.registerToken('fcm-xyz');

      expect(api.registeredTokens, ['fcm-xyz']);
    });

    test('서버 등록이 실패해도 예외를 전파하지 않는다', () async {
      api.throwOnRegister = true;

      await expectLater(registrar.registerToken('t'), completes);
      expect(api.registerAttempts, 1);
    });
  });

  group('unregister', () {
    test('서버 해제를 호출한다', () async {
      await registrar.unregister();

      expect(api.unregisterCalls, 1);
    });

    test('서버 해제가 실패해도 예외를 전파하지 않는다', () async {
      api.throwOnUnregister = true;

      await expectLater(registrar.unregister(), completes);
      expect(api.unregisterCalls, 1);
    });
  });

  group('clearBadge', () {
    test('기기 배지와 서버 카운터를 함께 초기화한다', () async {
      await registrar.clearBadge();

      // 서버 카운터를 리셋하지 않으면 다음 푸시가 이전 카운트를 이어받는다(APNs badge 는 절대값).
      expect(push.clearBadgeCalls, 1);
      expect(api.clearBadgeCalls, 1);
    });

    test('서버 리셋이 실패해도 예외를 전파하지 않는다', () async {
      api.throwOnClearBadge = true;

      await expectLater(registrar.clearBadge(), completes);
      expect(push.clearBadgeCalls, 1);
      expect(api.clearBadgeCalls, 1);
    });

    test('cancelNotifications 를 로컬 배지 처리에 그대로 전달한다', () async {
      // 포그라운드 수신 경로 — 방금 띄운 알림을 지우지 않아야 한다.
      await registrar.clearBadge(cancelNotifications: false);

      expect(push.lastCancelNotifications, isFalse);
      // 서버 카운터는 경로와 무관하게 리셋한다.
      expect(api.clearBadgeCalls, 1);
    });
  });
}

/// getToken / clearBadge 만 제어하는 Fake PushNotificationService.
class _FakePush implements PushNotificationService {
  String? tokenToReturn;
  int clearBadgeCalls = 0;
  bool? lastCancelNotifications;

  @override
  Future<String?> getToken() async => tokenToReturn;

  @override
  Future<void> clearBadge({bool cancelNotifications = true}) async {
    clearBadgeCalls++;
    lastCancelNotifications = cancelNotifications;
  }

  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// 호출을 기록하고 실패를 주입할 수 있는 Fake FcmTokenApiDataSource.
class _FakeApi implements FcmTokenApiDataSource {
  final List<String> registeredTokens = [];
  int registerAttempts = 0;
  int unregisterCalls = 0;
  int clearBadgeCalls = 0;
  bool throwOnRegister = false;
  bool throwOnUnregister = false;
  bool throwOnClearBadge = false;

  @override
  Future<void> register(String token) async {
    registerAttempts++;
    if (throwOnRegister) throw Exception('register 실패');
    registeredTokens.add(token);
  }

  @override
  Future<void> unregister() async {
    unregisterCalls++;
    if (throwOnUnregister) throw Exception('unregister 실패');
  }

  @override
  Future<void> clearBadge() async {
    clearBadgeCalls++;
    if (throwOnClearBadge) throw Exception('clearBadge 실패');
  }
}
