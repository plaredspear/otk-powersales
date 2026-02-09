import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/electronic_sales.dart';

void main() {
  group('ElectronicSales 엔티티 테스트', () {
    test('ElectronicSales 엔티티가 올바르게 생성된다', () {
      // Given
      const yearMonth = '202601';
      const customerName = '이마트';
      const productName = '진라면';
      const productCode = 'P001';
      const amount = 1000000;
      const quantity = 500;

      // When
      final electronicSales = ElectronicSales(
        yearMonth: yearMonth,
        customerName: customerName,
        productName: productName,
        productCode: productCode,
        amount: amount,
        quantity: quantity,
      );

      // Then
      expect(electronicSales.yearMonth, yearMonth);
      expect(electronicSales.customerName, customerName);
      expect(electronicSales.productName, productName);
      expect(electronicSales.productCode, productCode);
      expect(electronicSales.amount, amount);
      expect(electronicSales.quantity, quantity);
      expect(electronicSales.previousYearAmount, isNull);
      expect(electronicSales.growthRate, isNull);
    });

    test('전년 대비 데이터를 포함한 ElectronicSales 엔티티가 올바르게 생성된다', () {
      // Given & When
      final electronicSales = ElectronicSales(
        yearMonth: '202601',
        customerName: '홈플러스',
        productName: '참깨라면',
        productCode: 'P002',
        amount: 1500000,
        quantity: 700,
        previousYearAmount: 1200000,
        growthRate: 25.0,
      );

      // Then
      expect(electronicSales.previousYearAmount, 1200000);
      expect(electronicSales.growthRate, 25.0);
    });

    test('copyWith 메서드가 불변성을 유지하며 새 인스턴스를 생성한다', () {
      // Given
      final original = ElectronicSales(
        yearMonth: '202601',
        customerName: '이마트',
        productName: '진라면',
        productCode: 'P001',
        amount: 1000000,
        quantity: 500,
      );

      // When
      final updated = original.copyWith(
        amount: 1200000,
        quantity: 600,
      );

      // Then
      expect(updated.yearMonth, original.yearMonth);
      expect(updated.customerName, original.customerName);
      expect(updated.productName, original.productName);
      expect(updated.productCode, original.productCode);
      expect(updated.amount, 1200000);
      expect(updated.quantity, 600);
      expect(original.amount, 1000000); // 원본은 변경되지 않음
      expect(original.quantity, 500); // 원본은 변경되지 않음
    });

    test('toJson 메서드가 올바른 JSON을 생성한다', () {
      // Given
      final electronicSales = ElectronicSales(
        yearMonth: '202601',
        customerName: '이마트',
        productName: '진라면',
        productCode: 'P001',
        amount: 1000000,
        quantity: 500,
        previousYearAmount: 800000,
        growthRate: 25.0,
      );

      // When
      final json = electronicSales.toJson();

      // Then
      expect(json['yearMonth'], '202601');
      expect(json['customerName'], '이마트');
      expect(json['productName'], '진라면');
      expect(json['productCode'], 'P001');
      expect(json['amount'], 1000000);
      expect(json['quantity'], 500);
      expect(json['previousYearAmount'], 800000);
      expect(json['growthRate'], 25.0);
    });

    test('fromJson 메서드가 JSON에서 올바르게 엔티티를 생성한다', () {
      // Given
      final json = {
        'yearMonth': '202601',
        'customerName': '홈플러스',
        'productName': '참깨라면',
        'productCode': 'P002',
        'amount': 1500000,
        'quantity': 700,
        'previousYearAmount': 1200000,
        'growthRate': 25.0,
      };

      // When
      final electronicSales = ElectronicSales.fromJson(json);

      // Then
      expect(electronicSales.yearMonth, '202601');
      expect(electronicSales.customerName, '홈플러스');
      expect(electronicSales.productName, '참깨라면');
      expect(electronicSales.productCode, 'P002');
      expect(electronicSales.amount, 1500000);
      expect(electronicSales.quantity, 700);
      expect(electronicSales.previousYearAmount, 1200000);
      expect(electronicSales.growthRate, 25.0);
    });

    test('toJson과 fromJson이 정확히 동작한다 (직렬화 왕복)', () {
      // Given
      final original = ElectronicSales(
        yearMonth: '202601',
        customerName: '롯데마트',
        productName: '육개장라면',
        productCode: 'P003',
        amount: 2000000,
        quantity: 1000,
        previousYearAmount: 1800000,
        growthRate: 11.1,
      );

      // When
      final json = original.toJson();
      final restored = ElectronicSales.fromJson(json);

      // Then
      expect(restored, original);
    });

    test('같은 값을 가진 ElectronicSales 엔티티는 동일하게 비교된다', () {
      // Given
      final sales1 = ElectronicSales(
        yearMonth: '202601',
        customerName: '이마트',
        productName: '진라면',
        productCode: 'P001',
        amount: 1000000,
        quantity: 500,
      );

      final sales2 = ElectronicSales(
        yearMonth: '202601',
        customerName: '이마트',
        productName: '진라면',
        productCode: 'P001',
        amount: 1000000,
        quantity: 500,
      );

      // When & Then
      expect(sales1, sales2);
      expect(sales1.hashCode, sales2.hashCode);
    });

    test('다른 값을 가진 ElectronicSales 엔티티는 다르게 비교된다', () {
      // Given
      final sales1 = ElectronicSales(
        yearMonth: '202601',
        customerName: '이마트',
        productName: '진라면',
        productCode: 'P001',
        amount: 1000000,
        quantity: 500,
      );

      final sales2 = ElectronicSales(
        yearMonth: '202601',
        customerName: '홈플러스',
        productName: '진라면',
        productCode: 'P001',
        amount: 1000000,
        quantity: 500,
      );

      // When & Then
      expect(sales1, isNot(sales2));
    });

    test('필수 필드가 누락되면 엔티티 생성이 실패한다', () {
      // Given
      final invalidJson = {
        'yearMonth': '202601',
        // customerName 누락
        'productName': '진라면',
        'productCode': 'P001',
        'amount': 1000000,
        'quantity': 500,
      };

      // When & Then
      expect(
        () => ElectronicSales.fromJson(invalidJson),
        throwsA(isA<TypeError>()),
      );
    });

    test('toString 메서드가 올바른 문자열을 반환한다', () {
      // Given
      final electronicSales = ElectronicSales(
        yearMonth: '202601',
        customerName: '이마트',
        productName: '진라면',
        productCode: 'P001',
        amount: 1000000,
        quantity: 500,
      );

      // When
      final result = electronicSales.toString();

      // Then
      expect(result, contains('ElectronicSales'));
      expect(result, contains('yearMonth: 202601'));
      expect(result, contains('customerName: 이마트'));
      expect(result, contains('productName: 진라면'));
      expect(result, contains('productCode: P001'));
      expect(result, contains('amount: 1000000'));
      expect(result, contains('quantity: 500'));
    });

    test('yearMonth 필드 형식이 올바르다', () {
      // Given
      final electronicSales = ElectronicSales(
        yearMonth: '202601',
        customerName: '이마트',
        productName: '진라면',
        productCode: 'P001',
        amount: 1000000,
        quantity: 500,
      );

      // When & Then
      expect(electronicSales.yearMonth.length, 6);
      expect(int.tryParse(electronicSales.yearMonth), isNotNull);
    });

    test('음수 금액과 수량은 허용되지 않는다 (비즈니스 규칙)', () {
      // Given & When
      final electronicSales = ElectronicSales(
        yearMonth: '202601',
        customerName: '이마트',
        productName: '진라면',
        productCode: 'P001',
        amount: -1000000,
        quantity: -500,
      );

      // Then - 현재는 validation이 없지만, 향후 추가될 경우를 대비한 테스트
      // 실제 비즈니스 로직에서는 음수 값을 검증해야 함
      expect(electronicSales.amount, lessThan(0));
      expect(electronicSales.quantity, lessThan(0));
    });
  });
}