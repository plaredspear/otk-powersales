import '../../domain/entities/attendance_summary.dart';

/// AttendanceSummary API 모델 (DTO)
///
/// API 응답의 snake_case JSON을 Domain Entity로 변환한다.
class AttendanceSummaryModel {
  final int totalCount;
  final int registeredCount;

  const AttendanceSummaryModel({
    required this.totalCount,
    required this.registeredCount,
  });

  factory AttendanceSummaryModel.fromJson(Map<String, dynamic> json) {
    return AttendanceSummaryModel(
      totalCount: json['total_count'] as int,
      registeredCount: json['registered_count'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'total_count': totalCount,
      'registered_count': registeredCount,
    };
  }

  AttendanceSummary toEntity() {
    return AttendanceSummary(
      totalCount: totalCount,
      registeredCount: registeredCount,
    );
  }

  factory AttendanceSummaryModel.fromEntity(AttendanceSummary entity) {
    return AttendanceSummaryModel(
      totalCount: entity.totalCount,
      registeredCount: entity.registeredCount,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AttendanceSummaryModel &&
        other.totalCount == totalCount &&
        other.registeredCount == registeredCount;
  }

  @override
  int get hashCode {
    return Object.hash(totalCount, registeredCount);
  }

  @override
  String toString() {
    return 'AttendanceSummaryModel(totalCount: $totalCount, registeredCount: $registeredCount)';
  }
}
