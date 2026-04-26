import '../entities/attendance_result.dart';
import '../repositories/attendance_repository.dart';

/// 출근등록 UseCase
///
/// 선택한 거래처에 출근을 등록합니다.
/// scheduleId 또는 displayWorkScheduleId 중 하나를 전달합니다.
class RegisterAttendance {
  final AttendanceRepository _repository;

  RegisterAttendance(this._repository);

  /// 출근등록 실행
  ///
  /// [scheduleId]: 기존 TeamMemberSchedule ID (source=schedule일 때)
  /// [displayWorkScheduleId]: 진열마스터 ID (source=master일 때)
  /// [latitude]: GPS 위도
  /// [longitude]: GPS 경도
  Future<AttendanceResult> call({
    required int scheduleId,
    int? displayWorkScheduleId,
    required double latitude,
    required double longitude,
  }) async {
    // displayWorkScheduleId가 있으면 진열마스터 기반 등록
    if (displayWorkScheduleId != null && displayWorkScheduleId > 0) {
      return await _repository.registerAttendance(
        scheduleId: 0,
        displayWorkScheduleId: displayWorkScheduleId,
        latitude: latitude,
        longitude: longitude,
      );
    }

    // 기존 schedule_id 기반 등록
    if (scheduleId <= 0) {
      throw ArgumentError('유효하지 않은 거래처입니다');
    }

    return await _repository.registerAttendance(
      scheduleId: scheduleId,
      latitude: latitude,
      longitude: longitude,
    );
  }
}
