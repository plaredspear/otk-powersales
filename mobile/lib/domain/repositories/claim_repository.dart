import '../entities/claim_detail.dart';
import '../entities/claim_form.dart';
import '../entities/claim_form_entry.dart';
import '../entities/claim_list_item.dart';
import '../entities/claim_result.dart';

/// 클레임 Repository 인터페이스
abstract class ClaimRepository {
  /// 클레임 등록
  Future<ClaimRegisterResult> registerClaim(ClaimRegisterForm form);

  /// 등록 화면 진입 폼 조회 (메타데이터 + 이어쓰기용 임시저장).
  Future<ClaimFormEntry> getForm();

  /// 클레임 목록 조회
  Future<List<ClaimListItem>> getClaims({
    String? startDate,
    String? endDate,
    int? accountId,
  });

  /// 클레임 상세 조회
  Future<ClaimDetail> getClaimDetail(int claimId);

  /// 클레임 임시저장 (upsert)
  Future<void> saveDraft(ClaimRegisterForm? form);

  /// 클레임 임시저장 폐기
  Future<void> deleteDraft();
}
