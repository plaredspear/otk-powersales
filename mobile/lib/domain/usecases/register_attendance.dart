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
  /// [storeId]: 거래처 ID
  /// [workType]: 근무유형 ('ROOM_TEMP' 또는 'REFRIGERATED')
  Future<AttendanceResult> call({
    required int storeId,
    required String workType,
  }) async {
    // 입력값 검증
    if (storeId <= 0) {
      throw ArgumentError('유효하지 않은 거래처입니다');
    }

    if (workType != 'ROOM_TEMP' && workType != 'REFRIGERATED') {
      throw ArgumentError('유효하지 않은 근무유형입니다');
    }

    return await _repository.registerAttendance(
      storeId: storeId,
      workType: workType,
    );
  }
}
