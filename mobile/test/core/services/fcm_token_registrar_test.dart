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
}

/// getToken 만 제어하는 Fake PushNotificationService.
class _FakePush implements PushNotificationService {
  String? tokenToReturn;

  @override
  Future<String?> getToken() async => tokenToReturn;

  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// 호출을 기록하고 실패를 주입할 수 있는 Fake FcmTokenApiDataSource.
class _FakeApi implements FcmTokenApiDataSource {
  final List<String> registeredTokens = [];
  int registerAttempts = 0;
  int unregisterCalls = 0;
  bool throwOnRegister = false;
  bool throwOnUnregister = false;

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
}
