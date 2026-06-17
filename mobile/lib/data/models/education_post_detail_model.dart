import '../../domain/entities/education_category.dart';
import '../../domain/entities/education_post_detail.dart';

/// EducationImage API 모델 (DTO)
///
/// API 응답의 이미지 JSON을 Domain Entity로 변환한다.
class EducationImageModel {
  final int id;
  final String url;
  final int sortOrder;

  const EducationImageModel({
    required this.id,
    required this.url,
    required this.sortOrder,
  });

  factory EducationImageModel.fromJson(Map<String, dynamic> json) {
    return EducationImageModel(
      id: json['id'] as int,
      url: json['url'] as String,
      sortOrder: json['sortOrder'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'url': url,
      'sortOrder': sortOrder,
    };
  }

  EducationImage toEntity() {
    return EducationImage(
      id: id,
      url: url,
      sortOrder: sortOrder,
    );
  }

  factory EducationImageModel.fromEntity(EducationImage entity) {
    return EducationImageModel(
      id: entity.id,
      url: entity.url,
      sortOrder: entity.sortOrder,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EducationImageModel &&
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
    return 'EducationImageModel(id: $id, url: $url, sortOrder: $sortOrder)';
  }
}

/// EducationAttachment API 모델 (DTO)
///
/// API 응답의 첨부파일 JSON을 Domain Entity로 변환한다.
class EducationAttachmentModel {
  // 백엔드 EducationAttachmentResponse.id 는 fileKey(String) 이다.
  final String id;
  final String fileName;
  final String fileUrl;
  final String fileType;
  final int fileSize;

  const EducationAttachmentModel({
    required this.id,
    required this.fileName,
    required this.fileUrl,
    this.fileType = '',
    required this.fileSize,
  });

  factory EducationAttachmentModel.fromJson(Map<String, dynamic> json) {
    return EducationAttachmentModel(
      id: json['id'] as String,
      fileName: json['fileName'] as String,
      fileUrl: json['fileUrl'] as String,
      fileType: json['fileType'] as String? ?? '',
      fileSize: (json['fileSize'] as num).toInt(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'fileName': fileName,
      'fileUrl': fileUrl,
      'fileType': fileType,
      'fileSize': fileSize,
    };
  }

  EducationAttachment toEntity() {
    return EducationAttachment(
      id: id,
      fileName: fileName,
      fileUrl: fileUrl,
      fileType: fileType,
      fileSize: fileSize,
    );
  }

  factory EducationAttachmentModel.fromEntity(EducationAttachment entity) {
    return EducationAttachmentModel(
      id: entity.id,
      fileName: entity.fileName,
      fileUrl: entity.fileUrl,
      fileType: entity.fileType,
      fileSize: entity.fileSize,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EducationAttachmentModel &&
        other.id == id &&
        other.fileName == fileName &&
        other.fileUrl == fileUrl &&
        other.fileType == fileType &&
        other.fileSize == fileSize;
  }

  @override
  int get hashCode {
    return Object.hash(id, fileName, fileUrl, fileType, fileSize);
  }

  @override
  String toString() {
    return 'EducationAttachmentModel(id: $id, fileName: $fileName, fileType: $fileType, fileSize: $fileSize)';
  }
}

/// EducationPostDetail API 모델 (DTO)
///
/// API 응답의 게시물 상세 JSON을 Domain Entity로 변환한다.
class EducationPostDetailModel {
  final String id;
  final String category;
  final String categoryName;
  final String title;
  final String content;
  // 백엔드(EducationPostDetailResponse)는 createdAt 을 nullable 로 선언한다.
  final String? createdAt;
  final List<EducationImageModel> images;
  final List<EducationAttachmentModel> attachments;

  const EducationPostDetailModel({
    required this.id,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.content,
    required this.createdAt,
    required this.images,
    required this.attachments,
  });

  factory EducationPostDetailModel.fromJson(Map<String, dynamic> json) {
    return EducationPostDetailModel(
      id: json['id'] as String,
      category: json['category'] as String,
      categoryName: json['categoryName'] as String,
      title: json['title'] as String,
      content: json['content'] as String,
      createdAt: json['createdAt'] as String?,
      images: (json['images'] as List<dynamic>)
          .map((item) => EducationImageModel.fromJson(item as Map<String, dynamic>))
          .toList(),
      attachments: (json['attachments'] as List<dynamic>)
          .map((item) => EducationAttachmentModel.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category,
      'categoryName': categoryName,
      'title': title,
      'content': content,
      'createdAt': createdAt,
      'images': images.map((img) => img.toJson()).toList(),
      'attachments': attachments.map((att) => att.toJson()).toList(),
    };
  }

  EducationPostDetail toEntity() {
    return EducationPostDetail(
      id: id,
      category: EducationCategory.fromCode(category),
      categoryName: categoryName,
      title: title,
      content: content,
      // createdAt 이 null 인 경우(방어적) "미상"을 epoch 센티넬로 표현한다.
      createdAt: createdAt != null
          ? DateTime.parse(createdAt!)
          : DateTime.fromMillisecondsSinceEpoch(0),
      images: images.map((model) => model.toEntity()).toList(),
      attachments: attachments.map((model) => model.toEntity()).toList(),
    );
  }

  factory EducationPostDetailModel.fromEntity(EducationPostDetail entity) {
    return EducationPostDetailModel(
      id: entity.id,
      category: entity.category.code,
      categoryName: entity.categoryName,
      title: entity.title,
      content: entity.content,
      createdAt: entity.createdAt.toIso8601String(),
      images: entity.images.map((img) => EducationImageModel.fromEntity(img)).toList(),
      attachments: entity.attachments.map((att) => EducationAttachmentModel.fromEntity(att)).toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is EducationPostDetailModel &&
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
    return 'EducationPostDetailModel(id: $id, category: $category, title: $title, images: ${images.length}, attachments: ${attachments.length})';
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
