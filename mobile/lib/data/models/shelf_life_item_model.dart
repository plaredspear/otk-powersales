import '../../domain/entities/shelf_life_item.dart';

/// 유통기한 항목 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 ShelfLifeItem 엔티티로 변환합니다.
class ShelfLifeItemModel {
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

  const ShelfLifeItemModel({
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
  factory ShelfLifeItemModel.fromJson(Map<String, dynamic> json) {
    return ShelfLifeItemModel(
      seq: json['seq'] as int,
      productCode: json['product_code'] as String,
      productName: json['product_name'] as String,
      accountCode: json['account_code'] as String,
      accountName: json['account_name'] as String,
      expirationDate: json['expiration_date'] as String,
      alarmDate: json['alarm_date'] as String,
      dDay: json['d_day'] as int,
      description: json['description'] as String? ?? '',
      isExpired: json['is_expired'] as bool,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'seq': seq,
      'product_code': productCode,
      'product_name': productName,
      'account_code': accountCode,
      'account_name': accountName,
      'expiration_date': expirationDate,
      'alarm_date': alarmDate,
      'd_day': dDay,
      'description': description,
      'is_expired': isExpired,
    };
  }

  /// Domain Entity로 변환
  ShelfLifeItem toEntity() {
    return ShelfLifeItem(
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
  factory ShelfLifeItemModel.fromEntity(ShelfLifeItem entity) {
    return ShelfLifeItemModel(
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
    if (other is! ShelfLifeItemModel) return false;
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
    return 'ShelfLifeItemModel(seq: $seq, productCode: $productCode, '
        'productName: $productName, accountCode: $accountCode, '
        'accountName: $accountName, expirationDate: $expirationDate, '
        'alarmDate: $alarmDate, dDay: $dDay, '
        'description: $description, isExpired: $isExpired)';
  }
}
