import '../entities/attendance_result.dart';
import '../repositories/attendance_repository.dart';

/// 출근등록 UseCase
///
/// 선택한 거래처에 출근을 등록합니다.
class RegisterAttendance {
  final AttendanceRepository _repository;

  RegisterAttendance(this._repository);

  /// 출근등록 실행
  ///
  /// [scheduleId]: 스케줄 ID
  /// [latitude]: GPS 위도
  /// [longitude]: GPS 경도
  /// [workType]: 근무유형 (optional)
  Future<AttendanceResult> call({
    required int scheduleId,
    required double latitude,
    required double longitude,
    String? workType,
  }) async {
    if (scheduleId <= 0) {
      throw ArgumentError('유효하지 않은 거래처입니다');
    }

    return await _repository.registerAttendance(
      scheduleId: scheduleId,
      latitude: latitude,
      longitude: longitude,
      workType: workType,
    );
  }
}
