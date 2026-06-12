import 'dart:io';

/// 현장 점검 등록 임시저장(draft) 엔티티.
///
/// 서버에 저장된 임시저장 값을 등록 폼에 복원(prefill)하기 위한 데이터.
/// 테마/현장유형/분류는 코드만, 거래처/제품은 코드+이름을 함께 담는다.
/// 사진은 데이터소스가 URL 을 임시 파일로 내려받아 [photos] 로 채운다.
class InspectionDraft {
  final int? themeId;

  /// 분류 코드 (OWN / COMPETITOR)
  final String? category;

  final int? accountId;
  final String? accountName;

  /// 점검일 (yyyy-MM-dd)
  final String? inspectionDate;

  final String? fieldTypeCode;
  final String? description;
  final String? productCode;
  final String? productName;
  final String? competitorName;
  final String? competitorActivity;
  final bool? competitorTasting;
  final String? competitorProductName;
  final int? competitorProductPrice;
  final int? competitorSalesQuantity;

  /// 복원된 사진(임시 파일)
  final List<File> photos;

  const InspectionDraft({
    this.themeId,
    this.category,
    this.accountId,
    this.accountName,
    this.inspectionDate,
    this.fieldTypeCode,
    this.description,
    this.productCode,
    this.productName,
    this.competitorName,
    this.competitorActivity,
    this.competitorTasting,
    this.competitorProductName,
    this.competitorProductPrice,
    this.competitorSalesQuantity,
    this.photos = const [],
  });
}
