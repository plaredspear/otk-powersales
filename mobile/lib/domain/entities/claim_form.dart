import 'dart:io';

import 'claim_code.dart';

/// 클레임 등록 폼 Entity
///
/// 클레임 등록 시 사용자가 입력한 모든 정보를 담습니다.
class ClaimRegisterForm {
  /// copyWith 에서 "인자 미지정" 을 "명시적 null" 과 구분하기 위한 센티널.
  static const Object _unset = Object();

  const ClaimRegisterForm({
    required this.accountId,
    required this.accountName,
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

  final int accountId;
  final String accountName;
  final String productCode;
  final String productName;
  final ClaimDateType dateType;
  final DateTime date;
  /// 백엔드 ClaimType1.value (예: "A") — 등록 시 claimType1 으로 전송.
  final String categoryId;
  final String categoryName;

  /// 백엔드 ClaimType2.value (예: "AA") — 등록 시 claimType2 으로 전송.
  final String subcategoryId;
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
  ///
  /// 레거시 write.jsp:353 게이트 정합 — 구매금액/구매방법/영수증 중 **하나라도**
  /// 입력되면 구매 그룹이 활성화되어 금액·방법이 필수로 승격된다.
  /// (구매금액만으로 판정하면 방법/영수증만 채운 미완성 입력이 통과되는 격차 발생.)
  bool get hasPurchaseInfo =>
      (purchaseAmount != null && purchaseAmount! > 0) ||
      (purchaseMethodCode != null && purchaseMethodCode!.isNotEmpty) ||
      receiptPhoto != null;

  /// 요청사항 입력 여부
  bool get hasRequestType =>
      requestTypeCode != null && requestTypeCode!.isNotEmpty;

  /// 유효성 검증
  ///
  /// Returns: 에러 메시지 리스트 (빈 리스트면 유효)
  List<String> validate() {
    final errors = <String>[];

    // 필수 필드 검증
    if (accountId <= 0) {
      errors.add('거래처를 선택해주세요');
    }
    if (accountName.isEmpty) {
      errors.add('거래처명이 비어있습니다');
    }
    if (productCode.isEmpty) {
      errors.add('제품을 선택해주세요');
    }
    if (productName.isEmpty) {
      errors.add('제품명이 비어있습니다');
    }
    if (categoryId.isEmpty) {
      errors.add('클레임 종류를 선택해주세요');
    }
    if (categoryName.isEmpty) {
      errors.add('클레임 종류명이 비어있습니다');
    }
    if (subcategoryId.isEmpty) {
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
    // 사진 필수 검증 (레거시 write.jsp:348/350 — claimImgIdx/partImgIdx==0 차단).
    // 미첨부는 빈 경로 File 로 표현되므로 path 가 비었는지 확인한다.
    if (defectPhoto.path.isEmpty) {
      errors.add('불량 사진을 첨부해주세요');
    }
    if (labelPhoto.path.isEmpty) {
      errors.add('일부인 사진을 첨부해주세요');
    }

    // 조건부 필수 필드 검증 (구매 정보)
    // 검증 순서는 레거시 write.jsp:354→358→355 정합 (구매금액 → 구매방법 → 영수증).
    if (hasPurchaseInfo) {
      if (purchaseAmount == null || purchaseAmount! <= 0) {
        errors.add('구매 금액을 입력해주세요');
      }
      if (purchaseMethodCode == null || purchaseMethodCode!.isEmpty) {
        errors.add('구매 방법을 선택해주세요');
      }
      if (purchaseMethodName == null || purchaseMethodName!.isEmpty) {
        errors.add('구매 방법명이 비어있습니다');
      }
      // 영수증은 개인카드(B)/현금(C) 만 필수, 법인카드(A) 는 면제
      // (레거시 write.jsp 2024-02-22 정책 + 백엔드 ReceiptRequiredException 정합).
      final requiresReceipt =
          purchaseMethodCode == 'B' || purchaseMethodCode == 'C';
      if (requiresReceipt && receiptPhoto == null) {
        errors.add('구매 영수증 사진을 첨부해주세요');
      }
    }

    return errors;
  }

  /// 유효한 폼인지 확인
  bool get isValid => validate().isEmpty;

  /// copyWith
  ///
  /// 구매 금액은 선택 항목이라 명시적으로 `null` 을 넘겨 값을 지울 수 있어야 한다
  /// (레거시 write.jsp: 빈 값 → 서버 null 저장). 일반 `?? this.x` 병합은
  /// "미지정" 과 "명시적 null 삭제" 를 구분하지 못해 직전 값이 되살아나므로,
  /// 센티널(_unset) 로 인자 전달 여부를 판별한다.
  ClaimRegisterForm copyWith({
    int? accountId,
    String? accountName,
    String? productCode,
    String? productName,
    ClaimDateType? dateType,
    DateTime? date,
    String? categoryId,
    String? categoryName,
    String? subcategoryId,
    String? subcategoryName,
    String? defectDescription,
    int? defectQuantity,
    File? defectPhoto,
    File? labelPhoto,
    Object? purchaseAmount = _unset,
    String? purchaseMethodCode,
    String? purchaseMethodName,
    File? receiptPhoto,
    String? requestTypeCode,
    String? requestTypeName,
  }) {
    return ClaimRegisterForm(
      accountId: accountId ?? this.accountId,
      accountName: accountName ?? this.accountName,
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
      purchaseAmount: identical(purchaseAmount, _unset)
          ? this.purchaseAmount
          : purchaseAmount as int?,
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
        other.accountId == accountId &&
        other.accountName == accountName &&
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
        accountId,
        accountName,
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
        'accountId: $accountId, '
        'accountName: $accountName, '
        'productCode: $productCode, '
        'productName: $productName, '
        'categoryId: $categoryId, '
        'subcategoryId: $subcategoryId'
        ')';
  }
}
