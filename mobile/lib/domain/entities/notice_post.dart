import 'notice_category.dart';

/// 공지사항 목록 항목 엔티티
///
/// 공지사항 목록 화면에 표시되는 개별 공지사항 정보를 담는 도메인 엔티티입니다.
class NoticePost {
  /// 공지사항 ID
  final int id;

  /// 분류 (회사공지/지점공지)
  final NoticeCategory category;

  /// 분류 표시명
  final String categoryName;

  /// 게시물 제목
  final String title;

  /// 등록일
  final DateTime createdAt;

  const NoticePost({
    required this.id,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.createdAt,
  });

  NoticePost copyWith({
    int? id,
    NoticeCategory? category,
    String? categoryName,
    String? title,
    DateTime? createdAt,
  }) {
    return NoticePost(
      id: id ?? this.id,
      category: category ?? this.category,
      categoryName: categoryName ?? this.categoryName,
      title: title ?? this.title,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category.code,
      'categoryName': categoryName,
      'title': title,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  factory NoticePost.fromJson(Map<String, dynamic> json) {
    return NoticePost(
      id: json['id'] as int,
      category: NoticeCategory.fromCode(json['category'] as String),
      categoryName: json['categoryName'] as String,
      title: json['title'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is NoticePost &&
        other.id == id &&
        other.category == category &&
        other.categoryName == categoryName &&
        other.title == title &&
        other.createdAt == createdAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      category,
      categoryName,
      title,
      createdAt,
    );
  }

  @override
  String toString() {
    return 'NoticePost(id: $id, category: $category, categoryName: $categoryName, '
        'title: $title, createdAt: $createdAt)';
  }
}

/// 공지사항 페이지네이션 응답 엔티티
///
/// 페이지네이션된 공지사항 목록 조회 결과를 담는 도메인 엔티티입니다.
class NoticePostPage {
  /// 공지사항 목록
  final List<NoticePost> content;

  /// 전체 건수
  final int totalCount;

  /// 전체 페이지 수
  final int totalPages;

  /// 현재 페이지 (1부터 시작)
  final int currentPage;

  /// 페이지 크기
  final int size;

  const NoticePostPage({
    required this.content,
    required this.totalCount,
    required this.totalPages,
    required this.currentPage,
    required this.size,
  });

  NoticePostPage copyWith({
    List<NoticePost>? content,
    int? totalCount,
    int? totalPages,
    int? currentPage,
    int? size,
  }) {
    return NoticePostPage(
      content: content ?? this.content,
      totalCount: totalCount ?? this.totalCount,
      totalPages: totalPages ?? this.totalPages,
      currentPage: currentPage ?? this.currentPage,
      size: size ?? this.size,
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

  factory NoticePostPage.fromJson(Map<String, dynamic> json) {
    return NoticePostPage(
      content: (json['content'] as List)
          .map((item) => NoticePost.fromJson(item as Map<String, dynamic>))
          .toList(),
      totalCount: json['totalCount'] as int,
      totalPages: json['totalPages'] as int,
      currentPage: json['currentPage'] as int,
      size: json['size'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is NoticePostPage &&
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
    return 'NoticePostPage(content: $content, totalCount: $totalCount, '
        'totalPages: $totalPages, currentPage: $currentPage, size: $size)';
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
