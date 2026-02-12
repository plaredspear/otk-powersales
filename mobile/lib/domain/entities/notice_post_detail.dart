import 'notice_category.dart';

/// 공지사항 이미지 엔티티
///
/// 공지사항 상세 화면에 표시되는 첨부 이미지 정보를 담는 도메인 엔티티입니다.
class NoticeImage {
  /// 이미지 ID
  final int id;

  /// 이미지 URL
  final String url;

  /// 정렬 순서
  final int sortOrder;

  const NoticeImage({
    required this.id,
    required this.url,
    required this.sortOrder,
  });

  NoticeImage copyWith({
    int? id,
    String? url,
    int? sortOrder,
  }) {
    return NoticeImage(
      id: id ?? this.id,
      url: url ?? this.url,
      sortOrder: sortOrder ?? this.sortOrder,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'url': url,
      'sortOrder': sortOrder,
    };
  }

  factory NoticeImage.fromJson(Map<String, dynamic> json) {
    return NoticeImage(
      id: json['id'] as int,
      url: json['url'] as String,
      sortOrder: json['sortOrder'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is NoticeImage &&
        other.id == id &&
        other.url == url &&
        other.sortOrder == sortOrder;
  }

  @override
  int get hashCode {
    return Object.hash(id, url, sortOrder);
  }

  @override
  String toString() {
    return 'NoticeImage(id: $id, url: $url, sortOrder: $sortOrder)';
  }
}

/// 공지사항 상세 엔티티
///
/// 공지사항 상세 화면에 표시되는 전체 정보를 담는 도메인 엔티티입니다.
class NoticePostDetail {
  /// 공지사항 ID
  final int id;

  /// 분류 (회사공지/지점공지)
  final NoticeCategory category;

  /// 분류 표시명
  final String categoryName;

  /// 게시물 제목
  final String title;

  /// 본문 내용
  final String content;

  /// 등록일
  final DateTime createdAt;

  /// 이미지 목록 (빈 리스트 가능)
  final List<NoticeImage> images;

  const NoticePostDetail({
    required this.id,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.content,
    required this.createdAt,
    required this.images,
  });

  NoticePostDetail copyWith({
    int? id,
    NoticeCategory? category,
    String? categoryName,
    String? title,
    String? content,
    DateTime? createdAt,
    List<NoticeImage>? images,
  }) {
    return NoticePostDetail(
      id: id ?? this.id,
      category: category ?? this.category,
      categoryName: categoryName ?? this.categoryName,
      title: title ?? this.title,
      content: content ?? this.content,
      createdAt: createdAt ?? this.createdAt,
      images: images ?? this.images,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category.code,
      'categoryName': categoryName,
      'title': title,
      'content': content,
      'createdAt': createdAt.toIso8601String(),
      'images': images.map((img) => img.toJson()).toList(),
    };
  }

  factory NoticePostDetail.fromJson(Map<String, dynamic> json) {
    return NoticePostDetail(
      id: json['id'] as int,
      category: NoticeCategory.fromCode(json['category'] as String),
      categoryName: json['categoryName'] as String,
      title: json['title'] as String,
      content: json['content'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      images: (json['images'] as List)
          .map((item) => NoticeImage.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is NoticePostDetail &&
        other.id == id &&
        other.category == category &&
        other.categoryName == categoryName &&
        other.title == title &&
        other.content == content &&
        other.createdAt == createdAt &&
        _listEquals(other.images, images);
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      category,
      categoryName,
      title,
      content,
      createdAt,
      Object.hashAll(images),
    );
  }

  @override
  String toString() {
    return 'NoticePostDetail(id: $id, category: $category, categoryName: $categoryName, '
        'title: $title, content: $content, createdAt: $createdAt, images: $images)';
  }

  /// List equality helper
  bool _listEquals<T>(List<T>? a, List<T>? b) {
    if (a == null) return b == null;
    if (b == null || a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }
}
