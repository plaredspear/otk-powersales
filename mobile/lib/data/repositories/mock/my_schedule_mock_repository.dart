import '../../../domain/entities/daily_schedule_info.dart';
import '../../../domain/entities/monthly_schedule_day.dart';
import '../../../domain/entities/schedule_store_detail.dart';
import '../../../domain/repositories/my_schedule_repository.dart';

/// 마이페이지 일정 Mock Repository
///
/// Backend API가 준비되기 전까지 Mock 데이터로 동작하는 Repository.
class MyScheduleMockRepository implements MyScheduleRepository {
  /// Mock 데이터 커스텀 (테스트용)
  List<MonthlyScheduleDay>? customMonthlySchedule;
  DailyScheduleInfo? customDailySchedule;
  Exception? exceptionToThrow;

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 500));
  }

  @override
  Future<List<MonthlyScheduleDay>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    // 커스텀 데이터가 있으면 사용
    if (customMonthlySchedule != null) {
      return customMonthlySchedule!;
    }

    // 하드코딩된 Mock 데이터 생성
    return _generateMonthlySchedule(year, month);
  }

  @override
  Future<DailyScheduleInfo> getDailySchedule(DateTime date) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    // 커스텀 데이터가 있으면 사용
    if (customDailySchedule != null) {
      return customDailySchedule!;
    }

    // 하드코딩된 Mock 데이터 생성
    return _generateDailySchedule(date);
  }

  /// 월간 일정 Mock 데이터 생성
  ///
  /// 월요일, 수요일, 금요일에 근무가 있는 것으로 설정
  List<MonthlyScheduleDay> _generateMonthlySchedule(int year, int month) {
    final daysInMonth = DateTime(year, month + 1, 0).day;
    final schedule = <MonthlyScheduleDay>[];

    for (int day = 1; day <= daysInMonth; day++) {
      final date = DateTime(year, month, day);
      final weekday = date.weekday;

      // 월요일(1), 수요일(3), 금요일(5)에 근무
      final hasWork = weekday == 1 || weekday == 3 || weekday == 5;

      schedule.add(MonthlyScheduleDay(
        date: date,
        hasWork: hasWork,
      ));
    }

    return schedule;
  }

  /// 일간 일정 Mock 데이터 생성
  DailyScheduleInfo _generateDailySchedule(DateTime date) {
    final weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final dayOfWeek = weekdays[date.weekday - 1];

    final formattedDate =
        '${date.year}년 ${date.month.toString().padLeft(2, '0')}월 ${date.day.toString().padLeft(2, '0')}일($dayOfWeek)';

    // Mock 거래처 데이터
    final stores = [
      const ScheduleStoreDetail(
        storeId: 1,
        storeName: '(주)이마트트레이더스명지점',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      ),
      const ScheduleStoreDetail(
        storeId: 2,
        storeName: '롯데마트 사상',
        workType1: '진열',
        workType2: '전담',
        workType3: '격고',
        isRegistered: false,
      ),
      const ScheduleStoreDetail(
        storeId: 3,
        storeName: '미광종합물류',
        workType1: '진열',
        workType2: '전담',
        workType3: '고정',
        isRegistered: true,
      ),
    ];

    return DailyScheduleInfo(
      date: formattedDate,
      memberName: '최금주',
      employeeNumber: '20030117',
      reportProgress: const ReportProgress(
        completed: 1,
        total: 3,
        workType: '진열',
      ),
      stores: stores,
    );
  }
}
