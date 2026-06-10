/// 클레임 사진 도메인 엔티티
class ClaimPhoto {
  final int photoId;
  final String? photoType;
  final String? photoTypeLabel;
  final String url;
  final String? originalFileName;

  const ClaimPhoto({
    required this.photoId,
    this.photoType,
    this.photoTypeLabel,
    required this.url,
    this.originalFileName,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ClaimPhoto && photoId == other.photoId;

  @override
  int get hashCode => photoId.hashCode;
}
