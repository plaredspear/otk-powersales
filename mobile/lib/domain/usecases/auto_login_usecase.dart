import '../entities/auth_token.dart';
import '../repositories/auth_repository.dart';

/// 자동 로그인 UseCase
///
/// Refresh Token을 사용하여 자동 로그인을 수행합니다.
class AutoLoginUseCase {
  final AuthRepository _repository;

  AutoLoginUseCase(this._repository);

  /// 자동 로그인 실행
  ///
  /// [refreshToken]: Refresh Token (필수)
  ///
  /// Returns: 새로운 인증 토큰
  ///
  /// Throws:
  /// - [ArgumentError] refreshToken이 비어있는 경우
  Future<AuthToken> call({
    required String refreshToken,
  }) async {
    // 입력값 검증
    if (refreshToken.isEmpty) {
      throw ArgumentError('Refresh Token이 필요합니다');
    }

    // Repository에서 토큰 갱신 수행
    return await _repository.refreshToken(refreshToken);
  }
}
