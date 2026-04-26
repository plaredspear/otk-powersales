import '../entities/alternative_holiday.dart';

/// 대체휴무 Repository 인터페이스
abstract class AlternativeHolidayRepository {
  /// 대체휴무 신청
  Future<AlternativeHoliday> createAlternativeHoliday({
    required DateTime actualWorkDate,
    required DateTime targetAltHolidayDate,
  });

  /// 대체휴무 이력 조회
  Future<List<AlternativeHoliday>> getAlternativeHolidays({
    DateTime? startDate,
    DateTime? endDate,
  });

  /// 공휴일 목록 조회 (연도별)
  Future<List<DateTime>> getHolidays(int year);
}
