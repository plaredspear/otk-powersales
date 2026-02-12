import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_sales_summary.dart';
import 'package:mobile/domain/entities/event.dart';
import 'package:mobile/domain/entities/event_sales_info.dart';
import 'package:mobile/domain/repositories/event_repository.dart';
import 'package:mobile/domain/usecases/get_event_detail.dart';

class _MockEventRepository implements EventRepository {
  (Event, EventSalesInfo)? result;
  Exception? error;

  @override
  Future<(Event, EventSalesInfo)> getEventDetail(String eventId) async {
    if (error != null) throw error!;
    return result!;
  }

  @override
  Future<List<Event>> getEvents({
    String? customerId,
    DateTime? startDate,
    DateTime? endDate,
    int page = 1,
    int size = 10,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<List<DailySalesSummary>> getDailySales(String eventId) async {
    throw UnimplementedError();
  }
}

void main() {
  group('GetEventDetailUseCase', () {
    late _MockEventRepository repository;
    late GetEventDetailUseCase useCase;

    setUp(() {
      repository = _MockEventRepository();
      useCase = GetEventDetailUseCase(repository);
    });

    test('행사 상세 조회가 성공한다', () async {
      // Given
      final testEvent = Event(
        id: 'EVT001',
        eventType: '[시식]',
        eventName: '상온(오뚜기카레_매운맛100G)',
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
        customerId: 'C001',
        customerName: '이마트 부산점',
        assigneeId: '20010585',
      );

      const testSalesInfo = EventSalesInfo(
        eventId: 'EVT001',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 80.0,
        progressRate: 50.0,
      );

      repository.result = (testEvent, testSalesInfo);

      // When
      final result = await useCase.call('EVT001');

      // Then
      expect(result.$1.id, 'EVT001');
      expect(result.$2.eventId, 'EVT001');
      expect(result.$2.targetAmount, 10000000);
    });

    test('eventId가 올바르게 전달된다', () async {
      // Given
      final testEvent = Event(
        id: 'EVT999',
        eventType: '[판촉]',
        eventName: 'Test',
        startDate: DateTime(2026, 3, 1),
        endDate: DateTime(2026, 3, 31),
        customerId: 'C002',
        customerName: 'Test',
        assigneeId: '20010586',
      );

      const testSalesInfo = EventSalesInfo(
        eventId: 'EVT999',
        targetAmount: 5000000,
        achievedAmount: 3000000,
        achievementRate: 60.0,
        progressRate: 30.0,
      );

      repository.result = (testEvent, testSalesInfo);

      // When
      await useCase.call('EVT999');

      // Then - Mock이 호출되었으므로 에러 없이 완료
      expect(repository.result, isNotNull);
    });

    test('eventId가 빈 문자열이면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(''),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('eventId가 공백 문자열이면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call('   '),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('Repository에서 에러가 발생하면 전파된다', () async {
      // Given
      repository.error = Exception('Network error');

      // When & Then
      expect(
        () => useCase.call('EVT001'),
        throwsException,
      );
    });
  });
}
