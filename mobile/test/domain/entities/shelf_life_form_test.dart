import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_form.dart';

void main() {
  group('ShelfLifeRegisterForm', () {
    final testExpiryDate = DateTime(2025, 3, 15);
    final testAlertDate = DateTime(2025, 3, 10);

    final testForm = ShelfLifeRegisterForm(
      storeId: 101,
      productCode: 'P001',
      expiryDate: testExpiryDate,
      alertDate: testAlertDate,
      description: '유통기한 주의',
    );

    final testJson = {
      'storeId': 101,
      'productCode': 'P001',
      'expiryDate': '2025-03-15',
      'alertDate': '2025-03-10',
      'description': '유통기한 주의',
    };

    group('생성', () {
      test('필수 필드로 ShelfLifeRegisterForm이 올바르게 생성된다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 101,
          productCode: 'P001',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.storeId, 101);
        expect(form.productCode, 'P001');
        expect(form.expiryDate, testExpiryDate);
        expect(form.alertDate, testAlertDate);
        expect(form.description, '');
      });

      test('description 필드와 함께 ShelfLifeRegisterForm이 올바르게 생성된다', () {
        expect(testForm.storeId, 101);
        expect(testForm.productCode, 'P001');
        expect(testForm.expiryDate, testExpiryDate);
        expect(testForm.alertDate, testAlertDate);
        expect(testForm.description, '유통기한 주의');
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final copied = testForm.copyWith(
          productCode: 'P002',
          description: '신규 등록',
        );

        expect(copied.storeId, testForm.storeId);
        expect(copied.productCode, 'P002');
        expect(copied.expiryDate, testForm.expiryDate);
        expect(copied.alertDate, testForm.alertDate);
        expect(copied.description, '신규 등록');
      });

      test('모든 필드를 변경하여 복사할 수 있다', () {
        final newExpiryDate = DateTime(2025, 4, 20);
        final newAlertDate = DateTime(2025, 4, 15);

        final copied = testForm.copyWith(
          storeId: 102,
          productCode: 'P002',
          expiryDate: newExpiryDate,
          alertDate: newAlertDate,
          description: '긴급 등록',
        );

        expect(copied.storeId, 102);
        expect(copied.productCode, 'P002');
        expect(copied.expiryDate, newExpiryDate);
        expect(copied.alertDate, newAlertDate);
        expect(copied.description, '긴급 등록');
      });

      test('아무 필드도 변경하지 않으면 동일한 값의 새 인스턴스를 반환한다', () {
        final copied = testForm.copyWith();

        expect(copied, testForm);
        expect(identical(copied, testForm), isFalse);
      });
    });

    group('toJson', () {
      test('올바른 JSON Map을 반환한다', () {
        final result = testForm.toJson();

        expect(result['storeId'], 101);
        expect(result['productCode'], 'P001');
        expect(result['expiryDate'], '2025-03-15');
        expect(result['alertDate'], '2025-03-10');
        expect(result['description'], '유통기한 주의');
      });

      test('날짜가 YYYY-MM-DD 형식으로 직렬화된다', () {
        final result = testForm.toJson();

        expect(result['expiryDate'], matches(r'^\d{4}-\d{2}-\d{2}$'));
        expect(result['alertDate'], matches(r'^\d{4}-\d{2}-\d{2}$'));
      });

      test('description이 빈 문자열인 경우도 직렬화된다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 101,
          productCode: 'P001',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        final result = form.toJson();

        expect(result['description'], '');
      });
    });

    group('isValid', () {
      test('storeId > 0이고 productCode가 비어있지 않으면 유효하다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 1,
          productCode: 'P001',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.isValid, isTrue);
      });

      test('storeId가 0이면 유효하지 않다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 0,
          productCode: 'P001',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.isValid, isFalse);
      });

      test('storeId가 음수이면 유효하지 않다', () {
        final form = ShelfLifeRegisterForm(
          storeId: -1,
          productCode: 'P001',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.isValid, isFalse);
      });

      test('productCode가 빈 문자열이면 유효하지 않다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 101,
          productCode: '',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.isValid, isFalse);
      });

      test('storeId가 0이고 productCode가 비어있으면 유효하지 않다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 0,
          productCode: '',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.isValid, isFalse);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 ShelfLifeRegisterForm은 동일하다', () {
        final form1 = ShelfLifeRegisterForm(
          storeId: 101,
          productCode: 'P001',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '유통기한 주의',
        );

        final form2 = ShelfLifeRegisterForm(
          storeId: 101,
          productCode: 'P001',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '유통기한 주의',
        );

        expect(form1, form2);
      });

      test('다른 값을 가진 두 ShelfLifeRegisterForm은 동일하지 않다', () {
        final other = ShelfLifeRegisterForm(
          storeId: 102,
          productCode: 'P002',
          expiryDate: DateTime(2025, 4, 20),
          alertDate: DateTime(2025, 4, 15),
          description: '긴급',
        );

        expect(testForm, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 ShelfLifeRegisterForm은 같은 hashCode를 가진다', () {
        final form1 = ShelfLifeRegisterForm(
          storeId: 101,
          productCode: 'P001',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '유통기한 주의',
        );

        final form2 = ShelfLifeRegisterForm(
          storeId: 101,
          productCode: 'P001',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '유통기한 주의',
        );

        expect(form1.hashCode, form2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환한다', () {
        final result = testForm.toString();

        expect(result, contains('ShelfLifeRegisterForm'));
        expect(result, contains('storeId: 101'));
        expect(result, contains('productCode: P001'));
        expect(result, contains('description: 유통기한 주의'));
      });
    });
  });

  group('ShelfLifeUpdateForm', () {
    final testExpiryDate = DateTime(2025, 3, 15);
    final testAlertDate = DateTime(2025, 3, 10);

    final testForm = ShelfLifeUpdateForm(
      expiryDate: testExpiryDate,
      alertDate: testAlertDate,
      description: '수정된 설명',
    );

    final testJson = {
      'expiryDate': '2025-03-15',
      'alertDate': '2025-03-10',
      'description': '수정된 설명',
    };

    group('생성', () {
      test('필수 필드로 ShelfLifeUpdateForm이 올바르게 생성된다', () {
        final form = ShelfLifeUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.expiryDate, testExpiryDate);
        expect(form.alertDate, testAlertDate);
        expect(form.description, '');
      });

      test('description 필드와 함께 ShelfLifeUpdateForm이 올바르게 생성된다', () {
        expect(testForm.expiryDate, testExpiryDate);
        expect(testForm.alertDate, testAlertDate);
        expect(testForm.description, '수정된 설명');
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final copied = testForm.copyWith(
          description: '새로운 설명',
        );

        expect(copied.expiryDate, testForm.expiryDate);
        expect(copied.alertDate, testForm.alertDate);
        expect(copied.description, '새로운 설명');
      });

      test('모든 필드를 변경하여 복사할 수 있다', () {
        final newExpiryDate = DateTime(2025, 4, 20);
        final newAlertDate = DateTime(2025, 4, 15);

        final copied = testForm.copyWith(
          expiryDate: newExpiryDate,
          alertDate: newAlertDate,
          description: '전체 수정',
        );

        expect(copied.expiryDate, newExpiryDate);
        expect(copied.alertDate, newAlertDate);
        expect(copied.description, '전체 수정');
      });

      test('아무 필드도 변경하지 않으면 동일한 값의 새 인스턴스를 반환한다', () {
        final copied = testForm.copyWith();

        expect(copied, testForm);
        expect(identical(copied, testForm), isFalse);
      });
    });

    group('toJson', () {
      test('올바른 JSON Map을 반환한다', () {
        final result = testForm.toJson();

        expect(result['expiryDate'], '2025-03-15');
        expect(result['alertDate'], '2025-03-10');
        expect(result['description'], '수정된 설명');
      });

      test('날짜가 YYYY-MM-DD 형식으로 직렬화된다', () {
        final result = testForm.toJson();

        expect(result['expiryDate'], matches(r'^\d{4}-\d{2}-\d{2}$'));
        expect(result['alertDate'], matches(r'^\d{4}-\d{2}-\d{2}$'));
      });

      test('description이 빈 문자열인 경우도 직렬화된다', () {
        final form = ShelfLifeUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        final result = form.toJson();

        expect(result['description'], '');
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 ShelfLifeUpdateForm은 동일하다', () {
        final form1 = ShelfLifeUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '수정된 설명',
        );

        final form2 = ShelfLifeUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '수정된 설명',
        );

        expect(form1, form2);
      });

      test('다른 값을 가진 두 ShelfLifeUpdateForm은 동일하지 않다', () {
        final other = ShelfLifeUpdateForm(
          expiryDate: DateTime(2025, 4, 20),
          alertDate: DateTime(2025, 4, 15),
          description: '다른 설명',
        );

        expect(testForm, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 ShelfLifeUpdateForm은 같은 hashCode를 가진다', () {
        final form1 = ShelfLifeUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '수정된 설명',
        );

        final form2 = ShelfLifeUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '수정된 설명',
        );

        expect(form1.hashCode, form2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환한다', () {
        final result = testForm.toString();

        expect(result, contains('ShelfLifeUpdateForm'));
        expect(result, contains('description: 수정된 설명'));
      });
    });
  });
}
