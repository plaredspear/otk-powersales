/// 비밀번호 유효성 검증 결과 엔티티.
///
/// 비밀번호가 정책 규칙을 충족하는지 검증한 결과를 담습니다. 백엔드 권위는
/// `PasswordPolicyValidator` 이며, 모바일은 실시간 체크리스트 UX 용도로 동일 규칙을 표현합니다.
///
/// - [isLengthValid]: 8자 이상
/// - [hasEnoughCharacterTypes]: 영문 대문자 / 영문 소문자 / 숫자 / 특수문자 중 3종 이상 조합
class PasswordValidation {
  /// 8자 이상 충족 여부
  final bool isLengthValid;

  /// 문자 종류 3종 이상 조합 충족 여부
  final bool hasEnoughCharacterTypes;

  /// 전체 유효성 (모든 규칙 충족 시 true)
  final bool isValid;

  const PasswordValidation({
    required this.isLengthValid,
    required this.hasEnoughCharacterTypes,
  }) : isValid = isLengthValid && hasEnoughCharacterTypes;

  /// 비밀번호 최소 길이.
  static const int minLength = 8;

  /// 최소 문자 종류 조합 수.
  static const int minCharacterTypes = 3;

  /// 특수문자 = 유니코드 letter / number / 공백이 아닌 문자.
  /// 백엔드 `PasswordPolicyValidator` (`!isLetterOrDigit() && !isWhitespace()`) 와 동일 판정 —
  /// 한글 등 letter 는 어느 카테고리에도 넣지 않으므로 특수문자로 세지 않는다.
  static final RegExp _specialCharPattern = RegExp(r'[^\p{L}\p{N}\s]', unicode: true);

  /// 영문 대문자 / 소문자 / 숫자 / 특수문자 중 몇 종을 포함하는지 계산.
  static int countCharacterTypes(String password) {
    var types = 0;
    if (password.contains(RegExp(r'[A-Z]'))) types++;
    if (password.contains(RegExp(r'[a-z]'))) types++;
    if (password.contains(RegExp(r'[0-9]'))) types++;
    if (password.contains(_specialCharPattern)) types++;
    return types;
  }

  /// 비밀번호 문자열로부터 유효성 검증 객체 생성
  factory PasswordValidation.fromPassword(String password) {
    return PasswordValidation(
      isLengthValid: password.length >= minLength,
      hasEnoughCharacterTypes: countCharacterTypes(password) >= minCharacterTypes,
    );
  }

  PasswordValidation copyWith({
    bool? isLengthValid,
    bool? hasEnoughCharacterTypes,
  }) {
    return PasswordValidation(
      isLengthValid: isLengthValid ?? this.isLengthValid,
      hasEnoughCharacterTypes:
          hasEnoughCharacterTypes ?? this.hasEnoughCharacterTypes,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is PasswordValidation &&
        other.isLengthValid == isLengthValid &&
        other.hasEnoughCharacterTypes == hasEnoughCharacterTypes;
  }

  @override
  int get hashCode => isLengthValid.hashCode ^ hasEnoughCharacterTypes.hashCode;

  @override
  String toString() =>
      'PasswordValidation(isLengthValid: $isLengthValid, hasEnoughCharacterTypes: $hasEnoughCharacterTypes, isValid: $isValid)';
}
