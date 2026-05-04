/// 비밀번호 변경 요청 DTO (Spec #584).
///
/// Backend `POST /api/v1/mobile/auth/change-password` 요청 본문.
/// 강제 변경 (토큰 클레임 `passwordChangeRequired=true`) 시 [currentPassword] 는 null
/// 로 전달 가능 — 백엔드에서 무시한다.
class ChangePasswordRequest {
  final String? currentPassword;
  final String newPassword;

  const ChangePasswordRequest({
    this.currentPassword,
    required this.newPassword,
  });

  /// camelCase JSON 직렬화 (백엔드 Jackson LOWER_CAMEL_CASE 와 정합).
  /// `currentPassword` 가 null/blank 이면 키 자체를 누락한다.
  Map<String, dynamic> toJson() {
    return {
      if (currentPassword != null && currentPassword!.isNotEmpty)
        'currentPassword': currentPassword,
      'newPassword': newPassword,
    };
  }
}
