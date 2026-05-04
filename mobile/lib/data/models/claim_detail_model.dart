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
      claimId: json['claimId'] as int,
      accountName: json['accountName'] as String?,
      productName: json['productName'] as String?,
      productCode: json['productCode'] as String?,
      dateType: json['dateType'] as String?,
      dateTypeLabel: json['dateTypeLabel'] as String?,
      date: json['date'] as String?,
      categoryName: json['categoryName'] as String?,
      subcategoryName: json['subcategoryName'] as String?,
      defectDescription: json['defectDescription'] as String?,
      defectQuantity: json['defectQuantity'] as int?,
      purchaseAmount: json['purchaseAmount'] as int?,
      purchaseMethodName: json['purchaseMethodName'] as String?,
      requestTypeName: json['requestTypeName'] as String?,
      status: json['status'] as String,
      statusLabel: json['statusLabel'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
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
