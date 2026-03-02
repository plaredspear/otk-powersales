import '../../domain/entities/suggestion_form.dart';
import '../../domain/entities/suggestion_result.dart';

/// 제안하기 등록 결과 Model
class SuggestionRegisterResultModel {
  const SuggestionRegisterResultModel({
    required this.id,
    required this.category,
    required this.categoryName,
    this.productCode,
    this.productName,
    required this.title,
    required this.createdAt,
  });

  final int id;
  final String category; // SuggestionCategory의 code
  final String categoryName;
  final String? productCode;
  final String? productName;
  final String title;
  final String createdAt; // ISO 8601 String

  /// JSON 역직렬화
  factory SuggestionRegisterResultModel.fromJson(Map<String, dynamic> json) {
    return SuggestionRegisterResultModel(
      id: json['id'] as int,
      category: json['category'] as String,
      categoryName: json['category_name'] as String,
      productCode: json['product_code'] as String?,
      productName: json['product_name'] as String?,
      title: json['title'] as String,
      createdAt: json['created_at'] as String,
    );
  }

  /// Entity로 변환
  SuggestionRegisterResult toEntity() {
    return SuggestionRegisterResult(
      id: id,
      category: SuggestionCategory.fromCode(category),
      categoryName: categoryName,
      productCode: productCode,
      productName: productName,
      title: title,
      createdAt: DateTime.parse(createdAt),
    );
  }
}
