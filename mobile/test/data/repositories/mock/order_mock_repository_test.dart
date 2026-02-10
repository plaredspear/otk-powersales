import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/order.dart';

void main() {
  late OrderMockRepository repository;

  setUp(() {
    repository = OrderMockRepository();
  });

  group('OrderMockRepository', () {
    test('should return first page of orders with default query', () async {
      // Act
      final result = await repository.getMyOrders();

      // Assert
      expect(result.orders.length, equals(20)); // default page size
      expect(result.totalElements, equals(25)); // total mock orders
      expect(result.totalPages, equals(2)); // ceil(25/20) = 2
      expect(result.currentPage, equals(0));
      expect(result.pageSize, equals(20));
      expect(result.isFirst, isTrue);
      expect(result.isLast, isFalse);
      expect(result.hasNextPage, isTrue);
    });

    test('should filter orders by clientId', () async {
      // Act
      final result = await repository.getMyOrders(clientId: 1);

      // Assert
      expect(result.orders.every((o) => o.clientId == 1), isTrue);
      expect(result.orders.length, greaterThan(0));
      for (final order in result.orders) {
        expect(order.clientName, equals('천사푸드'));
      }
    });

    test('should filter orders by status', () async {
      // Act
      final result = await repository.getMyOrders(status: 'APPROVED');

      // Assert
      expect(result.orders.every((o) => o.approvalStatus == ApprovalStatus.approved), isTrue);
      expect(result.orders.length, greaterThan(0));
    });

    test('should filter orders by deliveryDate range', () async {
      // Act
      final result = await repository.getMyOrders(
        deliveryDateFrom: '2026-02-01',
        deliveryDateTo: '2026-02-08',
      );

      // Assert
      expect(result.orders.length, greaterThan(0));
      for (final order in result.orders) {
        final deliveryDate = order.deliveryDate;
        expect(
          deliveryDate.isAfter(DateTime(2026, 1, 31)) &&
              deliveryDate.isBefore(DateTime(2026, 2, 9)),
          isTrue,
          reason: 'Order ${order.id} deliveryDate ${order.deliveryDate} out of range',
        );
      }
    });

    test('should filter with deliveryDateFrom only', () async {
      // Act
      final result = await repository.getMyOrders(
        deliveryDateFrom: '2026-02-05',
      );

      // Assert
      expect(result.orders.length, greaterThan(0));
      for (final order in result.orders) {
        expect(
          !order.deliveryDate.isBefore(DateTime(2026, 2, 5)),
          isTrue,
          reason: 'Order ${order.id} deliveryDate ${order.deliveryDate} is before 2026-02-05',
        );
      }
    });

    test('should filter with deliveryDateTo only', () async {
      // Act
      final result = await repository.getMyOrders(
        deliveryDateTo: '2026-01-20',
      );

      // Assert
      expect(result.orders.length, greaterThan(0));
      for (final order in result.orders) {
        expect(
          !order.deliveryDate.isAfter(DateTime(2026, 1, 20)),
          isTrue,
          reason: 'Order ${order.id} deliveryDate ${order.deliveryDate} is after 2026-01-20',
        );
      }
    });

    test('should apply combined filters correctly', () async {
      // Act
      final result = await repository.getMyOrders(
        clientId: 1,
        status: 'APPROVED',
        deliveryDateFrom: '2026-01-01',
        deliveryDateTo: '2026-02-28',
      );

      // Assert
      expect(result.orders.length, greaterThan(0));
      for (final order in result.orders) {
        expect(order.clientId, equals(1));
        expect(order.approvalStatus, equals(ApprovalStatus.approved));
        expect(order.deliveryDate.isAfter(DateTime(2025, 12, 31)), isTrue);
        expect(order.deliveryDate.isBefore(DateTime(2026, 3, 1)), isTrue);
      }
    });

    test('should sort by orderDate DESC by default', () async {
      // Act
      final result = await repository.getMyOrders();

      // Assert
      expect(result.orders.length, greaterThan(1));
      for (var i = 0; i < result.orders.length - 1; i++) {
        final current = result.orders[i];
        final next = result.orders[i + 1];
        expect(
          current.orderDate.isAfter(next.orderDate) ||
              current.orderDate.isAtSameMomentAs(next.orderDate),
          isTrue,
          reason: 'Order ${current.id} orderDate ${current.orderDate} should be >= ${next.orderDate}',
        );
      }
    });

    test('should sort by totalAmount DESC', () async {
      // Act
      final result = await repository.getMyOrders(
        sortBy: 'totalAmount',
        sortDir: 'DESC',
      );

      // Assert
      expect(result.orders.length, greaterThan(1));
      for (var i = 0; i < result.orders.length - 1; i++) {
        final current = result.orders[i];
        final next = result.orders[i + 1];
        expect(
          current.totalAmount >= next.totalAmount,
          isTrue,
          reason: 'Order ${current.id} amount ${current.totalAmount} should be >= ${next.totalAmount}',
        );
      }
    });

    test('should sort by totalAmount ASC', () async {
      // Act
      final result = await repository.getMyOrders(
        sortBy: 'totalAmount',
        sortDir: 'ASC',
      );

      // Assert
      expect(result.orders.length, greaterThan(1));
      for (var i = 0; i < result.orders.length - 1; i++) {
        final current = result.orders[i];
        final next = result.orders[i + 1];
        expect(
          current.totalAmount <= next.totalAmount,
          isTrue,
          reason: 'Order ${current.id} amount ${current.totalAmount} should be <= ${next.totalAmount}',
        );
      }
    });

    test('should sort by deliveryDate DESC', () async {
      // Act
      final result = await repository.getMyOrders(
        sortBy: 'deliveryDate',
        sortDir: 'DESC',
      );

      // Assert
      expect(result.orders.length, greaterThan(1));
      for (var i = 0; i < result.orders.length - 1; i++) {
        final current = result.orders[i];
        final next = result.orders[i + 1];
        expect(
          current.deliveryDate.isAfter(next.deliveryDate) ||
              current.deliveryDate.isAtSameMomentAs(next.deliveryDate),
          isTrue,
          reason: 'Order ${current.id} deliveryDate ${current.deliveryDate} should be >= ${next.deliveryDate}',
        );
      }
    });

    test('should sort by deliveryDate ASC', () async {
      // Act
      final result = await repository.getMyOrders(
        sortBy: 'deliveryDate',
        sortDir: 'ASC',
      );

      // Assert
      expect(result.orders.length, greaterThan(1));
      for (var i = 0; i < result.orders.length - 1; i++) {
        final current = result.orders[i];
        final next = result.orders[i + 1];
        expect(
          current.deliveryDate.isBefore(next.deliveryDate) ||
              current.deliveryDate.isAtSameMomentAs(next.deliveryDate),
          isTrue,
          reason: 'Order ${current.id} deliveryDate ${current.deliveryDate} should be <= ${next.deliveryDate}',
        );
      }
    });

    test('should return page 0 with correct pagination info', () async {
      // Act
      final result = await repository.getMyOrders(page: 0, size: 10);

      // Assert
      expect(result.orders.length, equals(10));
      expect(result.totalElements, equals(25));
      expect(result.totalPages, equals(3)); // ceil(25/10) = 3
      expect(result.currentPage, equals(0));
      expect(result.pageSize, equals(10));
      expect(result.isFirst, isTrue);
      expect(result.isLast, isFalse);
    });

    test('should return page 1 with correct pagination info', () async {
      // Act
      final result = await repository.getMyOrders(page: 1, size: 10);

      // Assert
      expect(result.orders.length, equals(10));
      expect(result.totalElements, equals(25));
      expect(result.totalPages, equals(3));
      expect(result.currentPage, equals(1));
      expect(result.pageSize, equals(10));
      expect(result.isFirst, isFalse);
      expect(result.isLast, isFalse);
    });

    test('should return last page with correct pagination info', () async {
      // Act
      final result = await repository.getMyOrders(page: 2, size: 10);

      // Assert
      expect(result.orders.length, equals(5)); // remaining items
      expect(result.totalElements, equals(25));
      expect(result.totalPages, equals(3));
      expect(result.currentPage, equals(2));
      expect(result.pageSize, equals(10));
      expect(result.isFirst, isFalse);
      expect(result.isLast, isTrue);
      expect(result.hasNextPage, isFalse);
    });

    test('should return empty result when page is out of range', () async {
      // Act
      final result = await repository.getMyOrders(page: 10, size: 20);

      // Assert
      expect(result.orders, isEmpty);
      expect(result.totalElements, equals(25));
      expect(result.currentPage, equals(10));
      expect(result.isLast, isTrue);
    });

    test('should return empty result when no orders match filters', () async {
      // Act
      final result = await repository.getMyOrders(clientId: 999);

      // Assert
      expect(result.orders, isEmpty);
      expect(result.totalElements, equals(0));
      expect(result.totalPages, equals(1));
      expect(result.isFirst, isTrue);
      expect(result.isLast, isTrue);
    });

    test('should return unmodifiable list of orders', () async {
      // Act
      final result = await repository.getMyOrders();

      // Assert
      expect(() => result.orders.add(Order(
        id: 999,
        orderRequestNumber: 'TEST',
        clientId: 1,
        clientName: 'Test',
        orderDate: DateTime.now(),
        deliveryDate: DateTime.now(),
        totalAmount: 1000,
        approvalStatus: ApprovalStatus.pending,
        isClosed: false,
      )), throwsUnsupportedError);
    });

    test('should return correct mockClients map', () {
      // Act
      final clients = repository.mockClients;

      // Assert
      expect(clients.length, equals(8));
      expect(clients[1], equals('천사푸드'));
      expect(clients[2], equals('(유)경산식품'));
      expect(clients[3], equals('대한식품유통'));
      expect(clients[4], equals('행복마트'));
      expect(clients[5], equals('명품식자재'));
      expect(clients[6], equals('서울종합식품'));
      expect(clients[7], equals('그린유통'));
      expect(clients[8], equals('삼성식품'));
    });

    test('should return unmodifiable mockClients map', () {
      // Act
      final clients = repository.mockClients;

      // Assert
      expect(() => clients[999] = 'Test Client', throwsUnsupportedError);
    });

    test('should include all approval status types in mock data', () async {
      // Act
      final result = await repository.getMyOrders(page: 0, size: 100);

      // Assert
      final statuses = result.orders.map((o) => o.approvalStatus).toSet();
      expect(statuses.contains(ApprovalStatus.approved), isTrue);
      expect(statuses.contains(ApprovalStatus.pending), isTrue);
      expect(statuses.contains(ApprovalStatus.sendFailed), isTrue);
      expect(statuses.contains(ApprovalStatus.resend), isTrue);
    });

    test('should include both closed and open orders in mock data', () async {
      // Act
      final result = await repository.getMyOrders(page: 0, size: 100);

      // Assert
      final hasClosedOrders = result.orders.any((o) => o.isClosed);
      final hasOpenOrders = result.orders.any((o) => !o.isClosed);
      expect(hasClosedOrders, isTrue);
      expect(hasOpenOrders, isTrue);
    });
  });
}
