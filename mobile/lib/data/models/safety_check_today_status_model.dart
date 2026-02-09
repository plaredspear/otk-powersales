import '../../domain/entities/safety_check_today_status.dart';

/// 오늘 안전점검 상태 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 SafetyCheckTodayStatus 엔티티로 변환합니다.
class SafetyCheckTodayStatusModel {
  final bool completed;
  final DateTime? submittedAt;

  const SafetyCheckTodayStatusModel({
    required this.completed,
    this.submittedAt,
  });

  /// snake_case JSON에서 파싱
  factory SafetyCheckTodayStatusModel.fromJson(Map<String, dynamic> json) {
    return SafetyCheckTodayStatusModel(
      completed: json['completed'] as bool,
      submittedAt: json['submitted_at'] != null
          ? DateTime.parse(json['submitted_at'] as String)
          : null,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'completed': completed,
      'submitted_at': submittedAt?.toIso8601String(),
    };
  }

  /// Domain Entity로 변환
  SafetyCheckTodayStatus toEntity() {
    return SafetyCheckTodayStatus(
      completed: completed,
      submittedAt: submittedAt,
    );
  }

  /// Domain Entity에서 생성
  factory SafetyCheckTodayStatusModel.fromEntity(
      SafetyCheckTodayStatus entity) {
    return SafetyCheckTodayStatusModel(
      completed: entity.completed,
      submittedAt: entity.submittedAt,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckTodayStatusModel &&
        other.completed == completed &&
        other.submittedAt == submittedAt;
  }

  @override
  int get hashCode {
    return Object.hash(completed, submittedAt);
  }

  @override
  String toString() {
    return 'SafetyCheckTodayStatusModel(completed: $completed, submittedAt: $submittedAt)';
  }
}
