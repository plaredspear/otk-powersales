/// 안전점검 제출 결과 엔티티
///
/// 안전점검 제출 성공 시 반환되는 결과 정보를 나타냅니다.
class SafetyCheckSubmitResult {
  /// 제출 기록 ID
  final int submissionId;

  /// 제출 일시
  final DateTime submittedAt;

  /// 안전점검 완료 여부
  final bool safetyCheckCompleted;

  const SafetyCheckSubmitResult({
    required this.submissionId,
    required this.submittedAt,
    required this.safetyCheckCompleted,
  });

  SafetyCheckSubmitResult copyWith({
    int? submissionId,
    DateTime? submittedAt,
    bool? safetyCheckCompleted,
  }) {
    return SafetyCheckSubmitResult(
      submissionId: submissionId ?? this.submissionId,
      submittedAt: submittedAt ?? this.submittedAt,
      safetyCheckCompleted: safetyCheckCompleted ?? this.safetyCheckCompleted,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'submissionId': submissionId,
      'submittedAt': submittedAt.toIso8601String(),
      'safetyCheckCompleted': safetyCheckCompleted,
    };
  }

  factory SafetyCheckSubmitResult.fromJson(Map<String, dynamic> json) {
    return SafetyCheckSubmitResult(
      submissionId: json['submissionId'] as int,
      submittedAt: DateTime.parse(json['submittedAt'] as String),
      safetyCheckCompleted: json['safetyCheckCompleted'] as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckSubmitResult &&
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
    return 'SafetyCheckSubmitResult(submissionId: $submissionId, submittedAt: $submittedAt, safetyCheckCompleted: $safetyCheckCompleted)';
  }
}
