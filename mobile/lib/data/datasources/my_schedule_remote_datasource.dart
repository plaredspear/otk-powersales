import '../models/daily_schedule_info_model.dart';
import '../models/monthly_schedule_day_model.dart';

/// 마이페이지 일정 원격 데이터소스 인터페이스
///
/// API 서버와의 일정 관련 통신을 추상화합니다.
abstract class MyScheduleRemoteDataSource {
  /// 월간 일정 조회 API 호출
  ///
  /// GET /api/v1/mypage/schedule/monthly?year={year}&month={month}
  ///
  /// [year]: 조회할 연도 (예: 2026)
  /// [month]: 조회할 월 (1-12)
  ///
  /// Returns: 월간 근무일 목록
  Future<List<MonthlyScheduleDayModel>> getMonthlySchedule(
    int year,
    int month,
  );

  /// 일간 일정 상세 조회 API 호출
  ///
  /// GET /api/v1/mypage/schedule/daily?date={date}
  ///
  /// [date]: 조회할 날짜 (ISO 8601 형식: YYYY-MM-DD)
  ///
  /// Returns: 일간 일정 상세 정보
  Future<DailyScheduleInfoModel> getDailySchedule(String date);
}
