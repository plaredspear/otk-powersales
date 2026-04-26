/// 클레임 종류 Entity
///
/// 클레임 종류1과 종류2의 계층 구조를 표현합니다.

/// 클레임 종류2 (하위 종류)
class ClaimSubcategory {
  const ClaimSubcategory({
    required this.id,
    required this.name,
  });

  final int id;
  final String name;

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
    };
  }

  /// JSON 역직렬화
  factory ClaimSubcategory.fromJson(Map<String, dynamic> json) {
    return ClaimSubcategory(
      id: json['id'] as int,
      name: json['name'] as String,
    );
  }

  /// copyWith
  ClaimSubcategory copyWith({
    int? id,
    String? name,
  }) {
    return ClaimSubcategory(
      id: id ?? this.id,
      name: name ?? this.name,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ClaimSubcategory && other.id == id && other.name == name;
  }

  @override
  int get hashCode => Object.hash(id, name);

  @override
  String toString() => 'ClaimSubcategory(id: $id, name: $name)';
}

/// 클레임 종류1 (상위 종류)
class ClaimCategory {
  const ClaimCategory({
    required this.id,
    required this.name,
    required this.subcategories,
  });

  final int id;
  final String name;
  final List<ClaimSubcategory> subcategories;

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'subcategories': subcategories.map((s) => s.toJson()).toList(),
    };
  }

  /// JSON 역직렬화
  factory ClaimCategory.fromJson(Map<String, dynamic> json) {
    return ClaimCategory(
      id: json['id'] as int,
      name: json['name'] as String,
      subcategories: (json['subcategories'] as List)
          .map((s) => ClaimSubcategory.fromJson(s as Map<String, dynamic>))
          .toList(),
    );
  }

  /// copyWith
  ClaimCategory copyWith({
    int? id,
    String? name,
    List<ClaimSubcategory>? subcategories,
  }) {
    return ClaimCategory(
      id: id ?? this.id,
      name: name ?? this.name,
      subcategories: subcategories ?? this.subcategories,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ClaimCategory) return false;
    if (other.id != id || other.name != name) return false;
    if (other.subcategories.length != subcategories.length) return false;
    for (int i = 0; i < subcategories.length; i++) {
      if (other.subcategories[i] != subcategories[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode => Object.hash(id, name, Object.hashAll(subcategories));

  @override
  String toString() =>
      'ClaimCategory(id: $id, name: $name, subcategories: $subcategories)';
}
