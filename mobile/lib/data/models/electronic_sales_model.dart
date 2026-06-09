import '../../domain/entities/electronic_sales.dart';

/// 전산매출(ABC) API 응답 파서.
///
/// 백엔드 `GET /api/v1/mobile/sales/electronic` 의 `data` 객체를 [ElectronicSalesResult] 로 변환.
/// `data.items` 는 제품 1건당 1개의 [ElectronicSales] 로 펼쳐지고, 합계금액(`totalAmount`)·기간은
/// 결과 묶음 상단에 담긴다.
class ElectronicSalesModel {
  /// `data` 객체 → [ElectronicSalesResult].
  static ElectronicSalesResult resultFromJson(Map<String, dynamic> data) {
    final rawItems = (data['items'] as List<dynamic>?) ?? const [];

    final items = rawItems.map((raw) {
      final item = raw as Map<String, dynamic>;
      return ElectronicSales(
        productName: item['productName'] as String? ?? '',
        productCode: item['productCode'] as String? ?? '',
        barcode: item['barcode'] as String? ?? '',
        amount: (item['amount'] as num?)?.toInt() ?? 0,
        quantity: (item['quantity'] as num?)?.toInt() ?? 0,
      );
    }).toList();

    return ElectronicSalesResult(
      customerName: data['customerName'] as String? ?? '',
      startDate: data['startDate'] as String? ?? '',
      endDate: data['endDate'] as String? ?? '',
      totalAmount: (data['totalAmount'] as num?)?.toInt() ?? 0,
      items: items,
    );
  }
}
