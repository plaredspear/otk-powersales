import '../entities/daily_schedule_info.dart';
import '../repositories/my_schedule_repository.dart';

/// 일간 일정 상세 조회 UseCase
///
/// 특정 날짜의 상세 일정을 조회합니다.
class GetDailySchedule {
  final MyScheduleRepository repository;

  const GetDailySchedule(this.repository);

  /// 일간 일정 상세 조회 실행
  ///
  /// [date]: 조회할 날짜
  ///
  /// Returns: 일간 일정 상세 정보 (조원 정보, 보고 진행, 거래처 목록)
  ///
  /// Throws: Repository에서 발생한 예외를 그대로 전파
  Future<DailyScheduleInfo> call(DateTime date) async {
    return await repository.getDailySchedule(date);
  }
}
