import 'dart:io';

import 'claim_code.dart';

/// 클레임 등록 폼 Entity
///
/// 클레임 등록 시 사용자가 입력한 모든 정보를 담습니다.
class ClaimRegisterForm {
  const ClaimRegisterForm({
    required this.storeId,
    required this.storeName,
    required this.productCode,
    required this.productName,
    required this.dateType,
    required this.date,
    required this.categoryId,
    required this.categoryName,
    required this.subcategoryId,
    required this.subcategoryName,
    required this.defectDescription,
    required this.defectQuantity,
    required this.defectPhoto,
    required this.labelPhoto,
    this.purchaseAmount,
    this.purchaseMethodCode,
    this.purchaseMethodName,
    this.receiptPhoto,
    this.requestTypeCode,
    this.requestTypeName,
  });

  final int storeId;
  final String storeName;
  final String productCode;
  final String productName;
  final ClaimDateType dateType;
  final DateTime date;
  final int categoryId;
  final String categoryName;
  final int subcategoryId;
  final String subcategoryName;
  final String defectDescription;
  final int defectQuantity;
  final File defectPhoto;
  final File labelPhoto;

  // 선택 항목
  final int? purchaseAmount;
  final String? purchaseMethodCode;
  final String? purchaseMethodName;
  final File? receiptPhoto;
  final String? requestTypeCode;
  final String? requestTypeName;

  /// 구매 정보 입력 여부
  bool get hasPurchaseInfo => purchaseAmount != null && purchaseAmount! > 0;

  /// 요청사항 입력 여부
  bool get hasRequestType =>
      requestTypeCode != null && requestTypeCode!.isNotEmpty;

  /// 유효성 검증
  ///
  /// Returns: 에러 메시지 리스트 (빈 리스트면 유효)
  List<String> validate() {
    final errors = <String>[];

    // 필수 필드 검증
    if (storeId <= 0) {
      errors.add('거래처를 선택해주세요');
    }
    if (storeName.isEmpty) {
      errors.add('거래처명이 비어있습니다');
    }
    if (productCode.isEmpty) {
      errors.add('제품을 선택해주세요');
    }
    if (productName.isEmpty) {
      errors.add('제품명이 비어있습니다');
    }
    if (categoryId <= 0) {
      errors.add('클레임 종류를 선택해주세요');
    }
    if (categoryName.isEmpty) {
      errors.add('클레임 종류명이 비어있습니다');
    }
    if (subcategoryId <= 0) {
      errors.add('클레임 세부 종류를 선택해주세요');
    }
    if (subcategoryName.isEmpty) {
      errors.add('클레임 세부 종류명이 비어있습니다');
    }
    if (defectDescription.isEmpty) {
      errors.add('불량 내역을 입력해주세요');
    }
    if (defectQuantity <= 0) {
      errors.add('불량 수량을 입력해주세요 (1개 이상)');
    }

    // 조건부 필수 필드 검증 (구매 정보)
    if (hasPurchaseInfo) {
      if (purchaseMethodCode == null || purchaseMethodCode!.isEmpty) {
        errors.add('구매 방법을 선택해주세요');
      }
      if (purchaseMethodName == null || purchaseMethodName!.isEmpty) {
        errors.add('구매 방법명이 비어있습니다');
      }
      if (receiptPhoto == null) {
        errors.add('구매 영수증 사진을 첨부해주세요');
      }
    }

    return errors;
  }

  /// 유효한 폼인지 확인
  bool get isValid => validate().isEmpty;

  /// copyWith
  ClaimRegisterForm copyWith({
    int? storeId,
    String? storeName,
    String? productCode,
    String? productName,
    ClaimDateType? dateType,
    DateTime? date,
    int? categoryId,
    String? categoryName,
    int? subcategoryId,
    String? subcategoryName,
    String? defectDescription,
    int? defectQuantity,
    File? defectPhoto,
    File? labelPhoto,
    int? purchaseAmount,
    String? purchaseMethodCode,
    String? purchaseMethodName,
    File? receiptPhoto,
    String? requestTypeCode,
    String? requestTypeName,
  }) {
    return ClaimRegisterForm(
      storeId: storeId ?? this.storeId,
      storeName: storeName ?? this.storeName,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      dateType: dateType ?? this.dateType,
      date: date ?? this.date,
      categoryId: categoryId ?? this.categoryId,
      categoryName: categoryName ?? this.categoryName,
      subcategoryId: subcategoryId ?? this.subcategoryId,
      subcategoryName: subcategoryName ?? this.subcategoryName,
      defectDescription: defectDescription ?? this.defectDescription,
      defectQuantity: defectQuantity ?? this.defectQuantity,
      defectPhoto: defectPhoto ?? this.defectPhoto,
      labelPhoto: labelPhoto ?? this.labelPhoto,
      purchaseAmount: purchaseAmount ?? this.purchaseAmount,
      purchaseMethodCode: purchaseMethodCode ?? this.purchaseMethodCode,
      purchaseMethodName: purchaseMethodName ?? this.purchaseMethodName,
      receiptPhoto: receiptPhoto ?? this.receiptPhoto,
      requestTypeCode: requestTypeCode ?? this.requestTypeCode,
      requestTypeName: requestTypeName ?? this.requestTypeName,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClaimRegisterForm &&
        other.storeId == storeId &&
        other.storeName == storeName &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.dateType == dateType &&
        other.date == date &&
        other.categoryId == categoryId &&
        other.categoryName == categoryName &&
        other.subcategoryId == subcategoryId &&
        other.subcategoryName == subcategoryName &&
        other.defectDescription == defectDescription &&
        other.defectQuantity == defectQuantity &&
        other.defectPhoto.path == defectPhoto.path &&
        other.labelPhoto.path == labelPhoto.path &&
        other.purchaseAmount == purchaseAmount &&
        other.purchaseMethodCode == purchaseMethodCode &&
        other.purchaseMethodName == purchaseMethodName &&
        other.receiptPhoto?.path == receiptPhoto?.path &&
        other.requestTypeCode == requestTypeCode &&
        other.requestTypeName == requestTypeName;
  }

  @override
  int get hashCode => Object.hash(
        storeId,
        storeName,
        productCode,
        productName,
        dateType,
        date,
        categoryId,
        categoryName,
        subcategoryId,
        subcategoryName,
        defectDescription,
        defectQuantity,
        defectPhoto.path,
        labelPhoto.path,
        purchaseAmount,
        purchaseMethodCode,
        purchaseMethodName,
        receiptPhoto?.path,
        requestTypeCode,
        requestTypeName,
      );

  @override
  String toString() {
    return 'ClaimRegisterForm('
        'storeId: $storeId, '
        'storeName: $storeName, '
        'productCode: $productCode, '
        'productName: $productName, '
        'categoryId: $categoryId, '
        'subcategoryId: $subcategoryId'
        ')';
  }
}
