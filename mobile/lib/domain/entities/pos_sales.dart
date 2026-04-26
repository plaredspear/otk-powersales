/// POS 매출 엔티티
/// 대형마트 3대 (이마트, 홈플러스, 롯데마트) EDI 연동 데이터
class PosSales {
  /// 매장명
  final String accountName;

  /// 제품명
  final String productName;

  /// 판매일자
  final DateTime salesDate;

  /// 판매수량
  final int quantity;

  /// 판매금액 (원)
  final int amount;

  /// 제품 코드 (선택적)
  final String? productCode;

  /// 카테고리 (선택적)
  final String? category;

  const PosSales({
    required this.accountName,
    required this.productName,
    required this.salesDate,
    required this.quantity,
    required this.amount,
    this.productCode,
    this.category,
  });

  /// 엔티티 복사 (불변성 유지)
  PosSales copyWith({
    String? accountName,
    String? productName,
    DateTime? salesDate,
    int? quantity,
    int? amount,
    String? productCode,
    String? category,
  }) {
    return PosSales(
      accountName: accountName ?? this.accountName,
      productName: productName ?? this.productName,
      salesDate: salesDate ?? this.salesDate,
      quantity: quantity ?? this.quantity,
      amount: amount ?? this.amount,
      productCode: productCode ?? this.productCode,
      category: category ?? this.category,
    );
  }

  /// JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'accountName': accountName,
      'productName': productName,
      'salesDate': salesDate.toIso8601String(),
      'quantity': quantity,
      'amount': amount,
      'productCode': productCode,
      'category': category,
    };
  }

  /// JSON에서 역직렬화
  factory PosSales.fromJson(Map<String, dynamic> json) {
    return PosSales(
      accountName: json['accountName'] as String,
      productName: json['productName'] as String,
      salesDate: DateTime.parse(json['salesDate'] as String),
      quantity: json['quantity'] as int,
      amount: json['amount'] as int,
      productCode: json['productCode'] as String?,
      category: json['category'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is PosSales &&
        other.accountName == accountName &&
        other.productName == productName &&
        other.salesDate == salesDate &&
        other.quantity == quantity &&
        other.amount == amount &&
        other.productCode == productCode &&
        other.category == category;
  }

  @override
  int get hashCode {
    return Object.hash(
      accountName,
      productName,
      salesDate,
      quantity,
      amount,
      productCode,
      category,
    );
  }

  @override
  String toString() {
    return 'PosSales(accountName: $accountName, productName: $productName, '
        'salesDate: $salesDate, quantity: $quantity, amount: $amount, '
        'productCode: $productCode, category: $category)';
  }
}
