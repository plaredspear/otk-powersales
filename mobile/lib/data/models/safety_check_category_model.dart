import '../../domain/entities/safety_check_category.dart';
import 'safety_check_item_model.dart';

/// 안전점검 카테고리 모델 (V1 JSON 매핑)
class SafetyCheckCategoryModel {
  final int questionNum;
  final String title;
  final String inputType;
  final bool required;
  final List<String>? options;
  final List<SafetyCheckItemModel> items;

  const SafetyCheckCategoryModel({
    required this.questionNum,
    required this.title,
    required this.inputType,
    required this.required,
    this.options,
    required this.items,
  });

  factory SafetyCheckCategoryModel.fromJson(Map<String, dynamic> json) {
    return SafetyCheckCategoryModel(
      questionNum: json['questionNum'] as int,
      title: json['title'] as String,
      inputType: json['inputType'] as String,
      required: json['required'] as bool,
      options: (json['options'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      items: (json['items'] as List<dynamic>)
          .map((e) =>
              SafetyCheckItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  SafetyCheckCategory toEntity() {
    return SafetyCheckCategory(
      questionNum: questionNum,
      title: title,
      inputType: inputType,
      required: required,
      options: options,
      items: items.map((item) => item.toEntity()).toList(),
    );
  }
}
