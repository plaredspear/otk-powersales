import 'package:flutter/foundation.dart';

import 'safety_check_item.dart';

/// 안전점검 카테고리 엔티티
///
/// 안전점검 체크리스트의 카테고리를 나타냅니다.
/// 각 카테고리는 여러 개의 체크 항목(SafetyCheckItem)을 포함합니다.
class SafetyCheckCategory {
  /// 카테고리 ID
  final int id;

  /// 카테고리명
  final String name;

  /// 카테고리 설명 (안내 문구)
  final String? description;

  /// 해당 카테고리의 체크 항목 목록
  final List<SafetyCheckItem> items;

  const SafetyCheckCategory({
    required this.id,
    required this.name,
    this.description,
    required this.items,
  });

  SafetyCheckCategory copyWith({
    int? id,
    String? name,
    String? description,
    List<SafetyCheckItem>? items,
  }) {
    return SafetyCheckCategory(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      items: items ?? this.items,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'items': items.map((item) => item.toJson()).toList(),
    };
  }

  factory SafetyCheckCategory.fromJson(Map<String, dynamic> json) {
    return SafetyCheckCategory(
      id: json['id'] as int,
      name: json['name'] as String,
      description: json['description'] as String?,
      items: (json['items'] as List<dynamic>)
          .map((e) => SafetyCheckItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckCategory &&
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
    return 'SafetyCheckCategory(id: $id, name: $name, description: $description, items: ${items.length} items)';
  }
}
