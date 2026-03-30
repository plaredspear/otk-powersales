import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/utils/throttled_tap_mixin.dart';

/// 테스트용 StatefulWidget + Mixin
class TestWidget extends StatefulWidget {
  const TestWidget({super.key});

  @override
  State<TestWidget> createState() => TestWidgetState();
}

class TestWidgetState extends State<TestWidget> with ThrottledTapMixin {
  @override
  Widget build(BuildContext context) => const SizedBox();
}

void main() {
  group('ThrottledTapMixin', () {
    late TestWidgetState state;

    Future<void> pumpTestWidget(WidgetTester tester) async {
      final key = GlobalKey<TestWidgetState>();
      await tester.pumpWidget(TestWidget(key: key));
      state = key.currentState!;
    }

    group('throttledTap', () {
      testWidgets('첫 탭 실행', (tester) async {
        await pumpTestWidget(tester);
        int callCount = 0;
        state.throttledTap(() => callCount++);
        expect(callCount, 1);
      });

      testWidgets('연속 탭 차단 — interval 이내 재호출 시 action 무시', (tester) async {
        await pumpTestWidget(tester);
        int callCount = 0;
        state.throttledTap(() => callCount++);
        state.throttledTap(() => callCount++);
        state.throttledTap(() => callCount++);
        expect(callCount, 1);
      });

      testWidgets('interval 경과 후 재실행', (tester) async {
        await pumpTestWidget(tester);
        int callCount = 0;
        const shortInterval = Duration(milliseconds: 50);

        state.throttledTap(() => callCount++, interval: shortInterval);
        expect(callCount, 1);

        // runAsync로 실제 시간 대기 (fake async zone 탈출)
        await tester.runAsync(
          () => Future.delayed(const Duration(milliseconds: 60)),
        );

        state.throttledTap(() => callCount++, interval: shortInterval);
        expect(callCount, 2);
      });

      testWidgets('interval이 Duration.zero이면 쓰로틀 없이 즉시 실행', (tester) async {
        await pumpTestWidget(tester);
        int callCount = 0;
        state.throttledTap(() => callCount++, interval: Duration.zero);
        state.throttledTap(() => callCount++, interval: Duration.zero);
        state.throttledTap(() => callCount++, interval: Duration.zero);
        expect(callCount, 3);
      });
    });

    group('throttledTapAsync', () {
      testWidgets('async 중복 차단 — 실행 중 재호출 시 무시', (tester) async {
        await pumpTestWidget(tester);
        int callCount = 0;

        await tester.runAsync(() async {
          state.throttledTapAsync(
            () async {
              callCount++;
              await Future.delayed(const Duration(milliseconds: 100));
            },
            interval: Duration.zero,
          );

          // 즉시 재호출 (isProcessing == true 이므로 무시)
          state.throttledTapAsync(
            () async {
              callCount++;
            },
            interval: Duration.zero,
          );

          await Future.delayed(const Duration(milliseconds: 150));
        });

        expect(callCount, 1);
      });

      testWidgets('async 완료 후 재실행 가능', (tester) async {
        await pumpTestWidget(tester);
        int callCount = 0;

        await tester.runAsync(() async {
          state.throttledTapAsync(
            () async {
              callCount++;
              await Future.delayed(const Duration(milliseconds: 30));
            },
            interval: Duration.zero,
          );

          // 첫 작업 완료 대기
          await Future.delayed(const Duration(milliseconds: 50));

          // 완료 후 재호출
          state.throttledTapAsync(
            () async {
              callCount++;
            },
            interval: Duration.zero,
          );

          await Future.delayed(const Duration(milliseconds: 20));
        });

        expect(callCount, 2);
      });

      testWidgets('async action 예외 시 _isProcessing 리셋', (tester) async {
        await pumpTestWidget(tester);
        int callCount = 0;

        await tester.runAsync(() async {
          state.throttledTapAsync(
            () async {
              throw Exception('test error');
            },
            interval: Duration.zero,
          );

          // 예외 처리 대기
          await Future.delayed(const Duration(milliseconds: 20));

          // 리셋되었으므로 다음 호출 가능
          state.throttledTapAsync(
            () async {
              callCount++;
            },
            interval: Duration.zero,
          );

          await Future.delayed(const Duration(milliseconds: 20));
        });

        expect(callCount, 1);
      });
    });
  });
}
