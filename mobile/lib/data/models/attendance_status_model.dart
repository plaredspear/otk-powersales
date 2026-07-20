import '../../domain/entities/attendance_status.dart';

/// 출근등록 현황 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 AttendanceStatus 엔티티로 변환합니다.
class AttendanceStatusModel {
  final int scheduleId;
  final String accountName;
  final String workCategory;
  final String status;
  final String? secondWorkType;

  const AttendanceStatusModel({
    required this.scheduleId,
    required this.accountName,
    required this.workCategory,
    required this.status,
    this.secondWorkType,
  });

  /// snake_case JSON에서 파싱
  factory AttendanceStatusModel.fromJson(Map<String, dynamic> json) {
    return AttendanceStatusModel(
      scheduleId: json['scheduleId'] as int,
      accountName: json['accountName'] as String,
      workCategory: json['workCategory'] as String,
      status: json['status'] as String,
      secondWorkType: json['secondWorkType'] as String?,
    );
  }

  /// Domain Entity로 변환
  AttendanceStatus toEntity() {
    return AttendanceStatus(
      scheduleId: scheduleId,
      accountName: accountName,
      workCategory: workCategory,
      status: status,
      secondWorkType: secondWorkType,
    );
  }
}
