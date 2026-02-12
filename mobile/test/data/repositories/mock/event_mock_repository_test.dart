import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_sales_summary.dart';
import 'package:mobile/domain/entities/event.dart';
import 'package:mobile/domain/entities/event_sales_info.dart';
import 'package:mobile/data/repositories/mock/event_mock_repository.dart';

void main() {
  group('EventMockRepository', () {
    late EventMockRepository repository;

    setUp(() {
      repository = EventMockRepository();
    });

    group('getEvents', () {
      test('행사 목록을 반환한다', () async {
        final result = await repository.getEvents();

        expect(result, isNotEmpty);
        expect(result.every((event) => event.id.isNotEmpty), true);
      });

      test('거래처 ID로 필터링할 수 있다', () async {
        final result = await repository.getEvents(customerId: 'C001');

        expect(result.every((event) => event.customerId == 'C001'), true);
      });

      test('시작 날짜로 필터링할 수 있다', () async {
        final startDate = DateTime(2026, 2, 15);
        final result = await repository.getEvents(startDate: startDate);

        expect(
          result.every((event) => event.endDate.isAfter(startDate) || event.endDate.isAtSameMomentAs(startDate)),
          true,
        );
      });

      test('종료 날짜로 필터링할 수 있다', () async {
        final endDate = DateTime(2026, 2, 15);
        final result = await repository.getEvents(endDate: endDate);

        expect(
          result.every((event) => event.startDate.isBefore(endDate) || event.startDate.isAtSameMomentAs(endDate)),
          true,
        );
      });

      test('날짜 범위로 필터링할 수 있다', () async {
        final startDate = DateTime(2026, 2, 1);
        final endDate = DateTime(2026, 2, 28);
        final result = await repository.getEvents(
          startDate: startDate,
          endDate: endDate,
        );

        expect(result, isNotEmpty);
      });

      test('페이지네이션이 올바르게 동작한다', () async {
        final result = await repository.getEvents(page: 1, size: 2);

        expect(result.length, lessThanOrEqualTo(2));
      });

      test('페이지 번호가 범위를 벗어나면 빈 목록을 반환한다', () async {
        final result = await repository.getEvents(page: 999, size: 10);

        expect(result, isEmpty);
      });

      test('커스텀 데이터를 사용할 수 있다', () async {
        final customEvents = [
          Event(
            id: 'TEST001',
            eventType: '[테스트]',
            eventName: '테스트 행사',
            startDate: DateTime(2026, 3, 1),
            endDate: DateTime(2026, 3, 31),
            customerId: 'C999',
            customerName: '테스트 거래처',
            assigneeId: '99999999',
          ),
        ];
        repository.customEvents = customEvents;

        final result = await repository.getEvents();

        expect(result, customEvents);
      });

      test('Exception을 throw할 수 있다', () async {
        repository.exceptionToThrow = Exception('네트워크 오류');

        expect(
          () => repository.getEvents(),
          throwsA(isA<Exception>()),
        );
      });
    });

    group('getEventDetail', () {
      test('존재하는 행사의 상세를 반환한다', () async {
        final result = await repository.getEventDetail('EVT001');

        expect(result.$1.id, 'EVT001');
        expect(result.$2.eventId, 'EVT001');
      });

      test('행사 정보와 매출 정보를 함께 반환한다', () async {
        final result = await repository.getEventDetail('EVT001');

        final event = result.$1;
        final salesInfo = result.$2;

        expect(event.id, isNotEmpty);
        expect(event.eventName, isNotEmpty);
        expect(salesInfo.targetAmount, greaterThan(0));
      });

      test('제품 정보를 포함한 상세를 반환한다', () async {
        final result = await repository.getEventDetail('EVT001');

        final event = result.$1;

        expect(event.mainProduct, isNotNull);
        expect(event.subProducts, isNotEmpty);
      });

      test('존재하지 않는 행사는 Exception을 throw한다', () async {
        expect(
          () => repository.getEventDetail('NOT_EXIST'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('EVENT_NOT_FOUND'),
            ),
          ),
        );
      });

      test('커스텀 데이터를 사용할 수 있다', () async {
        final customEvent = Event(
          id: 'TEST001',
          eventType: '[테스트]',
          eventName: '테스트 행사',
          startDate: DateTime(2026, 3, 1),
          endDate: DateTime(2026, 3, 31),
          customerId: 'C999',
          customerName: '테스트 거래처',
          assigneeId: '99999999',
        );
        const customSalesInfo = EventSalesInfo(
          eventId: 'TEST001',
          targetAmount: 1000000,
          achievedAmount: 500000,
          achievementRate: 50.0,
          progressRate: 30.0,
        );

        repository.customEvents = [customEvent];
        repository.customSalesInfo = {'TEST001': customSalesInfo};

        final result = await repository.getEventDetail('TEST001');

        expect(result.$1, customEvent);
        expect(result.$2, customSalesInfo);
      });

      test('Exception을 throw할 수 있다', () async {
        repository.exceptionToThrow = Exception('네트워크 오류');

        expect(
          () => repository.getEventDetail('EVT001'),
          throwsA(isA<Exception>()),
        );
      });
    });

    group('getDailySales', () {
      test('일매출 목록을 반환한다', () async {
        final result = await repository.getDailySales('EVT001');

        expect(result, isNotEmpty);
        expect(result.every((sale) => sale.dailySalesId.isNotEmpty), true);
      });

      test('일매출이 없으면 빈 목록을 반환한다', () async {
        final result = await repository.getDailySales('EVT005');

        expect(result, isEmpty);
      });

      test('일매출 데이터를 올바르게 반환한다', () async {
        final result = await repository.getDailySales('EVT001');

        expect(result.first.totalAmount, greaterThan(0));
        expect(result.first.status, isIn(['DRAFT', 'REGISTERED']));
      });

      test('커스텀 데이터를 사용할 수 있다', () async {
        final customDailySales = [
          DailySalesSummary(
            dailySalesId: 'TEST001',
            salesDate: DateTime(2026, 3, 1),
            totalAmount: 500000,
            status: 'REGISTERED',
          ),
        ];
        repository.customDailySales = {'TEST001': customDailySales};

        final result = await repository.getDailySales('TEST001');

        expect(result, customDailySales);
      });

      test('Exception을 throw할 수 있다', () async {
        repository.exceptionToThrow = Exception('네트워크 오류');

        expect(
          () => repository.getDailySales('EVT001'),
          throwsA(isA<Exception>()),
        );
      });
    });
  });
}
