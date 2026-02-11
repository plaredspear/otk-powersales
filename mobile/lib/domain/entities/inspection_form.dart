import 'dart:io';

import 'inspection_list_item.dart';

/// 현장 점검 등록 폼 엔티티
///
/// 현장 점검 등록 시 사용하는 폼 데이터를 담는 도메인 엔티티입니다.
class InspectionRegisterForm {
  /// 테마 ID
  final int themeId;

  /// 분류 (자사/경쟁사)
  final InspectionCategory category;

  /// 거래처 ID
  final int storeId;

  /// 점검일
  final DateTime inspectionDate;

  /// 현장 유형 코드
  final String fieldTypeCode;

  /// 설명 (자사, 선택)
  final String? description;

  /// 제품 코드 (자사 필수)
  final String? productCode;

  /// 경쟁사명 (경쟁사 필수)
  final String? competitorName;

  /// 경쟁사 활동 내용 (경쟁사 필수)
  final String? competitorActivity;

  /// 시식 여부 (경쟁사 필수)
  final bool? competitorTasting;

  /// 경쟁사 상품명 (시식=예 시 필수)
  final String? competitorProductName;

  /// 제품 가격 (시식=예 시 필수)
  final int? competitorProductPrice;

  /// 판매 수량 (시식=예 시 필수)
  final int? competitorSalesQuantity;

  /// 사진 파일 (최대 2장, 필수)
  final List<File> photos;

  const InspectionRegisterForm({
    required this.themeId,
    required this.category,
    required this.storeId,
    required this.inspectionDate,
    required this.fieldTypeCode,
    this.description,
    this.productCode,
    this.competitorName,
    this.competitorActivity,
    this.competitorTasting,
    this.competitorProductName,
    this.competitorProductPrice,
    this.competitorSalesQuantity,
    required this.photos,
  });

  /// 자사 점검 여부
  bool get isOwn => category == InspectionCategory.OWN;

  /// 경쟁사 점검 여부
  bool get isCompetitor => category == InspectionCategory.COMPETITOR;

  InspectionRegisterForm copyWith({
    int? themeId,
    InspectionCategory? category,
    int? storeId,
    DateTime? inspectionDate,
    String? fieldTypeCode,
    String? description,
    String? productCode,
    String? competitorName,
    String? competitorActivity,
    bool? competitorTasting,
    String? competitorProductName,
    int? competitorProductPrice,
    int? competitorSalesQuantity,
    List<File>? photos,
  }) {
    return InspectionRegisterForm(
      themeId: themeId ?? this.themeId,
      category: category ?? this.category,
      storeId: storeId ?? this.storeId,
      inspectionDate: inspectionDate ?? this.inspectionDate,
      fieldTypeCode: fieldTypeCode ?? this.fieldTypeCode,
      description: description ?? this.description,
      productCode: productCode ?? this.productCode,
      competitorName: competitorName ?? this.competitorName,
      competitorActivity: competitorActivity ?? this.competitorActivity,
      competitorTasting: competitorTasting ?? this.competitorTasting,
      competitorProductName:
          competitorProductName ?? this.competitorProductName,
      competitorProductPrice:
          competitorProductPrice ?? this.competitorProductPrice,
      competitorSalesQuantity:
          competitorSalesQuantity ?? this.competitorSalesQuantity,
      photos: photos ?? this.photos,
    );
  }

  /// 폼 유효성 검증
  ///
  /// 검증 규칙:
  /// 1. 공통 필수: themeId, category, storeId, inspectionDate, fieldTypeCode, photos(최소 1장)
  /// 2. 자사 필수: productCode
  /// 3. 경쟁사 필수: competitorName, competitorActivity, competitorTasting
  /// 4. 시식=예 조건부 필수: competitorProductName, competitorProductPrice, competitorSalesQuantity
  /// 5. 사진 최대 2장
  ValidationResult validate() {
    final errors = <String>[];

    // 공통 필수 검증
    if (themeId <= 0) {
      errors.add('테마를 선택해주세요');
    }
    if (storeId <= 0) {
      errors.add('거래처를 선택해주세요');
    }
    if (fieldTypeCode.isEmpty) {
      errors.add('현장 유형을 선택해주세요');
    }

    // 사진 검증
    if (photos.isEmpty) {
      errors.add('사진을 1장 이상 첨부해주세요');
    }
    if (photos.length > 2) {
      errors.add('사진은 최대 2장까지 첨부 가능합니다');
    }

    // 자사 필수 검증
    if (isOwn) {
      if (productCode == null || productCode!.isEmpty) {
        errors.add('제품을 선택해주세요');
      }
    }

    // 경쟁사 필수 검증
    if (isCompetitor) {
      if (competitorName == null || competitorName!.isEmpty) {
        errors.add('경쟁사명을 입력해주세요');
      }
      if (competitorActivity == null || competitorActivity!.isEmpty) {
        errors.add('경쟁사 활동 내용을 입력해주세요');
      }
      if (competitorTasting == null) {
        errors.add('경쟁사 상품 시식 여부를 선택해주세요');
      }

      // 시식=예 조건부 필수 검증
      if (competitorTasting == true) {
        if (competitorProductName == null || competitorProductName!.isEmpty) {
          errors.add('경쟁사 상품명을 입력해주세요');
        }
        if (competitorProductPrice == null) {
          errors.add('제품 가격을 입력해주세요');
        } else if (competitorProductPrice! < 0) {
          errors.add('제품 가격은 0 이상이어야 합니다');
        }
        if (competitorSalesQuantity == null) {
          errors.add('판매 수량을 입력해주세요');
        } else if (competitorSalesQuantity! < 0) {
          errors.add('판매 수량은 0 이상이어야 합니다');
        }
      }
    }

    return ValidationResult(
      isValid: errors.isEmpty,
      errors: errors,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! InspectionRegisterForm) return false;
    return other.themeId == themeId &&
        other.category == category &&
        other.storeId == storeId &&
        other.inspectionDate == inspectionDate &&
        other.fieldTypeCode == fieldTypeCode &&
        other.description == description &&
        other.productCode == productCode &&
        other.competitorName == competitorName &&
        other.competitorActivity == competitorActivity &&
        other.competitorTasting == competitorTasting &&
        other.competitorProductName == competitorProductName &&
        other.competitorProductPrice == competitorProductPrice &&
        other.competitorSalesQuantity == competitorSalesQuantity &&
        _listEquals(other.photos, photos);
  }

  bool _listEquals<T>(List<T> a, List<T> b) {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      themeId,
      category,
      storeId,
      inspectionDate,
      fieldTypeCode,
      description,
      productCode,
      competitorName,
      competitorActivity,
      competitorTasting,
      competitorProductName,
      competitorProductPrice,
      competitorSalesQuantity,
      Object.hashAll(photos),
    );
  }

  @override
  String toString() {
    return 'InspectionRegisterForm(themeId: $themeId, category: $category, '
        'storeId: $storeId, inspectionDate: $inspectionDate, '
        'fieldTypeCode: $fieldTypeCode, description: $description, '
        'productCode: $productCode, competitorName: $competitorName, '
        'competitorActivity: $competitorActivity, competitorTasting: $competitorTasting, '
        'competitorProductName: $competitorProductName, competitorProductPrice: $competitorProductPrice, '
        'competitorSalesQuantity: $competitorSalesQuantity, photos: ${photos.length} files)';
  }
}

/// 유효성 검증 결과
class ValidationResult {
  /// 유효 여부
  final bool isValid;

  /// 에러 메시지 목록
  final List<String> errors;

  const ValidationResult({
    required this.isValid,
    required this.errors,
  });

  /// 첫 번째 에러 메시지 (없으면 null)
  String? get firstError => errors.isEmpty ? null : errors.first;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ValidationResult) return false;
    return other.isValid == isValid && _listEquals(other.errors, errors);
  }

  bool _listEquals<T>(List<T> a, List<T> b) {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode => Object.hash(isValid, Object.hashAll(errors));

  @override
  String toString() {
    return 'ValidationResult(isValid: $isValid, errors: $errors)';
  }
}
