import '../../domain/entities/claim_detail.dart';
import 'claim_photo_model.dart';

/// 클레임 상세 데이터 모델 (JSON 매핑)
class ClaimDetailModel {
  final int claimId;
  final String? accountName;
  final String? productName;
  final String? productCode;
  final String? dateType;
  final String? dateTypeLabel;
  final String? date;
  final String? categoryName;
  final String? subcategoryName;
  final String? defectDescription;
  final int? defectQuantity;
  final int? purchaseAmount;
  final String? purchaseMethodName;
  final String? requestTypeName;
  final String status;
  final String statusLabel;
  final DateTime createdAt;
  final List<ClaimPhotoModel> photos;

  const ClaimDetailModel({
    required this.claimId,
    this.accountName,
    this.productName,
    this.productCode,
    this.dateType,
    this.dateTypeLabel,
    this.date,
    this.categoryName,
    this.subcategoryName,
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

  factory ClaimDetailModel.fromJson(Map<String, dynamic> json) {
    return ClaimDetailModel(
      claimId: json['claim_id'] as int,
      accountName: json['account_name'] as String?,
      productName: json['product_name'] as String?,
      productCode: json['product_code'] as String?,
      dateType: json['date_type'] as String?,
      dateTypeLabel: json['date_type_label'] as String?,
      date: json['date'] as String?,
      categoryName: json['category_name'] as String?,
      subcategoryName: json['subcategory_name'] as String?,
      defectDescription: json['defect_description'] as String?,
      defectQuantity: json['defect_quantity'] as int?,
      purchaseAmount: json['purchase_amount'] as int?,
      purchaseMethodName: json['purchase_method_name'] as String?,
      requestTypeName: json['request_type_name'] as String?,
      status: json['status'] as String,
      statusLabel: json['status_label'] as String,
      createdAt: DateTime.parse(json['created_at'] as String),
      photos: (json['photos'] as List<dynamic>)
          .map((e) => ClaimPhotoModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  ClaimDetail toEntity() => ClaimDetail(
        claimId: claimId,
        accountName: accountName,
        productName: productName,
        productCode: productCode,
        dateType: dateType,
        dateTypeLabel: dateTypeLabel,
        date: date != null ? DateTime.parse(date!) : null,
        categoryName: categoryName,
        subcategoryName: subcategoryName,
        defectDescription: defectDescription,
        defectQuantity: defectQuantity,
        purchaseAmount: purchaseAmount,
        purchaseMethodName: purchaseMethodName,
        requestTypeName: requestTypeName,
        status: status,
        statusLabel: statusLabel,
        createdAt: createdAt,
        photos: photos.map((e) => e.toEntity()).toList(),
      );
}
