import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/store_schedule_item.dart';

void main() {
  group('StoreScheduleItem', () {
    test('StoreScheduleItem이 올바르게 생성된다', () {
      // Arrange & Act
      const item = StoreScheduleItem(
        storeId: 1,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: true,
        registeredWorkType: '방문',
      );

      // Assert
      expect(item.storeId, 1);
      expect(item.storeName, '테스트 거래처');
      expect(item.storeCode, 'ST001');
      expect(item.workCategory, '정기방문');
      expect(item.address, '서울시 강남구');
      expect(item.isRegistered, true);
      expect(item.registeredWorkType, '방문');
    });

    test('registeredWorkType이 null일 수 있다', () {
      // Arrange & Act
      const item = StoreScheduleItem(
        storeId: 1,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: false,
      );

      // Assert
      expect(item.registeredWorkType, isNull);
    });

    test('copyWith는 일부 필드만 변경할 수 있다', () {
      // Arrange
      const original = StoreScheduleItem(
        storeId: 1,
        storeName: '원본 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: false,
      );

      // Act
      final copied = original.copyWith(
        storeName: '변경된 거래처',
        isRegistered: true,
        registeredWorkType: '방문',
      );

      // Assert
      expect(copied.storeId, 1);
      expect(copied.storeName, '변경된 거래처');
      expect(copied.storeCode, 'ST001');
      expect(copied.workCategory, '정기방문');
      expect(copied.address, '서울시 강남구');
      expect(copied.isRegistered, true);
      expect(copied.registeredWorkType, '방문');
    });

    test('copyWith는 원본 객체를 변경하지 않는다 (불변성)', () {
      // Arrange
      const original = StoreScheduleItem(
        storeId: 1,
        storeName: '원본 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: false,
      );

      // Act
      final copied = original.copyWith(storeName: '변경된 거래처');

      // Assert
      expect(original.storeName, '원본 거래처');
      expect(copied.storeName, '변경된 거래처');
    });

    test('toJson과 fromJson은 양방향 변환이 가능하다', () {
      // Arrange
      const original = StoreScheduleItem(
        storeId: 1,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: true,
        registeredWorkType: '방문',
      );

      // Act
      final json = original.toJson();
      final restored = StoreScheduleItem.fromJson(json);

      // Assert
      expect(restored, original);
      expect(json['storeId'], 1);
      expect(json['storeName'], '테스트 거래처');
      expect(json['storeCode'], 'ST001');
      expect(json['workCategory'], '정기방문');
      expect(json['address'], '서울시 강남구');
      expect(json['isRegistered'], true);
      expect(json['registeredWorkType'], '방문');
    });

    test('fromJson은 null registeredWorkType을 처리한다', () {
      // Arrange
      final json = {
        'storeId': 1,
        'storeName': '테스트 거래처',
        'storeCode': 'ST001',
        'workCategory': '정기방문',
        'address': '서울시 강남구',
        'isRegistered': false,
        'registeredWorkType': null,
      };

      // Act
      final item = StoreScheduleItem.fromJson(json);

      // Assert
      expect(item.registeredWorkType, isNull);
    });

    test('같은 값을 가진 객체는 동일하다', () {
      // Arrange
      const item1 = StoreScheduleItem(
        storeId: 1,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: true,
        registeredWorkType: '방문',
      );
      const item2 = StoreScheduleItem(
        storeId: 1,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: true,
        registeredWorkType: '방문',
      );

      // Act & Assert
      expect(item1, item2);
    });

    test('다른 값을 가진 객체는 동일하지 않다', () {
      // Arrange
      const item1 = StoreScheduleItem(
        storeId: 1,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: true,
        registeredWorkType: '방문',
      );
      const item2 = StoreScheduleItem(
        storeId: 2,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: true,
        registeredWorkType: '방문',
      );

      // Act & Assert
      expect(item1, isNot(item2));
    });

    test('같은 객체는 같은 hashCode를 가진다', () {
      // Arrange
      const item1 = StoreScheduleItem(
        storeId: 1,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: true,
        registeredWorkType: '방문',
      );
      const item2 = StoreScheduleItem(
        storeId: 1,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: true,
        registeredWorkType: '방문',
      );

      // Act & Assert
      expect(item1.hashCode, item2.hashCode);
    });

    test('toString은 관련 정보를 포함한다', () {
      // Arrange
      const item = StoreScheduleItem(
        storeId: 1,
        storeName: '테스트 거래처',
        storeCode: 'ST001',
        workCategory: '정기방문',
        address: '서울시 강남구',
        isRegistered: true,
        registeredWorkType: '방문',
      );

      // Act
      final result = item.toString();

      // Assert
      expect(result, contains('StoreScheduleItem'));
      expect(result, contains('storeId: 1'));
      expect(result, contains('storeName: 테스트 거래처'));
      expect(result, contains('storeCode: ST001'));
      expect(result, contains('workCategory: 정기방문'));
      expect(result, contains('address: 서울시 강남구'));
      expect(result, contains('isRegistered: true'));
      expect(result, contains('registeredWorkType: 방문'));
    });
  });
}
