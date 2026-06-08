/// POS 매출 엔티티
///
/// 레거시 Heroku `promotion/month/posmain.jsp` (POS DB `live_pos_sales_dh`) 동등.
/// 거래처 1곳 + 연월 기준 제품별 POS 스캔 실적(금액/수량)을 표현한다.
class PosSales {
  /// 년월 (예: 202601)
  final String yearMonth;

  /// 거래처(매장)명
  final String customerName;

  /// 제품명
  final String productName;

  /// 제품 코드
  final String productCode;

  /// 대표 바코드 (없을 수 있음)
  final String? barcode;

  /// 매출 금액 (원)
  final int amount;

  /// 매출 수량 (EA)
  final int quantity;

  const PosSales({
    required this.yearMonth,
    required this.customerName,
    required this.productName,
    required this.productCode,
    this.barcode,
    required this.amount,
    required this.quantity,
  });

  /// 불변성을 유지하며 일부 필드를 변경한 새 인스턴스 생성
  PosSales copyWith({
    String? yearMonth,
    String? customerName,
    String? productName,
    String? productCode,
    String? barcode,
    int? amount,
    int? quantity,
  }) {
    return PosSales(
      yearMonth: yearMonth ?? this.yearMonth,
      customerName: customerName ?? this.customerName,
      productName: productName ?? this.productName,
      productCode: productCode ?? this.productCode,
      barcode: barcode ?? this.barcode,
      amount: amount ?? this.amount,
      quantity: quantity ?? this.quantity,
    );
  }

  /// JSON으로 변환
  Map<String, dynamic> toJson() {
    return {
      'yearMonth': yearMonth,
      'customerName': customerName,
      'productName': productName,
      'productCode': productCode,
      'barcode': barcode,
      'amount': amount,
      'quantity': quantity,
    };
  }

  /// JSON에서 엔티티 생성
  factory PosSales.fromJson(Map<String, dynamic> json) {
    return PosSales(
      yearMonth: json['yearMonth'] as String,
      customerName: json['customerName'] as String,
      productName: json['productName'] as String,
      productCode: json['productCode'] as String,
      barcode: json['barcode'] as String?,
      amount: json['amount'] as int,
      quantity: json['quantity'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is PosSales &&
        other.yearMonth == yearMonth &&
        other.customerName == customerName &&
        other.productName == productName &&
        other.productCode == productCode &&
        other.barcode == barcode &&
        other.amount == amount &&
        other.quantity == quantity;
  }

  @override
  int get hashCode {
    return Object.hash(
      yearMonth,
      customerName,
      productName,
      productCode,
      barcode,
      amount,
      quantity,
    );
  }

  @override
  String toString() {
    return 'PosSales(yearMonth: $yearMonth, customerName: $customerName, '
        'productName: $productName, productCode: $productCode, '
        'barcode: $barcode, amount: $amount, quantity: $quantity)';
  }
}
