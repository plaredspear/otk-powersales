import '../repositories/attendance_repository.dart';

/// 출근등록 현황 조회 UseCase
///
/// 오늘의 출근등록 현황 (거래처별 완료/대기 상태)을 조회합니다.
class GetAttendanceStatus {
  final AttendanceRepository _repository;

  GetAttendanceStatus(this._repository);

  /// 출근등록 현황 조회 실행
  Future<AttendanceStatusResult> call() async {
    return await _repository.getAttendanceStatus();
  }
}
