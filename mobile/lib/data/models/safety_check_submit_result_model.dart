import '../../domain/entities/safety_check_submit_result.dart';

/// 안전점검 제출 결과 모델 (V1 JSON 매핑)
class SafetyCheckSubmitResultModel {
  final DateTime submittedAt;
  final bool safetyCheckCompleted;

  const SafetyCheckSubmitResultModel({
    required this.submittedAt,
    required this.safetyCheckCompleted,
  });

  factory SafetyCheckSubmitResultModel.fromJson(Map<String, dynamic> json) {
    return SafetyCheckSubmitResultModel(
      submittedAt: DateTime.parse(json['submitted_at'] as String),
      safetyCheckCompleted: json['safety_check_completed'] as bool,
    );
  }

  SafetyCheckSubmitResult toEntity() {
    return SafetyCheckSubmitResult(
      submittedAt: submittedAt,
      safetyCheckCompleted: safetyCheckCompleted,
    );
  }
}
