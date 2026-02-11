import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/shelf_life_item_model.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';

void main() {
  group('ShelfLifeItemModel', () {
    const testModel = ShelfLifeItemModel(
      id: 1,
      productCode: 'P001',
      productName: '진라면',
      storeName: '이마트 강남점',
      storeId: 100,
      expiryDate: '2026-03-15',
      alertDate: '2026-03-08',
      dDay: 32,
      description: '3층 선반',
      isExpired: false,
    );

    final testJson = {
      'id': 1,
      'product_code': 'P001',
      'product_name': '진라면',
      'store_name': '이마트 강남점',
      'store_id': 100,
      'expiry_date': '2026-03-15',
      'alert_date': '2026-03-08',
      'd_day': 32,
      'description': '3층 선반',
      'is_expired': false,
    };

    final testEntity = ShelfLifeItem(
      id: 1,
      productCode: 'P001',
      productName: '진라면',
      storeName: '이마트 강남점',
      storeId: 100,
      expiryDate: DateTime(2026, 3, 15),
      alertDate: DateTime(2026, 3, 8),
      dDay: 32,
      description: '3층 선반',
      isExpired: false,
    );

    group('fromJson', () {
      test('snake_case JSON 키를 올바르게 파싱해야 한다', () {
        final result = ShelfLifeItemModel.fromJson(testJson);

        expect(result.id, 1);
        expect(result.productCode, 'P001');
        expect(result.productName, '진라면');
        expect(result.storeName, '이마트 강남점');
        expect(result.storeId, 100);
        expect(result.expiryDate, '2026-03-15');
        expect(result.alertDate, '2026-03-08');
        expect(result.dDay, 32);
        expect(result.description, '3층 선반');
        expect(result.isExpired, false);
      });

      test('description이 null이면 빈 문자열로 처리해야 한다', () {
        final jsonWithNull = Map<String, dynamic>.from(testJson);
        jsonWithNull['description'] = null;

        final result = ShelfLifeItemModel.fromJson(jsonWithNull);

        expect(result.description, '');
      });

      test('description 키가 없으면 빈 문자열로 처리해야 한다', () {
        final jsonWithout = Map<String, dynamic>.from(testJson);
        jsonWithout.remove('description');

        final result = ShelfLifeItemModel.fromJson(jsonWithout);

        expect(result.description, '');
      });

      test('만료된 항목을 올바르게 파싱해야 한다', () {
        final expiredJson = Map<String, dynamic>.from(testJson);
        expiredJson['is_expired'] = true;
        expiredJson['d_day'] = -5;

        final result = ShelfLifeItemModel.fromJson(expiredJson);

        expect(result.isExpired, true);
        expect(result.dDay, -5);
      });
    });

    group('toJson', () {
      test('snake_case JSON 키로 올바르게 직렬화해야 한다', () {
        final result = testModel.toJson();

        expect(result['id'], 1);
        expect(result['product_code'], 'P001');
        expect(result['product_name'], '진라면');
        expect(result['store_name'], '이마트 강남점');
        expect(result['store_id'], 100);
        expect(result['expiry_date'], '2026-03-15');
        expect(result['alert_date'], '2026-03-08');
        expect(result['d_day'], 32);
        expect(result['description'], '3층 선반');
        expect(result['is_expired'], false);
      });
    });

    group('toEntity', () {
      test('올바른 ShelfLifeItem 엔티티를 생성해야 한다', () {
        final result = testModel.toEntity();

        expect(result.id, testModel.id);
        expect(result.productCode, testModel.productCode);
        expect(result.productName, testModel.productName);
        expect(result.storeName, testModel.storeName);
        expect(result.storeId, testModel.storeId);
        expect(result.expiryDate, DateTime(2026, 3, 15));
        expect(result.alertDate, DateTime(2026, 3, 8));
        expect(result.dDay, testModel.dDay);
        expect(result.description, testModel.description);
        expect(result.isExpired, testModel.isExpired);
      });

      test('날짜 문자열을 DateTime으로 올바르게 변환해야 한다', () {
        final result = testModel.toEntity();

        expect(result.expiryDate.year, 2026);
        expect(result.expiryDate.month, 3);
        expect(result.expiryDate.day, 15);
        expect(result.alertDate.year, 2026);
        expect(result.alertDate.month, 3);
        expect(result.alertDate.day, 8);
      });
    });

    group('fromEntity', () {
      test('ShelfLifeItem 엔티티로부터 올바른 모델을 생성해야 한다', () {
        final result = ShelfLifeItemModel.fromEntity(testEntity);

        expect(result.id, testEntity.id);
        expect(result.productCode, testEntity.productCode);
        expect(result.productName, testEntity.productName);
        expect(result.storeName, testEntity.storeName);
        expect(result.storeId, testEntity.storeId);
        expect(result.expiryDate, '2026-03-15');
        expect(result.alertDate, '2026-03-08');
        expect(result.dDay, testEntity.dDay);
        expect(result.description, testEntity.description);
        expect(result.isExpired, testEntity.isExpired);
      });

      test('DateTime을 YYYY-MM-DD 문자열로 올바르게 변환해야 한다', () {
        final result = ShelfLifeItemModel.fromEntity(testEntity);

        expect(result.expiryDate, '2026-03-15');
        expect(result.alertDate, '2026-03-08');
      });
    });

    group('round trip', () {
      test('fromJson -> toEntity -> fromEntity -> toJson 변환이 일관성 있어야 한다', () {
        final modelFromJson = ShelfLifeItemModel.fromJson(testJson);
        final entity = modelFromJson.toEntity();
        final modelFromEntity = ShelfLifeItemModel.fromEntity(entity);
        final jsonResult = modelFromEntity.toJson();

        expect(jsonResult, testJson);
      });

      test('toJson -> fromJson 왕복 변환이 동일한 모델을 반환해야 한다', () {
        final json = testModel.toJson();
        final result = ShelfLifeItemModel.fromJson(json);

        expect(result, testModel);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 모델은 동일해야 한다', () {
        const model1 = ShelfLifeItemModel(
          id: 1,
          productCode: 'P001',
          productName: '진라면',
          storeName: '이마트 강남점',
          storeId: 100,
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
          dDay: 32,
          description: '3층 선반',
          isExpired: false,
        );
        const model2 = ShelfLifeItemModel(
          id: 1,
          productCode: 'P001',
          productName: '진라면',
          storeName: '이마트 강남점',
          storeId: 100,
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
          dDay: 32,
          description: '3층 선반',
          isExpired: false,
        );

        expect(model1, model2);
      });

      test('다른 값을 가진 두 모델은 동일하지 않아야 한다', () {
        const model2 = ShelfLifeItemModel(
          id: 2,
          productCode: 'P002',
          productName: '케첩',
          storeName: '이마트 강남점',
          storeId: 100,
          expiryDate: '2026-04-01',
          alertDate: '2026-03-25',
          dDay: 49,
          description: '',
          isExpired: false,
        );

        expect(testModel, isNot(model2));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 모델은 같은 hashCode를 가져야 한다', () {
        const model1 = ShelfLifeItemModel(
          id: 1,
          productCode: 'P001',
          productName: '진라면',
          storeName: '이마트 강남점',
          storeId: 100,
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
          dDay: 32,
          description: '3층 선반',
          isExpired: false,
        );
        const model2 = ShelfLifeItemModel(
          id: 1,
          productCode: 'P001',
          productName: '진라면',
          storeName: '이마트 강남점',
          storeId: 100,
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
          dDay: 32,
          description: '3층 선반',
          isExpired: false,
        );

        expect(model1.hashCode, model2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환해야 한다', () {
        final result = testModel.toString();

        expect(result, contains('ShelfLifeItemModel'));
        expect(result, contains('id: 1'));
        expect(result, contains('productCode: P001'));
        expect(result, contains('productName: 진라면'));
        expect(result, contains('storeName: 이마트 강남점'));
        expect(result, contains('storeId: 100'));
        expect(result, contains('expiryDate: 2026-03-15'));
        expect(result, contains('alertDate: 2026-03-08'));
        expect(result, contains('dDay: 32'));
        expect(result, contains('description: 3층 선반'));
        expect(result, contains('isExpired: false'));
      });
    });

    group('기본값', () {
      test('description 기본값은 빈 문자열이어야 한다', () {
        const model = ShelfLifeItemModel(
          id: 1,
          productCode: 'P001',
          productName: '진라면',
          storeName: '이마트 강남점',
          storeId: 100,
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
          dDay: 32,
          isExpired: false,
        );

        expect(model.description, '');
      });
    });
  });
}
