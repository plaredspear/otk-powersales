import '../repositories/auth_repository.dart';

/// 로그아웃 UseCase
///
/// 서버에 로그아웃을 알리고 로컬 토큰을 삭제합니다.
class LogoutUseCase {
  final AuthRepository _repository;

  LogoutUseCase(this._repository);

  /// 로그아웃 실행
  ///
  /// 서버에 로그아웃을 요청하고 로컬에 저장된 인증 정보를 삭제합니다.
  Future<void> call() async {
    await _repository.logout();
  }
}
