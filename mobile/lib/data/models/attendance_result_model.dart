import '../../domain/entities/attendance_result.dart';

/// 출근등록 결과 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 AttendanceResult 엔티티로 변환합니다.
class AttendanceResultModel {
  final int scheduleId;
  final String accountName;
  final String? workType;
  final String? secondWorkType;
  final double distanceKm;
  final int totalCount;
  final int registeredCount;

  const AttendanceResultModel({
    required this.scheduleId,
    required this.accountName,
    this.workType,
    this.secondWorkType,
    required this.distanceKm,
    required this.totalCount,
    required this.registeredCount,
  });

  /// snake_case JSON에서 파싱
  ///
  /// `workType`/`secondWorkType` 은 서버에서 nullable 이다. 일정에 근무유형이 없거나
  /// 근무유형4가 비어 있으면 null 이 내려오므로 non-null 캐스팅하지 않는다.
  factory AttendanceResultModel.fromJson(Map<String, dynamic> json) {
    return AttendanceResultModel(
      scheduleId: json['scheduleId'] as int,
      accountName: json['accountName'] as String,
      workType: json['workType'] as String?,
      secondWorkType: json['secondWorkType'] as String?,
      distanceKm: (json['distanceKm'] as num).toDouble(),
      totalCount: json['totalCount'] as int,
      registeredCount: json['registeredCount'] as int,
    );
  }

  /// Domain Entity로 변환
  AttendanceResult toEntity() {
    return AttendanceResult(
      scheduleId: scheduleId,
      accountName: accountName,
      workType: workType,
      secondWorkType: secondWorkType,
      distanceKm: distanceKm,
      totalCount: totalCount,
      registeredCount: registeredCount,
    );
  }
}
