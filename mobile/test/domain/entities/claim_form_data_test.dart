import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_category.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/domain/entities/claim_form_data.dart';

void main() {
  group('ClaimFormData Entity', () {
    final testCategories = [
      const ClaimCategory(
        id: 1,
        name: '이물',
        subcategories: [
          ClaimSubcategory(id: 101, name: '벌레'),
          ClaimSubcategory(id: 102, name: '금속'),
        ],
      ),
      const ClaimCategory(
        id: 2,
        name: '변질/변패',
        subcategories: [
          ClaimSubcategory(id: 201, name: '맛 변질'),
        ],
      ),
    ];

    const testPurchaseMethods = [
      PurchaseMethod(code: 'PM01', name: '대형마트'),
      PurchaseMethod(code: 'PM02', name: '편의점'),
      PurchaseMethod(code: 'PM03', name: '온라인'),
    ];

    const testRequestTypes = [
      ClaimRequestType(code: 'RT01', name: '교환'),
      ClaimRequestType(code: 'RT02', name: '환불'),
      ClaimRequestType(code: 'RT03', name: '원인 규명'),
    ];

    test('엔티티가 올바르게 생성된다', () {
      // Given & When
      final formData = ClaimFormData(
        categories: testCategories,
        purchaseMethods: testPurchaseMethods,
        requestTypes: testRequestTypes,
      );

      // Then
      expect(formData.categories.length, 2);
      expect(formData.purchaseMethods.length, 3);
      expect(formData.requestTypes.length, 3);
      expect(formData.categories[0].name, '이물');
      expect(formData.purchaseMethods[0].name, '대형마트');
      expect(formData.requestTypes[0].name, '교환');
    });

    test('toJson이 올바르게 동작한다 (중첩 구조 포함)', () {
      // Given
      final formData = ClaimFormData(
        categories: testCategories,
        purchaseMethods: testPurchaseMethods,
        requestTypes: testRequestTypes,
      );

      // When
      final json = formData.toJson();

      // Then
      expect(json['categories'], isA<List>());
      expect(json['categories'].length, 2);
      expect(json['categories'][0]['name'], '이물');
      expect(json['categories'][0]['subcategories'].length, 2);

      expect(json['purchaseMethods'], isA<List>());
      expect(json['purchaseMethods'].length, 3);
      expect(json['purchaseMethods'][0]['code'], 'PM01');

      expect(json['requestTypes'], isA<List>());
      expect(json['requestTypes'].length, 3);
      expect(json['requestTypes'][0]['code'], 'RT01');
    });

    test('fromJson이 올바르게 동작한다 (중첩 구조 포함)', () {
      // Given
      final json = {
        'categories': [
          {
            'id': 1,
            'name': '이물',
            'subcategories': [
              {'id': 101, 'name': '벌레'},
              {'id': 102, 'name': '금속'},
            ],
          },
        ],
        'purchaseMethods': [
          {'code': 'PM01', 'name': '대형마트'},
          {'code': 'PM02', 'name': '편의점'},
        ],
        'requestTypes': [
          {'code': 'RT01', 'name': '교환'},
        ],
      };

      // When
      final formData = ClaimFormData.fromJson(json);

      // Then
      expect(formData.categories.length, 1);
      expect(formData.categories[0].name, '이물');
      expect(formData.categories[0].subcategories.length, 2);
      expect(formData.categories[0].subcategories[0].name, '벌레');

      expect(formData.purchaseMethods.length, 2);
      expect(formData.purchaseMethods[0].code, 'PM01');

      expect(formData.requestTypes.length, 1);
      expect(formData.requestTypes[0].name, '교환');
    });

    test('toJson과 fromJson이 정확히 왕복 변환된다', () {
      // Given
      final original = ClaimFormData(
        categories: testCategories,
        purchaseMethods: testPurchaseMethods,
        requestTypes: testRequestTypes,
      );

      // When
      final json = original.toJson();
      final restored = ClaimFormData.fromJson(json);

      // Then
      expect(restored, original);
    });

    test('copyWith가 올바르게 동작한다', () {
      // Given
      final original = ClaimFormData(
        categories: testCategories,
        purchaseMethods: testPurchaseMethods,
        requestTypes: testRequestTypes,
      );

      final newPurchaseMethods = [
        const PurchaseMethod(code: 'PM99', name: '기타'),
      ];

      // When
      final copied = original.copyWith(purchaseMethods: newPurchaseMethods);

      // Then
      expect(copied.categories, testCategories);
      expect(copied.purchaseMethods.length, 1);
      expect(copied.purchaseMethods[0].code, 'PM99');
      expect(copied.requestTypes, testRequestTypes);
      expect(original.purchaseMethods.length, 3); // 원본 불변성 확인
    });

    test('copyWith로 categories를 변경할 수 있다', () {
      // Given
      final original = ClaimFormData(
        categories: testCategories,
        purchaseMethods: testPurchaseMethods,
        requestTypes: testRequestTypes,
      );

      final newCategories = [
        const ClaimCategory(
          id: 3,
          name: '기타',
          subcategories: [],
        ),
      ];

      // When
      final copied = original.copyWith(categories: newCategories);

      // Then
      expect(copied.categories.length, 1);
      expect(copied.categories[0].name, '기타');
      expect(original.categories.length, 2); // 원본 불변성 확인
    });

    test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
      // Given
      final data1 = ClaimFormData(
        categories: testCategories,
        purchaseMethods: testPurchaseMethods,
        requestTypes: testRequestTypes,
      );
      final data2 = ClaimFormData(
        categories: testCategories,
        purchaseMethods: testPurchaseMethods,
        requestTypes: testRequestTypes,
      );

      // Then
      expect(data1, data2);
      expect(data1.hashCode, data2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      // Given
      final data1 = ClaimFormData(
        categories: testCategories,
        purchaseMethods: testPurchaseMethods,
        requestTypes: testRequestTypes,
      );
      final data2 = ClaimFormData(
        categories: testCategories,
        purchaseMethods: const [
          PurchaseMethod(code: 'PM99', name: '기타'),
        ],
        requestTypes: testRequestTypes,
      );

      // Then
      expect(data1, isNot(data2));
    });

    test('빈 목록으로 엔티티를 생성할 수 있다', () {
      // Given & When
      const formData = ClaimFormData(
        categories: [],
        purchaseMethods: [],
        requestTypes: [],
      );

      // Then
      expect(formData.categories, isEmpty);
      expect(formData.purchaseMethods, isEmpty);
      expect(formData.requestTypes, isEmpty);
    });

    test('toString이 올바른 형식으로 출력된다', () {
      // Given
      final formData = ClaimFormData(
        categories: testCategories,
        purchaseMethods: testPurchaseMethods,
        requestTypes: testRequestTypes,
      );

      // When
      final str = formData.toString();

      // Then
      expect(str, contains('ClaimFormData'));
      expect(str, contains('categories: 2 items'));
      expect(str, contains('purchaseMethods: 3 items'));
      expect(str, contains('requestTypes: 3 items'));
    });
  });
}
