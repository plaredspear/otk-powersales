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

  group('OrderDeadline.earliestDeliveryDate (레거시 write.jsp:21 min 정합)', () {
    test('13:50 이전 → 내일', () {
      expect(
        OrderDeadline.earliestDeliveryDate(now: DateTime(2026, 7, 25, 13, 49)),
        DateTime(2026, 7, 26),
      );
    });

    test('13:50 정각 이후 → 모레 (내일 납기도 이미 마감)', () {
      expect(
        OrderDeadline.earliestDeliveryDate(now: DateTime(2026, 7, 25, 13, 50)),
        DateTime(2026, 7, 27),
      );
      expect(
        OrderDeadline.earliestDeliveryDate(now: DateTime(2026, 7, 25, 23, 59)),
        DateTime(2026, 7, 27),
      );
    });

    test('월 경계 — 말일 13:50 이후 → 익월 2일', () {
      expect(
        OrderDeadline.earliestDeliveryDate(now: DateTime(2026, 2, 28, 14, 0)),
        DateTime(2026, 3, 2),
      );
    });

    test('반환값은 항상 그 시점에 주문 가능한 납기일', () {
      for (final now in [
        DateTime(2026, 7, 25, 0, 0),
        DateTime(2026, 7, 25, 13, 49, 59),
        DateTime(2026, 7, 25, 13, 50),
        DateTime(2026, 12, 31, 20, 0),
      ]) {
        final earliest = OrderDeadline.earliestDeliveryDate(now: now);
        expect(
          OrderDeadline.isWithinDeadline(earliest, now: now),
          isTrue,
          reason: '$now → $earliest 는 마감 전이어야 한다',
        );
        // 하루 앞선 날짜는 반드시 마감 후여야 "가장 이른" 이 성립한다.
        final oneDayEarlier = DateTime(
          earliest.year,
          earliest.month,
          earliest.day - 1,
        );
        expect(
          OrderDeadline.isWithinDeadline(oneDayEarlier, now: now),
          isFalse,
          reason: '$now → $oneDayEarlier 는 이미 마감이어야 한다',
        );
      }
    });
  });
}
