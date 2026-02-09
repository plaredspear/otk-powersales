import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/logistics_sales_mock_repository.dart';
import 'package:mobile/domain/entities/logistics_sales.dart';
import 'package:mobile/domain/repositories/logistics_sales_repository.dart';

void main() {
  group('LogisticsSalesMockRepository', () {
    late LogisticsSalesRepository repository;

    setUp(() {
      repository = LogisticsSalesMockRepository();
    });

    group('getLogisticsSales', () {
      test('년월로 물류매출을 조회한다', () async {
        final results = await repository.getLogisticsSales(
          yearMonth: '202601',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.yearMonth == '202601'),
          true,
        );
      });

      test('카테고리로 물류매출을 필터링한다', () async {
        final results = await repository.getLogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.ramen,
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.category == LogisticsCategory.ramen),
          true,
        );
      });

      test('존재하지 않는 년월은 빈 리스트를 반환한다', () async {
        final results = await repository.getLogisticsSales(
          yearMonth: '202701',
        );

        expect(results, isEmpty);
      });

      test('결과가 카테고리 코드순으로 정렬된다', () async {
        final results = await repository.getLogisticsSales(
          yearMonth: '202601',
        );

        expect(results, isNotEmpty);

        // 카테고리 코드순 정렬 확인
        for (var i = 0; i < results.length - 1; i++) {
          expect(
            results[i].category.code.compareTo(results[i + 1].category.code) <= 0,
            true,
          );
        }
      });
    });

    group('getLogisticsSalesByCategory', () {
      test('특정 카테고리의 물류매출을 조회한다', () async {
        final results = await repository.getLogisticsSalesByCategory(
          yearMonth: '202601',
          category: LogisticsCategory.normal,
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) =>
              sales.yearMonth == '202601' &&
              sales.category == LogisticsCategory.normal),
          true,
        );
      });

      test('존재하지 않는 년월 + 카테고리는 빈 리스트를 반환한다', () async {
        final results = await repository.getLogisticsSalesByCategory(
          yearMonth: '202701',
          category: LogisticsCategory.ramen,
        );

        expect(results, isEmpty);
      });

      test('각 카테고리별로 데이터를 조회할 수 있다', () async {
        // 상온
        final normalResults = await repository.getLogisticsSalesByCategory(
          yearMonth: '202601',
          category: LogisticsCategory.normal,
        );
        expect(normalResults, isNotEmpty);

        // 라면
        final ramenResults = await repository.getLogisticsSalesByCategory(
          yearMonth: '202601',
          category: LogisticsCategory.ramen,
        );
        expect(ramenResults, isNotEmpty);

        // 냉동/냉장
        final frozenResults = await repository.getLogisticsSalesByCategory(
          yearMonth: '202601',
          category: LogisticsCategory.frozen,
        );
        expect(frozenResults, isNotEmpty);
      });
    });

    group('getLogisticsSalesTrend', () {
      test('카테고리별 월별 추이를 조회한다', () async {
        final results = await repository.getLogisticsSalesTrend(
          startYearMonth: '202510',
          endYearMonth: '202601',
          category: LogisticsCategory.ramen,
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.category == LogisticsCategory.ramen),
          true,
        );
      });

      test('결과가 년월순(오름차순)으로 정렬된다', () async {
        final results = await repository.getLogisticsSalesTrend(
          startYearMonth: '202510',
          endYearMonth: '202601',
          category: LogisticsCategory.ramen,
        );

        expect(results, isNotEmpty);

        // 년월순 정렬 확인 (오름차순 - 시간 순서)
        for (var i = 0; i < results.length - 1; i++) {
          expect(
            results[i].yearMonth.compareTo(results[i + 1].yearMonth) <= 0,
            true,
          );
        }
      });

      test('범위 밖의 데이터는 포함되지 않는다', () async {
        final results = await repository.getLogisticsSalesTrend(
          startYearMonth: '202511',
          endYearMonth: '202512',
          category: LogisticsCategory.ramen,
        );

        expect(
          results.every((sales) =>
              sales.yearMonth.compareTo('202511') >= 0 &&
              sales.yearMonth.compareTo('202512') <= 0),
          true,
        );
      });

      test('존재하지 않는 범위는 빈 리스트를 반환한다', () async {
        final results = await repository.getLogisticsSalesTrend(
          startYearMonth: '202701',
          endYearMonth: '202712',
          category: LogisticsCategory.ramen,
        );

        expect(results, isEmpty);
      });
    });

    group('getCurrentMonthSales', () {
      test('당월 물류예상실적을 조회한다', () async {
        final results = await repository.getCurrentMonthSales();

        expect(results, isNotEmpty);
        expect(
          results.every((sales) => sales.isCurrentMonth),
          true,
        );
      });

      test('카테고리로 당월 실적을 필터링한다', () async {
        final results = await repository.getCurrentMonthSales(
          category: LogisticsCategory.ramen,
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) =>
              sales.isCurrentMonth &&
              sales.category == LogisticsCategory.ramen),
          true,
        );
      });

      test('결과가 카테고리 코드순으로 정렬된다', () async {
        final results = await repository.getCurrentMonthSales();

        expect(results, isNotEmpty);

        // 카테고리 코드순 정렬 확인
        for (var i = 0; i < results.length - 1; i++) {
          expect(
            results[i].category.code.compareTo(results[i + 1].category.code) <= 0,
            true,
          );
        }
      });
    });

    group('getClosedSales', () {
      test('마감 실적을 조회한다', () async {
        final results = await repository.getClosedSales(
          yearMonth: '202512',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) =>
              !sales.isCurrentMonth && sales.yearMonth == '202512'),
          true,
        );
      });

      test('카테고리로 마감 실적을 필터링한다', () async {
        final results = await repository.getClosedSales(
          yearMonth: '202512',
          category: LogisticsCategory.frozen,
        );

        expect(results, isNotEmpty);
        expect(
          results.every((sales) =>
              !sales.isCurrentMonth &&
              sales.category == LogisticsCategory.frozen),
          true,
        );
      });

      test('존재하지 않는 년월은 빈 리스트를 반환한다', () async {
        final results = await repository.getClosedSales(
          yearMonth: '202701',
        );

        expect(results, isEmpty);
      });

      test('당월 물류예상실적은 포함되지 않는다', () async {
        final results = await repository.getClosedSales(
          yearMonth: '202601',
        );

        // 202601은 당월 물류예상실적만 있으므로 마감 실적은 없음
        expect(results, isEmpty);
      });

      test('결과가 카테고리 코드순으로 정렬된다', () async {
        final results = await repository.getClosedSales(
          yearMonth: '202512',
        );

        expect(results, isNotEmpty);

        // 카테고리 코드순 정렬 확인
        for (var i = 0; i < results.length - 1; i++) {
          expect(
            results[i].category.code.compareTo(results[i + 1].category.code) <= 0,
            true,
          );
        }
      });
    });

    group('비동기 처리 테스트', () {
      test('getLogisticsSales는 Future를 반환한다', () {
        final result = repository.getLogisticsSales(
          yearMonth: '202601',
        );

        expect(result, isA<Future<List<LogisticsSales>>>());
      });

      test('getLogisticsSalesByCategory는 Future를 반환한다', () {
        final result = repository.getLogisticsSalesByCategory(
          yearMonth: '202601',
          category: LogisticsCategory.ramen,
        );

        expect(result, isA<Future<List<LogisticsSales>>>());
      });

      test('getLogisticsSalesTrend는 Future를 반환한다', () {
        final result = repository.getLogisticsSalesTrend(
          startYearMonth: '202510',
          endYearMonth: '202601',
          category: LogisticsCategory.ramen,
        );

        expect(result, isA<Future<List<LogisticsSales>>>());
      });

      test('getCurrentMonthSales는 Future를 반환한다', () {
        final result = repository.getCurrentMonthSales();

        expect(result, isA<Future<List<LogisticsSales>>>());
      });

      test('getClosedSales는 Future를 반환한다', () {
        final result = repository.getClosedSales(
          yearMonth: '202512',
        );

        expect(result, isA<Future<List<LogisticsSales>>>());
      });
    });
  });
}
