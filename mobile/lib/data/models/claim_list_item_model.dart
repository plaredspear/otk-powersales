import '../../domain/entities/claim_list_item.dart';

/// 클레임 목록 아이템 데이터 모델 (JSON 매핑)
class ClaimListItemModel {
  final int claimId;
  final String? accountName;
  final String? productName;
  final String? productCode;
  final String? categoryName;
  final String? subcategoryName;
  final int? defectQuantity;
  final String status;
  final String statusLabel;
  final DateTime createdAt;

  const ClaimListItemModel({
    required this.claimId,
    this.accountName,
    this.productName,
    this.productCode,
    this.categoryName,
    this.subcategoryName,
    this.defectQuantity,
    required this.status,
    required this.statusLabel,
    required this.createdAt,
  });

  factory ClaimListItemModel.fromJson(Map<String, dynamic> json) {
    return ClaimListItemModel(
      claimId: json['claim_id'] as int,
      accountName: json['account_name'] as String?,
      productName: json['product_name'] as String?,
      productCode: json['product_code'] as String?,
      categoryName: json['category_name'] as String?,
      subcategoryName: json['subcategory_name'] as String?,
      defectQuantity: json['defect_quantity'] as int?,
      status: json['status'] as String,
      statusLabel: json['status_label'] as String,
      createdAt: DateTime.parse(json['created_at'] as String),
    );
  }

  ClaimListItem toEntity() => ClaimListItem(
        claimId: claimId,
        accountName: accountName,
        productName: productName,
        productCode: productCode,
        categoryName: categoryName,
        subcategoryName: subcategoryName,
        defectQuantity: defectQuantity,
        status: status,
        statusLabel: statusLabel,
        createdAt: createdAt,
      );
}
