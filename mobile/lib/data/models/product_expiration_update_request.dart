import '../../domain/entities/product_expiration_form.dart';

/// 유통기한 수정 API 요청 모델
///
/// PUT /api/v1/product-expiration/{seq} 요청 바디를 snake_case JSON으로 직렬화합니다.
class ProductExpirationUpdateRequest {
  final String expirationDate;
  final String alarmDate;
  final String description;

  const ProductExpirationUpdateRequest({
    required this.expirationDate,
    required this.alarmDate,
    this.description = '',
  });

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'expiration_date': expirationDate,
      'alarm_date': alarmDate,
      'description': description,
    };
  }

  /// Domain Form에서 생성
  factory ProductExpirationUpdateRequest.fromForm(ProductExpirationUpdateForm form) {
    return ProductExpirationUpdateRequest(
      expirationDate: form.expiryDate.toIso8601String().substring(0, 10),
      alarmDate: form.alertDate.toIso8601String().substring(0, 10),
      description: form.description,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ProductExpirationUpdateRequest) return false;
    return other.expirationDate == expirationDate &&
        other.alarmDate == alarmDate &&
        other.description == description;
  }

  @override
  int get hashCode => Object.hash(expirationDate, alarmDate, description);

  @override
  String toString() {
    return 'ProductExpirationUpdateRequest(expirationDate: $expirationDate, '
        'alarmDate: $alarmDate, description: $description)';
  }
}
