import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/product_model.dart';
import 'package:mobile/domain/entities/product.dart';

void main() {
  group('ProductModel', () {
    const model = ProductModel(
      productId: '18110014',
      productName: '열라면_용기105G',
      productCode: '18110014',
      barcode: '8801045570716',
      storageType: '상온',
      shelfLife: '7개월',
      categoryMid: '라면',
      categorySub: '용기면',
    );

    const entity = Product(
      productId: '18110014',
      productName: '열라면_용기105G',
      productCode: '18110014',
      barcode: '8801045570716',
      storageType: '상온',
      shelfLife: '7개월',
      categoryMid: '라면',
      categorySub: '용기면',
    );

    group('fromJson', () {
      test('snake_case JSON을 올바르게 파싱한다', () {
        final json = {
          'product_id': '18110014',
          'product_name': '열라면_용기105G',
          'product_code': '18110014',
          'barcode': '8801045570716',
          'storage_type': '상온',
          'shelf_life': '7개월',
          'category_mid': '라면',
          'category_sub': '용기면',
        };

        final parsed = ProductModel.fromJson(json);

        expect(parsed.productId, '18110014');
        expect(parsed.productName, '열라면_용기105G');
        expect(parsed.productCode, '18110014');
        expect(parsed.barcode, '8801045570716');
        expect(parsed.storageType, '상온');
        expect(parsed.shelfLife, '7개월');
        expect(parsed.categoryMid, '라면');
        expect(parsed.categorySub, '용기면');
      });

      test('nullable 필드가 null인 JSON을 올바르게 파싱한다', () {
        final json = {
          'product_id': '18110014',
          'product_name': '열라면_용기105G',
          'product_code': '18110014',
          'barcode': '8801045570716',
          'storage_type': '상온',
          'shelf_life': '7개월',
          'category_mid': null,
          'category_sub': null,
        };

        final parsed = ProductModel.fromJson(json);
        expect(parsed.categoryMid, isNull);
        expect(parsed.categorySub, isNull);
      });
    });

    group('toJson', () {
      test('snake_case JSON으로 올바르게 직렬화한다', () {
        final json = model.toJson();

        expect(json['product_id'], '18110014');
        expect(json['product_name'], '열라면_용기105G');
        expect(json['product_code'], '18110014');
        expect(json['barcode'], '8801045570716');
        expect(json['storage_type'], '상온');
        expect(json['shelf_life'], '7개월');
        expect(json['category_mid'], '라면');
        expect(json['category_sub'], '용기면');
      });

      test('toJson → fromJson 라운드트립이 정확하다', () {
        final json = model.toJson();
        final restored = ProductModel.fromJson(json);
        expect(restored, model);
      });
    });

    group('toEntity', () {
      test('Product 엔티티로 올바르게 변환한다', () {
        final converted = model.toEntity();
        expect(converted, entity);
      });

      test('nullable 필드도 올바르게 변환한다', () {
        const modelNoCategory = ProductModel(
          productId: '18110014',
          productName: '열라면_용기105G',
          productCode: '18110014',
          barcode: '8801045570716',
          storageType: '상온',
          shelfLife: '7개월',
        );

        final converted = modelNoCategory.toEntity();
        expect(converted.categoryMid, isNull);
        expect(converted.categorySub, isNull);
      });
    });

    group('fromEntity', () {
      test('Product 엔티티에서 올바르게 생성한다', () {
        final fromEntity = ProductModel.fromEntity(entity);
        expect(fromEntity, model);
      });

      test('toEntity → fromEntity 라운드트립이 정확하다', () {
        final converted = model.toEntity();
        final restored = ProductModel.fromEntity(converted);
        expect(restored, model);
      });
    });

    group('equality', () {
      test('같은 값을 가진 인스턴스는 동일하다', () {
        const model2 = ProductModel(
          productId: '18110014',
          productName: '열라면_용기105G',
          productCode: '18110014',
          barcode: '8801045570716',
          storageType: '상온',
          shelfLife: '7개월',
          categoryMid: '라면',
          categorySub: '용기면',
        );

        expect(model, model2);
        expect(model.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 인스턴스는 다르다', () {
        const different = ProductModel(
          productId: '99999999',
          productName: '다른제품',
          productCode: '99999999',
          barcode: '0000000000000',
          storageType: '냉동',
          shelfLife: '12개월',
        );

        expect(model, isNot(different));
      });
    });

    test('toString이 올바른 문자열을 반환한다', () {
      final str = model.toString();
      expect(str, contains('ProductModel('));
      expect(str, contains('productId: 18110014'));
      expect(str, contains('productName: 열라면_용기105G'));
    });
  });
}
