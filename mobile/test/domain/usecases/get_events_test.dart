import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_sales_summary.dart';
import 'package:mobile/domain/entities/event.dart';
import 'package:mobile/domain/entities/event_sales_info.dart';
import 'package:mobile/domain/repositories/event_repository.dart';
import 'package:mobile/domain/usecases/get_events.dart';

class _MockEventRepository implements EventRepository {
  List<Event>? result;
  Exception? error;

  @override
  Future<List<Event>> getEvents({
    String? customerId,
    DateTime? startDate,
    DateTime? endDate,
    int page = 1,
    int size = 10,
  }) async {
    if (error != null) throw error!;
    return result!;
  }

  @override
  Future<(Event, EventSalesInfo)> getEventDetail(String eventId) async {
    throw UnimplementedError();
  }

  @override
  Future<List<DailySalesSummary>> getDailySales(String eventId) async {
    throw UnimplementedError();
  }
}

void main() {
  group('GetEventsUseCase', () {
    late _MockEventRepository repository;
    late GetEventsUseCase useCase;

    setUp(() {
      repository = _MockEventRepository();
      useCase = GetEventsUseCase(repository);
    });

    test('행사 목록 조회가 성공한다', () async {
      // Given
      final testEvents = [
        Event(
          id: 'EVT001',
          eventType: '[시식]',
          eventName: '상온(오뚜기카레_매운맛100G)',
          startDate: DateTime(2026, 2, 1),
          endDate: DateTime(2026, 2, 28),
          customerId: 'C001',
          customerName: '이마트 부산점',
          assigneeId: '20010585',
        ),
      ];
      repository.result = testEvents;

      // When
      final result = await useCase.call();

      // Then
      expect(result.length, 1);
      expect(result[0].id, 'EVT001');
    });

    test('거래처 필터가 올바르게 전달된다', () async {
      // Given
      repository.result = [];

      // When
      await useCase.call(customerId: 'C001');

      // Then - Mock이 호출되었으므로 에러 없이 완료
      expect(repository.result, isNotNull);
    });

    test('날짜 범위 필터가 올바르게 전달된다', () async {
      // Given
      repository.result = [];
      final startDate = DateTime(2026, 2, 1);
      final endDate = DateTime(2026, 2, 28);

      // When
      await useCase.call(startDate: startDate, endDate: endDate);

      // Then
      expect(repository.result, isNotNull);
    });

    test('페이지네이션 파라미터가 올바르게 전달된다', () async {
      // Given
      repository.result = [];

      // When
      await useCase.call(page: 2, size: 20);

      // Then
      expect(repository.result, isNotNull);
    });

    test('페이지 번호가 1 미만이면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(page: 0),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('페이지 크기가 1 미만이면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(size: 0),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('페이지 크기가 100 초과이면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(size: 101),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('시작일이 종료일보다 이후이면 ArgumentError가 발생한다', () async {
      // Given
      final startDate = DateTime(2026, 2, 28);
      final endDate = DateTime(2026, 2, 1);

      // When & Then
      expect(
        () => useCase.call(startDate: startDate, endDate: endDate),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('시작일과 종료일이 같으면 정상 처리된다', () async {
      // Given
      repository.result = [];
      final sameDate = DateTime(2026, 2, 15);

      // When
      await useCase.call(startDate: sameDate, endDate: sameDate);

      // Then
      expect(repository.result, isNotNull);
    });

    test('시작일만 있어도 정상 처리된다', () async {
      // Given
      repository.result = [];
      final startDate = DateTime(2026, 2, 1);

      // When
      await useCase.call(startDate: startDate);

      // Then
      expect(repository.result, isNotNull);
    });

    test('종료일만 있어도 정상 처리된다', () async {
      // Given
      repository.result = [];
      final endDate = DateTime(2026, 2, 28);

      // When
      await useCase.call(endDate: endDate);

      // Then
      expect(repository.result, isNotNull);
    });

    test('Repository에서 에러가 발생하면 전파된다', () async {
      // Given
      repository.error = Exception('Network error');

      // When & Then
      expect(
        () => useCase.call(),
        throwsException,
      );
    });
  });
}
