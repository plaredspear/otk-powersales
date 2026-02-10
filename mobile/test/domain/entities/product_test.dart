import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product.dart';

void main() {
  group('Product 엔티티', () {
    const product = Product(
      productId: '18110014',
      productName: '열라면_용기105G',
      productCode: '18110014',
      barcode: '8801045570716',
      storageType: '상온',
      shelfLife: '7개월',
      categoryMid: '라면',
      categorySub: '용기면',
    );

    test('올바르게 생성된다', () {
      expect(product.productId, '18110014');
      expect(product.productName, '열라면_용기105G');
      expect(product.productCode, '18110014');
      expect(product.barcode, '8801045570716');
      expect(product.storageType, '상온');
      expect(product.shelfLife, '7개월');
      expect(product.categoryMid, '라면');
      expect(product.categorySub, '용기면');
    });

    test('optional 필드가 null인 경우 올바르게 생성된다', () {
      const productWithoutCategory = Product(
        productId: '18110014',
        productName: '열라면_용기105G',
        productCode: '18110014',
        barcode: '8801045570716',
        storageType: '상온',
        shelfLife: '7개월',
      );

      expect(productWithoutCategory.categoryMid, isNull);
      expect(productWithoutCategory.categorySub, isNull);
    });

    group('copyWith', () {
      test('일부 필드만 변경한 복사본을 생성한다', () {
        final copied = product.copyWith(
          productName: '변경된_제품명',
          storageType: '냉장',
        );

        expect(copied.productId, '18110014');
        expect(copied.productName, '변경된_제품명');
        expect(copied.productCode, '18110014');
        expect(copied.barcode, '8801045570716');
        expect(copied.storageType, '냉장');
        expect(copied.shelfLife, '7개월');
        expect(copied.categoryMid, '라면');
        expect(copied.categorySub, '용기면');
      });

      test('모든 필드를 변경한 복사본을 생성한다', () {
        final copied = product.copyWith(
          productId: 'NEW001',
          productName: '새제품',
          productCode: 'NEW001',
          barcode: '9999999999999',
          storageType: '냉동',
          shelfLife: '12개월',
          categoryMid: '냉동식품',
          categorySub: '만두',
        );

        expect(copied.productId, 'NEW001');
        expect(copied.productName, '새제품');
        expect(copied.productCode, 'NEW001');
        expect(copied.barcode, '9999999999999');
        expect(copied.storageType, '냉동');
        expect(copied.shelfLife, '12개월');
        expect(copied.categoryMid, '냉동식품');
        expect(copied.categorySub, '만두');
      });

      test('인자 없이 호출하면 동일한 객체를 반환한다', () {
        final copied = product.copyWith();
        expect(copied, product);
      });
    });

    group('toJson / fromJson', () {
      test('toJson이 올바른 Map을 반환한다', () {
        final json = product.toJson();

        expect(json['productId'], '18110014');
        expect(json['productName'], '열라면_용기105G');
        expect(json['productCode'], '18110014');
        expect(json['barcode'], '8801045570716');
        expect(json['storageType'], '상온');
        expect(json['shelfLife'], '7개월');
        expect(json['categoryMid'], '라면');
        expect(json['categorySub'], '용기면');
      });

      test('fromJson이 올바른 엔티티를 생성한다', () {
        final json = {
          'productId': '18110014',
          'productName': '열라면_용기105G',
          'productCode': '18110014',
          'barcode': '8801045570716',
          'storageType': '상온',
          'shelfLife': '7개월',
          'categoryMid': '라면',
          'categorySub': '용기면',
        };

        final fromJson = Product.fromJson(json);
        expect(fromJson, product);
      });

      test('toJson → fromJson 라운드트립이 정확하다', () {
        final json = product.toJson();
        final restored = Product.fromJson(json);
        expect(restored, product);
      });

      test('null 카테고리 필드도 올바르게 직렬화된다', () {
        const productNoCategory = Product(
          productId: '18110014',
          productName: '열라면_용기105G',
          productCode: '18110014',
          barcode: '8801045570716',
          storageType: '상온',
          shelfLife: '7개월',
        );

        final json = productNoCategory.toJson();
        expect(json['categoryMid'], isNull);
        expect(json['categorySub'], isNull);

        final restored = Product.fromJson(json);
        expect(restored, productNoCategory);
      });
    });

    group('equality', () {
      test('같은 값을 가진 인스턴스는 동일하다', () {
        const product2 = Product(
          productId: '18110014',
          productName: '열라면_용기105G',
          productCode: '18110014',
          barcode: '8801045570716',
          storageType: '상온',
          shelfLife: '7개월',
          categoryMid: '라면',
          categorySub: '용기면',
        );

        expect(product, product2);
        expect(product.hashCode, product2.hashCode);
      });

      test('다른 값을 가진 인스턴스는 다르다', () {
        const different = Product(
          productId: '99999999',
          productName: '다른제품',
          productCode: '99999999',
          barcode: '0000000000000',
          storageType: '냉동',
          shelfLife: '12개월',
        );

        expect(product, isNot(different));
      });

      test('productId만 다른 경우도 다르다', () {
        final different = product.copyWith(productId: 'DIFFERENT');
        expect(product, isNot(different));
      });
    });

    test('toString이 올바른 문자열을 반환한다', () {
      final str = product.toString();
      expect(str, contains('Product('));
      expect(str, contains('productId: 18110014'));
      expect(str, contains('productName: 열라면_용기105G'));
      expect(str, contains('barcode: 8801045570716'));
    });
  });
}
