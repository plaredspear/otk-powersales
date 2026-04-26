import '../entities/claim_detail.dart';
import '../entities/claim_form.dart';
import '../entities/claim_form_data.dart';
import '../entities/claim_list_item.dart';
import '../entities/claim_result.dart';

/// 클레임 Repository 인터페이스
abstract class ClaimRepository {
  /// 클레임 등록
  Future<ClaimRegisterResult> registerClaim(ClaimRegisterForm form);

  /// 폼 초기화 데이터 조회
  Future<ClaimFormData> getFormData();

  /// 클레임 목록 조회
  Future<List<ClaimListItem>> getClaims({
    String? startDate,
    String? endDate,
  });

  /// 클레임 상세 조회
  Future<ClaimDetail> getClaimDetail(int claimId);
}
