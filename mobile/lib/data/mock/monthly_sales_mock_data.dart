import '../../domain/entities/monthly_sales.dart';

/// 월매출 Mock 데이터
class MonthlySalesMockData {
  /// 거래처별 월매출 데이터
  static final Map<String, MonthlySales> monthlySalesMap = {
    // C001 - 이마트 부산점
    'C001-202601': MonthlySales(
      customerId: 'C001',
      customerName: '이마트 부산점',
      yearMonth: '202601',
      targetAmount: 100000000,
      achievedAmount: 95000000,
      achievementRate: 95.0,
      categorySales: const [
        CategorySales(
          category: '상온',
          targetAmount: 60000000,
          achievedAmount: 58000000,
          achievementRate: 96.7,
        ),
        CategorySales(
          category: '냉장/냉동',
          targetAmount: 40000000,
          achievedAmount: 37000000,
          achievementRate: 92.5,
        ),
      ],
      previousYearSameMonth: 90000000,
      monthlyAverage: const MonthlyAverage(
        currentYearAverage: 95000000,
        previousYearAverage: 90000000,
        startMonth: 1,
        endMonth: 1,
      ),
    ),
    'C001-202602': MonthlySales(
      customerId: 'C001',
      customerName: '이마트 부산점',
      yearMonth: '202602',
      targetAmount: 80000000,
      achievedAmount: 65000000,
      achievementRate: 81.25,
      categorySales: const [
        CategorySales(
          category: '상온',
          targetAmount: 50000000,
          achievedAmount: 40000000,
          achievementRate: 80.0,
        ),
        CategorySales(
          category: '냉장/냉동',
          targetAmount: 30000000,
          achievedAmount: 25000000,
          achievementRate: 83.3,
        ),
      ],
      previousYearSameMonth: 58000000,
      monthlyAverage: const MonthlyAverage(
        currentYearAverage: 80000000,
        previousYearAverage: 74000000,
        startMonth: 1,
        endMonth: 2,
      ),
    ),

    // C002 - 롯데마트 해운대점
    'C002-202601': MonthlySales(
      customerId: 'C002',
      customerName: '롯데마트 해운대점',
      yearMonth: '202601',
      targetAmount: 70000000,
      achievedAmount: 72000000,
      achievementRate: 102.9,
      categorySales: const [
        CategorySales(
          category: '상온',
          targetAmount: 40000000,
          achievedAmount: 42000000,
          achievementRate: 105.0,
        ),
        CategorySales(
          category: '냉장/냉동',
          targetAmount: 30000000,
          achievedAmount: 30000000,
          achievementRate: 100.0,
        ),
      ],
      previousYearSameMonth: 65000000,
      monthlyAverage: const MonthlyAverage(
        currentYearAverage: 72000000,
        previousYearAverage: 65000000,
        startMonth: 1,
        endMonth: 1,
      ),
    ),
    'C002-202602': MonthlySales(
      customerId: 'C002',
      customerName: '롯데마트 해운대점',
      yearMonth: '202602',
      targetAmount: 60000000,
      achievedAmount: 50000000,
      achievementRate: 83.3,
      categorySales: const [
        CategorySales(
          category: '상온',
          targetAmount: 35000000,
          achievedAmount: 30000000,
          achievementRate: 85.7,
        ),
        CategorySales(
          category: '냉장/냉동',
          targetAmount: 25000000,
          achievedAmount: 20000000,
          achievementRate: 80.0,
        ),
      ],
      previousYearSameMonth: 48000000,
      monthlyAverage: const MonthlyAverage(
        currentYearAverage: 61000000,
        previousYearAverage: 56500000,
        startMonth: 1,
        endMonth: 2,
      ),
    ),

    // C003 - 홈플러스 광복점
    'C003-202601': MonthlySales(
      customerId: 'C003',
      customerName: '홈플러스 광복점',
      yearMonth: '202601',
      targetAmount: 50000000,
      achievedAmount: 48000000,
      achievementRate: 96.0,
      categorySales: const [
        CategorySales(
          category: '상온',
          targetAmount: 30000000,
          achievedAmount: 29000000,
          achievementRate: 96.7,
        ),
        CategorySales(
          category: '냉장/냉동',
          targetAmount: 20000000,
          achievedAmount: 19000000,
          achievementRate: 95.0,
        ),
      ],
      previousYearSameMonth: 45000000,
      monthlyAverage: const MonthlyAverage(
        currentYearAverage: 48000000,
        previousYearAverage: 45000000,
        startMonth: 1,
        endMonth: 1,
      ),
    ),
    'C003-202602': MonthlySales(
      customerId: 'C003',
      customerName: '홈플러스 광복점',
      yearMonth: '202602',
      targetAmount: 55000000,
      achievedAmount: 40000000,
      achievementRate: 72.7,
      categorySales: const [
        CategorySales(
          category: '상온',
          targetAmount: 35000000,
          achievedAmount: 25000000,
          achievementRate: 71.4,
        ),
        CategorySales(
          category: '냉장/냉동',
          targetAmount: 20000000,
          achievedAmount: 15000000,
          achievementRate: 75.0,
        ),
      ],
      previousYearSameMonth: 42000000,
      monthlyAverage: const MonthlyAverage(
        currentYearAverage: 44000000,
        previousYearAverage: 43500000,
        startMonth: 1,
        endMonth: 2,
      ),
    ),

    // ALL - 전체 거래처 합산
    'ALL-202601': MonthlySales(
      customerId: 'ALL',
      customerName: '전체',
      yearMonth: '202601',
      targetAmount: 220000000,
      achievedAmount: 215000000,
      achievementRate: 97.7,
      categorySales: const [
        CategorySales(
          category: '상온',
          targetAmount: 130000000,
          achievedAmount: 129000000,
          achievementRate: 99.2,
        ),
        CategorySales(
          category: '냉장/냉동',
          targetAmount: 90000000,
          achievedAmount: 86000000,
          achievementRate: 95.6,
        ),
      ],
      previousYearSameMonth: 200000000,
      monthlyAverage: const MonthlyAverage(
        currentYearAverage: 215000000,
        previousYearAverage: 200000000,
        startMonth: 1,
        endMonth: 1,
      ),
    ),
    'ALL-202602': MonthlySales(
      customerId: 'ALL',
      customerName: '전체',
      yearMonth: '202602',
      targetAmount: 195000000,
      achievedAmount: 155000000,
      achievementRate: 79.5,
      categorySales: const [
        CategorySales(
          category: '상온',
          targetAmount: 120000000,
          achievedAmount: 95000000,
          achievementRate: 79.2,
        ),
        CategorySales(
          category: '냉장/냉동',
          targetAmount: 75000000,
          achievedAmount: 60000000,
          achievementRate: 80.0,
        ),
      ],
      previousYearSameMonth: 148000000,
      monthlyAverage: const MonthlyAverage(
        currentYearAverage: 185000000,
        previousYearAverage: 174000000,
        startMonth: 1,
        endMonth: 2,
      ),
    ),
  };

  /// 월매출 조회
  static MonthlySales? getMonthlySales({
    String? customerId,
    required String yearMonth,
  }) {
    final key = '${customerId ?? 'ALL'}-$yearMonth';
    return monthlySalesMap[key];
  }

  /// 거래처별 특정 월 매출 조회
  static MonthlySales? getMonthlySalesByCustomer({
    required String customerId,
    required String yearMonth,
  }) {
    final key = '$customerId-$yearMonth';
    return monthlySalesMap[key];
  }

  /// 전체 거래처 합산 월매출 조회
  static MonthlySales? getAllMonthlySales(String yearMonth) {
    final key = 'ALL-$yearMonth';
    return monthlySalesMap[key];
  }
}
