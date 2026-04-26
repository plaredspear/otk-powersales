import '../repositories/auth_repository.dart';

/// 비밀번호 변경 UseCase
///
/// 현재 비밀번호를 확인하고 새 비밀번호로 변경합니다.
class ChangePasswordUseCase {
  final AuthRepository _repository;

  ChangePasswordUseCase(this._repository);

  /// 비밀번호 변경 실행
  ///
  /// [currentPassword]: 현재 비밀번호 (필수)
  /// [newPassword]: 새 비밀번호 (필수, 4글자 이상, 동일 문자 반복 불가)
  ///
  /// Throws:
  /// - [ArgumentError] 입력값이 유효하지 않은 경우
  Future<void> call({
    required String currentPassword,
    required String newPassword,
  }) async {
    // 현재 비밀번호 검증
    if (currentPassword.isEmpty) {
      throw ArgumentError('현재 비밀번호를 입력해주세요');
    }

    // 새 비밀번호 검증 - 길이
    if (newPassword.length < 4) {
      throw ArgumentError('비밀번호는 4글자 이상이어야 합니다');
    }

    // 새 비밀번호 검증 - 동일 문자 반복 불가 (예: '1111', 'aaaa')
    if (newPassword.split('').toSet().length == 1) {
      throw ArgumentError('동일한 문자의 반복은 사용할 수 없습니다');
    }

    // Repository에서 비밀번호 변경 수행
    await _repository.changePassword(currentPassword, newPassword);
  }
}
