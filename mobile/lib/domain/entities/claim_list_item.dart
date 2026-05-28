/// 클레임 목록 아이템 도메인 엔티티
class ClaimListItem {
  final int claimId;
  final String? accountName;
  final String? productName;
  final String? productCode;
  final String? categoryLabel;
  final String? subcategoryLabel;
  final int? defectQuantity;
  final String status;
  final String statusLabel;
  final DateTime createdAt;

  const ClaimListItem({
    required this.claimId,
    this.accountName,
    this.productName,
    this.productCode,
    this.categoryLabel,
    this.subcategoryLabel,
    this.defectQuantity,
    required this.status,
    required this.statusLabel,
    required this.createdAt,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ClaimListItem && claimId == other.claimId;

  @override
  int get hashCode => claimId.hashCode;
}
