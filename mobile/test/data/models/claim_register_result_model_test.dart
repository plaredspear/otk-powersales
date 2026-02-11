import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/claim_register_result_model.dart';
import 'package:mobile/domain/entities/claim_result.dart';

void main() {
  group('ClaimRegisterResultModel', () {
    test('fromJson이 올바르게 동작한다', () {
      // Given
      final json = {
        'id': 100,
        'storeName': '미광종합물류',
        'storeId': 1025,
        'productName': '맛있는부대찌개라양념140G',
        'productCode': '12345678',
        'createdAt': '2026-02-11T10:30:00',
      };

      // When
      final model = ClaimRegisterResultModel.fromJson(json);

      // Then
      expect(model.id, 100);
      expect(model.storeName, '미광종합물류');
      expect(model.storeId, 1025);
      expect(model.productName, '맛있는부대찌개라양념140G');
      expect(model.productCode, '12345678');
      expect(model.createdAt, '2026-02-11T10:30:00');
    });

    test('toEntity가 올바르게 동작한다', () {
      // Given
      const model = ClaimRegisterResultModel(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: '2026-02-11T10:30:00',
      );

      // When
      final entity = model.toEntity();

      // Then
      expect(entity, isA<ClaimRegisterResult>());
      expect(entity.id, 100);
      expect(entity.storeName, '미광종합물류');
      expect(entity.createdAt, DateTime(2026, 2, 11, 10, 30));
    });

    test('fromJson -> toEntity 변환이 올바르게 동작한다', () {
      // Given
      final json = {
        'id': 200,
        'storeName': '테스트 거래처',
        'storeId': 2000,
        'productName': '테스트 제품',
        'productCode': '87654321',
        'createdAt': '2026-03-15T14:20:30.500',
      };

      // When
      final model = ClaimRegisterResultModel.fromJson(json);
      final entity = model.toEntity();

      // Then
      expect(entity.id, 200);
      expect(entity.storeName, '테스트 거래처');
      expect(entity.createdAt, DateTime(2026, 3, 15, 14, 20, 30, 500));
    });
  });
}
