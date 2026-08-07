import '../../domain/entities/claim_list_item.dart';

/// 클레임 목록 아이템 데이터 모델 (JSON 매핑)
class ClaimListItemModel {
  final int claimId;
  final String? claimNo;
  final String? accountName;
  final String? productName;
  final String? productCode;
  final String? categoryLabel;
  final String? subcategoryLabel;
  final num? defectQuantity;
  final String? defectDescription;
  /// 코스모스 조치상태 표시 문구 (서버가 미회신이면 '미확인' 을 채워 내려준다).
  final String actionStatusLabel;
  final String? date;
  final DateTime createdAt;

  const ClaimListItemModel({
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

  factory ClaimListItemModel.fromJson(Map<String, dynamic> json) {
    return ClaimListItemModel(
      claimId: json['claimId'] as int,
      claimNo: json['claimNo'] as String?,
      accountName: json['accountName'] as String?,
      productName: json['productName'] as String?,
      productCode: json['productCode'] as String?,
      categoryLabel: json['categoryLabel'] as String?,
      subcategoryLabel: json['subcategoryLabel'] as String?,
      defectQuantity: json['defectQuantity'] as num?,
      defectDescription: json['defectDescription'] as String?,
      // 구버전 서버 응답 대비 fallback — 서버가 채워 보내는 것이 정상 경로.
      actionStatusLabel: json['actionStatusLabel'] as String? ?? '미확인',
      date: json['date'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  ClaimListItem toEntity() => ClaimListItem(
        claimId: claimId,
        claimNo: claimNo,
        accountName: accountName,
        productName: productName,
        productCode: productCode,
        categoryLabel: categoryLabel,
        subcategoryLabel: subcategoryLabel,
        defectQuantity: defectQuantity?.toInt(),
        defectDescription: defectDescription,
        actionStatusLabel: actionStatusLabel,
        date: date != null ? DateTime.tryParse(date!) : null,
        createdAt: createdAt,
      );
}
