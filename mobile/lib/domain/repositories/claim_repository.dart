import '../entities/claim_form.dart';
import '../entities/claim_form_data.dart';
import '../entities/claim_result.dart';

/// 클레임 Repository 인터페이스
///
/// 클레임 등록 및 폼 데이터 조회 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class ClaimRepository {
  /// 클레임 등록
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 클레임 결과 정보
  Future<ClaimRegisterResult> registerClaim(ClaimRegisterForm form);

  /// 폼 초기화 데이터 조회
  ///
  /// 클레임 종류(categories + subcategories), 구매 방법, 요청사항 목록을 조회합니다.
  /// Returns: 폼 초기화에 필요한 모든 데이터
  Future<ClaimFormData> getFormData();
}
