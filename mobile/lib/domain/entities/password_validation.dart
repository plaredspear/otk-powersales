/// 비밀번호 유효성 검증 결과 엔티티 (Spec #584).
///
/// 비밀번호가 정책 규칙을 충족하는지 검증한 결과를 담습니다. 백엔드 권위는
/// `PasswordPolicyValidator` 이며, 모바일은 실시간 체크리스트 UX 용도로 동일 규칙을 표현합니다.
///
/// - [isLengthValid]: 4자 이상 32자 이하
/// - [isNotRepeating]: 동일 문자 4회 연속 사용 안함 (한글/특수문자 포함 모든 문자)
/// - [isNotTemporary]: 임시 비밀번호 ("1234") 와 동일하지 않음
class PasswordValidation {
  /// 4자 이상 32자 이하 충족 여부
  final bool isLengthValid;

  /// 동일 문자 4회 연속 차단 충족 여부
  final bool isNotRepeating;

  /// 임시 비밀번호 동일 차단 충족 여부
  final bool isNotTemporary;

  /// 전체 유효성 (모든 규칙 충족 시 true)
  final bool isValid;

  const PasswordValidation({
    required this.isLengthValid,
    required this.isNotRepeating,
    required this.isNotTemporary,
  }) : isValid = isLengthValid && isNotRepeating && isNotTemporary;

  /// 임시 비밀번호 (운영자 리셋 시 부여되는 기본값).
  static const String temporaryPassword = '1234';

  /// 동일 문자 4회 연속 차단 정규식 — 한글/특수문자 포함 모든 문자.
  static final RegExp _repeatedPattern = RegExp(r'(.)\1\1\1');

  /// 비밀번호 문자열로부터 유효성 검증 객체 생성
  factory PasswordValidation.fromPassword(String password) {
    return PasswordValidation(
      isLengthValid: password.length >= 4 && password.length <= 32,
      isNotRepeating: !_repeatedPattern.hasMatch(password),
      isNotTemporary: password != temporaryPassword,
    );
  }

  PasswordValidation copyWith({
    bool? isLengthValid,
    bool? isNotRepeating,
    bool? isNotTemporary,
  }) {
    return PasswordValidation(
      isLengthValid: isLengthValid ?? this.isLengthValid,
      isNotRepeating: isNotRepeating ?? this.isNotRepeating,
      isNotTemporary: isNotTemporary ?? this.isNotTemporary,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is PasswordValidation &&
        other.isLengthValid == isLengthValid &&
        other.isNotRepeating == isNotRepeating &&
        other.isNotTemporary == isNotTemporary;
  }

  @override
  int get hashCode =>
      isLengthValid.hashCode ^ isNotRepeating.hashCode ^ isNotTemporary.hashCode;

  @override
  String toString() =>
      'PasswordValidation(isLengthValid: $isLengthValid, isNotRepeating: $isNotRepeating, isNotTemporary: $isNotTemporary, isValid: $isValid)';
}
