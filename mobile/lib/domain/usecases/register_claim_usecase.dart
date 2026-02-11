import '../entities/claim_form.dart';
import '../entities/claim_result.dart';
import '../repositories/claim_repository.dart';

/// 클레임 등록 UseCase
///
/// 클레임 등록 폼을 검증하고 Repository를 통해 등록합니다.
class RegisterClaimUseCase {
  const RegisterClaimUseCase(this._repository);

  final ClaimRepository _repository;

  /// 클레임 등록 실행
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 클레임 결과
  /// Throws: [ClaimValidationException] 폼이 유효하지 않을 때
  Future<ClaimRegisterResult> call(ClaimRegisterForm form) async {
    // 폼 유효성 검증
    final errors = form.validate();
    if (errors.isNotEmpty) {
      throw ClaimValidationException(errors);
    }

    // Repository를 통해 등록
    return await _repository.registerClaim(form);
  }
}

/// 클레임 유효성 검증 예외
class ClaimValidationException implements Exception {
  const ClaimValidationException(this.errors);

  final List<String> errors;

  String get message => errors.join(', ');

  @override
  String toString() => 'ClaimValidationException: $message';
}
