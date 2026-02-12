import '../repositories/password_repository.dart';

/// 현재 비밀번호 검증 UseCase
///
/// 마이페이지에서 비밀번호 변경 전 현재 비밀번호를 확인합니다.
class VerifyCurrentPasswordUseCase {
  final PasswordRepository _repository;

  VerifyCurrentPasswordUseCase(this._repository);

  /// 현재 비밀번호 검증 실행
  ///
  /// [currentPassword]: 확인할 현재 비밀번호
  ///
  /// Returns: 비밀번호가 일치하면 true, 불일치하면 false
  ///
  /// Throws:
  /// - [ArgumentError] 비밀번호가 비어있는 경우
  Future<bool> call(String currentPassword) async {
    // 입력값 검증
    if (currentPassword.isEmpty) {
      throw ArgumentError('비밀번호를 입력해주세요');
    }

    // Repository에서 비밀번호 검증 수행
    return await _repository.verifyCurrentPassword(currentPassword);
  }
}
