import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/my_store.dart';

void main() {
  group('MyStore', () {
    const store = MyStore(
      storeId: 1025172,
      storeName: '(유)경산식품',
      storeCode: '1025172',
      address: '전라남도 목포시 임암로20번길 6 (상동)',
      representativeName: '김정자',
      phoneNumber: '061-123-4567',
    );

    group('생성', () {
      test('필수 필드로 올바르게 생성된다', () {
        expect(store.storeId, 1025172);
        expect(store.storeName, '(유)경산식품');
        expect(store.storeCode, '1025172');
        expect(store.address, '전라남도 목포시 임암로20번길 6 (상동)');
        expect(store.representativeName, '김정자');
        expect(store.phoneNumber, '061-123-4567');
      });

      test('전화번호 없이 생성할 수 있다', () {
        const storeWithoutPhone = MyStore(
          storeId: 1,
          storeName: '테스트마트',
          storeCode: '0000001',
          address: '서울시 강남구',
          representativeName: '홍길동',
        );

        expect(storeWithoutPhone.phoneNumber, isNull);
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final copied = store.copyWith(storeName: '변경된 이름');

        expect(copied.storeName, '변경된 이름');
        expect(copied.storeId, store.storeId);
        expect(copied.storeCode, store.storeCode);
        expect(copied.address, store.address);
        expect(copied.representativeName, store.representativeName);
        expect(copied.phoneNumber, store.phoneNumber);
      });

      test('모든 필드를 변경하여 복사할 수 있다', () {
        final copied = store.copyWith(
          storeId: 999,
          storeName: '새 거래처',
          storeCode: '999',
          address: '새 주소',
          representativeName: '새 대표자',
          phoneNumber: '010-1234-5678',
        );

        expect(copied.storeId, 999);
        expect(copied.storeName, '새 거래처');
        expect(copied.storeCode, '999');
        expect(copied.address, '새 주소');
        expect(copied.representativeName, '새 대표자');
        expect(copied.phoneNumber, '010-1234-5678');
      });

      test('원본 객체는 변경되지 않는다', () {
        store.copyWith(storeName: '변경');

        expect(store.storeName, '(유)경산식품');
      });
    });

    group('직렬화', () {
      test('toJson이 올바르게 동작한다', () {
        final json = store.toJson();

        expect(json['storeId'], 1025172);
        expect(json['storeName'], '(유)경산식품');
        expect(json['storeCode'], '1025172');
        expect(json['address'], '전라남도 목포시 임암로20번길 6 (상동)');
        expect(json['representativeName'], '김정자');
        expect(json['phoneNumber'], '061-123-4567');
      });

      test('fromJson이 올바르게 동작한다', () {
        final json = {
          'storeId': 1025172,
          'storeName': '(유)경산식품',
          'storeCode': '1025172',
          'address': '전라남도 목포시 임암로20번길 6 (상동)',
          'representativeName': '김정자',
          'phoneNumber': '061-123-4567',
        };

        final restored = MyStore.fromJson(json);

        expect(restored, store);
      });

      test('toJson과 fromJson 라운드트립이 동작한다', () {
        final json = store.toJson();
        final restored = MyStore.fromJson(json);

        expect(restored, store);
      });

      test('phoneNumber가 null인 경우 직렬화/역직렬화가 동작한다', () {
        const storeWithoutPhone = MyStore(
          storeId: 1,
          storeName: '테스트',
          storeCode: '1',
          address: '주소',
          representativeName: '대표자',
        );

        final json = storeWithoutPhone.toJson();
        expect(json['phoneNumber'], isNull);

        final restored = MyStore.fromJson(json);
        expect(restored.phoneNumber, isNull);
        expect(restored, storeWithoutPhone);
      });
    });

    group('동등성', () {
      test('같은 값을 가진 두 인스턴스는 동일하다', () {
        const store2 = MyStore(
          storeId: 1025172,
          storeName: '(유)경산식품',
          storeCode: '1025172',
          address: '전라남도 목포시 임암로20번길 6 (상동)',
          representativeName: '김정자',
          phoneNumber: '061-123-4567',
        );

        expect(store, store2);
        expect(store.hashCode, store2.hashCode);
      });

      test('다른 값을 가진 두 인스턴스는 다르다', () {
        const store2 = MyStore(
          storeId: 9999999,
          storeName: '다른 거래처',
          storeCode: '9999999',
          address: '다른 주소',
          representativeName: '다른 대표자',
        );

        expect(store, isNot(store2));
      });

      test('동일 참조는 동일하다', () {
        expect(store, store);
      });
    });

    group('toString', () {
      test('문자열 표현이 올바르다', () {
        final str = store.toString();

        expect(str, contains('MyStore'));
        expect(str, contains('1025172'));
        expect(str, contains('(유)경산식품'));
      });
    });
  });
}
