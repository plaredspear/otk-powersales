import '../../domain/entities/claim_result.dart';

/// 클레임 등록 결과 Model
class ClaimRegisterResultModel {
  const ClaimRegisterResultModel({
    required this.id,
    required this.accountName,
    required this.accountId,
    required this.productName,
    required this.productCode,
    required this.createdAt,
  });

  final int id;
  final String accountName;
  final int accountId;
  final String productName;
  final String productCode;
  final String createdAt;

  /// JSON 역직렬화
  factory ClaimRegisterResultModel.fromJson(Map<String, dynamic> json) {
    return ClaimRegisterResultModel(
      id: json['id'] as int,
      accountName: json['accountName'] as String,
      accountId: json['accountId'] as int,
      productName: json['productName'] as String,
      productCode: json['productCode'] as String,
      createdAt: json['createdAt'] as String,
    );
  }

  /// Entity로 변환
  ClaimRegisterResult toEntity() {
    return ClaimRegisterResult(
      id: id,
      accountName: accountName,
      accountId: accountId,
      productName: productName,
      productCode: productCode,
      createdAt: DateTime.parse(createdAt),
    );
  }
}
