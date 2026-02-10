import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/store_schedule_item_model.dart';
import 'package:mobile/domain/entities/store_schedule_item.dart';

void main() {
  group('StoreScheduleItemModel', () {
    // 테스트용 샘플 데이터
    const testModel = StoreScheduleItemModel(
      storeId: 123,
      storeName: '이마트 강남점',
      storeCode: 'EMART001',
      workCategory: '정기방문',
      address: '서울시 강남구 테헤란로 123',
      isRegistered: true,
      registeredWorkType: '영업',
    );

    const testJson = {
      'store_id': 123,
      'store_name': '이마트 강남점',
      'store_code': 'EMART001',
      'work_category': '정기방문',
      'address': '서울시 강남구 테헤란로 123',
      'is_registered': true,
      'registered_work_type': '영업',
    };

    const testEntity = StoreScheduleItem(
      storeId: 123,
      storeName: '이마트 강남점',
      storeCode: 'EMART001',
      workCategory: '정기방문',
      address: '서울시 강남구 테헤란로 123',
      isRegistered: true,
      registeredWorkType: '영업',
    );

    group('fromJson', () {
      test('snake_case JSON을 올바르게 파싱해야 함', () {
        // when
        final model = StoreScheduleItemModel.fromJson(testJson);

        // then
        expect(model.storeId, 123);
        expect(model.storeName, '이마트 강남점');
        expect(model.storeCode, 'EMART001');
        expect(model.workCategory, '정기방문');
        expect(model.address, '서울시 강남구 테헤란로 123');
        expect(model.isRegistered, true);
        expect(model.registeredWorkType, '영업');
      });

      test('registered_work_type이 null인 경우를 올바르게 처리해야 함', () {
        // given
        final jsonWithoutWorkType = {
          'store_id': 456,
          'store_name': '롯데마트 잠실점',
          'store_code': 'LOTTE002',
          'work_category': '신규개척',
          'address': '서울시 송파구 올림픽로 456',
          'is_registered': false,
          'registered_work_type': null,
        };

        // when
        final model = StoreScheduleItemModel.fromJson(jsonWithoutWorkType);

        // then
        expect(model.storeId, 456);
        expect(model.storeName, '롯데마트 잠실점');
        expect(model.storeCode, 'LOTTE002');
        expect(model.workCategory, '신규개척');
        expect(model.address, '서울시 송파구 올림픽로 456');
        expect(model.isRegistered, false);
        expect(model.registeredWorkType, isNull);
      });
    });

    group('toJson', () {
      test('snake_case JSON으로 올바르게 직렬화해야 함', () {
        // when
        final json = testModel.toJson();

        // then
        expect(json['store_id'], 123);
        expect(json['store_name'], '이마트 강남점');
        expect(json['store_code'], 'EMART001');
        expect(json['work_category'], '정기방문');
        expect(json['address'], '서울시 강남구 테헤란로 123');
        expect(json['is_registered'], true);
        expect(json['registered_work_type'], '영업');
      });

      test('registered_work_type이 null인 경우도 올바르게 직렬화해야 함', () {
        // given
        const modelWithoutWorkType = StoreScheduleItemModel(
          storeId: 456,
          storeName: '롯데마트 잠실점',
          storeCode: 'LOTTE002',
          workCategory: '신규개척',
          address: '서울시 송파구 올림픽로 456',
          isRegistered: false,
        );

        // when
        final json = modelWithoutWorkType.toJson();

        // then
        expect(json['registered_work_type'], isNull);
      });
    });

    group('toEntity', () {
      test('StoreScheduleItem 엔티티로 올바르게 변환해야 함', () {
        // when
        final entity = testModel.toEntity();

        // then
        expect(entity.storeId, testModel.storeId);
        expect(entity.storeName, testModel.storeName);
        expect(entity.storeCode, testModel.storeCode);
        expect(entity.workCategory, testModel.workCategory);
        expect(entity.address, testModel.address);
        expect(entity.isRegistered, testModel.isRegistered);
        expect(entity.registeredWorkType, testModel.registeredWorkType);
      });

      test('toEntity 결과는 원본 엔티티와 동일해야 함', () {
        // when
        final entity = testModel.toEntity();

        // then
        expect(entity, testEntity);
      });
    });

    group('fromEntity', () {
      test('StoreScheduleItem 엔티티에서 올바르게 생성해야 함', () {
        // when
        final model = StoreScheduleItemModel.fromEntity(testEntity);

        // then
        expect(model.storeId, testEntity.storeId);
        expect(model.storeName, testEntity.storeName);
        expect(model.storeCode, testEntity.storeCode);
        expect(model.workCategory, testEntity.workCategory);
        expect(model.address, testEntity.address);
        expect(model.isRegistered, testEntity.isRegistered);
        expect(model.registeredWorkType, testEntity.registeredWorkType);
      });

      test('fromEntity 결과는 원본 모델과 동일해야 함', () {
        // when
        final model = StoreScheduleItemModel.fromEntity(testEntity);

        // then
        expect(model, testModel);
      });
    });

    group('roundtrip', () {
      test('entity -> model -> entity 변환이 데이터를 보존해야 함', () {
        // when
        final model = StoreScheduleItemModel.fromEntity(testEntity);
        final convertedEntity = model.toEntity();

        // then
        expect(convertedEntity, testEntity);
      });

      test('model -> json -> model 변환이 데이터를 보존해야 함', () {
        // when
        final json = testModel.toJson();
        final convertedModel = StoreScheduleItemModel.fromJson(json);

        // then
        expect(convertedModel, testModel);
      });

      test('json -> model -> json 변환이 데이터를 보존해야 함', () {
        // when
        final model = StoreScheduleItemModel.fromJson(testJson);
        final convertedJson = model.toJson();

        // then
        expect(convertedJson, testJson);
      });
    });

    group('equality', () {
      test('동일한 값을 가진 두 모델은 equal해야 함', () {
        // given
        const model1 = StoreScheduleItemModel(
          storeId: 123,
          storeName: '이마트 강남점',
          storeCode: 'EMART001',
          workCategory: '정기방문',
          address: '서울시 강남구 테헤란로 123',
          isRegistered: true,
          registeredWorkType: '영업',
        );

        const model2 = StoreScheduleItemModel(
          storeId: 123,
          storeName: '이마트 강남점',
          storeCode: 'EMART001',
          workCategory: '정기방문',
          address: '서울시 강남구 테헤란로 123',
          isRegistered: true,
          registeredWorkType: '영업',
        );

        // then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 두 모델은 equal하지 않아야 함', () {
        // given
        const model1 = StoreScheduleItemModel(
          storeId: 123,
          storeName: '이마트 강남점',
          storeCode: 'EMART001',
          workCategory: '정기방문',
          address: '서울시 강남구 테헤란로 123',
          isRegistered: true,
          registeredWorkType: '영업',
        );

        const model2 = StoreScheduleItemModel(
          storeId: 456,
          storeName: '롯데마트 잠실점',
          storeCode: 'LOTTE002',
          workCategory: '신규개척',
          address: '서울시 송파구 올림픽로 456',
          isRegistered: false,
        );

        // then
        expect(model1, isNot(model2));
        expect(model1.hashCode, isNot(model2.hashCode));
      });

      test('registeredWorkType이 다르면 equal하지 않아야 함', () {
        // given
        const model1 = StoreScheduleItemModel(
          storeId: 123,
          storeName: '이마트 강남점',
          storeCode: 'EMART001',
          workCategory: '정기방문',
          address: '서울시 강남구 테헤란로 123',
          isRegistered: true,
          registeredWorkType: '영업',
        );

        const model2 = StoreScheduleItemModel(
          storeId: 123,
          storeName: '이마트 강남점',
          storeCode: 'EMART001',
          workCategory: '정기방문',
          address: '서울시 강남구 테헤란로 123',
          isRegistered: true,
          registeredWorkType: '배송',
        );

        // then
        expect(model1, isNot(model2));
      });

      test('null registeredWorkType을 가진 모델들은 equal해야 함', () {
        // given
        const model1 = StoreScheduleItemModel(
          storeId: 123,
          storeName: '이마트 강남점',
          storeCode: 'EMART001',
          workCategory: '정기방문',
          address: '서울시 강남구 테헤란로 123',
          isRegistered: false,
        );

        const model2 = StoreScheduleItemModel(
          storeId: 123,
          storeName: '이마트 강남점',
          storeCode: 'EMART001',
          workCategory: '정기방문',
          address: '서울시 강남구 테헤란로 123',
          isRegistered: false,
        );

        // then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });
    });

    group('toString', () {
      test('toString은 모든 필드를 포함해야 함', () {
        // when
        final string = testModel.toString();

        // then
        expect(string, contains('StoreScheduleItemModel'));
        expect(string, contains('storeId: 123'));
        expect(string, contains('storeName: 이마트 강남점'));
        expect(string, contains('storeCode: EMART001'));
        expect(string, contains('workCategory: 정기방문'));
        expect(string, contains('address: 서울시 강남구 테헤란로 123'));
        expect(string, contains('isRegistered: true'));
        expect(string, contains('registeredWorkType: 영업'));
      });
    });
  });
}
