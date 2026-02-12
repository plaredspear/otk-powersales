import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/monthly_sales.dart';
import '../../test_helper.dart';

void main() {
  setUpAll(() async {
    await TestHelper.initialize();
  });

  const testCategorySales1 = CategorySales(
    category: '상온',
    targetAmount: 50000000,
    achievedAmount: 40000000,
    achievementRate: 80.0,
  );

  const testCategorySales2 = CategorySales(
    category: '냉장/냉동',
    targetAmount: 30000000,
    achievedAmount: 25000000,
    achievementRate: 83.33,
  );

  const testMonthlyAverage = MonthlyAverage(
    currentYearAverage: 60000000,
    previousYearAverage: 55000000,
    startMonth: 1,
    endMonth: 8,
  );

  final testMonthlySales = MonthlySales(
    customerId: 'C001',
    customerName: '이마트 부산점',
    yearMonth: '202608',
    targetAmount: 80000000,
    achievedAmount: 65000000,
    achievementRate: 81.25,
    categorySales: const [testCategorySales1, testCategorySales2],
    previousYearSameMonth: 58000000,
    monthlyAverage: testMonthlyAverage,
  );

  group('CategorySales Entity 생성 테스트', () {
    test('CategorySales 인스턴스가 올바르게 생성되는지 확인', () {
      expect(testCategorySales1.category, '상온');
      expect(testCategorySales1.targetAmount, 50000000);
      expect(testCategorySales1.achievedAmount, 40000000);
      expect(testCategorySales1.achievementRate, 80.0);
    });
  });

  group('CategorySales copyWith 테스트', () {
    test('일부 필드만 변경', () {
      final updated = testCategorySales1.copyWith(
        achievedAmount: 45000000,
        achievementRate: 90.0,
      );

      expect(updated.category, testCategorySales1.category);
      expect(updated.targetAmount, testCategorySales1.targetAmount);
      expect(updated.achievedAmount, 45000000);
      expect(updated.achievementRate, 90.0);
    });
  });

  group('CategorySales toJson/fromJson 테스트', () {
    test('toJson 직렬화', () {
      final json = testCategorySales1.toJson();

      expect(json['category'], '상온');
      expect(json['targetAmount'], 50000000);
      expect(json['achievedAmount'], 40000000);
      expect(json['achievementRate'], 80.0);
    });

    test('fromJson 역직렬화', () {
      final json = {
        'category': '상온',
        'targetAmount': 50000000,
        'achievedAmount': 40000000,
        'achievementRate': 80.0,
      };

      final sales = CategorySales.fromJson(json);

      expect(sales.category, '상온');
      expect(sales.targetAmount, 50000000);
      expect(sales.achievedAmount, 40000000);
      expect(sales.achievementRate, 80.0);
    });

    test('toJson/fromJson 라운드트립', () {
      final json = testCategorySales1.toJson();
      final sales = CategorySales.fromJson(json);

      expect(sales, testCategorySales1);
    });
  });

  group('CategorySales equality 테스트', () {
    test('같은 값을 가진 CategorySales는 같은 객체', () {
      const sales1 = CategorySales(
        category: '상온',
        targetAmount: 50000000,
        achievedAmount: 40000000,
        achievementRate: 80.0,
      );

      const sales2 = CategorySales(
        category: '상온',
        targetAmount: 50000000,
        achievedAmount: 40000000,
        achievementRate: 80.0,
      );

      expect(sales1, sales2);
    });

    test('다른 값을 가진 CategorySales는 다른 객체', () {
      expect(testCategorySales1, isNot(testCategorySales2));
    });
  });

  group('MonthlyAverage Entity 생성 테스트', () {
    test('MonthlyAverage 인스턴스가 올바르게 생성되는지 확인', () {
      expect(testMonthlyAverage.currentYearAverage, 60000000);
      expect(testMonthlyAverage.previousYearAverage, 55000000);
      expect(testMonthlyAverage.startMonth, 1);
      expect(testMonthlyAverage.endMonth, 8);
    });
  });

  group('MonthlyAverage copyWith 테스트', () {
    test('일부 필드만 변경', () {
      final updated = testMonthlyAverage.copyWith(
        currentYearAverage: 70000000,
        endMonth: 9,
      );

      expect(updated.currentYearAverage, 70000000);
      expect(updated.previousYearAverage, testMonthlyAverage.previousYearAverage);
      expect(updated.startMonth, testMonthlyAverage.startMonth);
      expect(updated.endMonth, 9);
    });
  });

  group('MonthlyAverage toJson/fromJson 테스트', () {
    test('toJson 직렬화', () {
      final json = testMonthlyAverage.toJson();

      expect(json['currentYearAverage'], 60000000);
      expect(json['previousYearAverage'], 55000000);
      expect(json['startMonth'], 1);
      expect(json['endMonth'], 8);
    });

    test('fromJson 역직렬화', () {
      final json = {
        'currentYearAverage': 60000000,
        'previousYearAverage': 55000000,
        'startMonth': 1,
        'endMonth': 8,
      };

      final average = MonthlyAverage.fromJson(json);

      expect(average.currentYearAverage, 60000000);
      expect(average.previousYearAverage, 55000000);
      expect(average.startMonth, 1);
      expect(average.endMonth, 8);
    });

    test('toJson/fromJson 라운드트립', () {
      final json = testMonthlyAverage.toJson();
      final average = MonthlyAverage.fromJson(json);

      expect(average, testMonthlyAverage);
    });
  });

  group('MonthlyAverage equality 테스트', () {
    test('같은 값을 가진 MonthlyAverage는 같은 객체', () {
      const average1 = MonthlyAverage(
        currentYearAverage: 60000000,
        previousYearAverage: 55000000,
        startMonth: 1,
        endMonth: 8,
      );

      const average2 = MonthlyAverage(
        currentYearAverage: 60000000,
        previousYearAverage: 55000000,
        startMonth: 1,
        endMonth: 8,
      );

      expect(average1, average2);
    });

    test('다른 값을 가진 MonthlyAverage는 다른 객체', () {
      const average1 = MonthlyAverage(
        currentYearAverage: 60000000,
        previousYearAverage: 55000000,
        startMonth: 1,
        endMonth: 8,
      );

      const average2 = MonthlyAverage(
        currentYearAverage: 70000000,
        previousYearAverage: 60000000,
        startMonth: 1,
        endMonth: 9,
      );

      expect(average1, isNot(average2));
    });
  });

  group('MonthlySales Entity 생성 테스트', () {
    test('MonthlySales 인스턴스가 올바르게 생성되는지 확인', () {
      expect(testMonthlySales.customerId, 'C001');
      expect(testMonthlySales.customerName, '이마트 부산점');
      expect(testMonthlySales.yearMonth, '202608');
      expect(testMonthlySales.targetAmount, 80000000);
      expect(testMonthlySales.achievedAmount, 65000000);
      expect(testMonthlySales.achievementRate, 81.25);
      expect(testMonthlySales.categorySales.length, 2);
      expect(testMonthlySales.categorySales[0], testCategorySales1);
      expect(testMonthlySales.categorySales[1], testCategorySales2);
      expect(testMonthlySales.previousYearSameMonth, 58000000);
      expect(testMonthlySales.monthlyAverage, testMonthlyAverage);
    });
  });

  group('MonthlySales copyWith 테스트', () {
    test('일부 필드만 변경', () {
      final updated = testMonthlySales.copyWith(
        achievedAmount: 70000000,
        achievementRate: 87.5,
      );

      expect(updated.customerId, testMonthlySales.customerId);
      expect(updated.customerName, testMonthlySales.customerName);
      expect(updated.yearMonth, testMonthlySales.yearMonth);
      expect(updated.targetAmount, testMonthlySales.targetAmount);
      expect(updated.achievedAmount, 70000000);
      expect(updated.achievementRate, 87.5);
      expect(updated.categorySales, testMonthlySales.categorySales);
      expect(updated.previousYearSameMonth, testMonthlySales.previousYearSameMonth);
      expect(updated.monthlyAverage, testMonthlySales.monthlyAverage);
    });

    test('categorySales 변경', () {
      const newCategory = CategorySales(
        category: '신선',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 80.0,
      );

      final updated = testMonthlySales.copyWith(
        categorySales: const [newCategory],
      );

      expect(updated.categorySales.length, 1);
      expect(updated.categorySales[0], newCategory);
    });
  });

  group('MonthlySales toJson/fromJson 테스트', () {
    test('toJson 직렬화', () {
      final json = testMonthlySales.toJson();

      expect(json['customerId'], 'C001');
      expect(json['customerName'], '이마트 부산점');
      expect(json['yearMonth'], '202608');
      expect(json['targetAmount'], 80000000);
      expect(json['achievedAmount'], 65000000);
      expect(json['achievementRate'], 81.25);
      expect(json['categorySales'], isA<List>());
      expect(json['categorySales'].length, 2);
      expect(json['previousYearSameMonth'], 58000000);
      expect(json['monthlyAverage'], isA<Map<String, dynamic>>());
    });

    test('fromJson 역직렬화', () {
      final json = {
        'customerId': 'C001',
        'customerName': '이마트 부산점',
        'yearMonth': '202608',
        'targetAmount': 80000000,
        'achievedAmount': 65000000,
        'achievementRate': 81.25,
        'categorySales': [
          {
            'category': '상온',
            'targetAmount': 50000000,
            'achievedAmount': 40000000,
            'achievementRate': 80.0,
          },
          {
            'category': '냉장/냉동',
            'targetAmount': 30000000,
            'achievedAmount': 25000000,
            'achievementRate': 83.33,
          },
        ],
        'previousYearSameMonth': 58000000,
        'monthlyAverage': {
          'currentYearAverage': 60000000,
          'previousYearAverage': 55000000,
          'startMonth': 1,
          'endMonth': 8,
        },
      };

      final sales = MonthlySales.fromJson(json);

      expect(sales.customerId, 'C001');
      expect(sales.customerName, '이마트 부산점');
      expect(sales.yearMonth, '202608');
      expect(sales.targetAmount, 80000000);
      expect(sales.achievedAmount, 65000000);
      expect(sales.achievementRate, 81.25);
      expect(sales.categorySales.length, 2);
      expect(sales.categorySales[0].category, '상온');
      expect(sales.categorySales[1].category, '냉장/냉동');
      expect(sales.previousYearSameMonth, 58000000);
      expect(sales.monthlyAverage.currentYearAverage, 60000000);
    });

    test('toJson/fromJson 라운드트립', () {
      final json = testMonthlySales.toJson();
      final sales = MonthlySales.fromJson(json);

      expect(sales, testMonthlySales);
    });
  });

  group('MonthlySales equality 테스트', () {
    test('같은 값을 가진 MonthlySales는 같은 객체', () {
      final sales1 = MonthlySales(
        customerId: 'C001',
        customerName: '이마트 부산점',
        yearMonth: '202608',
        targetAmount: 80000000,
        achievedAmount: 65000000,
        achievementRate: 81.25,
        categorySales: const [testCategorySales1, testCategorySales2],
        previousYearSameMonth: 58000000,
        monthlyAverage: testMonthlyAverage,
      );

      final sales2 = MonthlySales(
        customerId: 'C001',
        customerName: '이마트 부산점',
        yearMonth: '202608',
        targetAmount: 80000000,
        achievedAmount: 65000000,
        achievementRate: 81.25,
        categorySales: const [testCategorySales1, testCategorySales2],
        previousYearSameMonth: 58000000,
        monthlyAverage: testMonthlyAverage,
      );

      expect(sales1, sales2);
    });

    test('다른 값을 가진 MonthlySales는 다른 객체', () {
      final sales1 = MonthlySales(
        customerId: 'C001',
        customerName: '이마트 부산점',
        yearMonth: '202608',
        targetAmount: 80000000,
        achievedAmount: 65000000,
        achievementRate: 81.25,
        categorySales: const [testCategorySales1],
        previousYearSameMonth: 58000000,
        monthlyAverage: testMonthlyAverage,
      );

      final sales2 = MonthlySales(
        customerId: 'C002',
        customerName: '홈플러스',
        yearMonth: '202609',
        targetAmount: 90000000,
        achievedAmount: 75000000,
        achievementRate: 83.33,
        categorySales: const [testCategorySales2],
        previousYearSameMonth: 60000000,
        monthlyAverage: testMonthlyAverage,
      );

      expect(sales1, isNot(sales2));
    });

    test('categorySales가 다른 MonthlySales는 다른 객체', () {
      final sales1 = MonthlySales(
        customerId: 'C001',
        customerName: '이마트 부산점',
        yearMonth: '202608',
        targetAmount: 80000000,
        achievedAmount: 65000000,
        achievementRate: 81.25,
        categorySales: const [testCategorySales1],
        previousYearSameMonth: 58000000,
        monthlyAverage: testMonthlyAverage,
      );

      final sales2 = MonthlySales(
        customerId: 'C001',
        customerName: '이마트 부산점',
        yearMonth: '202608',
        targetAmount: 80000000,
        achievedAmount: 65000000,
        achievementRate: 81.25,
        categorySales: const [testCategorySales2],
        previousYearSameMonth: 58000000,
        monthlyAverage: testMonthlyAverage,
      );

      expect(sales1, isNot(sales2));
    });
  });

  group('MonthlySales hashCode 테스트', () {
    test('같은 값을 가진 MonthlySales는 같은 hashCode', () {
      final sales1 = MonthlySales(
        customerId: 'C001',
        customerName: '이마트 부산점',
        yearMonth: '202608',
        targetAmount: 80000000,
        achievedAmount: 65000000,
        achievementRate: 81.25,
        categorySales: const [testCategorySales1, testCategorySales2],
        previousYearSameMonth: 58000000,
        monthlyAverage: testMonthlyAverage,
      );

      final sales2 = MonthlySales(
        customerId: 'C001',
        customerName: '이마트 부산점',
        yearMonth: '202608',
        targetAmount: 80000000,
        achievedAmount: 65000000,
        achievementRate: 81.25,
        categorySales: const [testCategorySales1, testCategorySales2],
        previousYearSameMonth: 58000000,
        monthlyAverage: testMonthlyAverage,
      );

      expect(sales1.hashCode, sales2.hashCode);
    });
  });

  group('MonthlySales toString 테스트', () {
    test('toString 포맷 확인', () {
      final result = testMonthlySales.toString();

      expect(result, contains('MonthlySales'));
      expect(result, contains('customerId: C001'));
      expect(result, contains('customerName: 이마트 부산점'));
      expect(result, contains('yearMonth: 202608'));
      expect(result, contains('targetAmount: 80000000'));
    });
  });
}
