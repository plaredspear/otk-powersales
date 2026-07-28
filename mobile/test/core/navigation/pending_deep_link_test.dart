import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/navigation/pending_deep_link.dart';

void main() {
  setUp(() {
    // 프로세스 전역 싱글턴이므로 테스트 간 잔여 값을 비운다.
    PendingDeepLink.instance.consume();
  });

  test('적재한 딥링크를 consume 으로 1회만 꺼낸다', () {
    PendingDeepLink.instance.store(
      const DeepLinkTarget(route: '/notices/detail', arguments: 42),
    );

    final first = PendingDeepLink.instance.consume();
    expect(first?.route, '/notices/detail');
    expect(first?.arguments, 42);

    // 소비 후에는 비어 있어 같은 화면으로 중복 이동하지 않는다.
    expect(PendingDeepLink.instance.consume(), isNull);
  });

  test('중복 적재 시 최신 1건만 유지한다', () {
    PendingDeepLink.instance
      ..store(const DeepLinkTarget(route: '/notices/detail', arguments: 1))
      ..store(const DeepLinkTarget(route: '/notices/detail', arguments: 2));

    expect(PendingDeepLink.instance.consume()?.arguments, 2);
    expect(PendingDeepLink.instance.consume(), isNull);
  });

  test('적재/소비 시 구독자에게 통지한다', () {
    var notified = 0;
    void listener() => notified++;
    PendingDeepLink.instance.pending.addListener(listener);
    addTearDown(
      () => PendingDeepLink.instance.pending.removeListener(listener),
    );

    PendingDeepLink.instance.store(
      const DeepLinkTarget(route: '/notices/detail', arguments: 7),
    );
    expect(notified, 1);

    PendingDeepLink.instance.consume();
    expect(notified, 2);
  });
}
