import '../entities/claim_list_item.dart';
import '../repositories/claim_repository.dart';

/// 클레임 목록 조회 유스케이스
class GetClaimsUseCase {
  final ClaimRepository _repository;

  GetClaimsUseCase(this._repository);

  Future<List<ClaimListItem>> call({
    String? startDate,
    String? endDate,
  }) async {
    return await _repository.getClaims(
      startDate: startDate,
      endDate: endDate,
    );
  }
}
