import '../../domain/entities/logistics_sales.dart';

/// 물류매출 API 응답 파서.
///
/// 백엔드 `GET /api/v1/mobile/sales/logistics` 의 `data` 객체를 [LogisticsSales] 목록으로 변환한다.
/// 응답은 온도대 1건당 1개의 [LogisticsSales] 로 펼쳐지며, 상단 공통 필드(yearMonth/
/// isCurrentMonth/customerName)는 각 항목에 복제된다.
class LogisticsSalesModel {
  /// `data` 객체 → [LogisticsSales] 목록.
  static List<LogisticsSales> listFromJson(Map<String, dynamic> data) {
    final yearMonth = data['yearMonth'] as String;
    final isCurrentMonth = data['isCurrentMonth'] as bool;
    final customerName = data['customerName'] as String?;
    final categories = (data['categories'] as List<dynamic>?) ?? const [];

    return categories.map((raw) {
      final c = raw as Map<String, dynamic>;
      return LogisticsSales(
        yearMonth: yearMonth,
        category: LogisticsCategory.fromCode(c['category'] as String),
        currentAmount: (c['currentAmount'] as num).toInt(),
        previousYearAmount: (c['previousYearAmount'] as num).toInt(),
        difference: (c['difference'] as num).toInt(),
        growthRate: (c['growthRate'] as num).toDouble(),
        isCurrentMonth: isCurrentMonth,
        customerName: customerName,
      );
    }).toList();
  }
}
