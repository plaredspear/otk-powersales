import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product_for_order.dart';

void main() {
  group('ProductForOrder', () {
    test('ProductForOrder 생성 테스트', () {
      final product = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '즉석식품',
        categorySub: '카레',
      );

      expect(product.productCode, 'P001');
      expect(product.productName, '오뚜기 카레');
      expect(product.barcode, '8801045535234');
      expect(product.storageType, '상온');
      expect(product.shelfLife, '24개월');
      expect(product.unitPrice, 15000);
      expect(product.boxSize, 20);
      expect(product.isFavorite, true);
      expect(product.categoryMid, '즉석식품');
      expect(product.categorySub, '카레');
    });

    test('ProductForOrder 생성 테스트 - optional 필드 null', () {
      final product = ProductForOrder(
        productCode: 'P002',
        productName: '오뚜기 참깨라면',
        barcode: '8801045012345',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 12000,
        boxSize: 30,
        isFavorite: false,
      );

      expect(product.productCode, 'P002');
      expect(product.productName, '오뚜기 참깨라면');
      expect(product.categoryMid, null);
      expect(product.categorySub, null);
    });

    test('copyWith 테스트', () {
      final original = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: false,
        categoryMid: '즉석식품',
        categorySub: '카레',
      );

      final copied = original.copyWith(
        unitPrice: 16000,
        isFavorite: true,
      );

      expect(copied.productCode, 'P001');
      expect(copied.productName, '오뚜기 카레');
      expect(copied.barcode, '8801045535234');
      expect(copied.storageType, '상온');
      expect(copied.shelfLife, '24개월');
      expect(copied.unitPrice, 16000);
      expect(copied.boxSize, 20);
      expect(copied.isFavorite, true);
      expect(copied.categoryMid, '즉석식품');
      expect(copied.categorySub, '카레');
    });

    test('toJson 테스트', () {
      final product = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '즉석식품',
        categorySub: '카레',
      );

      final json = product.toJson();

      expect(json['productCode'], 'P001');
      expect(json['productName'], '오뚜기 카레');
      expect(json['barcode'], '8801045535234');
      expect(json['storageType'], '상온');
      expect(json['shelfLife'], '24개월');
      expect(json['unitPrice'], 15000);
      expect(json['boxSize'], 20);
      expect(json['isFavorite'], true);
      expect(json['categoryMid'], '즉석식품');
      expect(json['categorySub'], '카레');
    });

    test('fromJson 테스트', () {
      final json = {
        'productCode': 'P002',
        'productName': '오뚜기 참깨라면',
        'barcode': '8801045012345',
        'storageType': '상온',
        'shelfLife': '12개월',
        'unitPrice': 12000,
        'boxSize': 30,
        'isFavorite': false,
        'categoryMid': '라면',
        'categorySub': '봉지라면',
      };

      final product = ProductForOrder.fromJson(json);

      expect(product.productCode, 'P002');
      expect(product.productName, '오뚜기 참깨라면');
      expect(product.barcode, '8801045012345');
      expect(product.storageType, '상온');
      expect(product.shelfLife, '12개월');
      expect(product.unitPrice, 12000);
      expect(product.boxSize, 30);
      expect(product.isFavorite, false);
      expect(product.categoryMid, '라면');
      expect(product.categorySub, '봉지라면');
    });

    test('toJson/fromJson 왕복 변환 테스트', () {
      final original = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '즉석식품',
        categorySub: '카레',
      );

      final json = original.toJson();
      final restored = ProductForOrder.fromJson(json);

      expect(restored, original);
    });

    test('toJson/fromJson with null optional fields', () {
      final original = ProductForOrder(
        productCode: 'P003',
        productName: '오뚜기 케첩',
        barcode: '8801045098765',
        storageType: '냉장',
        shelfLife: '18개월',
        unitPrice: 8000,
        boxSize: 24,
        isFavorite: false,
      );

      final json = original.toJson();
      final restored = ProductForOrder.fromJson(json);

      expect(restored, original);
      expect(restored.categoryMid, null);
      expect(restored.categorySub, null);
    });

    test('storageTypeIcon getter - 냉장', () {
      final product = ProductForOrder(
        productCode: 'P001',
        productName: '냉장 제품',
        barcode: '1234567890123',
        storageType: '냉장',
        shelfLife: '3개월',
        unitPrice: 10000,
        boxSize: 10,
        isFavorite: false,
      );

      expect(product.storageTypeIcon, '🧊');
    });

    test('storageTypeIcon getter - 냉동', () {
      final product = ProductForOrder(
        productCode: 'P002',
        productName: '냉동 제품',
        barcode: '1234567890123',
        storageType: '냉동',
        shelfLife: '12개월',
        unitPrice: 20000,
        boxSize: 15,
        isFavorite: false,
      );

      expect(product.storageTypeIcon, '❄️');
    });

    test('storageTypeIcon getter - 상온', () {
      final product = ProductForOrder(
        productCode: 'P003',
        productName: '상온 제품',
        barcode: '1234567890123',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: false,
      );

      expect(product.storageTypeIcon, '🌡️');
    });

    test('storageTypeIcon getter - 알 수 없는 타입', () {
      final product = ProductForOrder(
        productCode: 'P004',
        productName: '기타 제품',
        barcode: '1234567890123',
        storageType: '기타',
        shelfLife: '6개월',
        unitPrice: 5000,
        boxSize: 50,
        isFavorite: false,
      );

      expect(product.storageTypeIcon, '');
    });

    test('equality 테스트 - 동일한 객체', () {
      final product1 = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '즉석식품',
        categorySub: '카레',
      );

      final product2 = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '즉석식품',
        categorySub: '카레',
      );

      expect(product1, product2);
      expect(product1.hashCode, product2.hashCode);
    });

    test('equality 테스트 - 다른 객체', () {
      final product1 = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
      );

      final product2 = ProductForOrder(
        productCode: 'P002',
        productName: '오뚜기 참깨라면',
        barcode: '8801045012345',
        storageType: '상온',
        shelfLife: '12개월',
        unitPrice: 12000,
        boxSize: 30,
        isFavorite: false,
      );

      expect(product1, isNot(product2));
    });

    test('equality 테스트 - optional 필드가 다른 경우', () {
      final product1 = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '즉석식품',
      );

      final product2 = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '라면',
      );

      expect(product1, isNot(product2));
    });

    test('hashCode 테스트', () {
      final product1 = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '즉석식품',
        categorySub: '카레',
      );

      final product2 = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
        categoryMid: '즉석식품',
        categorySub: '카레',
      );

      expect(product1.hashCode, product2.hashCode);
    });

    test('toString 테스트', () {
      final product = ProductForOrder(
        productCode: 'P001',
        productName: '오뚜기 카레',
        barcode: '8801045535234',
        storageType: '상온',
        shelfLife: '24개월',
        unitPrice: 15000,
        boxSize: 20,
        isFavorite: true,
      );

      final str = product.toString();
      expect(str, contains('ProductForOrder'));
      expect(str, contains('P001'));
      expect(str, contains('오뚜기 카레'));
      expect(str, contains('상온'));
    });
  });
}
