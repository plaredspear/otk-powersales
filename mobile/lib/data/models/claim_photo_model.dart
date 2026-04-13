import '../../domain/entities/claim_photo.dart';

/// 클레임 사진 데이터 모델 (JSON 매핑)
class ClaimPhotoModel {
  final int photoId;
  final String photoType;
  final String photoTypeLabel;
  final String url;
  final String? originalFileName;

  const ClaimPhotoModel({
    required this.photoId,
    required this.photoType,
    required this.photoTypeLabel,
    required this.url,
    this.originalFileName,
  });

  factory ClaimPhotoModel.fromJson(Map<String, dynamic> json) {
    return ClaimPhotoModel(
      photoId: json['photo_id'] as int,
      photoType: json['photo_type'] as String,
      photoTypeLabel: json['photo_type_label'] as String,
      url: json['url'] as String,
      originalFileName: json['original_file_name'] as String?,
    );
  }

  ClaimPhoto toEntity() => ClaimPhoto(
        photoId: photoId,
        photoType: photoType,
        photoTypeLabel: photoTypeLabel,
        url: url,
        originalFileName: originalFileName,
      );
}
