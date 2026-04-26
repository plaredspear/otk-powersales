import 'dart:io';

import '../entities/daily_sales.dart';

/// 일매출 Repository 인터페이스
///
/// 일매출 등록, 임시저장 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class DailySalesRepository {
  /// 일매출 등록 (최종 제출)
  ///
  /// 모든 필수 항목(제품 정보, 사진)이 입력된 일매출을 서버에 등록합니다.
  /// 등록 후 상태는 REGISTERED가 됩니다.
  ///
  /// [eventId]: 행사 ID
  /// [salesDate]: 매출 일자
  /// [mainProductPrice]: 대표제품 판매단가 (nullable)
  /// [mainProductQuantity]: 대표제품 판매수량 (nullable)
  /// [mainProductAmount]: 대표제품 총 판매금액 (nullable)
  /// [subProductCode]: 기타제품 코드 (nullable)
  /// [subProductName]: 기타제품명 (nullable)
  /// [subProductQuantity]: 기타제품 판매수량 (nullable)
  /// [subProductAmount]: 기타제품 총 판매금액 (nullable)
  /// [photo]: 첨부 사진 (필수)
  ///
  /// Returns: 등록된 DailySales 엔티티
  ///
  /// Throws:
  /// - [InvalidParameterException]: 필수 입력 항목 누락
  /// - [InvalidPhotoException]: 사진 미첨부
  /// - [AlreadyRegisteredException]: 오늘 이미 등록됨
  /// - [EventPeriodExpiredException]: 행사 기간 외
  /// - [ForbiddenException]: 담당자가 아님
  Future<DailySales> registerDailySales({
    required String eventId,
    required DateTime salesDate,
    int? mainProductPrice,
    int? mainProductQuantity,
    int? mainProductAmount,
    String? subProductCode,
    String? subProductName,
    int? subProductQuantity,
    int? subProductAmount,
    required File photo,
  });

  /// 일매출 임시저장
  ///
  /// 입력 중인 일매출을 임시로 저장합니다.
  /// 필수 항목이 충족되지 않아도 저장 가능하며, 상태는 DRAFT가 됩니다.
  ///
  /// [eventId]: 행사 ID
  /// [salesDate]: 매출 일자
  /// [mainProductPrice]: 대표제품 판매단가 (nullable)
  /// [mainProductQuantity]: 대표제품 판매수량 (nullable)
  /// [mainProductAmount]: 대표제품 총 판매금액 (nullable)
  /// [subProductCode]: 기타제품 코드 (nullable)
  /// [subProductName]: 기타제품명 (nullable)
  /// [subProductQuantity]: 기타제품 판매수량 (nullable)
  /// [subProductAmount]: 기타제품 총 판매금액 (nullable)
  /// [photo]: 첨부 사진 (nullable)
  ///
  /// Returns: 임시저장된 DailySales 엔티티
  ///
  /// Throws:
  /// - [EventPeriodExpiredException]: 행사 기간 외
  /// - [ForbiddenException]: 담당자가 아님
  Future<DailySales> saveDraft({
    required String eventId,
    required DateTime salesDate,
    int? mainProductPrice,
    int? mainProductQuantity,
    int? mainProductAmount,
    String? subProductCode,
    String? subProductName,
    int? subProductQuantity,
    int? subProductAmount,
    File? photo,
  });
}
