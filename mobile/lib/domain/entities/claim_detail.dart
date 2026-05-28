import 'claim_photo.dart';

/// 클레임 상세 도메인 엔티티
class ClaimDetail {
  final int claimId;
  final String? accountName;
  final String? productName;
  final String? productCode;
  final String? dateType;
  final String? dateTypeLabel;
  final DateTime? date;
  final String? categoryLabel;
  final String? subcategoryLabel;
  final String? defectDescription;
  final int? defectQuantity;
  final int? purchaseAmount;
  final String? purchaseMethodName;
  final String? requestTypeName;
  final String status;
  final String statusLabel;
  final DateTime createdAt;
  final List<ClaimPhoto> photos;

  const ClaimDetail({
    required this.claimId,
    this.accountName,
    this.productName,
    this.productCode,
    this.dateType,
    this.dateTypeLabel,
    this.date,
    this.categoryLabel,
    this.subcategoryLabel,
    this.defectDescription,
    this.defectQuantity,
    this.purchaseAmount,
    this.purchaseMethodName,
    this.requestTypeName,
    required this.status,
    required this.statusLabel,
    required this.createdAt,
    required this.photos,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ClaimDetail && claimId == other.claimId;

  @override
  int get hashCode => claimId.hashCode;
}
