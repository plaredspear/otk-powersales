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
      // 백엔드 ClaimCreateResponse 의 account/product 필드는 nullable 이므로 방어적으로 파싱한다.
      id: (json['id'] as num).toInt(),
      accountName: json['accountName'] as String? ?? '',
      accountId: (json['accountId'] as num?)?.toInt() ?? 0,
      productName: json['productName'] as String? ?? '',
      productCode: json['productCode'] as String? ?? '',
      createdAt: json['createdAt'] as String? ?? '',
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
      createdAt: DateTime.tryParse(createdAt) ??
          DateTime.fromMillisecondsSinceEpoch(0),
    );
  }
}
