import '../../domain/entities/safety_check_submit_result.dart';

/// 안전점검 제출 결과 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 SafetyCheckSubmitResult 엔티티로 변환합니다.
class SafetyCheckSubmitResultModel {
  final int submissionId;
  final DateTime submittedAt;
  final bool safetyCheckCompleted;

  const SafetyCheckSubmitResultModel({
    required this.submissionId,
    required this.submittedAt,
    required this.safetyCheckCompleted,
  });

  /// snake_case JSON에서 파싱
  factory SafetyCheckSubmitResultModel.fromJson(Map<String, dynamic> json) {
    return SafetyCheckSubmitResultModel(
      submissionId: json['submission_id'] as int,
      submittedAt: DateTime.parse(json['submitted_at'] as String),
      safetyCheckCompleted: json['safety_check_completed'] as bool,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'submission_id': submissionId,
      'submitted_at': submittedAt.toIso8601String(),
      'safety_check_completed': safetyCheckCompleted,
    };
  }

  /// Domain Entity로 변환
  SafetyCheckSubmitResult toEntity() {
    return SafetyCheckSubmitResult(
      submissionId: submissionId,
      submittedAt: submittedAt,
      safetyCheckCompleted: safetyCheckCompleted,
    );
  }

  /// Domain Entity에서 생성
  factory SafetyCheckSubmitResultModel.fromEntity(
      SafetyCheckSubmitResult entity) {
    return SafetyCheckSubmitResultModel(
      submissionId: entity.submissionId,
      submittedAt: entity.submittedAt,
      safetyCheckCompleted: entity.safetyCheckCompleted,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckSubmitResultModel &&
        other.submissionId == submissionId &&
        other.submittedAt == submittedAt &&
        other.safetyCheckCompleted == safetyCheckCompleted;
  }

  @override
  int get hashCode {
    return Object.hash(submissionId, submittedAt, safetyCheckCompleted);
  }

  @override
  String toString() {
    return 'SafetyCheckSubmitResultModel(submissionId: $submissionId, submittedAt: $submittedAt, safetyCheckCompleted: $safetyCheckCompleted)';
  }
}
