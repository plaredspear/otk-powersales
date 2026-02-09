import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/pos_sales_mock_repository.dart';
import 'package:mobile/domain/repositories/pos_sales_repository.dart';

void main() {
  group('PosSalesMockRepository', () {
    late PosSalesRepository repository;

    setUp(() {
      repository = PosSalesMockRepository();
    });

    group('getPosSales', () {
      test('날짜 범위로 POS 매출을 조회한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSales(
          startDate: startDate,
          endDate: endDate,
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) =>
              sales.salesDate.isAfter(startDate.subtract(const Duration(days: 1))) &&
              sales.salesDate.isBefore(endDate.add(const Duration(days: 1)))),
          true,
        );
      });

      test('매장명으로 POS 매출을 필터링한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSales(
          startDate: startDate,
          endDate: endDate,
          storeName: '이마트 강남점',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.storeName.contains('이마트 강남점')),
          true,
        );
      });

      test('제품명으로 POS 매출을 필터링한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSales(
          startDate: startDate,
          endDate: endDate,
          productName: '진라면',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.productName.contains('진라면')),
          true,
        );
      });

      test('매장명과 제품명으로 동시에 필터링한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSales(
          startDate: startDate,
          endDate: endDate,
          storeName: '이마트',
          productName: '진라면',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) =>
              sales.storeName.contains('이마트') &&
              sales.productName.contains('진라면')),
          true,
        );
      });

      test('조건에 맞는 데이터가 없으면 빈 리스트를 반환한다', () async {
        final startDate = DateTime(2027, 1, 1);
        final endDate = DateTime(2027, 1, 31);

        final results = await repository.getPosSales(
          startDate: startDate,
          endDate: endDate,
        );

        expect(results, isEmpty);
      });

      test('결과가 날짜순(최신순)으로 정렬된다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSales(
          startDate: startDate,
          endDate: endDate,
        );

        expect(results, isNotEmpty);

        // 날짜순 정렬 확인 (최신순)
        for (var i = 0; i < results.length - 1; i++) {
          expect(
            results[i].salesDate.isAfter(results[i + 1].salesDate) ||
                results[i].salesDate.isAtSameMomentAs(results[i + 1].salesDate),
            true,
          );
        }
      });

      test('부분 매장명 검색이 동작한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSales(
          startDate: startDate,
          endDate: endDate,
          storeName: '강남',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.storeName.contains('강남')),
          true,
        );
      });

      test('부분 제품명 검색이 동작한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSales(
          startDate: startDate,
          endDate: endDate,
          productName: '라면',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.productName.contains('라면')),
          true,
        );
      });
    });

    group('getPosSalesByProduct', () {
      test('제품 코드로 POS 매출을 조회한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSalesByProduct(
          productCode: 'PROD001',
          startDate: startDate,
          endDate: endDate,
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.productCode == 'PROD001'),
          true,
        );
      });

      test('날짜 범위를 벗어난 데이터는 제외된다', () async {
        final startDate = DateTime(2026, 1, 15);
        final endDate = DateTime(2026, 1, 20);

        final results = await repository.getPosSalesByProduct(
          productCode: 'PROD001',
          startDate: startDate,
          endDate: endDate,
        );

        expect(
          results.every((sales) =>
              sales.salesDate.isAfter(startDate.subtract(const Duration(days: 1))) &&
              sales.salesDate.isBefore(endDate.add(const Duration(days: 1)))),
          true,
        );
      });

      test('존재하지 않는 제품 코드는 빈 리스트를 반환한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSalesByProduct(
          productCode: 'INVALID_CODE',
          startDate: startDate,
          endDate: endDate,
        );

        expect(results, isEmpty);
      });

      test('결과가 날짜순(최신순)으로 정렬된다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSalesByProduct(
          productCode: 'PROD001',
          startDate: startDate,
          endDate: endDate,
        );

        expect(results, isNotEmpty);

        // 날짜순 정렬 확인 (최신순)
        for (var i = 0; i < results.length - 1; i++) {
          expect(
            results[i].salesDate.isAfter(results[i + 1].salesDate) ||
                results[i].salesDate.isAtSameMomentAs(results[i + 1].salesDate),
            true,
          );
        }
      });
    });

    group('getPosSalesByStore', () {
      test('매장명으로 POS 매출을 조회한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSalesByStore(
          storeName: '이마트 강남점',
          startDate: startDate,
          endDate: endDate,
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.storeName == '이마트 강남점'),
          true,
        );
      });

      test('날짜 범위를 벗어난 데이터는 제외된다', () async {
        final startDate = DateTime(2026, 1, 10);
        final endDate = DateTime(2026, 1, 20);

        final results = await repository.getPosSalesByStore(
          storeName: '이마트 강남점',
          startDate: startDate,
          endDate: endDate,
        );

        expect(
          results.every((sales) =>
              sales.salesDate.isAfter(startDate.subtract(const Duration(days: 1))) &&
              sales.salesDate.isBefore(endDate.add(const Duration(days: 1)))),
          true,
        );
      });

      test('존재하지 않는 매장명은 빈 리스트를 반환한다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSalesByStore(
          storeName: '존재하지 않는 매장',
          startDate: startDate,
          endDate: endDate,
        );

        expect(results, isEmpty);
      });

      test('결과가 날짜순(최신순)으로 정렬된다', () async {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final results = await repository.getPosSalesByStore(
          storeName: '이마트 강남점',
          startDate: startDate,
          endDate: endDate,
        );

        expect(results, isNotEmpty);

        // 날짜순 정렬 확인 (최신순)
        for (var i = 0; i < results.length - 1; i++) {
          expect(
            results[i].salesDate.isAfter(results[i + 1].salesDate) ||
                results[i].salesDate.isAtSameMomentAs(results[i + 1].salesDate),
            true,
          );
        }
      });
    });

    group('비동기 처리 테스트', () {
      test('getPosSales는 Future를 반환한다', () {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final result = repository.getPosSales(
          startDate: startDate,
          endDate: endDate,
        );

        expect(result, isA<Future<List>>());
      });

      test('getPosSalesByProduct는 Future를 반환한다', () {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final result = repository.getPosSalesByProduct(
          productCode: 'PROD001',
          startDate: startDate,
          endDate: endDate,
        );

        expect(result, isA<Future<List>>());
      });

      test('getPosSalesByStore는 Future를 반환한다', () {
        final startDate = DateTime(2026, 1, 1);
        final endDate = DateTime(2026, 1, 31);

        final result = repository.getPosSalesByStore(
          storeName: '이마트 강남점',
          startDate: startDate,
          endDate: endDate,
        );

        expect(result, isA<Future<List>>());
      });
    });
  });
}
