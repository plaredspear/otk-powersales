import '../../domain/entities/attendance_status.dart';

/// 출근등록 현황 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 AttendanceStatus 엔티티로 변환합니다.
class AttendanceStatusModel {
  final int scheduleId;
  final String storeName;
  final String workCategory;
  final String status;
  final String? workType;

  const AttendanceStatusModel({
    required this.scheduleId,
    required this.storeName,
    required this.workCategory,
    required this.status,
    this.workType,
  });

  /// snake_case JSON에서 파싱
  factory AttendanceStatusModel.fromJson(Map<String, dynamic> json) {
    return AttendanceStatusModel(
      scheduleId: json['schedule_id'] as int,
      storeName: json['store_name'] as String,
      workCategory: json['work_category'] as String,
      status: json['status'] as String,
      workType: json['work_type'] as String?,
    );
  }

  /// Domain Entity로 변환
  AttendanceStatus toEntity() {
    return AttendanceStatus(
      scheduleId: scheduleId,
      storeName: storeName,
      workCategory: workCategory,
      status: status,
      workType: workType,
    );
  }
}
