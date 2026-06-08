import '../models/claim_detail_model.dart';
import '../models/claim_draft_model.dart';
import '../models/claim_draft_request.dart';
import '../models/claim_form_data_model.dart';
import '../models/claim_list_item_model.dart';
import '../models/claim_register_request.dart';
import '../models/claim_register_result_model.dart';

/// 클레임 Remote DataSource
abstract class ClaimRemoteDataSource {
  /// 클레임 등록
  Future<ClaimRegisterResultModel> registerClaim(ClaimRegisterRequest request);

  /// 폼 데이터 조회
  Future<ClaimFormDataModel> getFormData();

  /// 클레임 목록 조회
  Future<List<ClaimListItemModel>> getClaims({
    String? startDate,
    String? endDate,
  });

  /// 클레임 상세 조회
  Future<ClaimDetailModel> getClaimDetail(int claimId);

  /// 클레임 임시저장 (upsert)
  Future<void> saveDraft(ClaimDraftRequest request);

  /// 클레임 임시저장 조회 (없으면 null). 사진 URL 은 임시 파일로 내려받아 채운다.
  Future<ClaimDraftModel?> getDraft();

  /// 클레임 임시저장 폐기
  Future<void> deleteDraft();
}
