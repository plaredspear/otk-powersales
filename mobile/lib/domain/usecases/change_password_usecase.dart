import '../entities/auth_token.dart';
import '../entities/password_validation.dart';
import '../repositories/auth_repository.dart';

/// 비밀번호 변경 UseCase (강제/자발 통합 — Spec #584).
///
/// 강제 변경 시 [currentPassword] 는 null/empty 로 호출 가능. 자발 변경 시는 필수.
/// 정책 검증은 [PasswordValidation] 으로 사전 차단 — 백엔드는 동일 정책을 권위로 재검증한다.
class ChangePasswordUseCase {
  final AuthRepository _repository;

  ChangePasswordUseCase(this._repository);

  Future<AuthToken> call({
    String? currentPassword,
    required String newPassword,
  }) async {
    final validation = PasswordValidation.fromPassword(newPassword);
    if (!validation.isValid) {
      if (!validation.isLengthValid) {
        throw ArgumentError('비밀번호는 4자 이상 32자 이하여야 합니다');
      }
      if (!validation.isNotRepeating) {
        throw ArgumentError('같은 문자를 4번 연속 사용할 수 없습니다');
      }
      if (!validation.isNotTemporary) {
        throw ArgumentError('임시 비밀번호와 동일한 비밀번호는 사용할 수 없습니다');
      }
    }

    return await _repository.changePassword(
      currentPassword: currentPassword,
      newPassword: newPassword,
    );
  }
}
