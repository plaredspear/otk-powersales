import 'dart:io';

/// 클레임 임시저장(draft) Entity
///
/// GET /api/v1/mobile/claims/draft 응답을 담는다. 등록 폼 prefill 용.
/// 종류1/2·구매방법·요청사항은 코드(value)만 보관하고, 이름은 화면이 form-data 로 해석한다.
/// 거래처명/제품명은 form-data 에 없으므로 함께 보관한다.
/// 사진은 서버 presigned URL 을 임시 파일로 내려받아 [File] 로 보관한다(없으면 null).
class ClaimDraft {
  const ClaimDraft({
    this.accountId,
    this.accountName,
    this.productCode,
    this.productName,
    this.dateType,
    this.date,
    this.claimType1,
    this.claimType2,
    this.defectDescription,
    this.defectQuantity,
    this.purchaseAmount,
    this.purchaseMethodCode,
    this.requestTypeCode,
    this.defectPhoto,
    this.labelPhoto,
    this.receiptPhoto,
  });

  final int? accountId;
  final String? accountName;
  final String? productCode;
  final String? productName;

  /// 기한 종류 (EXPIRY_DATE / MANUFACTURE_DATE)
  final String? dateType;

  /// 기한 날짜 (yyyy-MM-dd)
  final String? date;

  /// 클레임 종류1 value (예: "A")
  final String? claimType1;

  /// 클레임 종류2 value (예: "AA")
  final String? claimType2;

  final String? defectDescription;
  final int? defectQuantity;
  final int? purchaseAmount;
  final String? purchaseMethodCode;

  /// 요청사항 코드 (";" 구분 multipicklist)
  final String? requestTypeCode;

  final File? defectPhoto;
  final File? labelPhoto;
  final File? receiptPhoto;
}
