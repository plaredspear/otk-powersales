import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/monthly_schedule_day.dart';

void main() {
  group('MonthlyScheduleDay', () {
    final testDate = DateTime(2026, 2, 12);

    test('엔티티가 올바르게 생성된다', () {
      final scheduleDay = MonthlyScheduleDay(
        date: testDate,
        hasWork: true,
      );

      expect(scheduleDay.date, testDate);
      expect(scheduleDay.hasWork, true);
    });

    test('copyWith가 올바르게 동작한다', () {
      final original = MonthlyScheduleDay(
        date: testDate,
        hasWork: true,
      );

      final copied = original.copyWith(hasWork: false);

      expect(copied.date, original.date);
      expect(copied.hasWork, false);
      expect(original.hasWork, true); // 원본은 변경되지 않음
    });

    test('toJson과 fromJson이 정확히 동작한다', () {
      final original = MonthlyScheduleDay(
        date: testDate,
        hasWork: true,
      );

      final json = original.toJson();
      final restored = MonthlyScheduleDay.fromJson(json);

      expect(restored, original);
    });

    test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
      final scheduleDay1 = MonthlyScheduleDay(
        date: testDate,
        hasWork: true,
      );

      final scheduleDay2 = MonthlyScheduleDay(
        date: testDate,
        hasWork: true,
      );

      expect(scheduleDay1, scheduleDay2);
      expect(scheduleDay1.hashCode, scheduleDay2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      final scheduleDay1 = MonthlyScheduleDay(
        date: testDate,
        hasWork: true,
      );

      final scheduleDay2 = MonthlyScheduleDay(
        date: testDate,
        hasWork: false,
      );

      expect(scheduleDay1, isNot(scheduleDay2));
    });

    test('toString이 올바르게 동작한다', () {
      final scheduleDay = MonthlyScheduleDay(
        date: testDate,
        hasWork: true,
      );

      final string = scheduleDay.toString();

      expect(string, contains('MonthlyScheduleDay'));
      expect(string, contains('hasWork: true'));
    });
  });
}
