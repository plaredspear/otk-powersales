import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/logistics_sales.dart';

void main() {
  group('LogisticsCategory Enum', () {
    test('카테고리 이름이 올바르게 정의된다', () {
      expect(LogisticsCategory.normal.displayName, '상온');
      expect(LogisticsCategory.ramen.displayName, '라면');
      expect(LogisticsCategory.frozen.displayName, '냉동/냉장');
    });

    test('카테고리 코드가 올바르게 정의된다', () {
      expect(LogisticsCategory.normal.code, 'NORMAL');
      expect(LogisticsCategory.ramen.code, 'RAMEN');
      expect(LogisticsCategory.frozen.code, 'FROZEN');
    });

    test('코드로 카테고리를 찾을 수 있다', () {
      expect(LogisticsCategory.fromCode('NORMAL'), LogisticsCategory.normal);
      expect(LogisticsCategory.fromCode('RAMEN'), LogisticsCategory.ramen);
      expect(LogisticsCategory.fromCode('FROZEN'), LogisticsCategory.frozen);
    });

    test('존재하지 않는 코드는 기본값(상온)을 반환한다', () {
      expect(LogisticsCategory.fromCode('INVALID'), LogisticsCategory.normal);
      expect(LogisticsCategory.fromCode(''), LogisticsCategory.normal);
    });

    test('JSON 직렬화가 올바르게 동작한다', () {
      expect(LogisticsCategory.normal.toJson(), 'NORMAL');
      expect(LogisticsCategory.ramen.toJson(), 'RAMEN');
      expect(LogisticsCategory.frozen.toJson(), 'FROZEN');
    });

    test('JSON 역직렬화가 올바르게 동작한다', () {
      expect(LogisticsCategory.fromJson('NORMAL'), LogisticsCategory.normal);
      expect(LogisticsCategory.fromJson('RAMEN'), LogisticsCategory.ramen);
      expect(LogisticsCategory.fromJson('FROZEN'), LogisticsCategory.frozen);
    });
  });

  group('LogisticsSales Entity', () {
    // 테스트용 기본 데이터
    final testLogisticsSales = LogisticsSales(
      yearMonth: '202601',
      category: LogisticsCategory.normal,
      currentAmount: 10000000,
      previousYearAmount: 8000000,
      difference: 2000000,
      growthRate: 25.0,
      isCurrentMonth: true,
      customerName: '오뚜기 본사',
    );

    group('생성 테스트', () {
      test('LogisticsSales 엔티티가 올바르게 생성된다', () {
        expect(testLogisticsSales.yearMonth, '202601');
        expect(testLogisticsSales.category, LogisticsCategory.normal);
        expect(testLogisticsSales.currentAmount, 10000000);
        expect(testLogisticsSales.previousYearAmount, 8000000);
        expect(testLogisticsSales.difference, 2000000);
        expect(testLogisticsSales.growthRate, 25.0);
        expect(testLogisticsSales.isCurrentMonth, true);
        expect(testLogisticsSales.customerName, '오뚜기 본사');
      });

      test('선택적 필드(customerName)가 null일 수 있다', () {
        final logisticsSalesWithoutCustomer = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.ramen,
          currentAmount: 5000000,
          previousYearAmount: 4000000,
          difference: 1000000,
          growthRate: 25.0,
          isCurrentMonth: false,
        );

        expect(logisticsSalesWithoutCustomer.customerName, isNull);
      });

      test('당월 물류예상실적 엔티티가 생성된다', () {
        final currentMonthSales = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.ramen,
          currentAmount: 5000000,
          previousYearAmount: 4500000,
          difference: 500000,
          growthRate: 11.1,
          isCurrentMonth: true,
        );

        expect(currentMonthSales.isCurrentMonth, true);
      });

      test('이전월 ABC물류배부 마감실적 엔티티가 생성된다', () {
        final closedSales = LogisticsSales(
          yearMonth: '202512',
          category: LogisticsCategory.frozen,
          currentAmount: 3000000,
          previousYearAmount: 2800000,
          difference: 200000,
          growthRate: 7.1,
          isCurrentMonth: false,
        );

        expect(closedSales.isCurrentMonth, false);
      });

      test('음수 증감율을 가진 엔티티가 생성된다 (전년 대비 감소)', () {
        final decreasingGrowth = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.normal,
          currentAmount: 7000000,
          previousYearAmount: 10000000,
          difference: -3000000,
          growthRate: -30.0,
          isCurrentMonth: true,
        );

        expect(decreasingGrowth.difference, -3000000);
        expect(decreasingGrowth.growthRate, -30.0);
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 모든 필드 변경', () {
        final copied = testLogisticsSales.copyWith(
          yearMonth: '202602',
          category: LogisticsCategory.frozen,
          currentAmount: 12000000,
          previousYearAmount: 9000000,
          difference: 3000000,
          growthRate: 33.3,
          isCurrentMonth: false,
          customerName: '신규 거래처',
        );

        expect(copied.yearMonth, '202602');
        expect(copied.category, LogisticsCategory.frozen);
        expect(copied.currentAmount, 12000000);
        expect(copied.previousYearAmount, 9000000);
        expect(copied.difference, 3000000);
        expect(copied.growthRate, 33.3);
        expect(copied.isCurrentMonth, false);
        expect(copied.customerName, '신규 거래처');
      });

      test('copyWith가 일부 필드만 변경한다', () {
        final copied = testLogisticsSales.copyWith(
          currentAmount: 15000000,
          difference: 7000000,
        );

        expect(copied.yearMonth, testLogisticsSales.yearMonth);
        expect(copied.category, testLogisticsSales.category);
        expect(copied.currentAmount, 15000000);
        expect(copied.previousYearAmount, testLogisticsSales.previousYearAmount);
        expect(copied.difference, 7000000);
        expect(copied.growthRate, testLogisticsSales.growthRate);
        expect(copied.isCurrentMonth, testLogisticsSales.isCurrentMonth);
        expect(copied.customerName, testLogisticsSales.customerName);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = testLogisticsSales;
        final copied = testLogisticsSales.copyWith(currentAmount: 99999999);

        expect(original.currentAmount, 10000000);
        expect(copied.currentAmount, 99999999);
      });

      test('copyWith로 카테고리를 변경할 수 있다', () {
        final copied = testLogisticsSales.copyWith(
          category: LogisticsCategory.ramen,
        );

        expect(copied.category, LogisticsCategory.ramen);
        expect(copied.category.displayName, '라면');
      });
    });

    group('직렬화 테스트', () {
      test('toJson이 올바르게 동작한다', () {
        final json = testLogisticsSales.toJson();

        expect(json['yearMonth'], '202601');
        expect(json['category'], 'NORMAL');
        expect(json['currentAmount'], 10000000);
        expect(json['previousYearAmount'], 8000000);
        expect(json['difference'], 2000000);
        expect(json['growthRate'], 25.0);
        expect(json['isCurrentMonth'], true);
        expect(json['customerName'], '오뚜기 본사');
      });

      test('fromJson이 올바르게 동작한다', () {
        final json = {
          'yearMonth': '202602',
          'category': 'RAMEN',
          'currentAmount': 6000000,
          'previousYearAmount': 5000000,
          'difference': 1000000,
          'growthRate': 20.0,
          'isCurrentMonth': false,
          'customerName': '농협',
        };

        final logisticsSales = LogisticsSales.fromJson(json);

        expect(logisticsSales.yearMonth, '202602');
        expect(logisticsSales.category, LogisticsCategory.ramen);
        expect(logisticsSales.currentAmount, 6000000);
        expect(logisticsSales.previousYearAmount, 5000000);
        expect(logisticsSales.difference, 1000000);
        expect(logisticsSales.growthRate, 20.0);
        expect(logisticsSales.isCurrentMonth, false);
        expect(logisticsSales.customerName, '농협');
      });

      test('toJson과 fromJson이 정확히 왕복 변환된다', () {
        final json = testLogisticsSales.toJson();
        final restored = LogisticsSales.fromJson(json);

        expect(restored, testLogisticsSales);
      });

      test('fromJson이 선택적 필드가 null인 경우를 처리한다', () {
        final json = {
          'yearMonth': '202601',
          'category': 'FROZEN',
          'currentAmount': 3000000,
          'previousYearAmount': 2500000,
          'difference': 500000,
          'growthRate': 20.0,
          'isCurrentMonth': true,
          'customerName': null,
        };

        final logisticsSales = LogisticsSales.fromJson(json);

        expect(logisticsSales.customerName, isNull);
      });

      test('fromJson이 정수형 growthRate를 double로 변환한다', () {
        final json = {
          'yearMonth': '202601',
          'category': 'NORMAL',
          'currentAmount': 10000000,
          'previousYearAmount': 8000000,
          'difference': 2000000,
          'growthRate': 25, // int 타입
          'isCurrentMonth': true,
          'customerName': null,
        };

        final logisticsSales = LogisticsSales.fromJson(json);

        expect(logisticsSales.growthRate, 25.0);
        expect(logisticsSales.growthRate, isA<double>());
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final logisticsSales1 = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.normal,
          currentAmount: 10000000,
          previousYearAmount: 8000000,
          difference: 2000000,
          growthRate: 25.0,
          isCurrentMonth: true,
        );

        final logisticsSales2 = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.normal,
          currentAmount: 10000000,
          previousYearAmount: 8000000,
          difference: 2000000,
          growthRate: 25.0,
          isCurrentMonth: true,
        );

        expect(logisticsSales1, logisticsSales2);
        expect(logisticsSales1.hashCode, logisticsSales2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final logisticsSales1 = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.normal,
          currentAmount: 10000000,
          previousYearAmount: 8000000,
          difference: 2000000,
          growthRate: 25.0,
          isCurrentMonth: true,
        );

        final logisticsSales2 = LogisticsSales(
          yearMonth: '202602',
          category: LogisticsCategory.normal,
          currentAmount: 10000000,
          previousYearAmount: 8000000,
          difference: 2000000,
          growthRate: 25.0,
          isCurrentMonth: true,
        );

        expect(logisticsSales1, isNot(logisticsSales2));
      });

      test('카테고리가 다르면 다르게 비교된다', () {
        final logisticsSales1 = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.normal,
          currentAmount: 10000000,
          previousYearAmount: 8000000,
          difference: 2000000,
          growthRate: 25.0,
          isCurrentMonth: true,
        );

        final logisticsSales2 = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.ramen,
          currentAmount: 10000000,
          previousYearAmount: 8000000,
          difference: 2000000,
          growthRate: 25.0,
          isCurrentMonth: true,
        );

        expect(logisticsSales1, isNot(logisticsSales2));
      });

      test('자기 자신과 비교하면 동일하다 (identical)', () {
        expect(testLogisticsSales, testLogisticsSales);
      });
    });

    group('Validation 테스트', () {
      test('금액이 0이어도 엔티티가 생성된다', () {
        final logisticsSalesWithZeroAmount = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.normal,
          currentAmount: 0,
          previousYearAmount: 0,
          difference: 0,
          growthRate: 0.0,
          isCurrentMonth: true,
        );

        expect(logisticsSalesWithZeroAmount.currentAmount, 0);
        expect(logisticsSalesWithZeroAmount.previousYearAmount, 0);
      });

      test('음수 금액도 엔티티가 생성된다 (비즈니스 검증은 UseCase에서)', () {
        final logisticsSalesWithNegative = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.frozen,
          currentAmount: -1000000,
          previousYearAmount: 5000000,
          difference: -6000000,
          growthRate: -120.0,
          isCurrentMonth: true,
        );

        expect(logisticsSalesWithNegative.currentAmount, -1000000);
      });
    });

    group('당월/이전월 구분 테스트', () {
      test('당월 실적은 isCurrentMonth가 true이다', () {
        final currentMonthSales = LogisticsSales(
          yearMonth: '202601',
          category: LogisticsCategory.normal,
          currentAmount: 10000000,
          previousYearAmount: 8000000,
          difference: 2000000,
          growthRate: 25.0,
          isCurrentMonth: true,
        );

        expect(currentMonthSales.isCurrentMonth, true);
      });

      test('이전월 실적은 isCurrentMonth가 false이다', () {
        final closedSales = LogisticsSales(
          yearMonth: '202512',
          category: LogisticsCategory.ramen,
          currentAmount: 5000000,
          previousYearAmount: 4000000,
          difference: 1000000,
          growthRate: 25.0,
          isCurrentMonth: false,
        );

        expect(closedSales.isCurrentMonth, false);
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final str = testLogisticsSales.toString();

        expect(str, contains('202601'));
        expect(str, contains('상온'));
        expect(str, contains('10000000'));
        expect(str, contains('8000000'));
        expect(str, contains('2000000'));
        expect(str, contains('25.0'));
        expect(str, contains('true'));
        expect(str, contains('오뚜기 본사'));
      });
    });
  });
}
