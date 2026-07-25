import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/utils/order_deadline.dart';

void main() {
  group('OrderDeadline (백엔드 OrderDeadlineCalculator 동등)', () {
    test('실질 마감 = (납기일 - 1일) 13:50', () {
      expect(
        OrderDeadline.deadlineFor(DateTime(2026, 7, 26)),
        DateTime(2026, 7, 25, 13, 50),
      );
    });

    test('월 경계를 넘는 납기일(3/1) → 전월 말일 13:50', () {
      expect(
        OrderDeadline.deadlineFor(DateTime(2026, 3, 1)),
        DateTime(2026, 2, 28, 13, 50),
      );
    });

    test('납기일 내일 + 지금 13:49 → 주문 가능', () {
      expect(
        OrderDeadline.isWithinDeadline(
          DateTime(2026, 7, 26),
          now: DateTime(2026, 7, 25, 13, 49, 59),
        ),
        isTrue,
      );
    });

    test('납기일 내일 + 지금 13:50 정각 → 마감 (경계 포함 거부)', () {
      expect(
        OrderDeadline.isWithinDeadline(
          DateTime(2026, 7, 26),
          now: DateTime(2026, 7, 25, 13, 50),
        ),
        isFalse,
      );
    });

    test('납기일 오늘 → 시각 무관 마감', () {
      expect(
        OrderDeadline.isWithinDeadline(
          DateTime(2026, 7, 25),
          now: DateTime(2026, 7, 25, 9, 0),
        ),
        isFalse,
      );
    });

    test('납기일 모레 → 내일 13:50 까지 가능', () {
      expect(
        OrderDeadline.isWithinDeadline(
          DateTime(2026, 7, 27),
          now: DateTime(2026, 7, 25, 23, 30),
        ),
        isTrue,
      );
      expect(
        OrderDeadline.isWithinDeadline(
          DateTime(2026, 7, 27),
          now: DateTime(2026, 7, 26, 14, 0),
        ),
        isFalse,
      );
    });
  });
}
