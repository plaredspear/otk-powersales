import '../../domain/entities/claim_category.dart';

/// 클레임 종류2 Model
class ClaimSubcategoryModel {
  const ClaimSubcategoryModel({
    required this.id,
    required this.name,
  });

  final int id;
  final String name;

  /// JSON 역직렬화
  factory ClaimSubcategoryModel.fromJson(Map<String, dynamic> json) {
    return ClaimSubcategoryModel(
      id: json['id'] as int,
      name: json['name'] as String,
    );
  }

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
    };
  }

  /// Entity로 변환
  ClaimSubcategory toEntity() {
    return ClaimSubcategory(
      id: id,
      name: name,
    );
  }

  /// Entity에서 변환
  factory ClaimSubcategoryModel.fromEntity(ClaimSubcategory entity) {
    return ClaimSubcategoryModel(
      id: entity.id,
      name: entity.name,
    );
  }
}

/// 클레임 종류1 Model
class ClaimCategoryModel {
  const ClaimCategoryModel({
    required this.id,
    required this.name,
    required this.subcategories,
  });

  final int id;
  final String name;
  final List<ClaimSubcategoryModel> subcategories;

  /// JSON 역직렬화 (중첩 구조 포함)
  factory ClaimCategoryModel.fromJson(Map<String, dynamic> json) {
    return ClaimCategoryModel(
      id: json['id'] as int,
      name: json['name'] as String,
      subcategories: (json['subcategories'] as List)
          .map((s) => ClaimSubcategoryModel.fromJson(s as Map<String, dynamic>))
          .toList(),
    );
  }

  /// JSON 직렬화 (중첩 구조 포함)
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'subcategories': subcategories.map((s) => s.toJson()).toList(),
    };
  }

  /// Entity로 변환
  ClaimCategory toEntity() {
    return ClaimCategory(
      id: id,
      name: name,
      subcategories: subcategories.map((s) => s.toEntity()).toList(),
    );
  }

  /// Entity에서 변환
  factory ClaimCategoryModel.fromEntity(ClaimCategory entity) {
    return ClaimCategoryModel(
      id: entity.id,
      name: entity.name,
      subcategories: entity.subcategories
          .map((s) => ClaimSubcategoryModel.fromEntity(s))
          .toList(),
    );
  }
}
