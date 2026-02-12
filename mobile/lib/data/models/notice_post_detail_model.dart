import '../../domain/entities/notice_category.dart';
import '../../domain/entities/notice_post_detail.dart';

/// 공지사항 이미지 Model
class NoticeImageModel {
  const NoticeImageModel({
    required this.id,
    required this.url,
    required this.sortOrder,
  });

  final int id;
  final String url;
  final int sortOrder;

  /// JSON 역직렬화
  factory NoticeImageModel.fromJson(Map<String, dynamic> json) {
    return NoticeImageModel(
      id: json['id'] as int,
      url: json['url'] as String,
      sortOrder: json['sortOrder'] as int,
    );
  }

  /// JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'url': url,
      'sortOrder': sortOrder,
    };
  }

  /// Entity로 변환
  NoticeImage toEntity() {
    return NoticeImage(
      id: id,
      url: url,
      sortOrder: sortOrder,
    );
  }

  /// Entity에서 변환
  factory NoticeImageModel.fromEntity(NoticeImage entity) {
    return NoticeImageModel(
      id: entity.id,
      url: entity.url,
      sortOrder: entity.sortOrder,
    );
  }
}

/// 공지사항 상세 Model
class NoticePostDetailModel {
  const NoticePostDetailModel({
    required this.id,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.content,
    required this.createdAt,
    required this.images,
  });

  final int id;
  final String category;
  final String categoryName;
  final String title;
  final String content;
  final String createdAt;
  final List<NoticeImageModel> images;

  /// JSON 역직렬화 (중첩 구조 포함)
  factory NoticePostDetailModel.fromJson(Map<String, dynamic> json) {
    return NoticePostDetailModel(
      id: json['id'] as int,
      category: json['category'] as String,
      categoryName: json['categoryName'] as String,
      title: json['title'] as String,
      content: json['content'] as String,
      createdAt: json['createdAt'] as String,
      images: (json['images'] as List)
          .map((item) => NoticeImageModel.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }

  /// JSON 직렬화 (중첩 구조 포함)
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category,
      'categoryName': categoryName,
      'title': title,
      'content': content,
      'createdAt': createdAt,
      'images': images.map((img) => img.toJson()).toList(),
    };
  }

  /// Entity로 변환
  NoticePostDetail toEntity() {
    return NoticePostDetail(
      id: id,
      category: NoticeCategory.fromCode(category),
      categoryName: categoryName,
      title: title,
      content: content,
      createdAt: DateTime.parse(createdAt),
      images: images.map((img) => img.toEntity()).toList(),
    );
  }

  /// Entity에서 변환
  factory NoticePostDetailModel.fromEntity(NoticePostDetail entity) {
    return NoticePostDetailModel(
      id: entity.id,
      category: entity.category.code,
      categoryName: entity.categoryName,
      title: entity.title,
      content: entity.content,
      createdAt: entity.createdAt.toIso8601String(),
      images: entity.images
          .map((img) => NoticeImageModel.fromEntity(img))
          .toList(),
    );
  }
}
