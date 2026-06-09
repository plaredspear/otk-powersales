/// 전산매출(ABC) 제품별 실적 엔티티.
///
/// 레거시 `promotion/month/abcmain.jsp` 의 결과 카드 1건(제품 1개)에 대응한다.
/// 거래처 1곳 + 기간 + 매출 조회 제품(바코드)으로 조회한 POS DB `live_tot_sales_dh` 의
/// 제품별 `SUM(SALES_RAMT)`(금액) / `SUM(SALES_RQTY)`(수량) 집계 결과.
class ElectronicSales {
  /// 제품명 (`ITEM_NM`)
  final String productName;

  /// 제품 코드 (`ITEM_CD`)
  final String productCode;

  /// 바코드 (`UPC_CD`)
  final String barcode;

  /// 실적 금액 (원, `SUM(SALES_RAMT)`)
  final int amount;

  /// 실적 수량 (`SUM(SALES_RQTY)`)
  final int quantity;

  const ElectronicSales({
    required this.productName,
    required this.productCode,
    required this.barcode,
    required this.amount,
    required this.quantity,
  });

  ElectronicSales copyWith({
    String? productName,
    String? productCode,
    String? barcode,
    int? amount,
    int? quantity,
  }) {
    return ElectronicSales(
      productName: productName ?? this.productName,
      productCode: productCode ?? this.productCode,
      barcode: barcode ?? this.barcode,
      amount: amount ?? this.amount,
      quantity: quantity ?? this.quantity,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productName': productName,
      'productCode': productCode,
      'barcode': barcode,
      'amount': amount,
      'quantity': quantity,
    };
  }

  factory ElectronicSales.fromJson(Map<String, dynamic> json) {
    return ElectronicSales(
      productName: json['productName'] as String? ?? '',
      productCode: json['productCode'] as String? ?? '',
      barcode: json['barcode'] as String? ?? '',
      amount: (json['amount'] as num?)?.toInt() ?? 0,
      quantity: (json['quantity'] as num?)?.toInt() ?? 0,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ElectronicSales &&
        other.productName == productName &&
        other.productCode == productCode &&
        other.barcode == barcode &&
        other.amount == amount &&
        other.quantity == quantity;
  }

  @override
  int get hashCode {
    return Object.hash(productName, productCode, barcode, amount, quantity);
  }

  @override
  String toString() {
    return 'ElectronicSales(productName: $productName, '
        'productCode: $productCode, barcode: $barcode, '
        'amount: $amount, quantity: $quantity)';
  }
}

/// 전산매출 조회 결과 묶음.
///
/// 레거시 `abcmain.jsp` 의 합계금액(`#totalAmount`) + 제품별 결과 리스트에 대응한다.
/// - 매출 조회 제품 선택 시: [items] = 선택 제품별 실적, [totalAmount] = 선택 제품 금액 합.
/// - 미선택 시: [items] 빈 목록, [totalAmount] = 거래처·기간 전체 합계금액.
class ElectronicSalesResult {
  /// 거래처명
  final String customerName;

  /// 조회 시작일 (`YYYY-MM-DD`)
  final String startDate;

  /// 조회 종료일 (`YYYY-MM-DD`)
  final String endDate;

  /// 합계금액 (원) — 서버 산출
  final int totalAmount;

  /// 제품별 실적 목록
  final List<ElectronicSales> items;

  const ElectronicSalesResult({
    required this.customerName,
    required this.startDate,
    required this.endDate,
    required this.totalAmount,
    required this.items,
  });

  /// 빈 결과 (합계 0, 제품 없음).
  factory ElectronicSalesResult.empty() => const ElectronicSalesResult(
        customerName: '',
        startDate: '',
        endDate: '',
        totalAmount: 0,
        items: [],
      );

  /// 총 수량 합계 (제품 목록 기준).
  int get totalQuantity => items.fold(0, (sum, e) => sum + e.quantity);
}
