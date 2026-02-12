import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_schedule_info.dart';
import 'package:mobile/domain/entities/monthly_schedule_day.dart';
import 'package:mobile/domain/repositories/my_schedule_repository.dart';
import 'package:mobile/domain/usecases/get_monthly_schedule.dart';

/// 테스트용 Mock MyScheduleRepository
class MockMyScheduleRepository implements MyScheduleRepository {
  List<MonthlyScheduleDay>? monthlySchedule;
  Exception? exceptionToThrow;
  int callCount = 0;
  int? lastYear;
  int? lastMonth;

  @override
  Future<List<MonthlyScheduleDay>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    callCount++;
    lastYear = year;
    lastMonth = month;

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return monthlySchedule!;
  }

  @override
  Future<DailyScheduleInfo> getDailySchedule(DateTime date) async {
    throw UnimplementedError();
  }
}

void main() {
  group('GetMonthlySchedule', () {
    late GetMonthlySchedule useCase;
    late MockMyScheduleRepository mockRepository;

    final testSchedule = [
      MonthlyScheduleDay(
        date: DateTime(2026, 2, 1),
        hasWork: false,
      ),
      MonthlyScheduleDay(
        date: DateTime(2026, 2, 3),
        hasWork: true,
      ),
      MonthlyScheduleDay(
        date: DateTime(2026, 2, 5),
        hasWork: true,
      ),
    ];

    setUp(() {
      mockRepository = MockMyScheduleRepository();
      useCase = GetMonthlySchedule(mockRepository);
    });

    test('월간 일정을 올바르게 조회한다', () async {
      // Arrange
      mockRepository.monthlySchedule = testSchedule;

      // Act
      final result = await useCase(2026, 2);

      // Assert
      expect(result, testSchedule);
      expect(mockRepository.callCount, 1);
      expect(mockRepository.lastYear, 2026);
      expect(mockRepository.lastMonth, 2);
    });

    test('Repository에 올바른 파라미터를 전달한다', () async {
      // Arrange
      mockRepository.monthlySchedule = testSchedule;

      // Act
      await useCase(2025, 12);

      // Assert
      expect(mockRepository.lastYear, 2025);
      expect(mockRepository.lastMonth, 12);
    });

    test('잘못된 월 값(0)에 대해 ArgumentError를 던진다', () async {
      // Act & Assert
      expect(
        () => useCase(2026, 0),
        throwsA(isA<ArgumentError>()),
      );
      expect(mockRepository.callCount, 0); // Repository가 호출되지 않음
    });

    test('잘못된 월 값(13)에 대해 ArgumentError를 던진다', () async {
      // Act & Assert
      expect(
        () => useCase(2026, 13),
        throwsA(isA<ArgumentError>()),
      );
      expect(mockRepository.callCount, 0); // Repository가 호출되지 않음
    });

    test('음수 월 값에 대해 ArgumentError를 던진다', () async {
      // Act & Assert
      expect(
        () => useCase(2026, -1),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('유효한 월 범위 경계값(1)을 처리한다', () async {
      // Arrange
      mockRepository.monthlySchedule = testSchedule;

      // Act
      final result = await useCase(2026, 1);

      // Assert
      expect(result, testSchedule);
      expect(mockRepository.lastMonth, 1);
    });

    test('유효한 월 범위 경계값(12)을 처리한다', () async {
      // Arrange
      mockRepository.monthlySchedule = testSchedule;

      // Act
      final result = await useCase(2026, 12);

      // Assert
      expect(result, testSchedule);
      expect(mockRepository.lastMonth, 12);
    });

    test('빈 일정 목록을 처리한다', () async {
      // Arrange
      mockRepository.monthlySchedule = [];

      // Act
      final result = await useCase(2026, 2);

      // Assert
      expect(result, isEmpty);
    });

    test('Repository에서 예외가 발생하면 예외를 전파한다', () async {
      // Arrange
      mockRepository.exceptionToThrow = Exception('Network error');

      // Act & Assert
      expect(
        () => useCase(2026, 2),
        throwsException,
      );
    });

    test('여러 번 호출 시 각 호출이 올바르게 처리된다', () async {
      // Arrange
      mockRepository.monthlySchedule = testSchedule;

      // Act
      await useCase(2026, 1);
      await useCase(2026, 2);
      await useCase(2026, 3);

      // Assert
      expect(mockRepository.callCount, 3);
      expect(mockRepository.lastMonth, 3); // 마지막 호출 월
    });
  });
}
