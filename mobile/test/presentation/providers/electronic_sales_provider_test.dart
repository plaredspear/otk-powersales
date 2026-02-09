import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/domain/entities/electronic_sales.dart';
import 'package:mobile/domain/repositories/electronic_sales_repository.dart';
import 'package:mobile/presentation/providers/electronic_sales_provider.dart';
import 'package:mobile/presentation/providers/electronic_sales_state.dart';

/// Mock Repository
class MockElectronicSalesRepository implements ElectronicSalesRepository {
  final List<ElectronicSales> _mockData = [
    const ElectronicSales(
      yearMonth: '202601',
      customerName: '농협',
      productName: '진라면',
      productCode: 'P001',
      amount: 1000000,
      quantity: 100,
      previousYearAmount: 900000,
      growthRate: 11.11,
    ),
    const ElectronicSales(
      yearMonth: '202601',
      customerName: '농협',
      productName: '케첩',
      productCode: 'P002',
      amount: 500000,
      quantity: 50,
      previousYearAmount: 550000,
      growthRate: -9.09,
    ),
    const ElectronicSales(
      yearMonth: '202601',
      customerName: 'GS25',
      productName: '진라면',
      productCode: 'P001',
      amount: 800000,
      quantity: 80,
      previousYearAmount: 700000,
      growthRate: 14.29,
    ),
  ];

  @override
  Future<List<ElectronicSales>> getElectronicSales({
    required String yearMonth,
    String? customerName,
    String? productName,
    String? productCode,
  }) async {
    var result = _mockData.where((sales) => sales.yearMonth == yearMonth);

    if (customerName != null) {
      result = result.where((sales) => sales.customerName == customerName);
    }

    if (productName != null) {
      result = result.where((sales) => sales.productName == productName);
    }

    if (productCode != null) {
      result = result.where((sales) => sales.productCode == productCode);
    }

    return result.toList();
  }

  @override
  Future<ElectronicSales?> getCustomerTotal({
    required String yearMonth,
    required String customerName,
  }) async {
    final sales = await getElectronicSales(
      yearMonth: yearMonth,
      customerName: customerName,
    );

    if (sales.isEmpty) return null;

    final totalAmount = sales.fold(0, (sum, s) => sum + s.amount);
    final totalQuantity = sales.fold(0, (sum, s) => sum + s.quantity);

    return ElectronicSales(
      yearMonth: yearMonth,
      customerName: customerName,
      productName: '합계',
      productCode: 'TOTAL',
      amount: totalAmount,
      quantity: totalQuantity,
    );
  }

  @override
  Future<ElectronicSales?> getProductTotal({
    required String yearMonth,
    required String productCode,
  }) async {
    final sales = await getElectronicSales(
      yearMonth: yearMonth,
      productCode: productCode,
    );

    if (sales.isEmpty) return null;

    final totalAmount = sales.fold(0, (sum, s) => sum + s.amount);
    final totalQuantity = sales.fold(0, (sum, s) => sum + s.quantity);
    final productName = sales.first.productName;

    return ElectronicSales(
      yearMonth: yearMonth,
      customerName: '합계',
      productName: productName,
      productCode: productCode,
      amount: totalAmount,
      quantity: totalQuantity,
    );
  }
}

void main() {
  group('ElectronicSalesProvider', () {
    late ProviderContainer container;
    late MockElectronicSalesRepository mockRepository;

    setUp(() {
      mockRepository = MockElectronicSalesRepository();
      container = ProviderContainer(
        overrides: [
          electronicSalesRepositoryProvider.overrideWithValue(mockRepository),
        ],
      );
    });

    tearDown(() {
      container.dispose();
    });

    test('초기 상태가 올바르게 설정된다', () {
      final state = container.read(electronicSalesProvider);

      expect(state.sales, isEmpty);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.totalAmount, 0);
      expect(state.totalQuantity, 0);
    });

    test('전산매출을 조회할 수 있다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchSales(yearMonth: '202601');

      final state = container.read(electronicSalesProvider);
      expect(state.sales.length, 3);
      expect(state.isLoading, false);
      expect(state.totalAmount, 2300000); // 1000000 + 500000 + 800000
      expect(state.totalQuantity, 230); // 100 + 50 + 80
    });

    test('거래처로 필터링하여 조회할 수 있다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchSales(
        yearMonth: '202601',
        customerName: '농협',
      );

      final state = container.read(electronicSalesProvider);
      expect(state.sales.length, 2);
      expect(state.sales.every((s) => s.customerName == '농협'), true);
      expect(state.totalAmount, 1500000);
    });

    test('제품 코드로 필터링하여 조회할 수 있다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchSales(
        yearMonth: '202601',
        productCode: 'P001',
      );

      final state = container.read(electronicSalesProvider);
      expect(state.sales.length, 2);
      expect(state.sales.every((s) => s.productCode == 'P001'), true);
      expect(state.totalAmount, 1800000);
    });

    test('평균 증감율이 올바르게 계산된다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchSales(yearMonth: '202601');

      final state = container.read(electronicSalesProvider);
      expect(state.averageGrowthRate, isNotNull);
      // (11.11 + (-9.09) + 14.29) / 3 = 5.437
      expect(state.averageGrowthRate!, closeTo(5.44, 0.01));
    });

    test('거래처 목록이 추출된다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchSales(yearMonth: '202601');

      final state = container.read(electronicSalesProvider);
      expect(state.customerList.length, 2);
      expect(state.customerList, contains('농협'));
      expect(state.customerList, contains('GS25'));
    });

    test('제품 목록이 추출된다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchSales(yearMonth: '202601');

      final state = container.read(electronicSalesProvider);
      expect(state.productList.length, 2);
      expect(state.productList, contains('진라면'));
      expect(state.productList, contains('케첩'));
    });

    test('거래처별로 조회할 수 있다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchSalesByCustomer(
        yearMonth: '202601',
        customerName: '농협',
      );

      final state = container.read(electronicSalesProvider);
      expect(state.sales.length, 2);
      expect(state.sales.every((s) => s.customerName == '농협'), true);
    });

    test('제품별로 조회할 수 있다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchSalesByProduct(
        yearMonth: '202601',
        productCode: 'P001',
      );

      final state = container.read(electronicSalesProvider);
      expect(state.sales.length, 2);
      expect(state.sales.every((s) => s.productCode == 'P001'), true);
    });

    test('거래처 합계를 조회할 수 있다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchCustomerTotal(
        yearMonth: '202601',
        customerName: '농협',
      );

      final state = container.read(electronicSalesProvider);
      expect(state.sales.length, 1);
      expect(state.sales.first.customerName, '농협');
      expect(state.totalAmount, 1500000);
      expect(state.totalQuantity, 150);
    });

    test('제품 합계를 조회할 수 있다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchProductTotal(
        yearMonth: '202601',
        productCode: 'P001',
      );

      final state = container.read(electronicSalesProvider);
      expect(state.sales.length, 1);
      expect(state.sales.first.productCode, 'P001');
      expect(state.totalAmount, 1800000);
      expect(state.totalQuantity, 180);
    });

    test('필터를 업데이트할 수 있다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      final filter = ElectronicSalesFilter(
        yearMonth: '202601',
        customerName: '농협',
      );

      await notifier.updateFilter(filter);

      final state = container.read(electronicSalesProvider);
      expect(state.filter.yearMonth, '202601');
      expect(state.filter.customerName, '농협');
      expect(state.sales.length, 2);
    });

    test('필터를 초기화할 수 있다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      // 먼저 필터 적용
      await notifier.fetchSales(
        yearMonth: '202601',
        customerName: '농협',
      );

      // 필터 초기화
      await notifier.resetFilter();

      final state = container.read(electronicSalesProvider);
      // 기본 필터 (현재 월)가 적용됨
      expect(state.filter.customerName, isNull);
      expect(state.filter.productName, isNull);
      expect(state.filter.productCode, isNull);
    });

    test('조회 완료 후 로딩 상태가 false가 된다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      // 비동기 호출
      await notifier.fetchSales(yearMonth: '202601');

      // 로딩 완료 후 상태 확인
      final state = container.read(electronicSalesProvider);
      expect(state.isLoading, false);
      expect(state.sales, isNotEmpty);
    });

    test('존재하지 않는 거래처 합계 조회 시 빈 리스트를 반환한다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchCustomerTotal(
        yearMonth: '202601',
        customerName: '존재하지않음',
      );

      final state = container.read(electronicSalesProvider);
      expect(state.sales, isEmpty);
    });

    test('존재하지 않는 제품 합계 조회 시 빈 리스트를 반환한다', () async {
      final notifier = container.read(electronicSalesProvider.notifier);

      await notifier.fetchProductTotal(
        yearMonth: '202601',
        productCode: 'P999',
      );

      final state = container.read(electronicSalesProvider);
      expect(state.sales, isEmpty);
    });
  });
}
