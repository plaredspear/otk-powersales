import '../../domain/entities/claim_photo.dart';

/// 클레임 사진 데이터 모델 (JSON 매핑)
class ClaimPhotoModel {
  final int photoId;
  final String? photoType;
  final String? photoTypeLabel;
  final String url;
  final String? originalFileName;

  const ClaimPhotoModel({
    required this.photoId,
    this.photoType,
    this.photoTypeLabel,
    required this.url,
    this.originalFileName,
  });

  factory ClaimPhotoModel.fromJson(Map<String, dynamic> json) {
    return ClaimPhotoModel(
      photoId: json['photoId'] as int,
      photoType: json['photoType'] as String?,
      photoTypeLabel: json['photoTypeLabel'] as String?,
      url: json['url'] as String,
      originalFileName: json['originalFileName'] as String?,
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
