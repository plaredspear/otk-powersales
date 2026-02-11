import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';

void main() {
  group('ShelfLifeItem', () {
    final testExpiryDate = DateTime(2025, 3, 15);
    final testAlertDate = DateTime(2025, 3, 10);

    final testItem = ShelfLifeItem(
      id: 1,
      productCode: 'P001',
      productName: '오뚜기 카레',
      storeName: '강남점',
      storeId: 101,
      expiryDate: testExpiryDate,
      alertDate: testAlertDate,
      dDay: 5,
      description: '유통기한 주의',
      isExpired: false,
    );

    final testJson = {
      'id': 1,
      'productCode': 'P001',
      'productName': '오뚜기 카레',
      'storeName': '강남점',
      'storeId': 101,
      'expiryDate': '2025-03-15',
      'alertDate': '2025-03-10',
      'dDay': 5,
      'description': '유통기한 주의',
      'isExpired': false,
    };

    group('생성', () {
      test('필수 필드로 ShelfLifeItem이 올바르게 생성된다', () {
        final item = ShelfLifeItem(
          id: 1,
          productCode: 'P001',
          productName: '오뚜기 카레',
          storeName: '강남점',
          storeId: 101,
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          dDay: 5,
          isExpired: false,
        );

        expect(item.id, 1);
        expect(item.productCode, 'P001');
        expect(item.productName, '오뚜기 카레');
        expect(item.storeName, '강남점');
        expect(item.storeId, 101);
        expect(item.expiryDate, testExpiryDate);
        expect(item.alertDate, testAlertDate);
        expect(item.dDay, 5);
        expect(item.description, '');
        expect(item.isExpired, false);
      });

      test('description 필드와 함께 ShelfLifeItem이 올바르게 생성된다', () {
        expect(testItem.id, 1);
        expect(testItem.productCode, 'P001');
        expect(testItem.productName, '오뚜기 카레');
        expect(testItem.storeName, '강남점');
        expect(testItem.storeId, 101);
        expect(testItem.expiryDate, testExpiryDate);
        expect(testItem.alertDate, testAlertDate);
        expect(testItem.dDay, 5);
        expect(testItem.description, '유통기한 주의');
        expect(testItem.isExpired, false);
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final copied = testItem.copyWith(
          dDay: 3,
          isExpired: true,
        );

        expect(copied.id, testItem.id);
        expect(copied.productCode, testItem.productCode);
        expect(copied.productName, testItem.productName);
        expect(copied.storeName, testItem.storeName);
        expect(copied.storeId, testItem.storeId);
        expect(copied.expiryDate, testItem.expiryDate);
        expect(copied.alertDate, testItem.alertDate);
        expect(copied.dDay, 3);
        expect(copied.description, testItem.description);
        expect(copied.isExpired, true);
      });

      test('모든 필드를 변경하여 복사할 수 있다', () {
        final newExpiryDate = DateTime(2025, 4, 20);
        final newAlertDate = DateTime(2025, 4, 15);

        final copied = testItem.copyWith(
          id: 2,
          productCode: 'P002',
          productName: '오뚜기 짜장',
          storeName: '서초점',
          storeId: 102,
          expiryDate: newExpiryDate,
          alertDate: newAlertDate,
          dDay: 10,
          description: '신규 입고',
          isExpired: true,
        );

        expect(copied.id, 2);
        expect(copied.productCode, 'P002');
        expect(copied.productName, '오뚜기 짜장');
        expect(copied.storeName, '서초점');
        expect(copied.storeId, 102);
        expect(copied.expiryDate, newExpiryDate);
        expect(copied.alertDate, newAlertDate);
        expect(copied.dDay, 10);
        expect(copied.description, '신규 입고');
        expect(copied.isExpired, true);
      });

      test('아무 필드도 변경하지 않으면 동일한 값의 새 인스턴스를 반환한다', () {
        final copied = testItem.copyWith();

        expect(copied, testItem);
        expect(identical(copied, testItem), isFalse);
      });
    });

    group('toJson', () {
      test('올바른 JSON Map을 반환한다', () {
        final result = testItem.toJson();

        expect(result['id'], 1);
        expect(result['productCode'], 'P001');
        expect(result['productName'], '오뚜기 카레');
        expect(result['storeName'], '강남점');
        expect(result['storeId'], 101);
        expect(result['expiryDate'], '2025-03-15');
        expect(result['alertDate'], '2025-03-10');
        expect(result['dDay'], 5);
        expect(result['description'], '유통기한 주의');
        expect(result['isExpired'], false);
      });

      test('날짜가 YYYY-MM-DD 형식으로 직렬화된다', () {
        final result = testItem.toJson();

        expect(result['expiryDate'], matches(r'^\d{4}-\d{2}-\d{2}$'));
        expect(result['alertDate'], matches(r'^\d{4}-\d{2}-\d{2}$'));
      });
    });

    group('fromJson', () {
      test('JSON Map에서 올바르게 생성된다', () {
        final result = ShelfLifeItem.fromJson(testJson);

        expect(result.id, 1);
        expect(result.productCode, 'P001');
        expect(result.productName, '오뚜기 카레');
        expect(result.storeName, '강남점');
        expect(result.storeId, 101);
        expect(result.expiryDate, testExpiryDate);
        expect(result.alertDate, testAlertDate);
        expect(result.dDay, 5);
        expect(result.description, '유통기한 주의');
        expect(result.isExpired, false);
      });

      test('description이 null이면 빈 문자열로 처리된다', () {
        final jsonWithoutDescription = Map<String, dynamic>.from(testJson)
          ..remove('description');

        final result = ShelfLifeItem.fromJson(jsonWithoutDescription);

        expect(result.description, '');
      });
    });

    group('round trip', () {
      test('toJson -> fromJson 변환이 일관성 있다', () {
        final json = testItem.toJson();
        final restored = ShelfLifeItem.fromJson(json);

        expect(restored, testItem);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 ShelfLifeItem은 동일하다', () {
        final item1 = ShelfLifeItem(
          id: 1,
          productCode: 'P001',
          productName: '오뚜기 카레',
          storeName: '강남점',
          storeId: 101,
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          dDay: 5,
          description: '유통기한 주의',
          isExpired: false,
        );

        final item2 = ShelfLifeItem(
          id: 1,
          productCode: 'P001',
          productName: '오뚜기 카레',
          storeName: '강남점',
          storeId: 101,
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          dDay: 5,
          description: '유통기한 주의',
          isExpired: false,
        );

        expect(item1, item2);
      });

      test('다른 값을 가진 두 ShelfLifeItem은 동일하지 않다', () {
        final other = ShelfLifeItem(
          id: 2,
          productCode: 'P002',
          productName: '오뚜기 짜장',
          storeName: '서초점',
          storeId: 102,
          expiryDate: DateTime(2025, 4, 20),
          alertDate: DateTime(2025, 4, 15),
          dDay: 10,
          description: '신규 입고',
          isExpired: true,
        );

        expect(testItem, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 ShelfLifeItem은 같은 hashCode를 가진다', () {
        final item1 = ShelfLifeItem(
          id: 1,
          productCode: 'P001',
          productName: '오뚜기 카레',
          storeName: '강남점',
          storeId: 101,
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          dDay: 5,
          description: '유통기한 주의',
          isExpired: false,
        );

        final item2 = ShelfLifeItem(
          id: 1,
          productCode: 'P001',
          productName: '오뚜기 카레',
          storeName: '강남점',
          storeId: 101,
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          dDay: 5,
          description: '유통기한 주의',
          isExpired: false,
        );

        expect(item1.hashCode, item2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환한다', () {
        final result = testItem.toString();

        expect(result, contains('ShelfLifeItem'));
        expect(result, contains('id: 1'));
        expect(result, contains('productCode: P001'));
        expect(result, contains('productName: 오뚜기 카레'));
        expect(result, contains('storeName: 강남점'));
        expect(result, contains('storeId: 101'));
        expect(result, contains('dDay: 5'));
        expect(result, contains('description: 유통기한 주의'));
        expect(result, contains('isExpired: false'));
      });
    });
  });

  group('ShelfLifeFilter', () {
    final testFromDate = DateTime(2025, 3, 1);
    final testToDate = DateTime(2025, 3, 31);

    final testFilter = ShelfLifeFilter(
      storeId: 101,
      fromDate: testFromDate,
      toDate: testToDate,
    );

    group('생성', () {
      test('ShelfLifeFilter가 올바르게 생성된다', () {
        final filter = ShelfLifeFilter(
          storeId: 101,
          fromDate: testFromDate,
          toDate: testToDate,
        );

        expect(filter.storeId, 101);
        expect(filter.fromDate, testFromDate);
        expect(filter.toDate, testToDate);
      });

      test('storeId가 null이면 전체 거래처 조회를 의미한다', () {
        final filter = ShelfLifeFilter(
          fromDate: testFromDate,
          toDate: testToDate,
        );

        expect(filter.storeId, isNull);
        expect(filter.fromDate, testFromDate);
        expect(filter.toDate, testToDate);
      });
    });

    group('defaultFilter', () {
      test('오늘 기준 앞뒤 7일의 필터를 생성한다', () {
        final filter = ShelfLifeFilter.defaultFilter();
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);

        expect(filter.storeId, isNull);
        expect(filter.fromDate, today.subtract(const Duration(days: 7)));
        expect(filter.toDate, today.add(const Duration(days: 7)));
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final newFromDate = DateTime(2025, 4, 1);
        final copied = testFilter.copyWith(fromDate: newFromDate);

        expect(copied.storeId, testFilter.storeId);
        expect(copied.fromDate, newFromDate);
        expect(copied.toDate, testFilter.toDate);
      });

      test('모든 필드를 변경하여 복사할 수 있다', () {
        final newFromDate = DateTime(2025, 4, 1);
        final newToDate = DateTime(2025, 4, 30);

        final copied = testFilter.copyWith(
          storeId: 102,
          fromDate: newFromDate,
          toDate: newToDate,
        );

        expect(copied.storeId, 102);
        expect(copied.fromDate, newFromDate);
        expect(copied.toDate, newToDate);
      });

      test('clearStoreId가 true면 storeId를 null로 설정한다', () {
        final copied = testFilter.copyWith(clearStoreId: true);

        expect(copied.storeId, isNull);
        expect(copied.fromDate, testFilter.fromDate);
        expect(copied.toDate, testFilter.toDate);
      });

      test('clearStoreId가 false면 storeId를 유지한다', () {
        final copied = testFilter.copyWith(clearStoreId: false);

        expect(copied.storeId, testFilter.storeId);
      });

      test('아무 필드도 변경하지 않으면 동일한 값의 새 인스턴스를 반환한다', () {
        final copied = testFilter.copyWith();

        expect(copied, testFilter);
        expect(identical(copied, testFilter), isFalse);
      });
    });

    group('isValidDateRange', () {
      test('fromDate와 toDate 차이가 0일이면 유효하다', () {
        final filter = ShelfLifeFilter(
          fromDate: testFromDate,
          toDate: testFromDate,
        );

        expect(filter.isValidDateRange, isTrue);
      });

      test('fromDate와 toDate 차이가 183일 이내면 유효하다', () {
        final filter = ShelfLifeFilter(
          fromDate: testFromDate,
          toDate: testFromDate.add(const Duration(days: 183)),
        );

        expect(filter.isValidDateRange, isTrue);
      });

      test('fromDate와 toDate 차이가 183일을 초과하면 유효하지 않다', () {
        final filter = ShelfLifeFilter(
          fromDate: testFromDate,
          toDate: testFromDate.add(const Duration(days: 184)),
        );

        expect(filter.isValidDateRange, isFalse);
      });

      test('toDate가 fromDate보다 이전이면 유효하지 않다', () {
        final filter = ShelfLifeFilter(
          fromDate: testFromDate,
          toDate: testFromDate.subtract(const Duration(days: 1)),
        );

        expect(filter.isValidDateRange, isFalse);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 ShelfLifeFilter는 동일하다', () {
        final filter1 = ShelfLifeFilter(
          storeId: 101,
          fromDate: testFromDate,
          toDate: testToDate,
        );

        final filter2 = ShelfLifeFilter(
          storeId: 101,
          fromDate: testFromDate,
          toDate: testToDate,
        );

        expect(filter1, filter2);
      });

      test('다른 값을 가진 두 ShelfLifeFilter는 동일하지 않다', () {
        final other = ShelfLifeFilter(
          storeId: 102,
          fromDate: testFromDate,
          toDate: testToDate,
        );

        expect(testFilter, isNot(other));
      });

      test('storeId가 null인 경우도 동일성 비교가 가능하다', () {
        final filter1 = ShelfLifeFilter(
          fromDate: testFromDate,
          toDate: testToDate,
        );

        final filter2 = ShelfLifeFilter(
          fromDate: testFromDate,
          toDate: testToDate,
        );

        expect(filter1, filter2);
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 ShelfLifeFilter는 같은 hashCode를 가진다', () {
        final filter1 = ShelfLifeFilter(
          storeId: 101,
          fromDate: testFromDate,
          toDate: testToDate,
        );

        final filter2 = ShelfLifeFilter(
          storeId: 101,
          fromDate: testFromDate,
          toDate: testToDate,
        );

        expect(filter1.hashCode, filter2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환한다', () {
        final result = testFilter.toString();

        expect(result, contains('ShelfLifeFilter'));
        expect(result, contains('storeId: 101'));
        expect(result, contains('fromDate:'));
        expect(result, contains('toDate:'));
      });

      test('storeId가 null인 경우 toString이 정상 동작한다', () {
        final filter = ShelfLifeFilter(
          fromDate: testFromDate,
          toDate: testToDate,
        );

        final result = filter.toString();

        expect(result, contains('ShelfLifeFilter'));
        expect(result, contains('storeId: null'));
      });
    });
  });
}
