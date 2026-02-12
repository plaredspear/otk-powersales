import '../../../domain/entities/daily_sales_summary.dart';
import '../../../domain/entities/event.dart';
import '../../../domain/entities/event_sales_info.dart';
import '../../../domain/repositories/event_repository.dart';
import '../../mock/event_mock_data.dart';

/// Event Mock Repository
///
/// Backend API가 준비되기 전까지 Mock 데이터로 동작하는 Repository.
class EventMockRepository implements EventRepository {
  /// Mock 데이터 커스텀 (테스트용)
  List<Event>? customEvents;
  Map<String, EventSalesInfo>? customSalesInfo;
  Map<String, List<DailySalesSummary>>? customDailySales;
  Exception? exceptionToThrow;

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 500));
  }

  @override
  Future<List<Event>> getEvents({
    String? customerId,
    DateTime? startDate,
    DateTime? endDate,
    int page = 1,
    int size = 10,
  }) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    // Mock 데이터 조회
    List<Event> allEvents = customEvents ?? EventMockData.allEvents;

    // 거래처 필터링
    if (customerId != null && customerId.isNotEmpty) {
      allEvents = allEvents.where((event) => event.customerId == customerId).toList();
    }

    // 날짜 범위 필터링
    if (startDate != null || endDate != null) {
      allEvents = allEvents.where((event) {
        if (startDate != null && event.endDate.isBefore(startDate)) {
          return false;
        }
        if (endDate != null && event.startDate.isAfter(endDate)) {
          return false;
        }
        return true;
      }).toList();
    }

    // 페이지네이션 계산
    final totalCount = allEvents.length;
    final startIndex = (page - 1) * size;
    final endIndex = (startIndex + size).clamp(0, totalCount);

    // 페이지 범위 유효성 검사
    if (startIndex >= totalCount && totalCount > 0) {
      return [];
    }

    return allEvents.sublist(startIndex, endIndex);
  }

  @override
  Future<(Event, EventSalesInfo)> getEventDetail(String eventId) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    // 커스텀 데이터 또는 Mock 데이터에서 조회
    Event? event;
    EventSalesInfo? salesInfo;

    if (customEvents != null) {
      try {
        event = customEvents!.firstWhere((e) => e.id == eventId);
      } catch (e) {
        event = null;
      }
    } else {
      event = EventMockData.getEventById(eventId);
    }

    if (customSalesInfo != null) {
      salesInfo = customSalesInfo![eventId];
    } else {
      salesInfo = EventMockData.getSalesInfoByEventId(eventId);
    }

    if (event == null || salesInfo == null) {
      throw Exception('EVENT_NOT_FOUND: Event with ID $eventId not found');
    }

    return (event, salesInfo);
  }

  @override
  Future<List<DailySalesSummary>> getDailySales(String eventId) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    // 커스텀 데이터 또는 Mock 데이터에서 조회
    List<DailySalesSummary> dailySales;

    if (customDailySales != null) {
      dailySales = customDailySales![eventId] ?? [];
    } else {
      dailySales = EventMockData.getDailySalesByEventId(eventId);
    }

    return dailySales;
  }
}
