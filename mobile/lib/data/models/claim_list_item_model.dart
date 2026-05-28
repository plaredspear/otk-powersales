import '../../domain/entities/claim_list_item.dart';

/// 클레임 목록 아이템 데이터 모델 (JSON 매핑)
class ClaimListItemModel {
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

  const ClaimListItemModel({
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

  factory ClaimListItemModel.fromJson(Map<String, dynamic> json) {
    return ClaimListItemModel(
      claimId: json['claimId'] as int,
      accountName: json['accountName'] as String?,
      productName: json['productName'] as String?,
      productCode: json['productCode'] as String?,
      categoryLabel: json['categoryLabel'] as String?,
      subcategoryLabel: json['subcategoryLabel'] as String?,
      defectQuantity: json['defectQuantity'] as int?,
      status: json['status'] as String,
      statusLabel: json['statusLabel'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  ClaimListItem toEntity() => ClaimListItem(
        claimId: claimId,
        accountName: accountName,
        productName: productName,
        productCode: productCode,
        categoryLabel: categoryLabel,
        subcategoryLabel: subcategoryLabel,
        defectQuantity: defectQuantity,
        status: status,
        statusLabel: statusLabel,
        createdAt: createdAt,
      );
}
