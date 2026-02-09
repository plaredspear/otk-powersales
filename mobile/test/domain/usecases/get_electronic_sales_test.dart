import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/electronic_sales.dart';
import 'package:mobile/domain/repositories/electronic_sales_repository.dart';
import 'package:mobile/domain/usecases/get_electronic_sales.dart';

/// Mock Repository
class MockElectronicSalesRepository implements ElectronicSalesRepository {
  final List<ElectronicSales> _mockData = [
    ElectronicSales(
      yearMonth: '202601',
      customerName: '농협',
      productName: '진라면',
      productCode: 'P001',
      amount: 1000000,
      quantity: 100,
      previousYearAmount: 900000,
      growthRate: 11.11,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: '농협',
      productName: '케첩',
      productCode: 'P002',
      amount: 500000,
      quantity: 50,
      previousYearAmount: 550000,
      growthRate: -9.09,
    ),
    ElectronicSales(
      yearMonth: '202601',
      customerName: 'GS25',
      productName: '진라면',
      productCode: 'P001',
      amount: 800000,
      quantity: 80,
      previousYearAmount: 700000,
      growthRate: 14.29,
    ),
    ElectronicSales(
      yearMonth: '202512',
      customerName: '농협',
      productName: '진라면',
      productCode: 'P001',
      amount: 950000,
      quantity: 95,
      previousYearAmount: 850000,
      growthRate: 11.76,
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
  group('GetElectronicSales UseCase', () {
    late GetElectronicSales useCase;
    late MockElectronicSalesRepository mockRepository;

    setUp(() {
      mockRepository = MockElectronicSalesRepository();
      useCase = GetElectronicSales(mockRepository);
    });

    group('기본 조회 (call)', () {
      test('년월로 전산매출을 조회한다', () async {
        final result = await useCase(yearMonth: '202601');

        expect(result.length, 3);
        expect(result.every((s) => s.yearMonth == '202601'), true);
      });

      test('년월 + 거래처로 필터링한다', () async {
        final result = await useCase(
          yearMonth: '202601',
          customerName: '농협',
        );

        expect(result.length, 2);
        expect(result.every((s) => s.customerName == '농협'), true);
      });

      test('년월 + 제품명으로 필터링한다', () async {
        final result = await useCase(
          yearMonth: '202601',
          productName: '진라면',
        );

        expect(result.length, 2);
        expect(result.every((s) => s.productName == '진라면'), true);
      });

      test('년월 + 제품 코드로 필터링한다', () async {
        final result = await useCase(
          yearMonth: '202601',
          productCode: 'P001',
        );

        expect(result.length, 2);
        expect(result.every((s) => s.productCode == 'P001'), true);
      });

      test('조건에 맞는 데이터가 없으면 빈 리스트를 반환한다', () async {
        final result = await useCase(
          yearMonth: '202601',
          customerName: '존재하지않는거래처',
        );

        expect(result, isEmpty);
      });
    });

    group('거래처별 조회 (getByCustomer)', () {
      test('특정 거래처의 전산매출을 조회한다', () async {
        final result = await useCase.getByCustomer(
          yearMonth: '202601',
          customerName: '농협',
        );

        expect(result.length, 2);
        expect(result.every((s) => s.customerName == '농협'), true);
      });

      test('존재하지 않는 거래처는 빈 리스트를 반환한다', () async {
        final result = await useCase.getByCustomer(
          yearMonth: '202601',
          customerName: '존재하지않음',
        );

        expect(result, isEmpty);
      });
    });

    group('제품별 조회 (getByProduct)', () {
      test('특정 제품의 전산매출을 조회한다', () async {
        final result = await useCase.getByProduct(
          yearMonth: '202601',
          productCode: 'P001',
        );

        expect(result.length, 2);
        expect(result.every((s) => s.productCode == 'P001'), true);
      });

      test('존재하지 않는 제품은 빈 리스트를 반환한다', () async {
        final result = await useCase.getByProduct(
          yearMonth: '202601',
          productCode: 'P999',
        );

        expect(result, isEmpty);
      });
    });

    group('거래처 합계 조회 (getCustomerTotal)', () {
      test('거래처의 전체 실적 합계를 조회한다', () async {
        final result = await useCase.getCustomerTotal(
          yearMonth: '202601',
          customerName: '농협',
        );

        expect(result, isNotNull);
        expect(result!.customerName, '농협');
        expect(result.amount, 1500000); // 1000000 + 500000
        expect(result.quantity, 150); // 100 + 50
      });

      test('존재하지 않는 거래처는 null을 반환한다', () async {
        final result = await useCase.getCustomerTotal(
          yearMonth: '202601',
          customerName: '존재하지않음',
        );

        expect(result, isNull);
      });
    });

    group('제품 합계 조회 (getProductTotal)', () {
      test('제품의 전체 실적 합계를 조회한다', () async {
        final result = await useCase.getProductTotal(
          yearMonth: '202601',
          productCode: 'P001',
        );

        expect(result, isNotNull);
        expect(result!.productCode, 'P001');
        expect(result.amount, 1800000); // 1000000 + 800000
        expect(result.quantity, 180); // 100 + 80
      });

      test('존재하지 않는 제품은 null을 반환한다', () async {
        final result = await useCase.getProductTotal(
          yearMonth: '202601',
          productCode: 'P999',
        );

        expect(result, isNull);
      });
    });

    group('총 금액 계산 (calculateTotalAmount)', () {
      test('전산매출 목록의 총 금액을 계산한다', () async {
        final salesList = await useCase(yearMonth: '202601');
        final total = useCase.calculateTotalAmount(salesList);

        expect(total, 2300000); // 1000000 + 500000 + 800000
      });

      test('빈 리스트는 0을 반환한다', () {
        final total = useCase.calculateTotalAmount([]);
        expect(total, 0);
      });
    });

    group('총 수량 계산 (calculateTotalQuantity)', () {
      test('전산매출 목록의 총 수량을 계산한다', () async {
        final salesList = await useCase(yearMonth: '202601');
        final total = useCase.calculateTotalQuantity(salesList);

        expect(total, 230); // 100 + 50 + 80
      });

      test('빈 리스트는 0을 반환한다', () {
        final total = useCase.calculateTotalQuantity([]);
        expect(total, 0);
      });
    });

    group('평균 증감율 계산 (calculateAverageGrowthRate)', () {
      test('전산매출 목록의 평균 증감율을 계산한다', () async {
        final salesList = await useCase(yearMonth: '202601');
        final avgRate = useCase.calculateAverageGrowthRate(salesList);

        expect(avgRate, isNotNull);
        // (11.11 + (-9.09) + 14.29) / 3 = 5.437
        expect(avgRate!, closeTo(5.44, 0.01));
      });

      test('빈 리스트는 null을 반환한다', () {
        final avgRate = useCase.calculateAverageGrowthRate([]);
        expect(avgRate, isNull);
      });

      test('증감율이 없는 데이터만 있으면 null을 반환한다', () {
        final salesList = [
          const ElectronicSales(
            yearMonth: '202601',
            customerName: '테스트',
            productName: '테스트',
            productCode: 'TEST',
            amount: 100,
            quantity: 10,
            growthRate: null,
          ),
        ];

        final avgRate = useCase.calculateAverageGrowthRate(salesList);
        expect(avgRate, isNull);
      });
    });

    group('증감율 계산 (calculateGrowthRate)', () {
      test('전년 대비 증감율을 계산한다', () {
        final rate = useCase.calculateGrowthRate(
          currentAmount: 1100,
          previousAmount: 1000,
        );

        expect(rate, 10.0); // 10% 증가
      });

      test('감소한 경우 음수 증감율을 반환한다', () {
        final rate = useCase.calculateGrowthRate(
          currentAmount: 900,
          previousAmount: 1000,
        );

        expect(rate, -10.0); // 10% 감소
      });

      test('전년 실적이 0이면 null을 반환한다', () {
        final rate = useCase.calculateGrowthRate(
          currentAmount: 1000,
          previousAmount: 0,
        );

        expect(rate, isNull);
      });

      test('둘 다 0이면 null을 반환한다', () {
        final rate = useCase.calculateGrowthRate(
          currentAmount: 0,
          previousAmount: 0,
        );

        expect(rate, isNull);
      });
    });

    group('거래처 목록 추출 (extractCustomers)', () {
      test('중복 제거된 거래처 목록을 추출한다', () async {
        final salesList = await useCase(yearMonth: '202601');
        final customers = useCase.extractCustomers(salesList);

        expect(customers.length, 2);
        expect(customers, contains('농협'));
        expect(customers, contains('GS25'));
      });

      test('거래처 목록이 정렬되어 있다', () async {
        final salesList = await useCase(yearMonth: '202601');
        final customers = useCase.extractCustomers(salesList);

        expect(customers, ['GS25', '농협']); // 알파벳 순
      });

      test('빈 리스트는 빈 목록을 반환한다', () {
        final customers = useCase.extractCustomers([]);
        expect(customers, isEmpty);
      });
    });

    group('제품 목록 추출 (extractProducts)', () {
      test('중복 제거된 제품 목록을 추출한다', () async {
        final salesList = await useCase(yearMonth: '202601');
        final products = useCase.extractProducts(salesList);

        expect(products.length, 2);
        expect(products, contains('진라면'));
        expect(products, contains('케첩'));
      });

      test('제품 목록이 정렬되어 있다', () async {
        final salesList = await useCase(yearMonth: '202601');
        final products = useCase.extractProducts(salesList);

        expect(products.length, 2);
        expect(products, contains('진라면'));
        expect(products, contains('케첩'));
        // 정렬 확인 (첫 번째 항목이 마지막 항목보다 사전순으로 앞서야 함)
        expect(products[0].compareTo(products[1]), lessThan(0));
      });

      test('빈 리스트는 빈 목록을 반환한다', () {
        final products = useCase.extractProducts([]);
        expect(products, isEmpty);
      });
    });
  });
}
