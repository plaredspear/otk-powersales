import 'claim_draft.dart';
import 'claim_form_data.dart';

/// 클레임 등록 화면 진입 폼 (GET /api/v1/mobile/claims/form).
///
/// 화면 진입 1콜로 받는 묶음:
/// - [formData] : 종류1/2·구매방법·요청사항 등 정적 메타데이터
/// - [draft]    : 이어쓰기용 임시저장(없으면 null)
///
/// 일매출 진입 폼(getForm) 과 동일한 "진입 1콜 + draft 동봉" 컨벤션이다.
class ClaimFormEntry {
  const ClaimFormEntry({
    required this.formData,
    this.draft,
  });

  final ClaimFormData formData;
  final ClaimDraft? draft;
}
