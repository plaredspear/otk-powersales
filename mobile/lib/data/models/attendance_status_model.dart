import '../../domain/entities/attendance_status.dart';

/// 출근등록 현황 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 AttendanceStatus 엔티티로 변환합니다.
class AttendanceStatusModel {
  final String scheduleSfid;
  final String storeName;
  final String workCategory;
  final String status;
  final String? workType;

  const AttendanceStatusModel({
    required this.scheduleSfid,
    required this.storeName,
    required this.workCategory,
    required this.status,
    this.workType,
  });

  /// snake_case JSON에서 파싱
  factory AttendanceStatusModel.fromJson(Map<String, dynamic> json) {
    return AttendanceStatusModel(
      scheduleSfid: json['schedule_sfid'] as String,
      storeName: json['store_name'] as String,
      workCategory: json['work_category'] as String,
      status: json['status'] as String,
      workType: json['work_type'] as String?,
    );
  }

  /// Domain Entity로 변환
  AttendanceStatus toEntity() {
    return AttendanceStatus(
      scheduleSfid: scheduleSfid,
      storeName: storeName,
      workCategory: workCategory,
      status: status,
      workType: workType,
    );
  }
}
