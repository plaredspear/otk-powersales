import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/electronic_sales_mock_repository.dart';
import 'package:mobile/domain/repositories/electronic_sales_repository.dart';

void main() {
  group('ElectronicSalesMockRepository', () {
    late ElectronicSalesRepository repository;

    setUp(() {
      repository = ElectronicSalesMockRepository();
    });

    group('getElectronicSales', () {
      test('년월로 전산매출을 조회한다', () async {
        final results = await repository.getElectronicSales(
          yearMonth: '202601',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.yearMonth == '202601'),
          true,
        );
      });

      test('거래처명으로 전산매출을 필터링한다', () async {
        final results = await repository.getElectronicSales(
          yearMonth: '202601',
          customerName: '농협',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.customerName.contains('농협')),
          true,
        );
      });

      test('제품명으로 전산매출을 필터링한다', () async {
        final results = await repository.getElectronicSales(
          yearMonth: '202601',
          productName: '진라면',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.productName.contains('진라면')),
          true,
        );
      });

      test('제품 코드로 전산매출을 필터링한다', () async {
        final results = await repository.getElectronicSales(
          yearMonth: '202601',
          productCode: 'PROD001',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.productCode == 'PROD001'),
          true,
        );
      });

      test('거래처명과 제품명으로 동시에 필터링한다', () async {
        final results = await repository.getElectronicSales(
          yearMonth: '202601',
          customerName: '농협',
          productName: '진라면',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) =>
              sales.customerName.contains('농협') &&
              sales.productName.contains('진라면')),
          true,
        );
      });

      test('조건에 맞는 데이터가 없으면 빈 리스트를 반환한다', () async {
        final results = await repository.getElectronicSales(
          yearMonth: '202701',
        );

        expect(results, isEmpty);
      });

      test('결과가 금액순(내림차순)으로 정렬된다', () async {
        final results = await repository.getElectronicSales(
          yearMonth: '202601',
        );

        expect(results, isNotEmpty);

        // 금액순 정렬 확인 (내림차순)
        for (var i = 0; i < results.length - 1; i++) {
          expect(
            results[i].amount >= results[i + 1].amount,
            true,
          );
        }
      });

      test('부분 거래처명 검색이 동작한다', () async {
        final results = await repository.getElectronicSales(
          yearMonth: '202601',
          customerName: 'GS',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.customerName.contains('GS')),
          true,
        );
      });

      test('부분 제품명 검색이 동작한다', () async {
        final results = await repository.getElectronicSales(
          yearMonth: '202601',
          productName: '라면',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.productName.contains('라면')),
          true,
        );
      });
    });

    group('getCustomerTotal', () {
      test('거래처별 전산매출 합계를 조회한다', () async {
        final result = await repository.getCustomerTotal(
          yearMonth: '202601',
          customerName: '농협',
        );

        expect(result, isNotNull);
        expect(result!.yearMonth, '202601');
        expect(result.customerName, '농협');
        expect(result.productName, '전체 합계');
        expect(result.productCode, 'TOTAL');
      });

      test('합계 금액이 정확하게 계산된다', () async {
        final result = await repository.getCustomerTotal(
          yearMonth: '202601',
          customerName: '농협',
        );

        expect(result, isNotNull);
        expect(result!.amount, greaterThan(0));
        expect(result.quantity, greaterThan(0));
      });

      test('전년 대비 데이터가 있으면 증감율이 계산된다', () async {
        final result = await repository.getCustomerTotal(
          yearMonth: '202601',
          customerName: '농협',
        );

        expect(result, isNotNull);
        // 농협 거래처는 전년 대비 데이터가 있음
        expect(result!.previousYearAmount, isNotNull);
        expect(result.previousYearAmount, greaterThan(0));
        expect(result.growthRate, isNotNull);
      });

      test('존재하지 않는 거래처는 null을 반환한다', () async {
        final result = await repository.getCustomerTotal(
          yearMonth: '202601',
          customerName: '존재하지 않는 거래처',
        );

        expect(result, isNull);
      });

      test('존재하지 않는 년월은 null을 반환한다', () async {
        final result = await repository.getCustomerTotal(
          yearMonth: '202701',
          customerName: '농협',
        );

        expect(result, isNull);
      });

      test('신규 거래처는 전년 대비 데이터가 없다', () async {
        final result = await repository.getCustomerTotal(
          yearMonth: '202601',
          customerName: '이마트24',
        );

        expect(result, isNotNull);
        // 이마트24는 신규 거래처 (전년 대비 데이터 없음)
        expect(result!.previousYearAmount, isNull);
        expect(result.growthRate, isNull);
      });
    });

    group('getProductTotal', () {
      test('제품별 전산매출 합계를 조회한다', () async {
        final result = await repository.getProductTotal(
          yearMonth: '202601',
          productCode: 'PROD001',
        );

        expect(result, isNotNull);
        expect(result!.yearMonth, '202601');
        expect(result.customerName, '전체 거래처');
        expect(result.productCode, 'PROD001');
        expect(result.productName, '진라면 매운맛');
      });

      test('합계 금액이 정확하게 계산된다', () async {
        final result = await repository.getProductTotal(
          yearMonth: '202601',
          productCode: 'PROD001',
        );

        expect(result, isNotNull);
        expect(result!.amount, greaterThan(0));
        expect(result.quantity, greaterThan(0));
      });

      test('전년 대비 데이터가 있으면 증감율이 계산된다', () async {
        final result = await repository.getProductTotal(
          yearMonth: '202601',
          productCode: 'PROD001',
        );

        expect(result, isNotNull);
        expect(result!.previousYearAmount, isNotNull);
        expect(result.previousYearAmount, greaterThan(0));
        expect(result.growthRate, isNotNull);
      });

      test('존재하지 않는 제품 코드는 null을 반환한다', () async {
        final result = await repository.getProductTotal(
          yearMonth: '202601',
          productCode: 'INVALID_CODE',
        );

        expect(result, isNull);
      });

      test('존재하지 않는 년월은 null을 반환한다', () async {
        final result = await repository.getProductTotal(
          yearMonth: '202701',
          productCode: 'PROD001',
        );

        expect(result, isNull);
      });

      test('여러 거래처의 합계가 정확하게 계산된다', () async {
        // PROD001(진라면 매운맛)은 여러 거래처에 판매됨
        final result = await repository.getProductTotal(
          yearMonth: '202601',
          productCode: 'PROD001',
        );

        expect(result, isNotNull);

        // 개별 조회 결과와 비교
        final individualSales = await repository.getElectronicSales(
          yearMonth: '202601',
          productCode: 'PROD001',
        );

        final totalAmount = individualSales.fold<int>(
          0,
          (sum, sales) => sum + sales.amount,
        );
        final totalQuantity = individualSales.fold<int>(
          0,
          (sum, sales) => sum + sales.quantity,
        );

        expect(result!.amount, totalAmount);
        expect(result.quantity, totalQuantity);
      });
    });

    group('비동기 처리 테스트', () {
      test('getElectronicSales는 Future를 반환한다', () {
        final result = repository.getElectronicSales(
          yearMonth: '202601',
        );

        expect(result, isA<Future<List>>());
      });

      test('getCustomerTotal은 Future를 반환한다', () {
        final result = repository.getCustomerTotal(
          yearMonth: '202601',
          customerName: '농협',
        );

        expect(result, isA<Future>());
      });

      test('getProductTotal은 Future를 반환한다', () {
        final result = repository.getProductTotal(
          yearMonth: '202601',
          productCode: 'PROD001',
        );

        expect(result, isA<Future>());
      });
    });
  });
}
