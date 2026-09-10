import 'promotion.dart';

/// 일매출 마감 폼 (서버 GET 응답 / 임시저장 응답).
///
/// 임시저장(draft)이 있으면 draft 값이, 없으면 PromotionEmployee 현재 값이 prefill 된다.
class DailySalesForm {
  final int promotionEmployeeId;
  final int? promotionId;

  /// 행사유형. 레거시 행사 필드 `[행사유형]행사명` 의 앞부분.
  final String? promotionType;

  /// 행사명(`제품온도타입(대표제품명)`). 서버 파생값.
  final String? promotionName;

  /// 대표제품명 (행사마스터). 레거시 "대표 제품" 영역 1행.
  final String? primaryProductName;

  /// 대표제품코드 (행사마스터). 레거시 "대표 제품" 영역 2행.
  final String? primaryProductCode;

  /// 행사마스터의 기타제품 텍스트. 레거시 "기타 제품" 영역.
  final String? otherProduct;
  final String? scheduleDate;
  final String? employeeName;

  /// 여사원 마감 완료 여부.
  final bool isClosed;

  /// 입력/수정 가능 여부 (본인 + 미마감).
  final bool editable;

  /// 출근 등록 완료 여부 (마감 선행 조건). 레거시 commutelogId 존재 여부에 대응.
  final bool attendanceRegistered;

  /// 임시저장 값으로 prefill 되었는지 여부.
  final bool hasDraft;

  final num? basePrice;
  final num? primarySalesQuantity;
  final num? primarySalesPrice;
  final num? primaryProductAmount;
  final num? otherSalesQuantity;
  final num? otherSalesAmount;

  /// 기타(대체) 상품명. 백엔드 SF 필드 `Description__c`(레거시 행사 대체 제품 이름)에 대응한다.
  final String? description;

  /// prefill 기준 이미지의 접근 URL (없으면 null).
  final String? imageUrl;

  const DailySalesForm({
    required this.promotionEmployeeId,
    this.promotionId,
    this.promotionType,
    this.promotionName,
    this.primaryProductName,
    this.primaryProductCode,
    this.otherProduct,
    this.scheduleDate,
    this.employeeName,
    required this.isClosed,
    required this.editable,
    this.attendanceRegistered = false,
    required this.hasDraft,
    this.basePrice,
    this.primarySalesQuantity,
    this.primarySalesPrice,
    this.primaryProductAmount,
    this.otherSalesQuantity,
    this.otherSalesAmount,
    this.description,
    this.imageUrl,
  });

  /// 레거시 `write.jsp` 행사 필드 표기 `[행사유형]행사명`.
  String? get promotionLabel =>
      formatPromotionLabel(promotionType, promotionName);
}

/// 일매출 마감 처리 결과.
class DailySalesCloseResult {
  final int promotionEmployeeId;
  final bool isClosed;
  final int? actualAmount;
  final String? imageUrl;

  const DailySalesCloseResult({
    required this.promotionEmployeeId,
    required this.isClosed,
    this.actualAmount,
    this.imageUrl,
  });
}

/// 일매출 마감/임시저장 입력값 (모바일 → 서버).
///
/// 레거시(Heroku) 마스터 개선안에 따라 대표제품 판매단가(primarySalesPrice)는
/// 입력 대상에서 제외되었으며, 총 판매금액(primaryProductAmount)은 직접 입력값이다.
class DailySalesInput {
  final num? primarySalesQuantity;
  final num? primaryProductAmount;
  final num? otherSalesQuantity;
  final num? otherSalesAmount;
  final String? description;

  const DailySalesInput({
    this.primarySalesQuantity,
    this.primaryProductAmount,
    this.otherSalesQuantity,
    this.otherSalesAmount,
    this.description,
  });
}
