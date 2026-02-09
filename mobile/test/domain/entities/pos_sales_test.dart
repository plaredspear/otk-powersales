import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/pos_sales.dart';

void main() {
  group('PosSales Entity', () {
    // 테스트용 기본 데이터
    final testDate = DateTime(2026, 1, 15);
    final testPosSales = PosSales(
      storeName: '이마트 강남점',
      productName: '진라면 매운맛',
      salesDate: testDate,
      quantity: 100,
      amount: 50000,
      productCode: 'P001',
      category: '라면',
    );

    group('생성 테스트', () {
      test('PosSales 엔티티가 올바르게 생성된다', () {
        expect(testPosSales.storeName, '이마트 강남점');
        expect(testPosSales.productName, '진라면 매운맛');
        expect(testPosSales.salesDate, testDate);
        expect(testPosSales.quantity, 100);
        expect(testPosSales.amount, 50000);
        expect(testPosSales.productCode, 'P001');
        expect(testPosSales.category, '라면');
      });

      test('선택적 필드(productCode, category)가 null일 수 있다', () {
        final posSalesWithoutOptionals = PosSales(
          storeName: '홈플러스',
          productName: '케찹',
          salesDate: testDate,
          quantity: 50,
          amount: 25000,
        );

        expect(posSalesWithoutOptionals.productCode, isNull);
        expect(posSalesWithoutOptionals.category, isNull);
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 모든 필드 변경', () {
        final newDate = DateTime(2026, 1, 20);
        final copied = testPosSales.copyWith(
          storeName: '롯데마트',
          productName: '짜파게티',
          salesDate: newDate,
          quantity: 200,
          amount: 100000,
          productCode: 'P002',
          category: '라면류',
        );

        expect(copied.storeName, '롯데마트');
        expect(copied.productName, '짜파게티');
        expect(copied.salesDate, newDate);
        expect(copied.quantity, 200);
        expect(copied.amount, 100000);
        expect(copied.productCode, 'P002');
        expect(copied.category, '라면류');
      });

      test('copyWith가 일부 필드만 변경한다', () {
        final copied = testPosSales.copyWith(
          quantity: 150,
          amount: 75000,
        );

        expect(copied.storeName, testPosSales.storeName);
        expect(copied.productName, testPosSales.productName);
        expect(copied.salesDate, testPosSales.salesDate);
        expect(copied.quantity, 150);
        expect(copied.amount, 75000);
        expect(copied.productCode, testPosSales.productCode);
        expect(copied.category, testPosSales.category);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = testPosSales;
        final copied = testPosSales.copyWith(quantity: 999);

        expect(original.quantity, 100);
        expect(copied.quantity, 999);
      });
    });

    group('직렬화 테스트', () {
      test('toJson이 올바르게 동작한다', () {
        final json = testPosSales.toJson();

        expect(json['storeName'], '이마트 강남점');
        expect(json['productName'], '진라면 매운맛');
        expect(json['salesDate'], testDate.toIso8601String());
        expect(json['quantity'], 100);
        expect(json['amount'], 50000);
        expect(json['productCode'], 'P001');
        expect(json['category'], '라면');
      });

      test('fromJson이 올바르게 동작한다', () {
        final json = {
          'storeName': '홈플러스 서초점',
          'productName': '오뚜기 카레',
          'salesDate': '2026-01-25T00:00:00.000',
          'quantity': 80,
          'amount': 40000,
          'productCode': 'P003',
          'category': '카레',
        };

        final posSales = PosSales.fromJson(json);

        expect(posSales.storeName, '홈플러스 서초점');
        expect(posSales.productName, '오뚜기 카레');
        expect(posSales.salesDate, DateTime(2026, 1, 25));
        expect(posSales.quantity, 80);
        expect(posSales.amount, 40000);
        expect(posSales.productCode, 'P003');
        expect(posSales.category, '카레');
      });

      test('toJson과 fromJson이 정확히 왕복 변환된다', () {
        final json = testPosSales.toJson();
        final restored = PosSales.fromJson(json);

        expect(restored, testPosSales);
      });

      test('fromJson이 선택적 필드가 null인 경우를 처리한다', () {
        final json = {
          'storeName': '이마트',
          'productName': '케찹',
          'salesDate': '2026-01-15T00:00:00.000',
          'quantity': 30,
          'amount': 15000,
          'productCode': null,
          'category': null,
        };

        final posSales = PosSales.fromJson(json);

        expect(posSales.productCode, isNull);
        expect(posSales.category, isNull);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final posSales1 = PosSales(
          storeName: '이마트',
          productName: '진라면',
          salesDate: DateTime(2026, 1, 15),
          quantity: 100,
          amount: 50000,
        );

        final posSales2 = PosSales(
          storeName: '이마트',
          productName: '진라면',
          salesDate: DateTime(2026, 1, 15),
          quantity: 100,
          amount: 50000,
        );

        expect(posSales1, posSales2);
        expect(posSales1.hashCode, posSales2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final posSales1 = PosSales(
          storeName: '이마트',
          productName: '진라면',
          salesDate: DateTime(2026, 1, 15),
          quantity: 100,
          amount: 50000,
        );

        final posSales2 = PosSales(
          storeName: '홈플러스',
          productName: '진라면',
          salesDate: DateTime(2026, 1, 15),
          quantity: 100,
          amount: 50000,
        );

        expect(posSales1, isNot(posSales2));
      });

      test('자기 자신과 비교하면 동일하다 (identical)', () {
        expect(testPosSales, testPosSales);
      });
    });

    group('Validation 테스트', () {
      test('수량이 음수일 때도 엔티티가 생성된다 (비즈니스 검증은 UseCase에서)', () {
        final posSalesWithNegative = PosSales(
          storeName: '이마트',
          productName: '진라면',
          salesDate: testDate,
          quantity: -10,
          amount: 50000,
        );

        expect(posSalesWithNegative.quantity, -10);
      });

      test('금액이 0이어도 엔티티가 생성된다', () {
        final posSalesWithZeroAmount = PosSales(
          storeName: '이마트',
          productName: '진라면',
          salesDate: testDate,
          quantity: 100,
          amount: 0,
        );

        expect(posSalesWithZeroAmount.amount, 0);
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final str = testPosSales.toString();

        expect(str, contains('이마트 강남점'));
        expect(str, contains('진라면 매운맛'));
        expect(str, contains('100'));
        expect(str, contains('50000'));
        expect(str, contains('P001'));
        expect(str, contains('라면'));
      });
    });
  });
}
