import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/inspection_list_item_model.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionListItemModel', () {
    group('fromJson', () {
      test('JSON에서 모델을 생성한다', () {
        // Given
        final json = {
          'id': 1,
          'category': 'OWN',
          'storeName': '이마트 죽전점',
          'storeId': 100,
          'inspectionDate': '2020-08-13',
          'fieldType': '본매대',
          'fieldTypeCode': 'FT01',
        };

        // When
        final model = InspectionListItemModel.fromJson(json);

        // Then
        expect(model.id, 1);
        expect(model.category, 'OWN');
        expect(model.storeName, '이마트 죽전점');
        expect(model.storeId, 100);
        expect(model.inspectionDate, '2020-08-13');
        expect(model.fieldType, '본매대');
        expect(model.fieldTypeCode, 'FT01');
      });

      test('경쟁사 카테고리 JSON을 파싱한다', () {
        // Given
        final json = {
          'id': 2,
          'category': 'COMPETITOR',
          'storeName': '홈플러스 강남점',
          'storeId': 200,
          'inspectionDate': '2020-08-14',
          'fieldType': '시식',
          'fieldTypeCode': 'FT02',
        };

        // When
        final model = InspectionListItemModel.fromJson(json);

        // Then
        expect(model.category, 'COMPETITOR');
      });
    });

    group('toJson', () {
      test('모델을 JSON으로 직렬화한다', () {
        // Given
        const model = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // When
        final json = model.toJson();

        // Then
        expect(json['id'], 1);
        expect(json['category'], 'OWN');
        expect(json['storeName'], '이마트 죽전점');
        expect(json['storeId'], 100);
        expect(json['inspectionDate'], '2020-08-13');
        expect(json['fieldType'], '본매대');
        expect(json['fieldTypeCode'], 'FT01');
      });
    });

    group('toEntity', () {
      test('모델을 엔티티로 변환한다', () {
        // Given
        const model = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.id, 1);
        expect(entity.category, InspectionCategory.OWN);
        expect(entity.storeName, '이마트 죽전점');
        expect(entity.storeId, 100);
        expect(entity.inspectionDate, DateTime(2020, 8, 13));
        expect(entity.fieldType, '본매대');
        expect(entity.fieldTypeCode, 'FT01');
      });

      test('경쟁사 카테고리를 올바르게 변환한다', () {
        // Given
        const model = InspectionListItemModel(
          id: 2,
          category: 'COMPETITOR',
          storeName: '홈플러스 강남점',
          storeId: 200,
          inspectionDate: '2020-08-14',
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.category, InspectionCategory.COMPETITOR);
      });

      test('날짜 문자열을 DateTime으로 변환한다', () {
        // Given
        const model = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-12-31',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // When
        final entity = model.toEntity();

        // Then
        expect(entity.inspectionDate, DateTime(2020, 12, 31));
      });
    });

    group('fromEntity', () {
      test('엔티티에서 모델을 생성한다', () {
        // Given
        final entity = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // When
        final model = InspectionListItemModel.fromEntity(entity);

        // Then
        expect(model.id, 1);
        expect(model.category, 'OWN');
        expect(model.storeName, '이마트 죽전점');
        expect(model.storeId, 100);
        expect(model.inspectionDate, '2020-08-13');
        expect(model.fieldType, '본매대');
        expect(model.fieldTypeCode, 'FT01');
      });

      test('경쟁사 카테고리를 문자열로 변환한다', () {
        // Given
        final entity = InspectionListItem(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '홈플러스 강남점',
          storeId: 200,
          inspectionDate: DateTime(2020, 8, 14),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        // When
        final model = InspectionListItemModel.fromEntity(entity);

        // Then
        expect(model.category, 'COMPETITOR');
      });

      test('DateTime을 ISO 8601 날짜 문자열로 변환한다', () {
        // Given
        final entity = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: DateTime(2020, 12, 31, 15, 30, 45),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // When
        final model = InspectionListItemModel.fromEntity(entity);

        // Then
        expect(model.inspectionDate, '2020-12-31');
      });
    });

    group('round-trip conversion', () {
      test('Entity → Model → Entity 변환이 정확하다', () {
        // Given
        final originalEntity = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // When
        final model = InspectionListItemModel.fromEntity(originalEntity);
        final convertedEntity = model.toEntity();

        // Then
        expect(convertedEntity.id, originalEntity.id);
        expect(convertedEntity.category, originalEntity.category);
        expect(convertedEntity.storeName, originalEntity.storeName);
        expect(convertedEntity.storeId, originalEntity.storeId);
        expect(convertedEntity.inspectionDate, originalEntity.inspectionDate);
        expect(convertedEntity.fieldType, originalEntity.fieldType);
        expect(convertedEntity.fieldTypeCode, originalEntity.fieldTypeCode);
      });

      test('JSON → Model → JSON 변환이 정확하다', () {
        // Given
        final originalJson = {
          'id': 1,
          'category': 'OWN',
          'storeName': '이마트 죽전점',
          'storeId': 100,
          'inspectionDate': '2020-08-13',
          'fieldType': '본매대',
          'fieldTypeCode': 'FT01',
        };

        // When
        final model = InspectionListItemModel.fromJson(originalJson);
        final convertedJson = model.toJson();

        // Then
        expect(convertedJson, originalJson);
      });
    });

    group('equality and hashCode', () {
      test('같은 값을 가진 인스턴스는 동일하다', () {
        // Given
        const model1 = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );
        const model2 = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // Then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 인스턴스는 동일하지 않다', () {
        // Given
        const model1 = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );
        const model2 = InspectionListItemModel(
          id: 2,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // Then
        expect(model1, isNot(model2));
      });

      test('자기 자신과 동일하다', () {
        // Given
        const model = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // Then
        expect(model, model);
      });
    });

    group('toString', () {
      test('문자열 표현을 반환한다', () {
        // Given
        const model = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        // When
        final result = model.toString();

        // Then
        expect(result, contains('InspectionListItemModel'));
        expect(result, contains('id: 1'));
        expect(result, contains('category: OWN'));
        expect(result, contains('storeName: 이마트 죽전점'));
        expect(result, contains('storeId: 100'));
        expect(result, contains('inspectionDate: 2020-08-13'));
        expect(result, contains('fieldType: 본매대'));
        expect(result, contains('fieldTypeCode: FT01'));
      });
    });
  });
}
