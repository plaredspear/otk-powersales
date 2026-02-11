import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';

void main() {
  group('InspectionCategory Enum', () {
    test('분류 이름이 올바르게 정의된다', () {
      expect(InspectionCategory.OWN.displayName, '자사');
      expect(InspectionCategory.COMPETITOR.displayName, '경쟁사');
    });

    test('toJson이 올바른 문자열을 반환한다', () {
      expect(InspectionCategory.OWN.toJson(), 'OWN');
      expect(InspectionCategory.COMPETITOR.toJson(), 'COMPETITOR');
    });

    test('fromJson이 올바른 enum을 반환한다', () {
      expect(
        InspectionCategoryExtension.fromJson('OWN'),
        InspectionCategory.OWN,
      );
      expect(
        InspectionCategoryExtension.fromJson('COMPETITOR'),
        InspectionCategory.COMPETITOR,
      );
    });

    test('fromJson이 잘못된 값에 대해 예외를 던진다', () {
      expect(
        () => InspectionCategoryExtension.fromJson('INVALID'),
        throwsArgumentError,
      );
    });
  });

  group('InspectionListItem Entity', () {
    group('생성 테스트', () {
      test('InspectionListItem 엔티티가 올바르게 생성된다', () {
        final item = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        expect(item.id, 1);
        expect(item.category, InspectionCategory.OWN);
        expect(item.storeName, '롯데마트 사상');
        expect(item.storeId, 2001);
        expect(item.inspectionDate, DateTime(2020, 8, 18));
        expect(item.fieldType, '시식');
        expect(item.fieldTypeCode, 'FT02');
      });

      test('경쟁사 분류로 InspectionListItem이 생성된다', () {
        final item = InspectionListItem(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '미광종합물류',
          storeId: 1025,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        expect(item.category, InspectionCategory.COMPETITOR);
        expect(item.category.displayName, '경쟁사');
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 모든 필드 변경', () {
        final original = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        final copied = original.copyWith(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '미광종합물류',
          storeId: 1025,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        expect(copied.id, 2);
        expect(copied.category, InspectionCategory.COMPETITOR);
        expect(copied.storeName, '미광종합물류');
        expect(copied.storeId, 1025);
        expect(copied.inspectionDate, DateTime(2020, 8, 19));
        expect(copied.fieldType, '본매대');
        expect(copied.fieldTypeCode, 'FT01');
      });

      test('copyWith가 일부 필드만 변경한다', () {
        final original = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        final copied = original.copyWith(
          storeName: '이마트',
          fieldType: '행사매대',
        );

        expect(copied.id, original.id);
        expect(copied.category, original.category);
        expect(copied.storeName, '이마트');
        expect(copied.storeId, original.storeId);
        expect(copied.inspectionDate, original.inspectionDate);
        expect(copied.fieldType, '행사매대');
        expect(copied.fieldTypeCode, original.fieldTypeCode);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        final copied = original.copyWith(storeName: '이마트');

        expect(original.storeName, '롯데마트 사상');
        expect(copied.storeName, '이마트');
      });
    });

    group('직렬화 테스트', () {
      test('toJson이 올바르게 동작한다', () {
        final item = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        final json = item.toJson();

        expect(json['id'], 1);
        expect(json['category'], 'OWN');
        expect(json['storeName'], '롯데마트 사상');
        expect(json['storeId'], 2001);
        expect(json['inspectionDate'], '2020-08-18');
        expect(json['fieldType'], '시식');
        expect(json['fieldTypeCode'], 'FT02');
      });

      test('fromJson이 올바르게 동작한다', () {
        final json = {
          'id': 1,
          'category': 'OWN',
          'storeName': '롯데마트 사상',
          'storeId': 2001,
          'inspectionDate': '2020-08-18',
          'fieldType': '시식',
          'fieldTypeCode': 'FT02',
        };

        final item = InspectionListItem.fromJson(json);

        expect(item.id, 1);
        expect(item.category, InspectionCategory.OWN);
        expect(item.storeName, '롯데마트 사상');
        expect(item.storeId, 2001);
        expect(item.inspectionDate, DateTime(2020, 8, 18));
        expect(item.fieldType, '시식');
        expect(item.fieldTypeCode, 'FT02');
      });

      test('toJson과 fromJson이 정확히 왕복 변환된다', () {
        final original = InspectionListItem(
          id: 1,
          category: InspectionCategory.COMPETITOR,
          storeName: '미광종합물류',
          storeId: 1025,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        final json = original.toJson();
        final restored = InspectionListItem.fromJson(json);

        expect(restored, original);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final item1 = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        final item2 = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        expect(item1, item2);
        expect(item1.hashCode, item2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final item1 = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        final item2 = InspectionListItem(
          id: 2,
          category: InspectionCategory.COMPETITOR,
          storeName: '미광종합물류',
          storeId: 1025,
          inspectionDate: DateTime(2020, 8, 19),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );

        expect(item1, isNot(item2));
      });

      test('자기 자신과 비교하면 동일하다 (identical)', () {
        final item = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        expect(item, item);
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final item = InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          storeName: '롯데마트 사상',
          storeId: 2001,
          inspectionDate: DateTime(2020, 8, 18),
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );

        final str = item.toString();

        expect(str, contains('1'));
        expect(str, contains('OWN'));
        expect(str, contains('롯데마트 사상'));
        expect(str, contains('2001'));
        expect(str, contains('시식'));
        expect(str, contains('FT02'));
      });
    });
  });

  group('InspectionFilter', () {
    group('생성 테스트', () {
      test('InspectionFilter가 올바르게 생성된다 (전체)', () {
        final filter = InspectionFilter(
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        expect(filter.storeId, isNull);
        expect(filter.category, isNull);
        expect(filter.fromDate, DateTime(2020, 8, 1));
        expect(filter.toDate, DateTime(2020, 8, 31));
      });

      test('InspectionFilter가 올바르게 생성된다 (필터 지정)', () {
        final filter = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        expect(filter.storeId, 2001);
        expect(filter.category, InspectionCategory.OWN);
        expect(filter.fromDate, DateTime(2020, 8, 1));
        expect(filter.toDate, DateTime(2020, 8, 31));
      });

      test('defaultFilter가 오늘-7일 ~ 오늘 범위로 생성된다', () {
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        final filter = InspectionFilter.defaultFilter();

        expect(filter.fromDate, today.subtract(const Duration(days: 7)));
        expect(filter.toDate, today);
        expect(filter.storeId, isNull);
        expect(filter.category, isNull);
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 모든 필드 변경', () {
        final original = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        final copied = original.copyWith(
          storeId: 1025,
          category: InspectionCategory.COMPETITOR,
          fromDate: DateTime(2020, 9, 1),
          toDate: DateTime(2020, 9, 30),
        );

        expect(copied.storeId, 1025);
        expect(copied.category, InspectionCategory.COMPETITOR);
        expect(copied.fromDate, DateTime(2020, 9, 1));
        expect(copied.toDate, DateTime(2020, 9, 30));
      });

      test('copyWith가 일부 필드만 변경한다', () {
        final original = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        final copied = original.copyWith(
          storeId: 1025,
        );

        expect(copied.storeId, 1025);
        expect(copied.category, original.category);
        expect(copied.fromDate, original.fromDate);
        expect(copied.toDate, original.toDate);
      });

      test('copyWith로 storeId를 null로 설정할 수 있다 (clearStoreId)', () {
        final original = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        final copied = original.copyWith(clearStoreId: true);

        expect(copied.storeId, isNull);
        expect(copied.category, original.category);
      });

      test('copyWith로 category를 null로 설정할 수 있다 (clearCategory)', () {
        final original = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        final copied = original.copyWith(clearCategory: true);

        expect(copied.storeId, original.storeId);
        expect(copied.category, isNull);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        final copied = original.copyWith(storeId: 1025);

        expect(original.storeId, 2001);
        expect(copied.storeId, 1025);
      });
    });

    group('유효성 검증 테스트', () {
      test('시작일 <= 종료일이면 isValidDateRange가 true', () {
        final filter = InspectionFilter(
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        expect(filter.isValidDateRange, true);
      });

      test('시작일 == 종료일이면 isValidDateRange가 true', () {
        final filter = InspectionFilter(
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 1),
        );

        expect(filter.isValidDateRange, true);
      });

      test('시작일 > 종료일이면 isValidDateRange가 false', () {
        final filter = InspectionFilter(
          fromDate: DateTime(2020, 8, 31),
          toDate: DateTime(2020, 8, 1),
        );

        expect(filter.isValidDateRange, false);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 필터가 동일하게 비교된다', () {
        final filter1 = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        final filter2 = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        expect(filter1, filter2);
        expect(filter1.hashCode, filter2.hashCode);
      });

      test('다른 값을 가진 필터가 다르게 비교된다', () {
        final filter1 = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        final filter2 = InspectionFilter(
          storeId: 1025,
          category: InspectionCategory.COMPETITOR,
          fromDate: DateTime(2020, 9, 1),
          toDate: DateTime(2020, 9, 30),
        );

        expect(filter1, isNot(filter2));
      });

      test('자기 자신과 비교하면 동일하다 (identical)', () {
        final filter = InspectionFilter(
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        expect(filter, filter);
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final filter = InspectionFilter(
          storeId: 2001,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );

        final str = filter.toString();

        expect(str, contains('2001'));
        expect(str, contains('OWN'));
      });
    });
  });
}
