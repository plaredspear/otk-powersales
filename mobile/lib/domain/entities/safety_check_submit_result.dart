/// 안전점검 제출 결과 엔티티 (V1)
class SafetyCheckSubmitResult {
  /// 제출 일시
  final DateTime submittedAt;

  /// 안전점검 완료 여부
  final bool safetyCheckCompleted;

  const SafetyCheckSubmitResult({
    required this.submittedAt,
    required this.safetyCheckCompleted,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckSubmitResult &&
        other.submittedAt == submittedAt &&
        other.safetyCheckCompleted == safetyCheckCompleted;
  }

  @override
  int get hashCode => Object.hash(submittedAt, safetyCheckCompleted);

  @override
  String toString() =>
      'SafetyCheckSubmitResult(submittedAt: $submittedAt, safetyCheckCompleted: $safetyCheckCompleted)';
}
