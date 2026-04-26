import '../../domain/entities/notice.dart';

/// Notice API 모델 (DTO)
///
/// API 응답의 snake_case JSON을 Domain Entity로 변환한다.
class NoticeModel {
  final int id;
  final String title;
  final String category;
  final String categoryName;
  final DateTime createdAt;

  const NoticeModel({
    required this.id,
    required this.title,
    required this.category,
    required this.categoryName,
    required this.createdAt,
  });

  factory NoticeModel.fromJson(Map<String, dynamic> json) {
    return NoticeModel(
      id: json['id'] as int,
      title: json['title'] as String,
      category: json['category'] as String,
      categoryName: json['category_name'] as String,
      createdAt: DateTime.parse(json['created_at'] as String),
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

  Notice toEntity() {
    return Notice(
      id: id,
      title: title,
      category: category,
      categoryName: categoryName,
      createdAt: createdAt,
    );
  }

  factory NoticeModel.fromEntity(Notice entity) {
    return NoticeModel(
      id: entity.id,
      title: entity.title,
      category: entity.category,
      categoryName: entity.categoryName,
      createdAt: entity.createdAt,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is NoticeModel &&
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
    return 'NoticeModel(id: $id, title: $title, category: $category, categoryName: $categoryName, createdAt: $createdAt)';
  }
}
