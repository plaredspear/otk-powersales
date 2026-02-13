import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/my_schedule_mock_repository.dart';
import 'package:mobile/domain/entities/daily_schedule_info.dart';
import 'package:mobile/domain/entities/monthly_schedule_day.dart';
import 'package:mobile/domain/entities/schedule_store_detail.dart';

void main() {
  group('MyScheduleMockRepository', () {
    late MyScheduleMockRepository repository;

    setUp(() {
      repository = MyScheduleMockRepository();
    });

    group('getMonthlySchedule', () {
      test('월간 일정 데이터를 반환한다', () async {
        // Act
        final result = await repository.getMonthlySchedule(2026, 2);

        // Assert
        expect(result, isNotEmpty);
        expect(result.first, isA<MonthlyScheduleDay>());
        // 2월은 28일 또는 29일
        expect(result.length, greaterThanOrEqualTo(28));
      });

      test('월요일, 수요일, 금요일에 근무가 있다', () async {
        // Act
        final result = await repository.getMonthlySchedule(2026, 2);

        // Assert
        // 일부 월요일 찾기 (2026년 2월 2일은 월요일)
        final monday = result.firstWhere((day) => day.date.day == 2);
        expect(monday.hasWork, true);

        // 수요일 (2026년 2월 4일은 수요일)
        final wednesday = result.firstWhere((day) => day.date.day == 4);
        expect(wednesday.hasWork, true);

        // 금요일 (2026년 2월 6일은 금요일)
        final friday = result.firstWhere((day) => day.date.day == 6);
        expect(friday.hasWork, true);
      });

      test('화요일, 목요일에는 근무가 없다', () async {
        // Act
        final result = await repository.getMonthlySchedule(2026, 2);

        // Assert
        // 화요일 (2026년 2월 3일은 화요일)
        final tuesday = result.firstWhere((day) => day.date.day == 3);
        expect(tuesday.hasWork, false);

        // 목요일 (2026년 2월 5일은 목요일)
        final thursday = result.firstWhere((day) => day.date.day == 5);
        expect(thursday.hasWork, false);
      });

      test('다른 월의 데이터를 요청할 수 있다', () async {
        // Act
        final result = await repository.getMonthlySchedule(2026, 3);

        // Assert
        expect(result, isNotEmpty);
        expect(result.first.date.year, 2026);
        expect(result.first.date.month, 3);
        // 3월은 31일
        expect(result.length, 31);
      });

      test('커스텀 데이터를 사용할 수 있다', () async {
        // Arrange
        final customSchedule = [
          MonthlyScheduleDay(
            date: DateTime(2026, 2, 1),
            hasWork: true,
          ),
          MonthlyScheduleDay(
            date: DateTime(2026, 2, 2),
            hasWork: false,
          ),
        ];
        repository.customMonthlySchedule = customSchedule;

        // Act
        final result = await repository.getMonthlySchedule(2026, 2);

        // Assert
        expect(result, customSchedule);
        expect(result.length, 2);
      });

      test('예외를 던지도록 설정할 수 있다', () async {
        // Arrange
        repository.exceptionToThrow = Exception('Network error');

        // Act & Assert
        expect(
          () => repository.getMonthlySchedule(2026, 2),
          throwsException,
        );
      });
    });

    group('getDailySchedule', () {
      test('일간 일정 상세 데이터를 반환한다', () async {
        // Arrange
        final date = DateTime(2026, 2, 4);

        // Act
        final result = await repository.getDailySchedule(date);

        // Assert
        expect(result, isA<DailyScheduleInfo>());
        expect(result.date, contains('2026년 02월 04일'));
        expect(result.memberName, isNotEmpty);
        expect(result.employeeNumber, isNotEmpty);
        expect(result.stores, isNotEmpty);
      });

      test('포맷된 날짜 문자열을 생성한다', () async {
        // Arrange
        final date = DateTime(2026, 2, 4); // 수요일

        // Act
        final result = await repository.getDailySchedule(date);

        // Assert
        expect(result.date, '2026년 02월 04일(수)');
      });

      test('거래처 목록을 포함한다', () async {
        // Arrange
        final date = DateTime(2026, 2, 4);

        // Act
        final result = await repository.getDailySchedule(date);

        // Assert
        expect(result.stores, isNotEmpty);
        expect(result.stores.first, isA<ScheduleStoreDetail>());
        expect(result.stores.first.storeName, isNotEmpty);
      });

      test('보고 진행 상황을 포함한다', () async {
        // Arrange
        final date = DateTime(2026, 2, 4);

        // Act
        final result = await repository.getDailySchedule(date);

        // Assert
        expect(result.reportProgress.total, greaterThan(0));
        expect(result.reportProgress.workType, isNotEmpty);
      });

      test('커스텀 데이터를 사용할 수 있다', () async {
        // Arrange
        final customSchedule = DailyScheduleInfo(
          date: '2026년 02월 04일(수)',
          memberName: '홍길동',
          employeeNumber: '12345678',
          reportProgress: const ReportProgress(
            completed: 2,
            total: 2,
            workType: '순회',
          ),
          stores: const [],
        );
        repository.customDailySchedule = customSchedule;

        // Act
        final result = await repository.getDailySchedule(DateTime(2026, 2, 4));

        // Assert
        expect(result, customSchedule);
        expect(result.memberName, '홍길동');
      });

      test('예외를 던지도록 설정할 수 있다', () async {
        // Arrange
        repository.exceptionToThrow = Exception('Network error');

        // Act & Assert
        expect(
          () => repository.getDailySchedule(DateTime(2026, 2, 4)),
          throwsException,
        );
      });
    });
  });
}
