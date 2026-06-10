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
  final String status;
  final String statusLabel;
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
    required this.status,
    required this.statusLabel,
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
      status: json['status'] as String,
      statusLabel: json['statusLabel'] as String,
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
        status: status,
        statusLabel: statusLabel,
        date: date != null ? DateTime.tryParse(date!) : null,
        createdAt: createdAt,
      );
}
