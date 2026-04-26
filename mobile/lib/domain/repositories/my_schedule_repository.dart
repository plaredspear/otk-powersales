import '../entities/daily_schedule_info.dart';
import '../entities/monthly_schedule_day.dart';

/// 마이페이지 일정 데이터 접근 Repository 인터페이스
abstract class MyScheduleRepository {
  /// 월간 일정 조회
  ///
  /// [year]와 [month]로 지정된 월의 근무일 목록을 조회합니다.
  ///
  /// Returns: 해당 월의 날짜별 근무 여부 목록
  Future<List<MonthlyScheduleDay>> getMonthlySchedule(int year, int month);

  /// 일간 일정 상세 조회
  ///
  /// [date]로 지정된 날짜의 상세 일정을 조회합니다.
  /// 조원 정보, 보고 진행 상황, 거래처 목록을 포함합니다.
  ///
  /// Returns: 일간 일정 상세 정보
  Future<DailyScheduleInfo> getDailySchedule(DateTime date);
}
