import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/logistics_sales.dart';
import 'package:mobile/domain/repositories/logistics_sales_repository.dart';
import 'package:mobile/domain/usecases/get_logistics_sales.dart';

/// Mock Repository for testing
class MockLogisticsSalesRepository implements LogisticsSalesRepository {
  final List<LogisticsSales> _mockData;

  MockLogisticsSalesRepository(this._mockData);

  @override
  Future<List<LogisticsSales>> getLogisticsSales({
    required String yearMonth,
    LogisticsCategory? category,
    String? customerName,
  }) async {
    var result = _mockData.where((sale) {
      if (sale.yearMonth != yearMonth) return false;
      if (category != null && sale.category != category) return false;
      if (customerName != null && sale.customerName != customerName) {
        return false;
      }
      return true;
    }).toList();

    result.sort((a, b) => a.category.code.compareTo(b.category.code));
    return result;
  }

  @override
  Future<List<LogisticsSales>> getLogisticsSalesByCategory({
    required String yearMonth,
    required LogisticsCategory category,
  }) async {
    final result = _mockData
        .where((sale) =>
            sale.yearMonth == yearMonth && sale.category == category)
        .toList();
    return result;
  }

  @override
  Future<List<LogisticsSales>> getLogisticsSalesTrend({
    required String startYearMonth,
    required String endYearMonth,
    required LogisticsCategory category,
  }) async {
    final result = _mockData.where((sale) {
      return sale.yearMonth.compareTo(startYearMonth) >= 0 &&
          sale.yearMonth.compareTo(endYearMonth) <= 0 &&
          sale.category == category;
    }).toList();

    result.sort((a, b) => a.yearMonth.compareTo(b.yearMonth));
    return result;
  }

  @override
  Future<List<LogisticsSales>> getCurrentMonthSales({
    LogisticsCategory? category,
  }) async {
    var result =
        _mockData.where((sale) => sale.isCurrentMonth == true).toList();

    if (category != null) {
      result = result.where((sale) => sale.category == category).toList();
    }

    result.sort((a, b) => a.category.code.compareTo(b.category.code));
    return result;
  }

  @override
  Future<List<LogisticsSales>> getClosedSales({
    required String yearMonth,
    LogisticsCategory? category,
  }) async {
    var result = _mockData.where((sale) {
      return sale.yearMonth == yearMonth && sale.isCurrentMonth == false;
    }).toList();

    if (category != null) {
      result = result.where((sale) => sale.category == category).toList();
    }

    result.sort((a, b) => a.category.code.compareTo(b.category.code));
    return result;
  }
}

void main() {
  late List<LogisticsSales> mockData;
  late MockLogisticsSalesRepository mockRepository;
  late GetLogisticsSales useCase;

  setUp(() {
    // Mock 데이터 생성
    mockData = [
      // 당월 물류예상실적 (202602)
      const LogisticsSales(
        yearMonth: '202602',
        category: LogisticsCategory.normal,
        currentAmount: 50000000,
        previousYearAmount: 45000000,
        difference: 5000000,
        growthRate: 11.11,
        isCurrentMonth: true,
      ),
      const LogisticsSales(
        yearMonth: '202602',
        category: LogisticsCategory.ramen,
        currentAmount: 30000000,
        previousYearAmount: 28000000,
        difference: 2000000,
        growthRate: 7.14,
        isCurrentMonth: true,
      ),
      const LogisticsSales(
        yearMonth: '202602',
        category: LogisticsCategory.frozen,
        currentAmount: 20000000,
        previousYearAmount: 18000000,
        difference: 2000000,
        growthRate: 11.11,
        isCurrentMonth: true,
      ),

      // 이전월 마감실적 (202601)
      const LogisticsSales(
        yearMonth: '202601',
        category: LogisticsCategory.normal,
        currentAmount: 48000000,
        previousYearAmount: 44000000,
        difference: 4000000,
        growthRate: 9.09,
        isCurrentMonth: false,
      ),
      const LogisticsSales(
        yearMonth: '202601',
        category: LogisticsCategory.ramen,
        currentAmount: 28000000,
        previousYearAmount: 26000000,
        difference: 2000000,
        growthRate: 7.69,
        isCurrentMonth: false,
      ),
      const LogisticsSales(
        yearMonth: '202601',
        category: LogisticsCategory.frozen,
        currentAmount: 19000000,
        previousYearAmount: 17000000,
        difference: 2000000,
        growthRate: 11.76,
        isCurrentMonth: false,
      ),

      // 이전월 마감실적 (202512)
      const LogisticsSales(
        yearMonth: '202512',
        category: LogisticsCategory.normal,
        currentAmount: 52000000,
        previousYearAmount: 46000000,
        difference: 6000000,
        growthRate: 13.04,
        isCurrentMonth: false,
      ),
      const LogisticsSales(
        yearMonth: '202512',
        category: LogisticsCategory.ramen,
        currentAmount: 32000000,
        previousYearAmount: 29000000,
        difference: 3000000,
        growthRate: 10.34,
        isCurrentMonth: false,
      ),

      // 거래처명 포함 데이터
      const LogisticsSales(
        yearMonth: '202601',
        category: LogisticsCategory.normal,
        currentAmount: 10000000,
        previousYearAmount: 9000000,
        difference: 1000000,
        growthRate: 11.11,
        isCurrentMonth: false,
        customerName: '이마트',
      ),
    ];

    mockRepository = MockLogisticsSalesRepository(mockData);
    useCase = GetLogisticsSales(mockRepository);
  });

  group('GetLogisticsSales - 기본 조회', () {
    test('call 메서드가 년월별 물류매출을 조회한다', () async {
      final result = await useCase(yearMonth: '202602');

      expect(result.length, 3);
      expect(result.every((s) => s.yearMonth == '202602'), true);
      expect(result.every((s) => s.isCurrentMonth == true), true);
    });

    test('call 메서드가 카테고리 필터링을 적용한다', () async {
      final result = await useCase(
        yearMonth: '202602',
        category: LogisticsCategory.normal,
      );

      expect(result.length, 1);
      expect(result.first.category, LogisticsCategory.normal);
    });

    test('call 메서드가 거래처명 필터링을 적용한다', () async {
      final result = await useCase(
        yearMonth: '202601',
        customerName: '이마트',
      );

      expect(result.length, 1);
      expect(result.first.customerName, '이마트');
    });

    test('call 메서드가 조건에 맞는 데이터가 없으면 빈 리스트를 반환한다', () async {
      final result = await useCase(yearMonth: '202699');

      expect(result, isEmpty);
    });
  });

  group('GetLogisticsSales - 카테고리별 조회', () {
    test('getByCategory가 특정 카테고리의 매출을 조회한다', () async {
      final result = await useCase.getByCategory(
        yearMonth: '202602',
        category: LogisticsCategory.ramen,
      );

      expect(result.length, 1);
      expect(result.first.category, LogisticsCategory.ramen);
      expect(result.first.currentAmount, 30000000);
    });

    test('getByCategory가 냉동/냉장 카테고리를 조회한다', () async {
      final result = await useCase.getByCategory(
        yearMonth: '202602',
        category: LogisticsCategory.frozen,
      );

      expect(result.length, 1);
      expect(result.first.category, LogisticsCategory.frozen);
    });
  });

  group('GetLogisticsSales - 월별 추이 조회', () {
    test('getTrend가 기간 내 카테고리별 추이를 조회한다', () async {
      final result = await useCase.getTrend(
        startYearMonth: '202512',
        endYearMonth: '202602',
        category: LogisticsCategory.normal,
      );

      expect(result.length, 4); // 202512, 202601(2개 - 일반+이마트), 202602
      expect(result.every((s) => s.category == LogisticsCategory.normal), true);
      // 년월순 정렬 확인
      expect(result[0].yearMonth, '202512');
      expect(result[1].yearMonth, '202601');
      expect(result[2].yearMonth, '202601'); // 이마트 거래처 데이터
      expect(result[3].yearMonth, '202602');
    });

    test('getTrend가 시작 년월이 종료 년월보다 크면 에러를 발생시킨다', () async {
      expect(
        () => useCase.getTrend(
          startYearMonth: '202602',
          endYearMonth: '202601',
          category: LogisticsCategory.normal,
        ),
        throwsArgumentError,
      );
    });
  });

  group('GetLogisticsSales - 당월/이전월 조회', () {
    test('getCurrentMonthSales가 당월 물류예상실적을 조회한다', () async {
      final result = await useCase.getCurrentMonthSales();

      expect(result.length, 3);
      expect(result.every((s) => s.isCurrentMonth == true), true);
      expect(result.every((s) => s.yearMonth == '202602'), true);
    });

    test('getCurrentMonthSales가 카테고리 필터링을 적용한다', () async {
      final result = await useCase.getCurrentMonthSales(
        category: LogisticsCategory.ramen,
      );

      expect(result.length, 1);
      expect(result.first.category, LogisticsCategory.ramen);
      expect(result.first.isCurrentMonth, true);
    });

    test('getClosedSales가 이전월 마감실적을 조회한다', () async {
      final result = await useCase.getClosedSales(
        yearMonth: '202601',
      );

      expect(result.length, 4); // 3개 카테고리 + 이마트 거래처 데이터
      expect(result.every((s) => s.isCurrentMonth == false), true);
      expect(result.every((s) => s.yearMonth == '202601'), true);
    });

    test('getClosedSales가 카테고리 필터링을 적용한다', () async {
      final result = await useCase.getClosedSales(
        yearMonth: '202601',
        category: LogisticsCategory.frozen,
      );

      expect(result.length, 1);
      expect(result.first.category, LogisticsCategory.frozen);
      expect(result.first.isCurrentMonth, false);
    });
  });

  group('GetLogisticsSales - 당월/이전월 구분 로직', () {
    test('getSalesByMonthType가 당월이면 물류예상실적을 조회한다', () async {
      final result = await useCase.getSalesByMonthType(
        yearMonth: '202602',
        currentYearMonth: '202602',
      );

      expect(result.length, 3);
      expect(result.every((s) => s.isCurrentMonth == true), true);
    });

    test('getSalesByMonthType가 이전월이면 마감실적을 조회한다', () async {
      final result = await useCase.getSalesByMonthType(
        yearMonth: '202601',
        currentYearMonth: '202602',
      );

      expect(result.length, 4); // 3개 카테고리 + 이마트 거래처 데이터
      expect(result.every((s) => s.isCurrentMonth == false), true);
      expect(result.every((s) => s.yearMonth == '202601'), true);
    });

    test('getSalesByMonthType가 currentYearMonth가 없으면 시스템 현재 년월을 사용한다',
        () async {
      // 시스템 현재 년월은 항상 당월 데이터와 다르므로 마감실적 조회됨
      final result = await useCase.getSalesByMonthType(
        yearMonth: '202601',
      );

      // 현재 시스템 년월이 202602가 아니므로 마감실적 조회
      expect(result.every((s) => s.yearMonth == '202601'), true);
    });

    test('getSalesByMonthType가 카테고리 필터링을 적용한다', () async {
      final result = await useCase.getSalesByMonthType(
        yearMonth: '202602',
        currentYearMonth: '202602',
        category: LogisticsCategory.normal,
      );

      expect(result.length, 1);
      expect(result.first.category, LogisticsCategory.normal);
    });
  });

  group('GetLogisticsSales - 금액 계산', () {
    test('calculateTotalAmount가 총 금액을 계산한다', () async {
      final sales = await useCase(yearMonth: '202602');
      final total = useCase.calculateTotalAmount(sales);

      expect(total, 100000000); // 50M + 30M + 20M
    });

    test('calculateTotalAmount가 빈 리스트에서 0을 반환한다', () {
      final total = useCase.calculateTotalAmount([]);

      expect(total, 0);
    });

    test('calculateTotalPreviousYearAmount가 전년 총 금액을 계산한다', () async {
      final sales = await useCase(yearMonth: '202602');
      final total = useCase.calculateTotalPreviousYearAmount(sales);

      expect(total, 91000000); // 45M + 28M + 18M
    });

    test('calculateTotalDifference가 총 증감 금액을 계산한다', () async {
      final sales = await useCase(yearMonth: '202602');
      final total = useCase.calculateTotalDifference(sales);

      expect(total, 9000000); // 5M + 2M + 2M
    });

    test('calculateTotalGrowthRate가 전체 증감율을 계산한다', () async {
      final sales = await useCase(yearMonth: '202602');
      final growthRate = useCase.calculateTotalGrowthRate(sales);

      expect(growthRate, isNotNull);
      // (100M - 91M) / 91M * 100 = 9.89%
      expect(growthRate!, closeTo(9.89, 0.1));
    });

    test('calculateTotalGrowthRate가 전년 실적이 0이면 null을 반환한다', () {
      final sales = [
        const LogisticsSales(
          yearMonth: '202602',
          category: LogisticsCategory.normal,
          currentAmount: 50000000,
          previousYearAmount: 0,
          difference: 50000000,
          growthRate: 0,
          isCurrentMonth: true,
        ),
      ];

      final growthRate = useCase.calculateTotalGrowthRate(sales);

      expect(growthRate, isNull);
    });

    test('calculateTotalGrowthRate가 빈 리스트에서 null을 반환한다', () {
      final growthRate = useCase.calculateTotalGrowthRate([]);

      expect(growthRate, isNull);
    });
  });

  group('GetLogisticsSales - 카테고리별 합계', () {
    test('calculateCategoryTotals가 카테고리별 합계를 계산한다', () async {
      final sales = await useCase(yearMonth: '202602');
      final totals = useCase.calculateCategoryTotals(sales);

      expect(totals.length, 3);
      expect(totals[LogisticsCategory.normal], 50000000);
      expect(totals[LogisticsCategory.ramen], 30000000);
      expect(totals[LogisticsCategory.frozen], 20000000);
    });

    test('calculateCategoryTotals가 빈 리스트에서 빈 Map을 반환한다', () {
      final totals = useCase.calculateCategoryTotals([]);

      expect(totals, isEmpty);
    });

    test('calculateCategoryTotals가 같은 카테고리의 합계를 누적한다', () async {
      final allSales = await useCase(yearMonth: '202601');
      // 202601에는 일반 카테고리가 2건 (48M + 10M)
      final totals = useCase.calculateCategoryTotals(allSales);

      expect(totals[LogisticsCategory.normal], 58000000);
    });
  });

  group('GetLogisticsSales - 추출 및 필터링', () {
    test('extractCategories가 카테고리 목록을 추출한다', () async {
      final sales = await useCase(yearMonth: '202602');
      final categories = useCase.extractCategories(sales);

      expect(categories.length, 3);
      expect(categories, contains(LogisticsCategory.normal));
      expect(categories, contains(LogisticsCategory.ramen));
      expect(categories, contains(LogisticsCategory.frozen));
      // 카테고리 코드순 정렬 확인
      expect(categories[0], LogisticsCategory.frozen);
      expect(categories[1], LogisticsCategory.normal);
      expect(categories[2], LogisticsCategory.ramen);
    });

    test('filterByCategory가 특정 카테고리만 필터링한다', () async {
      final sales = await useCase(yearMonth: '202602');
      final filtered =
          useCase.filterByCategory(sales, LogisticsCategory.ramen);

      expect(filtered.length, 1);
      expect(filtered.first.category, LogisticsCategory.ramen);
    });

    test('filterCurrentMonthSales가 당월 실적만 필터링한다', () {
      final filtered = useCase.filterCurrentMonthSales(mockData);

      expect(filtered.length, 3);
      expect(filtered.every((s) => s.isCurrentMonth == true), true);
    });

    test('filterClosedSales가 마감 실적만 필터링한다', () {
      final filtered = useCase.filterClosedSales(mockData);

      expect(filtered.length, 6);
      expect(filtered.every((s) => s.isCurrentMonth == false), true);
    });
  });
}
