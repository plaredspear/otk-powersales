import 'suggestion_form.dart';

/// 제안하기 등록 결과 Entity
///
/// 제안하기 등록 API 응답 데이터를 담습니다.
class SuggestionRegisterResult {
  const SuggestionRegisterResult({
    required this.id,
    required this.category,
    required this.categoryName,
    this.productCode,
    this.productName,
    required this.title,
    required this.createdAt,
  });

  /// 제안 ID
  final int id;

  /// 분류
  final SuggestionCategory category;

  /// 분류명
  final String categoryName;

  /// 제품 코드
  final String? productCode;

  /// 제품명
  final String? productName;

  /// 제안 제목
  final String title;

  /// 등록 일시
  final DateTime createdAt;

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category.code,
      'categoryName': categoryName,
      'productCode': productCode,
      'productName': productName,
      'title': title,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  /// JSON 역직렬화
  factory SuggestionRegisterResult.fromJson(Map<String, dynamic> json) {
    return SuggestionRegisterResult(
      id: json['id'] as int,
      category: SuggestionCategory.fromCode(json['category'] as String),
      categoryName: json['categoryName'] as String,
      productCode: json['productCode'] as String?,
      productName: json['productName'] as String?,
      title: json['title'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  /// copyWith
  SuggestionRegisterResult copyWith({
    int? id,
    SuggestionCategory? category,
    String? categoryName,
    String? productCode,
    String? productName,
    String? title,
    DateTime? createdAt,
  }) {
    return SuggestionRegisterResult(
      id: id ?? this.id,
      category: category ?? this.category,
      categoryName: categoryName ?? this.categoryName,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      title: title ?? this.title,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SuggestionRegisterResult &&
        other.id == id &&
        other.category == category &&
        other.categoryName == categoryName &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.title == title &&
        other.createdAt == createdAt;
  }

  @override
  int get hashCode => Object.hash(
        id,
        category,
        categoryName,
        productCode,
        productName,
        title,
        createdAt,
      );

  @override
  String toString() {
    return 'SuggestionRegisterResult('
        'id: $id, '
        'category: ${category.displayName}, '
        'productCode: $productCode, '
        'productName: $productName, '
        'title: $title, '
        'createdAt: $createdAt'
        ')';
  }
}
