import '../../domain/entities/claim_result.dart';

/// 클레임 등록 결과 Model
class ClaimRegisterResultModel {
  const ClaimRegisterResultModel({
    required this.id,
    required this.storeName,
    required this.storeId,
    required this.productName,
    required this.productCode,
    required this.createdAt,
  });

  final int id;
  final String storeName;
  final int storeId;
  final String productName;
  final String productCode;
  final String createdAt;

  /// JSON 역직렬화
  factory ClaimRegisterResultModel.fromJson(Map<String, dynamic> json) {
    return ClaimRegisterResultModel(
      id: json['id'] as int,
      storeName: json['store_name'] as String,
      storeId: json['store_id'] as int,
      productName: json['product_name'] as String,
      productCode: json['product_code'] as String,
      createdAt: json['created_at'] as String,
    );
  }

  /// Entity로 변환
  ClaimRegisterResult toEntity() {
    return ClaimRegisterResult(
      id: id,
      storeName: storeName,
      storeId: storeId,
      productName: productName,
      productCode: productCode,
      createdAt: DateTime.parse(createdAt),
    );
  }
}
