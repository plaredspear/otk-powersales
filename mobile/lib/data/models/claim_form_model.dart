import '../../domain/entities/claim_form_entry.dart';
import 'claim_draft_model.dart';
import 'claim_form_data_model.dart';

/// 클레임 등록 진입 폼 응답 Model (GET /api/v1/mobile/claims/form).
///
/// `{ metadata, draft }` 형태. scalar 는 [ClaimFormDataModel]/[ClaimDraftModel] 로 파싱하고,
/// draft 사진은 데이터소스가 URL 을 임시 파일로 내려받아 [ClaimDraftModel.withPhotos] 로 채운다.
class ClaimFormModel {
  const ClaimFormModel({
    required this.metadata,
    this.draft,
  });

  final ClaimFormDataModel metadata;

  /// 사진 [File] 까지 채워진 임시저장 Model (없으면 null).
  final ClaimDraftModel? draft;

  ClaimFormEntry toEntity() {
    return ClaimFormEntry(
      formData: metadata.toEntity(),
      draft: draft?.toEntity(),
    );
  }
}
