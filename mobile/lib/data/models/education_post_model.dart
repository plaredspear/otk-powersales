import '../../domain/entities/education_post.dart';

/// EducationPost API 모델 (DTO)
///
/// API 응답의 JSON을 Domain Entity로 변환한다.
class EducationPostModel {
  final int id;
  final String title;
  final String createdAt;

  const EducationPostModel({
    required this.id,
    required this.title,
    required this.createdAt,
  });

  factory EducationPostModel.fromJson(Map<String, dynamic> json) {
    return EducationPostModel(
      id: json['id'] as int,
      title: json['title'] as String,
      createdAt: json['createdAt'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'createdAt': createdAt,
    };
  }

  EducationPost toEntity() {
    return EducationPost(
      id: id,
      title: title,
      createdAt: DateTime.parse(createdAt),
    );
  }

  factory EducationPostModel.fromEntity(EducationPost entity) {
    return EducationPostModel(
      id: entity.id,
      title: entity.title,
      createdAt: entity.createdAt.toIso8601String(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EducationPostModel &&
        other.id == id &&
        other.title == title &&
        other.createdAt == createdAt;
  }

  @override
  int get hashCode {
    return Object.hash(id, title, createdAt);
  }

  @override
  String toString() {
    return 'EducationPostModel(id: $id, title: $title, createdAt: $createdAt)';
  }
}

/// EducationPostPage API 모델 (DTO)
///
/// API 응답의 페이지네이션 JSON을 Domain Entity로 변환한다.
class EducationPostPageModel {
  final List<EducationPostModel> content;
  final int totalCount;
  final int totalPages;
  final int currentPage;
  final int size;

  const EducationPostPageModel({
    required this.content,
    required this.totalCount,
    required this.totalPages,
    required this.currentPage,
    required this.size,
  });

  factory EducationPostPageModel.fromJson(Map<String, dynamic> json) {
    return EducationPostPageModel(
      content: (json['content'] as List<dynamic>)
          .map((item) => EducationPostModel.fromJson(item as Map<String, dynamic>))
          .toList(),
      totalCount: json['totalCount'] as int,
      totalPages: json['totalPages'] as int,
      currentPage: json['currentPage'] as int,
      size: json['size'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'content': content.map((post) => post.toJson()).toList(),
      'totalCount': totalCount,
      'totalPages': totalPages,
      'currentPage': currentPage,
      'size': size,
    };
  }

  EducationPostPage toEntity() {
    return EducationPostPage(
      content: content.map((model) => model.toEntity()).toList(),
      totalCount: totalCount,
      totalPages: totalPages,
      currentPage: currentPage,
      size: size,
    );
  }

  factory EducationPostPageModel.fromEntity(EducationPostPage entity) {
    return EducationPostPageModel(
      content: entity.content.map((post) => EducationPostModel.fromEntity(post)).toList(),
      totalCount: entity.totalCount,
      totalPages: entity.totalPages,
      currentPage: entity.currentPage,
      size: entity.size,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EducationPostPageModel &&
        _listEquals(other.content, content) &&
        other.totalCount == totalCount &&
        other.totalPages == totalPages &&
        other.currentPage == currentPage &&
        other.size == size;
  }

  @override
  int get hashCode {
    return Object.hash(
      Object.hashAll(content),
      totalCount,
      totalPages,
      currentPage,
      size,
    );
  }

  @override
  String toString() {
    return 'EducationPostPageModel(content: ${content.length} items, totalCount: $totalCount, currentPage: $currentPage/$totalPages)';
  }
}

/// List equality helper
bool _listEquals<T>(List<T> a, List<T> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
