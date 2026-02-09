import 'package:flutter/foundation.dart';

import '../../domain/entities/safety_check_category.dart';
import 'safety_check_item_model.dart';

/// 안전점검 카테고리 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 SafetyCheckCategory 엔티티로 변환합니다.
class SafetyCheckCategoryModel {
  final int id;
  final String name;
  final String? description;
  final List<SafetyCheckItemModel> items;

  const SafetyCheckCategoryModel({
    required this.id,
    required this.name,
    this.description,
    required this.items,
  });

  /// snake_case JSON에서 파싱
  factory SafetyCheckCategoryModel.fromJson(Map<String, dynamic> json) {
    return SafetyCheckCategoryModel(
      id: json['id'] as int,
      name: json['name'] as String,
      description: json['description'] as String?,
      items: (json['items'] as List<dynamic>)
          .map((e) =>
              SafetyCheckItemModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'items': items.map((item) => item.toJson()).toList(),
    };
  }

  /// Domain Entity로 변환
  SafetyCheckCategory toEntity() {
    return SafetyCheckCategory(
      id: id,
      name: name,
      description: description,
      items: items.map((item) => item.toEntity()).toList(),
    );
  }

  /// Domain Entity에서 생성
  factory SafetyCheckCategoryModel.fromEntity(SafetyCheckCategory entity) {
    return SafetyCheckCategoryModel(
      id: entity.id,
      name: entity.name,
      description: entity.description,
      items: entity.items
          .map((item) => SafetyCheckItemModel.fromEntity(item))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckCategoryModel &&
        other.id == id &&
        other.name == name &&
        other.description == description &&
        listEquals(other.items, items);
  }

  @override
  int get hashCode {
    return Object.hash(id, name, description, Object.hashAll(items));
  }

  @override
  String toString() {
    return 'SafetyCheckCategoryModel(id: $id, name: $name, description: $description, items: ${items.length} items)';
  }
}
