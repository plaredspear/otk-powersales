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
        throw ArgumentError('비밀번호는 8자 이상이어야 합니다');
      }
      if (!validation.hasEnoughCharacterTypes) {
        throw ArgumentError('영문 대/소문자·숫자·특수문자 중 3종 이상을 조합해주세요');
      }
    }

    return await _repository.changePassword(
      currentPassword: currentPassword,
      newPassword: newPassword,
    );
  }
}
