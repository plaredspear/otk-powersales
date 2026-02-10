import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/my_store_model.dart';
import 'package:mobile/domain/entities/my_store.dart';

void main() {
  group('MyStoreModel', () {
    const model = MyStoreModel(
      storeId: 1025172,
      storeName: '(유)경산식품',
      storeCode: '1025172',
      address: '전라남도 목포시 임암로20번길 6 (상동)',
      representativeName: '김정자',
      phoneNumber: '061-123-4567',
    );

    group('fromJson', () {
      test('snake_case JSON을 올바르게 파싱한다', () {
        final json = {
          'store_id': 1025172,
          'store_name': '(유)경산식품',
          'store_code': '1025172',
          'address': '전라남도 목포시 임암로20번길 6 (상동)',
          'representative_name': '김정자',
          'phone_number': '061-123-4567',
        };

        final parsed = MyStoreModel.fromJson(json);

        expect(parsed.storeId, 1025172);
        expect(parsed.storeName, '(유)경산식품');
        expect(parsed.storeCode, '1025172');
        expect(parsed.address, '전라남도 목포시 임암로20번길 6 (상동)');
        expect(parsed.representativeName, '김정자');
        expect(parsed.phoneNumber, '061-123-4567');
      });

      test('phone_number가 null인 JSON을 파싱한다', () {
        final json = {
          'store_id': 1,
          'store_name': '테스트',
          'store_code': '1',
          'address': '주소',
          'representative_name': '대표자',
          'phone_number': null,
        };

        final parsed = MyStoreModel.fromJson(json);

        expect(parsed.phoneNumber, isNull);
      });
    });

    group('toJson', () {
      test('snake_case JSON으로 직렬화한다', () {
        final json = model.toJson();

        expect(json['store_id'], 1025172);
        expect(json['store_name'], '(유)경산식품');
        expect(json['store_code'], '1025172');
        expect(json['address'], '전라남도 목포시 임암로20번길 6 (상동)');
        expect(json['representative_name'], '김정자');
        expect(json['phone_number'], '061-123-4567');
      });

      test('fromJson과 toJson 라운드트립이 동작한다', () {
        final json = model.toJson();
        final restored = MyStoreModel.fromJson(json);

        expect(restored, model);
      });
    });

    group('toEntity', () {
      test('MyStore 엔티티로 올바르게 변환한다', () {
        final entity = model.toEntity();

        expect(entity, isA<MyStore>());
        expect(entity.storeId, model.storeId);
        expect(entity.storeName, model.storeName);
        expect(entity.storeCode, model.storeCode);
        expect(entity.address, model.address);
        expect(entity.representativeName, model.representativeName);
        expect(entity.phoneNumber, model.phoneNumber);
      });

      test('phoneNumber null인 경우 엔티티 변환이 동작한다', () {
        const modelWithoutPhone = MyStoreModel(
          storeId: 1,
          storeName: '테스트',
          storeCode: '1',
          address: '주소',
          representativeName: '대표자',
        );

        final entity = modelWithoutPhone.toEntity();

        expect(entity.phoneNumber, isNull);
      });
    });

    group('fromEntity', () {
      test('MyStore 엔티티에서 올바르게 생성한다', () {
        const entity = MyStore(
          storeId: 1025172,
          storeName: '(유)경산식품',
          storeCode: '1025172',
          address: '전라남도 목포시 임암로20번길 6 (상동)',
          representativeName: '김정자',
          phoneNumber: '061-123-4567',
        );

        final fromEntity = MyStoreModel.fromEntity(entity);

        expect(fromEntity.storeId, entity.storeId);
        expect(fromEntity.storeName, entity.storeName);
        expect(fromEntity.storeCode, entity.storeCode);
        expect(fromEntity.address, entity.address);
        expect(fromEntity.representativeName, entity.representativeName);
        expect(fromEntity.phoneNumber, entity.phoneNumber);
      });
    });

    group('동등성', () {
      test('같은 값을 가진 두 인스턴스는 동일하다', () {
        const model2 = MyStoreModel(
          storeId: 1025172,
          storeName: '(유)경산식품',
          storeCode: '1025172',
          address: '전라남도 목포시 임암로20번길 6 (상동)',
          representativeName: '김정자',
          phoneNumber: '061-123-4567',
        );

        expect(model, model2);
        expect(model.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 두 인스턴스는 다르다', () {
        const model2 = MyStoreModel(
          storeId: 9999999,
          storeName: '다른 거래처',
          storeCode: '9999999',
          address: '다른 주소',
          representativeName: '다른 대표자',
        );

        expect(model, isNot(model2));
      });
    });

    group('toString', () {
      test('문자열 표현이 올바르다', () {
        final str = model.toString();

        expect(str, contains('MyStoreModel'));
        expect(str, contains('1025172'));
        expect(str, contains('(유)경산식품'));
      });
    });
  });
}
