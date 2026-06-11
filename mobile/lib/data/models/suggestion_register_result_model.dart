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
  ///
  /// backend `SuggestionCreateResponse` 는 `id`/`proposalNumber`/`attachments`
  /// 만 반환하고 `category`/`categoryName`/`title`/`createdAt` 은 내려주지 않으므로
  /// null 을 허용하고 기본값으로 방어한다(등록 결과는 성공 메시지 외 미사용).
  factory SuggestionRegisterResultModel.fromJson(Map<String, dynamic> json) {
    return SuggestionRegisterResultModel(
      id: (json['id'] as num).toInt(),
      category: json['category'] as String? ?? '',
      categoryName: json['categoryName'] as String? ?? '',
      productCode: json['productCode'] as String?,
      productName: json['productName'] as String?,
      title: json['title'] as String? ?? '',
      createdAt: json['createdAt'] as String? ?? '',
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
      createdAt: createdAt.isEmpty ? DateTime.now() : DateTime.parse(createdAt),
    );
  }
}
