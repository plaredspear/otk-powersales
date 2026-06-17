import 'dart:io';

/// 제안하기 임시저장(draft) Entity
///
/// `GET /api/v1/mobile/suggestions/draft` 의 응답을 담는다. 등록 폼 prefill 용.
/// category 는 enum name(= `SuggestionCategory.code`, 예 "LOGISTICS_CLAIM").
/// claimDate 는 서버가 "yyyy-MM-dd" 로 내려주므로 [DateTime] 으로 파싱해 보관한다.
/// 사진은 서버 presigned URL 을 임시 파일로 내려받아 [File] 로 보관한다(없으면 빈 리스트).
class SuggestionDraft {
  const SuggestionDraft({
    this.category,
    this.title,
    this.content,
    this.productCode,
    this.productName,
    this.accountId,
    this.accountName,
    this.sapAccountCode,
    this.claimType,
    this.claimDate,
    this.carNumber,
    this.logisticsResponsibility,
    this.duplicateProposalNum,
    this.actionStatus,
    this.photos = const [],
  });

  /// 분류 코드 (예: "LOGISTICS_CLAIM" / "NEW_PRODUCT" / "EXISTING_PRODUCT")
  final String? category;
  final String? title;
  final String? content;
  final String? productCode;
  final String? productName;
  final int? accountId;
  final String? accountName;
  final String? sapAccountCode;
  final String? claimType;

  /// 클레임 발생일자 (yyyy-MM-dd 파싱 결과)
  final DateTime? claimDate;
  final String? carNumber;

  /// 물류 책임소재 (등록 폼에는 입력 필드가 없으나 round-trip 보존)
  final String? logisticsResponsibility;

  /// 중복 제안 번호 (등록 폼에는 입력 필드가 없으나 round-trip 보존)
  final String? duplicateProposalNum;

  /// 조치 상태 (등록 폼에는 입력 필드가 없으나 round-trip 보존)
  final String? actionStatus;

  /// 내려받은 사진 임시 파일 (최대 2장)
  final List<File> photos;
}
