import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_schedule_info.dart';
import 'package:mobile/domain/entities/schedule_store_detail.dart';

void main() {
  group('ReportProgress', () {
    test('엔티티가 올바르게 생성된다', () {
      final progress = ReportProgress(
        completed: 0,
        total: 3,
        workType: '진열',
      );

      expect(progress.completed, 0);
      expect(progress.total, 3);
      expect(progress.workType, '진열');
    });

    test('copyWith가 올바르게 동작한다', () {
      final original = ReportProgress(
        completed: 0,
        total: 3,
        workType: '진열',
      );

      final copied = original.copyWith(completed: 1);

      expect(copied.completed, 1);
      expect(copied.total, original.total);
      expect(original.completed, 0); // 원본은 변경되지 않음
    });

    test('toJson과 fromJson이 정확히 동작한다', () {
      final original = ReportProgress(
        completed: 0,
        total: 3,
        workType: '진열',
      );

      final json = original.toJson();
      final restored = ReportProgress.fromJson(json);

      expect(restored, original);
    });

    test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
      final progress1 = ReportProgress(
        completed: 0,
        total: 3,
        workType: '진열',
      );

      final progress2 = ReportProgress(
        completed: 0,
        total: 3,
        workType: '진열',
      );

      expect(progress1, progress2);
      expect(progress1.hashCode, progress2.hashCode);
    });
  });

  group('DailyScheduleInfo', () {
    final testStores = [
      ScheduleStoreDetail(
        storeId: 1,
        storeName: '(주)이마트트레이더스명지점',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      ),
      ScheduleStoreDetail(
        storeId: 2,
        storeName: '롯데마트 사상',
        workType1: '진열',
        workType2: '전담',
        workType3: '격고',
        isRegistered: false,
      ),
    ];

    final testProgress = ReportProgress(
      completed: 0,
      total: 3,
      workType: '진열',
    );

    test('엔티티가 올바르게 생성된다', () {
      final scheduleInfo = DailyScheduleInfo(
        date: '2020년 08월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: testProgress,
        stores: testStores,
      );

      expect(scheduleInfo.date, '2020년 08월 04일(화)');
      expect(scheduleInfo.memberName, '최금주');
      expect(scheduleInfo.employeeNumber, '20030117');
      expect(scheduleInfo.reportProgress, testProgress);
      expect(scheduleInfo.stores, testStores);
    });

    test('copyWith가 올바르게 동작한다', () {
      final original = DailyScheduleInfo(
        date: '2020년 08월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: testProgress,
        stores: testStores,
      );

      final newProgress = ReportProgress(
        completed: 1,
        total: 3,
        workType: '진열',
      );

      final copied = original.copyWith(reportProgress: newProgress);

      expect(copied.date, original.date);
      expect(copied.reportProgress, newProgress);
      expect(original.reportProgress, testProgress); // 원본은 변경되지 않음
    });

    test('toJson과 fromJson이 정확히 동작한다', () {
      final original = DailyScheduleInfo(
        date: '2020년 08월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: testProgress,
        stores: testStores,
      );

      final json = original.toJson();
      final restored = DailyScheduleInfo.fromJson(json);

      expect(restored, original);
    });

    test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
      final info1 = DailyScheduleInfo(
        date: '2020년 08월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: testProgress,
        stores: testStores,
      );

      final info2 = DailyScheduleInfo(
        date: '2020년 08월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: testProgress,
        stores: testStores,
      );

      expect(info1, info2);
      expect(info1.hashCode, info2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      final info1 = DailyScheduleInfo(
        date: '2020년 08월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: testProgress,
        stores: testStores,
      );

      final info2 = DailyScheduleInfo(
        date: '2020년 08월 05일(수)',
        memberName: '김철수',
        employeeNumber: '20030118',
        reportProgress: testProgress,
        stores: testStores,
      );

      expect(info1, isNot(info2));
    });

    test('빈 stores 리스트를 처리할 수 있다', () {
      final scheduleInfo = DailyScheduleInfo(
        date: '2020년 08월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: testProgress,
        stores: [],
      );

      expect(scheduleInfo.stores, isEmpty);

      final json = scheduleInfo.toJson();
      final restored = DailyScheduleInfo.fromJson(json);

      expect(restored.stores, isEmpty);
      expect(restored, scheduleInfo);
    });

    test('toString이 올바르게 동작한다', () {
      final scheduleInfo = DailyScheduleInfo(
        date: '2020년 08월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: testProgress,
        stores: testStores,
      );

      final string = scheduleInfo.toString();

      expect(string, contains('DailyScheduleInfo'));
      expect(string, contains('최금주'));
      expect(string, contains('20030117'));
    });
  });
}
