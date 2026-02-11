import '../models/claim_form_data_model.dart';
import '../models/claim_register_request.dart';
import '../models/claim_register_result_model.dart';

/// 클레임 Remote DataSource
abstract class ClaimRemoteDataSource {
  /// 클레임 등록
  Future<ClaimRegisterResultModel> registerClaim(ClaimRegisterRequest request);

  /// 폼 데이터 조회
  Future<ClaimFormDataModel> getFormData();
}
