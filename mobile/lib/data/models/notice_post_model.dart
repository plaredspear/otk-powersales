import '../../domain/entities/notice_category.dart';
import '../../domain/entities/notice_post.dart';

/// 공지사항 목록 항목 Model
class NoticePostModel {
  const NoticePostModel({
    required this.id,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.createdAt,
  });

  final int id;
  final String category;
  final String categoryName;
  final String title;
  final String createdAt;

  /// JSON 역직렬화
  factory NoticePostModel.fromJson(Map<String, dynamic> json) {
    return NoticePostModel(
      id: json['id'] as int,
      category: json['category'] as String,
      categoryName: json['categoryName'] as String,
      title: json['title'] as String,
      createdAt: json['createdAt'] as String,
    );
  }

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category,
      'categoryName': categoryName,
      'title': title,
      'createdAt': createdAt,
    };
  }

  /// Entity로 변환
  NoticePost toEntity() {
    return NoticePost(
      id: id,
      category: NoticeCategory.fromCode(category),
      categoryName: categoryName,
      title: title,
      createdAt: DateTime.parse(createdAt),
    );
  }

  /// Entity에서 변환
  factory NoticePostModel.fromEntity(NoticePost entity) {
    return NoticePostModel(
      id: entity.id,
      category: entity.category.code,
      categoryName: entity.categoryName,
      title: entity.title,
      createdAt: entity.createdAt.toIso8601String(),
    );
  }
}

/// 공지사항 페이지네이션 응답 Model
class NoticePostPageModel {
  const NoticePostPageModel({
    required this.content,
    required this.totalCount,
    required this.totalPages,
    required this.currentPage,
    required this.size,
  });

  final List<NoticePostModel> content;
  final int totalCount;
  final int totalPages;
  final int currentPage;
  final int size;

  /// JSON 역직렬화 (중첩 구조 포함)
  factory NoticePostPageModel.fromJson(Map<String, dynamic> json) {
    return NoticePostPageModel(
      content: (json['content'] as List)
          .map((item) => NoticePostModel.fromJson(item as Map<String, dynamic>))
          .toList(),
      totalCount: json['totalCount'] as int,
      totalPages: json['totalPages'] as int,
      currentPage: json['currentPage'] as int,
      size: json['size'] as int,
    );
  }

  /// JSON 직렬화 (중첩 구조 포함)
  Map<String, dynamic> toJson() {
    return {
      'content': content.map((post) => post.toJson()).toList(),
      'totalCount': totalCount,
      'totalPages': totalPages,
      'currentPage': currentPage,
      'size': size,
    };
  }

  /// Entity로 변환
  NoticePostPage toEntity() {
    return NoticePostPage(
      content: content.map((post) => post.toEntity()).toList(),
      totalCount: totalCount,
      totalPages: totalPages,
      currentPage: currentPage,
      size: size,
    );
  }

  /// Entity에서 변환
  factory NoticePostPageModel.fromEntity(NoticePostPage entity) {
    return NoticePostPageModel(
      content: entity.content
          .map((post) => NoticePostModel.fromEntity(post))
          .toList(),
      totalCount: entity.totalCount,
      totalPages: entity.totalPages,
      currentPage: entity.currentPage,
      size: entity.size,
    );
  }
}
