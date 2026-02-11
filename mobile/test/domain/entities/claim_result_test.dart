import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_result.dart';

void main() {
  group('ClaimRegisterResult Entity', () {
    final testCreatedAt = DateTime(2026, 2, 11, 10, 30);

    test('엔티티가 올바르게 생성된다', () {
      // Given & When
      final result = ClaimRegisterResult(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: testCreatedAt,
      );

      // Then
      expect(result.id, 100);
      expect(result.storeName, '미광종합물류');
      expect(result.storeId, 1025);
      expect(result.productName, '맛있는부대찌개라양념140G');
      expect(result.productCode, '12345678');
      expect(result.createdAt, testCreatedAt);
    });

    test('toJson이 올바르게 동작한다', () {
      // Given
      final result = ClaimRegisterResult(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: testCreatedAt,
      );

      // When
      final json = result.toJson();

      // Then
      expect(json, {
        'id': 100,
        'storeName': '미광종합물류',
        'storeId': 1025,
        'productName': '맛있는부대찌개라양념140G',
        'productCode': '12345678',
        'createdAt': testCreatedAt.toIso8601String(),
      });
    });

    test('fromJson이 올바르게 동작한다', () {
      // Given
      final json = {
        'id': 100,
        'storeName': '미광종합물류',
        'storeId': 1025,
        'productName': '맛있는부대찌개라양념140G',
        'productCode': '12345678',
        'createdAt': '2026-02-11T10:30:00.000',
      };

      // When
      final result = ClaimRegisterResult.fromJson(json);

      // Then
      expect(result.id, 100);
      expect(result.storeName, '미광종합물류');
      expect(result.storeId, 1025);
      expect(result.productName, '맛있는부대찌개라양념140G');
      expect(result.productCode, '12345678');
      expect(result.createdAt, DateTime(2026, 2, 11, 10, 30));
    });

    test('toJson과 fromJson이 정확히 왕복 변환된다', () {
      // Given
      final original = ClaimRegisterResult(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: testCreatedAt,
      );

      // When
      final json = original.toJson();
      final restored = ClaimRegisterResult.fromJson(json);

      // Then
      expect(restored, original);
    });

    test('copyWith가 올바르게 동작한다', () {
      // Given
      final original = ClaimRegisterResult(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: testCreatedAt,
      );

      // When
      final copied = original.copyWith(
        id: 200,
        productName: '변경된 제품명',
      );

      // Then
      expect(copied.id, 200);
      expect(copied.productName, '변경된 제품명');
      expect(copied.storeName, original.storeName);
      expect(copied.storeId, original.storeId);
      expect(original.id, 100); // 원본 불변성 확인
    });

    test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
      // Given
      final result1 = ClaimRegisterResult(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: testCreatedAt,
      );
      final result2 = ClaimRegisterResult(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: testCreatedAt,
      );

      // Then
      expect(result1, result2);
      expect(result1.hashCode, result2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      // Given
      final result1 = ClaimRegisterResult(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: testCreatedAt,
      );
      final result2 = ClaimRegisterResult(
        id: 200,
        storeName: '다른 거래처',
        storeId: 2000,
        productName: '다른 제품',
        productCode: '87654321',
        createdAt: testCreatedAt,
      );

      // Then
      expect(result1, isNot(result2));
    });

    test('toString이 올바른 형식으로 출력된다', () {
      // Given
      final result = ClaimRegisterResult(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: testCreatedAt,
      );

      // When
      final str = result.toString();

      // Then
      expect(str, contains('ClaimRegisterResult'));
      expect(str, contains('id: 100'));
      expect(str, contains('storeName: 미광종합물류'));
      expect(str, contains('productName: 맛있는부대찌개라양념140G'));
    });
  });
}
