import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/schedule_store_detail.dart';

void main() {
  group('ScheduleStoreDetail', () {
    test('엔티티가 올바르게 생성된다', () {
      final storeDetail = ScheduleStoreDetail(
        storeId: 1,
        storeName: '(주)이마트트레이더스명지점',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      );

      expect(storeDetail.storeId, 1);
      expect(storeDetail.storeName, '(주)이마트트레이더스명지점');
      expect(storeDetail.workType1, '진열');
      expect(storeDetail.workType2, '전담');
      expect(storeDetail.workType3, '순회');
      expect(storeDetail.isRegistered, false);
    });

    test('copyWith가 올바르게 동작한다', () {
      final original = ScheduleStoreDetail(
        storeId: 1,
        storeName: '이마트',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      );

      final copied = original.copyWith(
        isRegistered: true,
        storeName: '롯데마트',
      );

      expect(copied.storeId, original.storeId);
      expect(copied.storeName, '롯데마트');
      expect(copied.isRegistered, true);
      expect(original.isRegistered, false); // 원본은 변경되지 않음
    });

    test('toJson과 fromJson이 정확히 동작한다', () {
      final original = ScheduleStoreDetail(
        storeId: 1,
        storeName: '(주)이마트트레이더스명지점',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      );

      final json = original.toJson();
      final restored = ScheduleStoreDetail.fromJson(json);

      expect(restored, original);
    });

    test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
      final store1 = ScheduleStoreDetail(
        storeId: 1,
        storeName: '이마트',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      );

      final store2 = ScheduleStoreDetail(
        storeId: 1,
        storeName: '이마트',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      );

      expect(store1, store2);
      expect(store1.hashCode, store2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      final store1 = ScheduleStoreDetail(
        storeId: 1,
        storeName: '이마트',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      );

      final store2 = ScheduleStoreDetail(
        storeId: 2,
        storeName: '롯데마트',
        workType1: '진열',
        workType2: '전담',
        workType3: '격고',
        isRegistered: true,
      );

      expect(store1, isNot(store2));
    });

    test('toString이 올바르게 동작한다', () {
      final storeDetail = ScheduleStoreDetail(
        storeId: 1,
        storeName: '이마트',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      );

      final string = storeDetail.toString();

      expect(string, contains('ScheduleStoreDetail'));
      expect(string, contains('storeId: 1'));
      expect(string, contains('이마트'));
    });
  });
}
