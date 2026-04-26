/// 비밀번호 검증 응답 DTO
///
/// Backend API의 POST /api/v1/auth/verify-password 응답을 파싱합니다.
class PasswordVerificationResponse {
  final bool isValid;

  const PasswordVerificationResponse({
    required this.isValid,
  });

  /// snake_case JSON에서 파싱
  factory PasswordVerificationResponse.fromJson(Map<String, dynamic> json) {
    return PasswordVerificationResponse(
      isValid: json['is_valid'] as bool,
    );
  }
}
