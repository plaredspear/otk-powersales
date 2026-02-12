import 'dart:io';

import '../entities/daily_sales.dart';
import '../repositories/daily_sales_repository.dart';

/// 일매출 등록 요청 데이터
class DailySalesRequest {
  final String eventId;
  final DateTime salesDate;
  final int? mainProductPrice;
  final int? mainProductQuantity;
  final int? mainProductAmount;
  final String? subProductCode;
  final String? subProductName;
  final int? subProductQuantity;
  final int? subProductAmount;
  final File? photo;

  const DailySalesRequest({
    required this.eventId,
    required this.salesDate,
    this.mainProductPrice,
    this.mainProductQuantity,
    this.mainProductAmount,
    this.subProductCode,
    this.subProductName,
    this.subProductQuantity,
    this.subProductAmount,
    this.photo,
  });

  /// 대표제품 정보가 완전히 입력되었는지 확인
  bool get hasMainProduct =>
      mainProductPrice != null &&
      mainProductQuantity != null &&
      mainProductAmount != null;

  /// 기타제품 정보가 완전히 입력되었는지 확인
  bool get hasSubProduct =>
      subProductCode != null &&
      subProductName != null &&
      subProductQuantity != null &&
      subProductAmount != null;

  /// 최소 하나의 제품 정보가 입력되었는지 확인
  bool get hasAnyProduct => hasMainProduct || hasSubProduct;

  /// 등록 가능 여부 (필수 항목 충족)
  bool get canRegister => hasAnyProduct && photo != null;

  /// 임시저장 가능 여부 (항상 true)
  bool get canSaveDraft => true;
}

/// 일매출 등록 UseCase
///
/// 일매출 최종 등록 및 임시저장 기능을 제공합니다.
/// - 등록: 필수 항목(제품 정보, 사진) 검증 후 REGISTERED 상태로 저장
/// - 임시저장: 검증 없이 DRAFT 상태로 저장
class RegisterDailySales {
  final DailySalesRepository _repository;

  RegisterDailySales(this._repository);

  /// 일매출 최종 등록
  ///
  /// [request]: 일매출 등록 요청 데이터
  /// Returns: 등록된 DailySales 엔티티 (status: REGISTERED)
  /// Throws:
  /// - [Exception] 필수 항목(제품 정보, 사진) 미입력 시
  /// - Repository 관련 예외 (AlreadyRegisteredException, EventPeriodExpiredException 등)
  Future<DailySales> call(DailySalesRequest request) async {
    // 필수 항목 검증
    if (!request.hasAnyProduct) {
      throw Exception('대표 제품 또는 기타 제품 중 최소 하나를 입력해주세요');
    }

    if (request.photo == null) {
      throw Exception('사진을 첨부해주세요');
    }

    // 등록 실행
    return await _repository.registerDailySales(
      eventId: request.eventId,
      salesDate: request.salesDate,
      mainProductPrice: request.mainProductPrice,
      mainProductQuantity: request.mainProductQuantity,
      mainProductAmount: request.mainProductAmount,
      subProductCode: request.subProductCode,
      subProductName: request.subProductName,
      subProductQuantity: request.subProductQuantity,
      subProductAmount: request.subProductAmount,
      photo: request.photo!,
    );
  }

  /// 일매출 임시저장
  ///
  /// [request]: 일매출 등록 요청 데이터
  /// Returns: 임시저장된 DailySales 엔티티 (status: DRAFT)
  /// Throws: Repository 관련 예외 (EventPeriodExpiredException 등)
  Future<DailySales> saveDraft(DailySalesRequest request) async {
    // 임시저장은 유효성 검증 없이 실행
    return await _repository.saveDraft(
      eventId: request.eventId,
      salesDate: request.salesDate,
      mainProductPrice: request.mainProductPrice,
      mainProductQuantity: request.mainProductQuantity,
      mainProductAmount: request.mainProductAmount,
      subProductCode: request.subProductCode,
      subProductName: request.subProductName,
      subProductQuantity: request.subProductQuantity,
      subProductAmount: request.subProductAmount,
      photo: request.photo,
    );
  }
}
