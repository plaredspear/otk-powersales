/// 비밀번호 변경 요청 DTO (Spec #584).
///
/// Backend `POST /api/v1/mobile/auth/change-password` 요청 본문.
/// 강제 변경 (토큰 클레임 `passwordChangeRequired=true`) 시 [currentPassword] 는 null
/// 로 전달 가능 — 백엔드에서 무시한다.
class ChangePasswordRequest {
  final String? currentPassword;
  final String newPassword;

  /// 자동로그인 선택 여부. 변경 시 새 refresh family 로 재발급되므로, 저장된 선호를
  /// 전달해 ON 세션의 장수명(60일)을 유지한다. 기본 false = 7일 세션.
  final bool autoLogin;

  const ChangePasswordRequest({
    this.currentPassword,
    required this.newPassword,
    this.autoLogin = false,
  });

  /// camelCase JSON 직렬화 (백엔드 Jackson LOWER_CAMEL_CASE 와 정합).
  /// `currentPassword` 가 null/blank 이면 키 자체를 누락한다.
  Map<String, dynamic> toJson() {
    return {
      if (currentPassword != null && currentPassword!.isNotEmpty)
        'currentPassword': currentPassword,
      'newPassword': newPassword,
      'autoLogin': autoLogin,
    };
  }
}
