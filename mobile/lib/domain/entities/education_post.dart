/// 교육 게시물 목록 항목
///
/// 교육 목록 화면에 표시되는 게시물 정보.
class EducationPost {
  final int id;
  final String title;
  final DateTime createdAt;

  const EducationPost({
    required this.id,
    required this.title,
    required this.createdAt,
  });

  EducationPost copyWith({
    int? id,
    String? title,
    DateTime? createdAt,
  }) {
    return EducationPost(
      id: id ?? this.id,
      title: title ?? this.title,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  factory EducationPost.fromJson(Map<String, dynamic> json) {
    return EducationPost(
      id: json['id'] as int,
      title: json['title'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EducationPost &&
        other.id == id &&
        other.title == title &&
        other.createdAt == createdAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      title,
      createdAt,
    );
  }

  @override
  String toString() {
    return 'EducationPost(id: $id, title: $title, createdAt: $createdAt)';
  }
}

/// 교육 게시물 페이지네이션 응답
///
/// 서버에서 반환하는 페이지네이션 정보를 포함한 게시물 목록.
class EducationPostPage {
  final List<EducationPost> content;
  final int totalCount;
  final int totalPages;
  final int currentPage;
  final int size;

  const EducationPostPage({
    required this.content,
    required this.totalCount,
    required this.totalPages,
    required this.currentPage,
    required this.size,
  });

  /// 빈 페이지
  const EducationPostPage.empty()
      : content = const [],
        totalCount = 0,
        totalPages = 0,
        currentPage = 1,
        size = 10;

  /// 마지막 페이지 여부
  bool get isLastPage => currentPage >= totalPages;

  /// 첫 페이지 여부
  bool get isFirstPage => currentPage <= 1;

  EducationPostPage copyWith({
    List<EducationPost>? content,
    int? totalCount,
    int? totalPages,
    int? currentPage,
    int? size,
  }) {
    return EducationPostPage(
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

  factory EducationPostPage.fromJson(Map<String, dynamic> json) {
    return EducationPostPage(
      content: (json['content'] as List<dynamic>)
          .map((item) => EducationPost.fromJson(item as Map<String, dynamic>))
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
    return other is EducationPostPage &&
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
    return 'EducationPostPage(content: ${content.length} items, totalCount: $totalCount, currentPage: $currentPage/$totalPages)';
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
