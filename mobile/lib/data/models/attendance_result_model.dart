import '../../domain/entities/attendance_result.dart';

/// 출근등록 결과 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 AttendanceResult 엔티티로 변환합니다.
class AttendanceResultModel {
  final String scheduleSfid;
  final String storeName;
  final String workType;
  final double distanceKm;
  final int totalCount;
  final int registeredCount;

  const AttendanceResultModel({
    required this.scheduleSfid,
    required this.storeName,
    required this.workType,
    required this.distanceKm,
    required this.totalCount,
    required this.registeredCount,
  });

  /// snake_case JSON에서 파싱
  factory AttendanceResultModel.fromJson(Map<String, dynamic> json) {
    return AttendanceResultModel(
      scheduleSfid: json['schedule_sfid'] as String,
      storeName: json['store_name'] as String,
      workType: json['work_type'] as String,
      distanceKm: (json['distance_km'] as num).toDouble(),
      totalCount: json['total_count'] as int,
      registeredCount: json['registered_count'] as int,
    );
  }

  /// Domain Entity로 변환
  AttendanceResult toEntity() {
    return AttendanceResult(
      scheduleSfid: scheduleSfid,
      storeName: storeName,
      workType: workType,
      distanceKm: distanceKm,
      totalCount: totalCount,
      registeredCount: registeredCount,
    );
  }
}
