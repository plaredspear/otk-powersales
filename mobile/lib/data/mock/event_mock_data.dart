import '../../domain/entities/daily_sales_summary.dart';
import '../../domain/entities/event.dart';
import '../../domain/entities/event_sales_info.dart';

/// 행사 Mock 데이터
class EventMockData {
  /// 행사 목록 (전체)
  static final List<Event> allEvents = [
    Event(
      id: 'EVT001',
      eventType: '[시식]',
      eventName: '상온(오뚜기카레_매운맛100G)',
      startDate: DateTime(2026, 2, 1),
      endDate: DateTime(2026, 2, 28),
      customerId: 'C001',
      customerName: '이마트 부산점',
      assigneeId: '20010585',
      mainProduct: const EventProduct(
        productCode: 'P001',
        productName: '오뚜기카레_매운맛100G',
        isMainProduct: true,
      ),
      subProducts: const [
        EventProduct(
          productCode: 'P002',
          productName: '오뚜기카레_중간맛100G',
          isMainProduct: false,
        ),
        EventProduct(
          productCode: 'P003',
          productName: '오뚜기카레_순한맛100G',
          isMainProduct: false,
        ),
      ],
    ),
    Event(
      id: 'EVT002',
      eventType: '[판촉]',
      eventName: '냉장/냉동(진라면순한맛)',
      startDate: DateTime(2026, 2, 10),
      endDate: DateTime(2026, 2, 20),
      customerId: 'C002',
      customerName: '롯데마트 해운대점',
      assigneeId: '20010586',
      mainProduct: const EventProduct(
        productCode: 'P101',
        productName: '진라면순한맛',
        isMainProduct: true,
      ),
      subProducts: const [],
    ),
    Event(
      id: 'EVT003',
      eventType: '[시식]',
      eventName: '상온(진짬뽕)',
      startDate: DateTime(2026, 2, 15),
      endDate: DateTime(2026, 2, 25),
      customerId: 'C001',
      customerName: '이마트 부산점',
      assigneeId: '20010585',
      mainProduct: const EventProduct(
        productCode: 'P201',
        productName: '진짬뽕',
        isMainProduct: true,
      ),
      subProducts: const [
        EventProduct(
          productCode: 'P202',
          productName: '진짜장',
          isMainProduct: false,
        ),
      ],
    ),
    Event(
      id: 'EVT004',
      eventType: '[판촉]',
      eventName: '상온(오뚜기밥)',
      startDate: DateTime(2026, 1, 1),
      endDate: DateTime(2026, 1, 31),
      customerId: 'C003',
      customerName: '홈플러스 광복점',
      assigneeId: '20010585',
      mainProduct: const EventProduct(
        productCode: 'P301',
        productName: '오뚜기밥',
        isMainProduct: true,
      ),
      subProducts: const [],
    ),
    Event(
      id: 'EVT005',
      eventType: '[시식]',
      eventName: '냉장/냉동(미숫가루)',
      startDate: DateTime(2026, 3, 1),
      endDate: DateTime(2026, 3, 15),
      customerId: 'C002',
      customerName: '롯데마트 해운대점',
      assigneeId: '20010586',
      mainProduct: const EventProduct(
        productCode: 'P401',
        productName: '오뚜기미숫가루',
        isMainProduct: true,
      ),
      subProducts: const [],
    ),
  ];

  /// 행사별 매출 정보
  static final Map<String, EventSalesInfo> salesInfoMap = {
    'EVT001': const EventSalesInfo(
      eventId: 'EVT001',
      targetAmount: 10000000,
      achievedAmount: 8000000,
      achievementRate: 80.0,
      progressRate: 50.0,
    ),
    'EVT002': const EventSalesInfo(
      eventId: 'EVT002',
      targetAmount: 5000000,
      achievedAmount: 3000000,
      achievementRate: 60.0,
      progressRate: 40.0,
    ),
    'EVT003': const EventSalesInfo(
      eventId: 'EVT003',
      targetAmount: 8000000,
      achievedAmount: 5000000,
      achievementRate: 62.5,
      progressRate: 30.0,
    ),
    'EVT004': const EventSalesInfo(
      eventId: 'EVT004',
      targetAmount: 12000000,
      achievedAmount: 12500000,
      achievementRate: 104.2,
      progressRate: 100.0,
    ),
    'EVT005': const EventSalesInfo(
      eventId: 'EVT005',
      targetAmount: 6000000,
      achievedAmount: 0,
      achievementRate: 0.0,
      progressRate: 0.0,
    ),
  };

  /// 행사별 일매출 목록
  static final Map<String, List<DailySalesSummary>> dailySalesMap = {
    'EVT001': [
      DailySalesSummary(
        dailySalesId: 'DS001',
        salesDate: DateTime(2026, 2, 15),
        totalAmount: 500000,
        status: 'REGISTERED',
      ),
      DailySalesSummary(
        dailySalesId: 'DS002',
        salesDate: DateTime(2026, 2, 14),
        totalAmount: 450000,
        status: 'REGISTERED',
      ),
      DailySalesSummary(
        dailySalesId: 'DS003',
        salesDate: DateTime(2026, 2, 13),
        totalAmount: 600000,
        status: 'REGISTERED',
      ),
      DailySalesSummary(
        dailySalesId: 'DS004',
        salesDate: DateTime(2026, 2, 12),
        totalAmount: 300000,
        status: 'DRAFT',
      ),
    ],
    'EVT002': [
      DailySalesSummary(
        dailySalesId: 'DS101',
        salesDate: DateTime(2026, 2, 15),
        totalAmount: 200000,
        status: 'REGISTERED',
      ),
      DailySalesSummary(
        dailySalesId: 'DS102',
        salesDate: DateTime(2026, 2, 14),
        totalAmount: 250000,
        status: 'REGISTERED',
      ),
    ],
    'EVT003': [
      DailySalesSummary(
        dailySalesId: 'DS201',
        salesDate: DateTime(2026, 2, 16),
        totalAmount: 400000,
        status: 'REGISTERED',
      ),
    ],
    'EVT004': [
      DailySalesSummary(
        dailySalesId: 'DS301',
        salesDate: DateTime(2026, 1, 30),
        totalAmount: 800000,
        status: 'REGISTERED',
      ),
      DailySalesSummary(
        dailySalesId: 'DS302',
        salesDate: DateTime(2026, 1, 29),
        totalAmount: 750000,
        status: 'REGISTERED',
      ),
      DailySalesSummary(
        dailySalesId: 'DS303',
        salesDate: DateTime(2026, 1, 28),
        totalAmount: 900000,
        status: 'REGISTERED',
      ),
    ],
    'EVT005': [],
  };

  /// 거래처 필터링된 행사 목록 조회
  static List<Event> getEventsByCustomer(String? customerId) {
    if (customerId == null || customerId.isEmpty) {
      return allEvents;
    }
    return allEvents.where((event) => event.customerId == customerId).toList();
  }

  /// 기간 필터링된 행사 목록 조회
  static List<Event> getEventsByDateRange({
    DateTime? startDate,
    DateTime? endDate,
  }) {
    return allEvents.where((event) {
      if (startDate != null && event.endDate.isBefore(startDate)) {
        return false;
      }
      if (endDate != null && event.startDate.isAfter(endDate)) {
        return false;
      }
      return true;
    }).toList();
  }

  /// 행사 ID로 행사 조회
  static Event? getEventById(String eventId) {
    try {
      return allEvents.firstWhere((event) => event.id == eventId);
    } catch (e) {
      return null;
    }
  }

  /// 행사 ID로 매출 정보 조회
  static EventSalesInfo? getSalesInfoByEventId(String eventId) {
    return salesInfoMap[eventId];
  }

  /// 행사 ID로 일매출 목록 조회
  static List<DailySalesSummary> getDailySalesByEventId(String eventId) {
    return dailySalesMap[eventId] ?? [];
  }

  /// 행사 상세 조회 (행사 정보 + 매출 정보 튜플)
  static (Event, EventSalesInfo)? getEventDetail(String eventId) {
    final event = getEventById(eventId);
    final salesInfo = getSalesInfoByEventId(eventId);

    if (event == null || salesInfo == null) {
      return null;
    }

    return (event, salesInfo);
  }
}
