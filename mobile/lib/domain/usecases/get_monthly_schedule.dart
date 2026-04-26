import '../entities/monthly_schedule_day.dart';
import '../repositories/my_schedule_repository.dart';

/// 월간 일정 조회 UseCase
///
/// 지정된 연월의 근무일 목록을 조회합니다.
class GetMonthlySchedule {
  final MyScheduleRepository repository;

  const GetMonthlySchedule(this.repository);

  /// 월간 일정 조회 실행
  ///
  /// [year]: 조회할 연도 (예: 2026)
  /// [month]: 조회할 월 (1-12)
  ///
  /// Returns: 해당 월의 날짜별 근무 여부 목록
  ///
  /// Throws: Repository에서 발생한 예외를 그대로 전파
  Future<List<MonthlyScheduleDay>> call(int year, int month) async {
    if (month < 1 || month > 12) {
      throw ArgumentError('month must be between 1 and 12');
    }

    return await repository.getMonthlySchedule(year, month);
  }
}
