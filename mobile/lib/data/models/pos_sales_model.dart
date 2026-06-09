import '../../domain/entities/pos_sales.dart';
import '../../domain/entities/pos_sales_result.dart';

/// POS 매출 API 응답 파서.
///
/// 백엔드 `GET /api/v1/mobile/sales/pos/by-range` 의 `data` 객체를 [PosSalesResult] 로 변환.
/// 제품 1건당 1개의 [PosSales] 로 펼쳐지며, 상단 공통 필드(customerName)는 각 항목에 복제된다.
class PosSalesModel {
  /// `data` 객체 → [PosSalesResult].
  static PosSalesResult resultFromJson(Map<String, dynamic> data) {
    final customerName = data['customerName'] as String? ?? '';
    final items = (data['items'] as List<dynamic>?) ?? const [];

    final sales = items.map((raw) {
      final item = raw as Map<String, dynamic>;
      return PosSales(
        yearMonth: '',
        customerName: customerName,
        productName: item['productName'] as String? ?? '',
        productCode: item['productCode'] as String? ?? '',
        barcode: item['barcode'] as String?,
        amount: (item['amount'] as num?)?.toInt() ?? 0,
        quantity: (item['quantity'] as num?)?.toInt() ?? 0,
      );
    }).toList();

    return PosSalesResult(
      items: sales,
      totalAmount: (data['totalAmount'] as num?)?.toInt() ?? 0,
      totalQuantity: (data['totalQuantity'] as num?)?.toInt() ?? 0,
    );
  }
}
