/// 비밀번호 관리 Repository 인터페이스
///
/// 비밀번호 검증 및 변경 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class PasswordRepository {
  /// 현재 비밀번호 검증
  ///
  /// 마이페이지에서 비밀번호 변경 전 현재 비밀번호를 확인합니다.
  ///
  /// [currentPassword]: 확인할 현재 비밀번호
  ///
  /// Returns: 비밀번호가 일치하면 true, 불일치하면 false
  ///
  /// Throws: Exception - 네트워크 오류, 인증 오류 등
  Future<bool> verifyCurrentPassword(String currentPassword);

  /// 비밀번호 변경
  ///
  /// [currentPassword]: 현재 비밀번호
  /// [newPassword]: 새 비밀번호
  ///
  /// Throws: Exception - 현재 비밀번호 불일치, 약한 비밀번호, 네트워크 오류 등
  Future<void> changePassword(String currentPassword, String newPassword);
}
