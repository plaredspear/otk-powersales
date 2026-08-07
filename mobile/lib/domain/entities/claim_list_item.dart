/// 클레임 목록 아이템 도메인 엔티티
class ClaimListItem {
  final int claimId;
  final String? claimNo;
  final String? accountName;
  final String? productName;
  final String? productCode;
  final String? categoryLabel;
  final String? subcategoryLabel;
  final int? defectQuantity;
  final String? defectDescription;
  /// 화면 상태 뱃지 문구 — 코스모스 조치상태(미회신 시 '미확인').
  /// 전송상태(SF DKRetail__Status__c) 는 신규 시스템에서 전이되지 않아 표시 축으로 쓰지 않는다.
  final String actionStatusLabel;
  final DateTime? date;
  final DateTime createdAt;

  const ClaimListItem({
    required this.claimId,
    this.claimNo,
    this.accountName,
    this.productName,
    this.productCode,
    this.categoryLabel,
    this.subcategoryLabel,
    this.defectQuantity,
    this.defectDescription,
    required this.actionStatusLabel,
    this.date,
    required this.createdAt,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ClaimListItem && claimId == other.claimId;

  @override
  int get hashCode => claimId.hashCode;
}
