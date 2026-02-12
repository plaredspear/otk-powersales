import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_schedule_info.dart';
import 'package:mobile/domain/entities/monthly_schedule_day.dart';
import 'package:mobile/domain/entities/schedule_store_detail.dart';
import 'package:mobile/domain/repositories/my_schedule_repository.dart';
import 'package:mobile/domain/usecases/get_daily_schedule.dart';

/// 테스트용 Mock MyScheduleRepository
class MockMyScheduleRepository implements MyScheduleRepository {
  DailyScheduleInfo? dailySchedule;
  Exception? exceptionToThrow;
  int callCount = 0;
  DateTime? lastDate;

  @override
  Future<DailyScheduleInfo> getDailySchedule(DateTime date) async {
    callCount++;
    lastDate = date;

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return dailySchedule!;
  }

  @override
  Future<List<MonthlyScheduleDay>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    throw UnimplementedError();
  }
}

void main() {
  group('GetDailySchedule', () {
    late GetDailySchedule useCase;
    late MockMyScheduleRepository mockRepository;

    final testDate = DateTime(2026, 2, 4);
    final testScheduleInfo = DailyScheduleInfo(
      date: '2026년 02월 04일(화)',
      memberName: '최금주',
      employeeNumber: '20030117',
      reportProgress: ReportProgress(
        completed: 0,
        total: 3,
        workType: '진열',
      ),
      stores: [
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
      ],
    );

    setUp(() {
      mockRepository = MockMyScheduleRepository();
      useCase = GetDailySchedule(mockRepository);
    });

    test('일간 일정을 올바르게 조회한다', () async {
      // Arrange
      mockRepository.dailySchedule = testScheduleInfo;

      // Act
      final result = await useCase(testDate);

      // Assert
      expect(result, testScheduleInfo);
      expect(mockRepository.callCount, 1);
      expect(mockRepository.lastDate, testDate);
    });

    test('Repository에 올바른 날짜를 전달한다', () async {
      // Arrange
      mockRepository.dailySchedule = testScheduleInfo;
      final specificDate = DateTime(2025, 12, 25);

      // Act
      await useCase(specificDate);

      // Assert
      expect(mockRepository.lastDate, specificDate);
    });

    test('다른 날짜로 여러 번 호출할 수 있다', () async {
      // Arrange
      mockRepository.dailySchedule = testScheduleInfo;
      final date1 = DateTime(2026, 2, 1);
      final date2 = DateTime(2026, 2, 2);
      final date3 = DateTime(2026, 2, 3);

      // Act
      await useCase(date1);
      await useCase(date2);
      await useCase(date3);

      // Assert
      expect(mockRepository.callCount, 3);
      expect(mockRepository.lastDate, date3); // 마지막 호출 날짜
    });

    test('Repository에서 예외가 발생하면 예외를 전파한다', () async {
      // Arrange
      mockRepository.exceptionToThrow = Exception('Network error');

      // Act & Assert
      expect(
        () => useCase(testDate),
        throwsException,
      );
    });

    test('오늘 날짜를 조회할 수 있다', () async {
      // Arrange
      mockRepository.dailySchedule = testScheduleInfo;
      final today = DateTime.now();

      // Act
      final result = await useCase(today);

      // Assert
      expect(result, testScheduleInfo);
      expect(mockRepository.lastDate!.year, today.year);
      expect(mockRepository.lastDate!.month, today.month);
      expect(mockRepository.lastDate!.day, today.day);
    });

    test('과거 날짜를 조회할 수 있다', () async {
      // Arrange
      mockRepository.dailySchedule = testScheduleInfo;
      final pastDate = DateTime(2020, 1, 1);

      // Act
      final result = await useCase(pastDate);

      // Assert
      expect(result, testScheduleInfo);
      expect(mockRepository.lastDate, pastDate);
    });

    test('미래 날짜를 조회할 수 있다', () async {
      // Arrange
      mockRepository.dailySchedule = testScheduleInfo;
      final futureDate = DateTime(2030, 12, 31);

      // Act
      final result = await useCase(futureDate);

      // Assert
      expect(result, testScheduleInfo);
      expect(mockRepository.lastDate, futureDate);
    });

    test('빈 거래처 목록을 가진 일정을 처리한다', () async {
      // Arrange
      final emptyStoreSchedule = DailyScheduleInfo(
        date: '2026년 02월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: ReportProgress(
          completed: 0,
          total: 0,
          workType: '진열',
        ),
        stores: [],
      );
      mockRepository.dailySchedule = emptyStoreSchedule;

      // Act
      final result = await useCase(testDate);

      // Assert
      expect(result.stores, isEmpty);
    });
  });
}
