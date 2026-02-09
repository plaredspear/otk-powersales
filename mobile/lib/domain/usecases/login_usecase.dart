import '../repositories/auth_repository.dart';

/// 로그인 UseCase
///
/// 사번과 비밀번호로 로그인을 수행합니다.
/// rememberEmployeeId와 autoLogin 옵션은 Presentation 레이어에서 처리됩니다.
class LoginUseCase {
  final AuthRepository _repository;

  LoginUseCase(this._repository);

  /// 로그인 실행
  ///
  /// [employeeId]: 사번 (8자리 숫자, 필수)
  /// [password]: 비밀번호 (필수)
  /// [rememberEmployeeId]: 사번 저장 여부 (Presentation 레이어에서 처리)
  /// [autoLogin]: 자동 로그인 여부 (Presentation 레이어에서 처리)
  ///
  /// Returns: 로그인 결과
  ///
  /// Throws:
  /// - [ArgumentError] 사번 또는 비밀번호가 비어있는 경우
  Future<LoginResult> call({
    required String employeeId,
    required String password,
    required bool rememberEmployeeId,
    required bool autoLogin,
  }) async {
    // 입력값 검증
    if (employeeId.isEmpty) {
      throw ArgumentError('사번을 입력해주세요');
    }

    if (password.isEmpty) {
      throw ArgumentError('비밀번호를 입력해주세요');
    }

    // Repository에서 로그인 수행
    return await _repository.login(employeeId, password);
  }
}
