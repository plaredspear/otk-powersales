import '../entities/auth_token.dart';

/// 비밀번호 관리 Repository 인터페이스 (자발 변경 흐름 — Spec #584).
abstract class PasswordRepository {
  /// 현재 비밀번호 검증 (자발 변경 1단계).
  ///
  /// Returns: 비밀번호가 일치하면 true, 불일치하면 false
  Future<bool> verifyCurrentPassword(String currentPassword);

  /// 비밀번호 변경 (자발 변경 2단계).
  ///
  /// 응답에 새 토큰 페어 포함.
  Future<AuthToken> changePassword({
    required String currentPassword,
    required String newPassword,
  });
}
