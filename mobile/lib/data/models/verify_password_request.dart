/// 현재 비밀번호 검증 요청 DTO
///
/// Backend API의 POST /api/v1/auth/verify-password 요청에 사용됩니다.
class VerifyPasswordRequest {
  final String currentPassword;

  const VerifyPasswordRequest({
    required this.currentPassword,
  });

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'current_password': currentPassword,
    };
  }
}
