import '../../domain/entities/shelf_life_form.dart';

/// 유통기한 등록 API 요청 모델
///
/// POST /api/v1/shelf-life 요청 바디를 snake_case JSON으로 직렬화합니다.
class ShelfLifeRegisterRequest {
  final int storeId;
  final String productCode;
  final String expiryDate;
  final String alertDate;
  final String description;

  const ShelfLifeRegisterRequest({
    required this.storeId,
    required this.productCode,
    required this.expiryDate,
    required this.alertDate,
    this.description = '',
  });

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'store_id': storeId,
      'product_code': productCode,
      'expiry_date': expiryDate,
      'alert_date': alertDate,
      'description': description,
    };
  }

  /// Domain Form에서 생성
  factory ShelfLifeRegisterRequest.fromForm(ShelfLifeRegisterForm form) {
    return ShelfLifeRegisterRequest(
      storeId: form.storeId,
      productCode: form.productCode,
      expiryDate: form.expiryDate.toIso8601String().substring(0, 10),
      alertDate: form.alertDate.toIso8601String().substring(0, 10),
      description: form.description,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeRegisterRequest) return false;
    return other.storeId == storeId &&
        other.productCode == productCode &&
        other.expiryDate == expiryDate &&
        other.alertDate == alertDate &&
        other.description == description;
  }

  @override
  int get hashCode {
    return Object.hash(storeId, productCode, expiryDate, alertDate, description);
  }

  @override
  String toString() {
    return 'ShelfLifeRegisterRequest(storeId: $storeId, '
        'productCode: $productCode, expiryDate: $expiryDate, '
        'alertDate: $alertDate, description: $description)';
  }
}
