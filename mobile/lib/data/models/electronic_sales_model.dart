import '../../domain/entities/electronic_sales.dart';

/// 전산매출(ABC) API 응답 파서.
///
/// 백엔드 `GET /api/v1/mobile/sales/electronic` 의 `data` 객체를 [ElectronicSales] 목록으로 변환.
/// 응답은 제품 1건당 1개의 [ElectronicSales] 로 펼쳐지며, 상단 공통 필드(yearMonth/customerName)는
/// 각 항목에 복제된다. 레거시 ABC 제품 명세에는 전년 비교가 없어 previousYearAmount/growthRate 는 null.
class ElectronicSalesModel {
  /// `data` 객체 → [ElectronicSales] 목록.
  static List<ElectronicSales> listFromJson(Map<String, dynamic> data) {
    final yearMonth = data['yearMonth'] as String;
    final customerName = data['customerName'] as String? ?? '';
    final items = (data['items'] as List<dynamic>?) ?? const [];

    return items.map((raw) {
      final item = raw as Map<String, dynamic>;
      return ElectronicSales(
        yearMonth: yearMonth,
        customerName: customerName,
        productName: item['productName'] as String? ?? '',
        productCode: item['productCode'] as String? ?? '',
        amount: (item['amount'] as num?)?.toInt() ?? 0,
        quantity: (item['quantity'] as num?)?.toInt() ?? 0,
      );
    }).toList();
  }
}
