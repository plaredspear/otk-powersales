import '../../domain/entities/shelf_life_form.dart';

/// 유통기한 등록 API 요청 모델
///
/// POST /api/v1/shelf-life 요청 바디를 snake_case JSON으로 직렬화합니다.
class ShelfLifeRegisterRequest {
  final String accountCode;
  final String accountName;
  final String productCode;
  final String productName;
  final String expirationDate;
  final String alarmDate;
  final String description;

  const ShelfLifeRegisterRequest({
    required this.accountCode,
    required this.accountName,
    required this.productCode,
    required this.productName,
    required this.expirationDate,
    required this.alarmDate,
    this.description = '',
  });

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'account_code': accountCode,
      'account_name': accountName,
      'product_code': productCode,
      'product_name': productName,
      'expiration_date': expirationDate,
      'alarm_date': alarmDate,
      'description': description,
    };
  }

  /// Domain Form에서 생성
  factory ShelfLifeRegisterRequest.fromForm(ShelfLifeRegisterForm form) {
    return ShelfLifeRegisterRequest(
      accountCode: form.accountCode,
      accountName: form.accountName,
      productCode: form.productCode,
      productName: form.productName,
      expirationDate: form.expiryDate.toIso8601String().substring(0, 10),
      alarmDate: form.alertDate.toIso8601String().substring(0, 10),
      description: form.description,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeRegisterRequest) return false;
    return other.accountCode == accountCode &&
        other.accountName == accountName &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.expirationDate == expirationDate &&
        other.alarmDate == alarmDate &&
        other.description == description;
  }

  @override
  int get hashCode {
    return Object.hash(
      accountCode,
      accountName,
      productCode,
      productName,
      expirationDate,
      alarmDate,
      description,
    );
  }

  @override
  String toString() {
    return 'ShelfLifeRegisterRequest(accountCode: $accountCode, '
        'accountName: $accountName, productCode: $productCode, '
        'productName: $productName, expirationDate: $expirationDate, '
        'alarmDate: $alarmDate, description: $description)';
  }
}
