import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/product_for_order_model.dart';
import 'package:mobile/domain/entities/product_for_order.dart';

void main() {
  group('ProductForOrderModel', () {
    test('should create ProductForOrderModel from snake_case JSON', () {
      // Arrange
      final json = {
        'product_code': '01234567',
        'product_name': '진라면',
        'barcode': '8801234567890',
        'storage_type': '상온',
        'shelf_life': '12개월',
        'unit_price': 5000,
        'box_size': 20,
        'is_favorite': true,
        'category_mid': '라면류',
        'category_sub': '봉지라면',
      };

      // Act
      final model = ProductForOrderModel.fromJson(json);

      // Assert
      expect(model.productCode, equals('01234567'));
      expect(model.productName, equals('진라면'));
      expect(model.barcode, equals('8801234567890'));
      expect(model.storageType, equals('상온'));
      expect(model.shelfLife, equals('12개월'));
      expect(model.unitPrice, equals(5000));
      expect(model.boxSize, equals(20));
      expect(model.isFavorite, equals(true));
      expect(model.categoryMid, equals('라면류'));
      expect(model.categorySub, equals('봉지라면'));
    });

    test('should serialize to snake_case JSON', () {
      // Arrange
      const model = ProductForOrderModel(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면류',
        categorySub: '봉지라면',
      );

      // Act
      final json = model.toJson();

      // Assert
      expect(json['product_code'], equals('01234567'));
      expect(json['product_name'], equals('진라면'));
      expect(json['barcode'], equals('8801234567890'));
      expect(json['storage_type'], equals('상온'));
      expect(json['shelf_life'], equals('12개월'));
      expect(json['unit_price'], equals(5000));
      expect(json['box_size'], equals(20));
      expect(json['is_favorite'], equals(true));
      expect(json['category_mid'], equals('라면류'));
      expect(json['category_sub'], equals('봉지라면'));
    });

    test('should convert to ProductForOrder entity correctly', () {
      // Arrange
      const model = ProductForOrderModel(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면류',
        categorySub: '봉지라면',
      );

      // Act
      final entity = model.toEntity();

      // Assert
      expect(entity.productCode, equals('01234567'));
      expect(entity.productName, equals('진라면'));
      expect(entity.barcode, equals('8801234567890'));
      expect(entity.storageType, equals('상온'));
      expect(entity.shelfLife, equals('12개월'));
      expect(entity.unitPrice, equals(5000));
      expect(entity.boxSize, equals(20));
      expect(entity.isFavorite, equals(true));
      expect(entity.categoryMid, equals('라면류'));
      expect(entity.categorySub, equals('봉지라면'));
    });

    test('should create model from ProductForOrder entity', () {
      // Arrange
      const entity = ProductForOrder(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면류',
        categorySub: '봉지라면',
      );

      // Act
      final model = ProductForOrderModel.fromEntity(entity);

      // Assert
      expect(model.productCode, equals('01234567'));
      expect(model.productName, equals('진라면'));
      expect(model.barcode, equals('8801234567890'));
      expect(model.storageType, equals('상온'));
      expect(model.shelfLife, equals('12개월'));
      expect(model.unitPrice, equals(5000));
      expect(model.boxSize, equals(20));
      expect(model.isFavorite, equals(true));
      expect(model.categoryMid, equals('라면류'));
      expect(model.categorySub, equals('봉지라면'));
    });

    test('should handle null optional fields (category_mid, category_sub)', () {
      // Arrange
      final json = {
        'product_code': '01234567',
        'product_name': '진라면',
        'barcode': '8801234567890',
        'storage_type': '상온',
        'shelf_life': '12개월',
        'unit_price': 5000,
        'box_size': 20,
        'is_favorite': false,
        'category_mid': null,
        'category_sub': null,
      };

      // Act
      final model = ProductForOrderModel.fromJson(json);

      // Assert
      expect(model.productCode, equals('01234567'));
      expect(model.productName, equals('진라면'));
      expect(model.barcode, equals('8801234567890'));
      expect(model.storageType, equals('상온'));
      expect(model.shelfLife, equals('12개월'));
      expect(model.unitPrice, equals(5000));
      expect(model.boxSize, equals(20));
      expect(model.isFavorite, equals(false));
      expect(model.categoryMid, isNull);
      expect(model.categorySub, isNull);
    });

    test('should handle omitted optional fields in JSON', () {
      // Arrange
      final json = {
        'product_code': '01234567',
        'product_name': '진라면',
        'barcode': '8801234567890',
        'storage_type': '상온',
        'shelf_life': '12개월',
        'unit_price': 5000,
        'box_size': 20,
        'is_favorite': false,
      };

      // Act
      final model = ProductForOrderModel.fromJson(json);

      // Assert
      expect(model.categoryMid, isNull);
      expect(model.categorySub, isNull);
    });

    test('should support entity roundtrip conversion', () {
      // Arrange
      const originalEntity = ProductForOrder(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면류',
        categorySub: '봉지라면',
      );

      // Act - entity -> model -> entity
      final model = ProductForOrderModel.fromEntity(originalEntity);
      final reconstructedEntity = model.toEntity();

      // Assert
      expect(reconstructedEntity, equals(originalEntity));
    });

    test('should support entity roundtrip with null categories', () {
      // Arrange
      const originalEntity = ProductForOrder(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: false,
        categoryMid: null,
        categorySub: null,
      );

      // Act - entity -> model -> entity
      final model = ProductForOrderModel.fromEntity(originalEntity);
      final reconstructedEntity = model.toEntity();

      // Assert
      expect(reconstructedEntity, equals(originalEntity));
    });

    test('should compare ProductForOrderModels correctly with equality operator',
        () {
      // Arrange
      const model1 = ProductForOrderModel(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면류',
        categorySub: '봉지라면',
      );

      const model2 = ProductForOrderModel(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면류',
        categorySub: '봉지라면',
      );

      const model3 = ProductForOrderModel(
        productCode: '89012345',
        productName: '육개장',
        barcode: '8809876543210',
        storageType: '냉장',
        shelfLife: '6개월',
        unitPrice: 6000,
        boxSize: 15,
        isFavorite: false,
        categoryMid: '라면류',
        categorySub: '컵라면',
      );

      // Assert
      expect(model1, equals(model2));
      expect(model1, isNot(equals(model3)));
    });

    test('should generate consistent hashCode for equal ProductForOrderModels',
        () {
      // Arrange
      const model1 = ProductForOrderModel(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면류',
        categorySub: '봉지라면',
      );

      const model2 = ProductForOrderModel(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면류',
        categorySub: '봉지라면',
      );

      // Assert
      expect(model1.hashCode, equals(model2.hashCode));
    });

    test('should generate toString with all fields', () {
      // Arrange
      const model = ProductForOrderModel(
        productCode: '01234567',
        productName: '진라면',
        barcode: '8801234567890',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 5000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면류',
        categorySub: '봉지라면',
      );

      // Act
      final str = model.toString();

      // Assert
      expect(str, contains('ProductForOrderModel('));
      expect(str, contains('productCode: 01234567'));
      expect(str, contains('productName: 진라면'));
      expect(str, contains('barcode: 8801234567890'));
      expect(str, contains('storageType: 상온'));
      expect(str, contains('shelfLife: 12개월'));
      expect(str, contains('unitPrice: 5000'));
      expect(str, contains('boxSize: 20'));
      expect(str, contains('isFavorite: true'));
      expect(str, contains('categoryMid: 라면류'));
      expect(str, contains('categorySub: 봉지라면'));
    });

    test('should handle different storage types', () {
      // Test different storage type values
      const testCases = [
        '상온',
        '냉장',
        '냉동',
      ];

      for (final storageType in testCases) {
        final model = ProductForOrderModel(
          productCode: '01234567',
          productName: '진라면',
          barcode: '8801234567890',
          storageType: storageType,
          shelfLife: '12개월',
          unitPrice: 5000,
          boxSize: 20,
          isFavorite: false,
        );

        expect(model.storageType, equals(storageType),
            reason: 'Failed for storage type: $storageType');
      }
    });

    test('should handle boolean isFavorite values', () {
      // Test both true and false
      const testCases = [true, false];

      for (final isFavorite in testCases) {
        final model = ProductForOrderModel(
          productCode: '01234567',
          productName: '진라면',
          barcode: '8801234567890',
          storageType: '상온',
          shelfLife: '12개월',
          unitPrice: 5000,
          boxSize: 20,
          isFavorite: isFavorite,
        );

        expect(model.isFavorite, equals(isFavorite),
            reason: 'Failed for isFavorite: $isFavorite');
      }
    });
  });
}
