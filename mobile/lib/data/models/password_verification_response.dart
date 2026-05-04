/// 비밀번호 검증 응답 DTO
///
/// Backend API의 POST /api/v1/mobile/auth/verify-password 응답을 파싱합니다.
class PasswordVerificationResponse {
  final bool isValid;

  const PasswordVerificationResponse({
    required this.isValid,
  });

  /// snake_case JSON에서 파싱
  factory PasswordVerificationResponse.fromJson(Map<String, dynamic> json) {
    return PasswordVerificationResponse(
      isValid: json['isValid'] as bool,
    );
  }
}
