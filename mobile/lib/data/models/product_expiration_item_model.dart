import '../../domain/entities/product_expiration_item.dart';

/// 유통기한 항목 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 ProductExpirationItem 엔티티로 변환합니다.
class ProductExpirationItemModel {
  final int seq;
  final String productCode;
  final String productName;
  final String accountCode;
  final String accountName;
  final String expirationDate;
  final String alarmDate;
  final int dDay;
  final String description;
  final bool isExpired;

  const ProductExpirationItemModel({
    required this.seq,
    required this.productCode,
    required this.productName,
    required this.accountCode,
    required this.accountName,
    required this.expirationDate,
    required this.alarmDate,
    required this.dDay,
    this.description = '',
    required this.isExpired,
  });

  /// snake_case JSON에서 파싱
  factory ProductExpirationItemModel.fromJson(Map<String, dynamic> json) {
    return ProductExpirationItemModel(
      seq: json['seq'] as int,
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      accountCode: json['accountCode'] as String,
      accountName: json['accountName'] as String,
      expirationDate: json['expirationDate'] as String,
      alarmDate: json['alarmDate'] as String,
      dDay: json['dDay'] as int,
      description: json['description'] as String? ?? '',
      isExpired: json['isExpired'] as bool,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'seq': seq,
      'productCode': productCode,
      'productName': productName,
      'accountCode': accountCode,
      'accountName': accountName,
      'expirationDate': expirationDate,
      'alarmDate': alarmDate,
      'dDay': dDay,
      'description': description,
      'isExpired': isExpired,
    };
  }

  /// Domain Entity로 변환
  ProductExpirationItem toEntity() {
    return ProductExpirationItem(
      seq: seq,
      productCode: productCode,
      productName: productName,
      accountCode: accountCode,
      accountName: accountName,
      expiryDate: DateTime.parse(expirationDate),
      alertDate: DateTime.parse(alarmDate),
      dDay: dDay,
      description: description,
      isExpired: isExpired,
    );
  }

  /// Domain Entity에서 생성
  factory ProductExpirationItemModel.fromEntity(ProductExpirationItem entity) {
    return ProductExpirationItemModel(
      seq: entity.seq,
      productCode: entity.productCode,
      productName: entity.productName,
      accountCode: entity.accountCode,
      accountName: entity.accountName,
      expirationDate: entity.expiryDate.toIso8601String().substring(0, 10),
      alarmDate: entity.alertDate.toIso8601String().substring(0, 10),
      dDay: entity.dDay,
      description: entity.description,
      isExpired: entity.isExpired,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ProductExpirationItemModel) return false;
    return other.seq == seq &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.accountCode == accountCode &&
        other.accountName == accountName &&
        other.expirationDate == expirationDate &&
        other.alarmDate == alarmDate &&
        other.dDay == dDay &&
        other.description == description &&
        other.isExpired == isExpired;
  }

  @override
  int get hashCode {
    return Object.hash(
      seq,
      productCode,
      productName,
      accountCode,
      accountName,
      expirationDate,
      alarmDate,
      dDay,
      description,
      isExpired,
    );
  }

  @override
  String toString() {
    return 'ProductExpirationItemModel(seq: $seq, productCode: $productCode, '
        'productName: $productName, accountCode: $accountCode, '
        'accountName: $accountName, expirationDate: $expirationDate, '
        'alarmDate: $alarmDate, dDay: $dDay, '
        'description: $description, isExpired: $isExpired)';
  }
}
