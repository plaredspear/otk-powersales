/// 공지사항 엔티티
///
/// 홈화면에 표시되는 공지사항 정보를 나타낸다.
class Notice {
  final int id;
  final String title;
  final String category;
  final String categoryName;
  final DateTime createdAt;

  const Notice({
    required this.id,
    required this.title,
    required this.category,
    required this.categoryName,
    required this.createdAt,
  });

  Notice copyWith({
    int? id,
    String? title,
    String? category,
    String? categoryName,
    DateTime? createdAt,
  }) {
    return Notice(
      id: id ?? this.id,
      title: title ?? this.title,
      category: category ?? this.category,
      categoryName: categoryName ?? this.categoryName,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'category': category,
      'category_name': categoryName,
      'created_at': createdAt.toIso8601String(),
    };
  }

  factory Notice.fromJson(Map<String, dynamic> json) {
    return Notice(
      id: json['id'] as int,
      title: json['title'] as String,
      category: json['category'] as String,
      categoryName: json['category_name'] as String,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Notice &&
        other.id == id &&
        other.title == title &&
        other.category == category &&
        other.categoryName == categoryName &&
        other.createdAt == createdAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      title,
      category,
      categoryName,
      createdAt,
    );
  }

  @override
  String toString() {
    return 'Notice(id: $id, title: $title, category: $category, categoryName: $categoryName, createdAt: $createdAt)';
  }
}
