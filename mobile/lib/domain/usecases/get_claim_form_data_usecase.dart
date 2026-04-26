import '../entities/claim_form_data.dart';
import '../repositories/claim_repository.dart';

/// 클레임 폼 초기화 데이터 조회 UseCase
///
/// 클레임 등록 폼에 필요한 모든 초기화 데이터를 조회합니다.
/// (클레임 종류, 구매 방법, 요청사항 목록)
class GetClaimFormDataUseCase {
  const GetClaimFormDataUseCase(this._repository);

  final ClaimRepository _repository;

  /// 폼 초기화 데이터 조회 실행
  ///
  /// Returns: 폼 초기화에 필요한 모든 데이터
  Future<ClaimFormData> call() async {
    return await _repository.getFormData();
  }
}
