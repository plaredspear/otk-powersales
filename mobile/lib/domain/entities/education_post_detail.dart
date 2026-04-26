import 'education_category.dart';

/// 교육 게시물 상세
///
/// 교육 상세 화면에 표시되는 게시물의 전체 정보.
/// 본문 내용, 이미지 목록, 첨부파일 목록을 포함한다.
class EducationPostDetail {
  final int id;
  final EducationCategory category;
  final String categoryName;
  final String title;
  final String content;
  final DateTime createdAt;
  final List<EducationImage> images;
  final List<EducationAttachment> attachments;

  const EducationPostDetail({
    required this.id,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.content,
    required this.createdAt,
    required this.images,
    required this.attachments,
  });

  /// 이미지 존재 여부
  bool get hasImages => images.isNotEmpty;

  /// 첨부파일 존재 여부
  bool get hasAttachments => attachments.isNotEmpty;

  EducationPostDetail copyWith({
    int? id,
    EducationCategory? category,
    String? categoryName,
    String? title,
    String? content,
    DateTime? createdAt,
    List<EducationImage>? images,
    List<EducationAttachment>? attachments,
  }) {
    return EducationPostDetail(
      id: id ?? this.id,
      category: category ?? this.category,
      categoryName: categoryName ?? this.categoryName,
      title: title ?? this.title,
      content: content ?? this.content,
      createdAt: createdAt ?? this.createdAt,
      images: images ?? this.images,
      attachments: attachments ?? this.attachments,
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
      'attachments': attachments.map((att) => att.toJson()).toList(),
    };
  }

  factory EducationPostDetail.fromJson(Map<String, dynamic> json) {
    return EducationPostDetail(
      id: json['id'] as int,
      category: EducationCategory.fromCode(json['category'] as String),
      categoryName: json['categoryName'] as String,
      title: json['title'] as String,
      content: json['content'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      images: (json['images'] as List<dynamic>)
          .map((item) => EducationImage.fromJson(item as Map<String, dynamic>))
          .toList(),
      attachments: (json['attachments'] as List<dynamic>)
          .map((item) => EducationAttachment.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EducationPostDetail &&
        other.id == id &&
        other.category == category &&
        other.categoryName == categoryName &&
        other.title == title &&
        other.content == content &&
        other.createdAt == createdAt &&
        _listEquals(other.images, images) &&
        _listEquals(other.attachments, attachments);
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
      Object.hashAll(attachments),
    );
  }

  @override
  String toString() {
    return 'EducationPostDetail(id: $id, category: ${category.displayName}, title: $title, images: ${images.length}, attachments: ${attachments.length})';
  }
}

/// 교육 이미지
///
/// 교육 게시물에 첨부된 이미지 정보.
class EducationImage {
  final int id;
  final String url;
  final int sortOrder;

  const EducationImage({
    required this.id,
    required this.url,
    required this.sortOrder,
  });

  EducationImage copyWith({
    int? id,
    String? url,
    int? sortOrder,
  }) {
    return EducationImage(
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

  factory EducationImage.fromJson(Map<String, dynamic> json) {
    return EducationImage(
      id: json['id'] as int,
      url: json['url'] as String,
      sortOrder: json['sortOrder'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EducationImage &&
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
    return 'EducationImage(id: $id, url: $url, sortOrder: $sortOrder)';
  }
}

/// 교육 첨부파일
///
/// 교육 게시물에 첨부된 파일 정보.
class EducationAttachment {
  final int id;
  final String fileName;
  final String fileUrl;
  final int fileSize;

  const EducationAttachment({
    required this.id,
    required this.fileName,
    required this.fileUrl,
    required this.fileSize,
  });

  /// 파일 크기를 사람이 읽을 수 있는 형식으로 변환 (KB, MB)
  String get fileSizeFormatted {
    if (fileSize < 1024) {
      return '$fileSize B';
    } else if (fileSize < 1024 * 1024) {
      final kb = fileSize / 1024;
      return '${kb.toStringAsFixed(1)} KB';
    } else {
      final mb = fileSize / (1024 * 1024);
      return '${mb.toStringAsFixed(1)} MB';
    }
  }

  EducationAttachment copyWith({
    int? id,
    String? fileName,
    String? fileUrl,
    int? fileSize,
  }) {
    return EducationAttachment(
      id: id ?? this.id,
      fileName: fileName ?? this.fileName,
      fileUrl: fileUrl ?? this.fileUrl,
      fileSize: fileSize ?? this.fileSize,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'fileName': fileName,
      'fileUrl': fileUrl,
      'fileSize': fileSize,
    };
  }

  factory EducationAttachment.fromJson(Map<String, dynamic> json) {
    return EducationAttachment(
      id: json['id'] as int,
      fileName: json['fileName'] as String,
      fileUrl: json['fileUrl'] as String,
      fileSize: json['fileSize'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EducationAttachment &&
        other.id == id &&
        other.fileName == fileName &&
        other.fileUrl == fileUrl &&
        other.fileSize == fileSize;
  }

  @override
  int get hashCode {
    return Object.hash(id, fileName, fileUrl, fileSize);
  }

  @override
  String toString() {
    return 'EducationAttachment(id: $id, fileName: $fileName, fileSize: $fileSizeFormatted)';
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
