import 'claim_category.dart';
import 'claim_code.dart';

/// 클레임 등록 폼 초기화 데이터 Entity
///
/// GET /api/v1/claims/form-data API 응답을 담는 통합 엔티티입니다.
/// 클레임 종류 (categories + subcategories), 구매 방법, 요청사항 목록을 포함합니다.
class ClaimFormData {
  const ClaimFormData({
    required this.categories,
    required this.purchaseMethods,
    required this.requestTypes,
  });

  /// 클레임 종류1 목록 (각 항목에 subcategories 중첩)
  final List<ClaimCategory> categories;

  /// 구매 방법 목록
  final List<PurchaseMethod> purchaseMethods;

  /// 요청사항 목록
  final List<ClaimRequestType> requestTypes;

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'categories': categories.map((c) => c.toJson()).toList(),
      'purchaseMethods': purchaseMethods.map((p) => p.toJson()).toList(),
      'requestTypes': requestTypes.map((r) => r.toJson()).toList(),
    };
  }

  /// JSON 역직렬화
  factory ClaimFormData.fromJson(Map<String, dynamic> json) {
    return ClaimFormData(
      categories: (json['categories'] as List)
          .map((c) => ClaimCategory.fromJson(c as Map<String, dynamic>))
          .toList(),
      purchaseMethods: (json['purchaseMethods'] as List)
          .map((p) => PurchaseMethod.fromJson(p as Map<String, dynamic>))
          .toList(),
      requestTypes: (json['requestTypes'] as List)
          .map((r) => ClaimRequestType.fromJson(r as Map<String, dynamic>))
          .toList(),
    );
  }

  /// copyWith
  ClaimFormData copyWith({
    List<ClaimCategory>? categories,
    List<PurchaseMethod>? purchaseMethods,
    List<ClaimRequestType>? requestTypes,
  }) {
    return ClaimFormData(
      categories: categories ?? this.categories,
      purchaseMethods: purchaseMethods ?? this.purchaseMethods,
      requestTypes: requestTypes ?? this.requestTypes,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ClaimFormData) return false;

    // categories 비교
    if (other.categories.length != categories.length) return false;
    for (int i = 0; i < categories.length; i++) {
      if (other.categories[i] != categories[i]) return false;
    }

    // purchaseMethods 비교
    if (other.purchaseMethods.length != purchaseMethods.length) return false;
    for (int i = 0; i < purchaseMethods.length; i++) {
      if (other.purchaseMethods[i] != purchaseMethods[i]) return false;
    }

    // requestTypes 비교
    if (other.requestTypes.length != requestTypes.length) return false;
    for (int i = 0; i < requestTypes.length; i++) {
      if (other.requestTypes[i] != requestTypes[i]) return false;
    }

    return true;
  }

  @override
  int get hashCode => Object.hash(
        Object.hashAll(categories),
        Object.hashAll(purchaseMethods),
        Object.hashAll(requestTypes),
      );

  @override
  String toString() {
    return 'ClaimFormData('
        'categories: ${categories.length} items, '
        'purchaseMethods: ${purchaseMethods.length} items, '
        'requestTypes: ${requestTypes.length} items'
        ')';
  }
}
