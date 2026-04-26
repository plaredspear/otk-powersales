/// 전산매출 엔티티
///
/// Orora 영업 고객품목실적일 연동
/// 물류배부 실적 제외 전산실적 조회
class ElectronicSales {
  /// 년월 (예: 202601)
  final String yearMonth;

  /// 거래처명
  final String customerName;

  /// 제품명
  final String productName;

  /// 제품 코드
  final String productCode;

  /// 실적 금액 (원)
  final int amount;

  /// 실적 수량
  final int quantity;

  /// 전년 동월 실적 금액 (원)
  final int? previousYearAmount;

  /// 전년 대비 증감율 (%)
  final double? growthRate;

  const ElectronicSales({
    required this.yearMonth,
    required this.customerName,
    required this.productName,
    required this.productCode,
    required this.amount,
    required this.quantity,
    this.previousYearAmount,
    this.growthRate,
  });

  /// 불변성을 유지하며 일부 필드를 변경한 새 인스턴스 생성
  ElectronicSales copyWith({
    String? yearMonth,
    String? customerName,
    String? productName,
    String? productCode,
    int? amount,
    int? quantity,
    int? previousYearAmount,
    double? growthRate,
  }) {
    return ElectronicSales(
      yearMonth: yearMonth ?? this.yearMonth,
      customerName: customerName ?? this.customerName,
      productName: productName ?? this.productName,
      productCode: productCode ?? this.productCode,
      amount: amount ?? this.amount,
      quantity: quantity ?? this.quantity,
      previousYearAmount: previousYearAmount ?? this.previousYearAmount,
      growthRate: growthRate ?? this.growthRate,
    );
  }

  /// JSON으로 변환
  Map<String, dynamic> toJson() {
    return {
      'yearMonth': yearMonth,
      'customerName': customerName,
      'productName': productName,
      'productCode': productCode,
      'amount': amount,
      'quantity': quantity,
      'previousYearAmount': previousYearAmount,
      'growthRate': growthRate,
    };
  }

  /// JSON에서 엔티티 생성
  factory ElectronicSales.fromJson(Map<String, dynamic> json) {
    return ElectronicSales(
      yearMonth: json['yearMonth'] as String,
      customerName: json['customerName'] as String,
      productName: json['productName'] as String,
      productCode: json['productCode'] as String,
      amount: json['amount'] as int,
      quantity: json['quantity'] as int,
      previousYearAmount: json['previousYearAmount'] as int?,
      growthRate: json['growthRate'] as double?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is ElectronicSales &&
        other.yearMonth == yearMonth &&
        other.customerName == customerName &&
        other.productName == productName &&
        other.productCode == productCode &&
        other.amount == amount &&
        other.quantity == quantity &&
        other.previousYearAmount == previousYearAmount &&
        other.growthRate == growthRate;
  }

  @override
  int get hashCode {
    return Object.hash(
      yearMonth,
      customerName,
      productName,
      productCode,
      amount,
      quantity,
      previousYearAmount,
      growthRate,
    );
  }

  @override
  String toString() {
    return 'ElectronicSales(yearMonth: $yearMonth, customerName: $customerName, '
        'productName: $productName, productCode: $productCode, amount: $amount, '
        'quantity: $quantity, previousYearAmount: $previousYearAmount, '
        'growthRate: $growthRate)';
  }
}