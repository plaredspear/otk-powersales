import '../entities/claim_detail.dart';
import '../repositories/claim_repository.dart';

/// 클레임 상세 조회 유스케이스
class GetClaimDetailUseCase {
  final ClaimRepository _repository;

  GetClaimDetailUseCase(this._repository);

  Future<ClaimDetail> call(int claimId) async {
    return await _repository.getClaimDetail(claimId);
  }
}
