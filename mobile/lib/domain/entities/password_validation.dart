/// 비밀번호 유효성 검증 결과 엔티티
///
/// 비밀번호가 정책 규칙을 충족하는지 검증한 결과를 담습니다.
/// - 최소 길이 (4글자 이상)
/// - 동일 문자 반복 불가 (예: 1111, aaaa)
class PasswordValidation {
  /// 4글자 이상 조건 충족 여부
  final bool isLengthValid;

  /// 동일 문자 반복 금지 조건 충족 여부
  final bool isNotRepeating;

  /// 전체 유효성 (모든 규칙 충족 시 true)
  final bool isValid;

  const PasswordValidation({
    required this.isLengthValid,
    required this.isNotRepeating,
  }) : isValid = isLengthValid && isNotRepeating;

  /// 비밀번호 문자열로부터 유효성 검증 객체 생성
  ///
  /// [password] 검증할 비밀번호 문자열
  factory PasswordValidation.fromPassword(String password) {
    final isLengthValid = password.length >= 4;
    final isNotRepeating = password.split('').toSet().length > 1;

    return PasswordValidation(
      isLengthValid: isLengthValid,
      isNotRepeating: isNotRepeating,
    );
  }

  PasswordValidation copyWith({
    bool? isLengthValid,
    bool? isNotRepeating,
  }) {
    return PasswordValidation(
      isLengthValid: isLengthValid ?? this.isLengthValid,
      isNotRepeating: isNotRepeating ?? this.isNotRepeating,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is PasswordValidation &&
        other.isLengthValid == isLengthValid &&
        other.isNotRepeating == isNotRepeating;
  }

  @override
  int get hashCode =>
      isLengthValid.hashCode ^ isNotRepeating.hashCode ^ isValid.hashCode;

  @override
  String toString() =>
      'PasswordValidation(isLengthValid: $isLengthValid, isNotRepeating: $isNotRepeating, isValid: $isValid)';
}
