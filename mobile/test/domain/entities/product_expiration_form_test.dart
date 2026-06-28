import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product_expiration_form.dart';

void main() {
  group('ProductExpirationRegisterForm', () {
    final testExpiryDate = DateTime(2025, 3, 15);
    final testAlertDate = DateTime(2025, 3, 10);

    final testForm = ProductExpirationRegisterForm(
      accountCode: 'ACC101',
      accountName: '그린유통D',
      productCode: 'P001',
      productName: '진라면',
      expiryDate: testExpiryDate,
      alertDate: testAlertDate,
      description: '소비기한 주의',
    );

    final testJson = {
      'accountCode': 'ACC101',
      'accountName': '그린유통D',
      'productCode': 'P001',
      'productName': '진라면',
      'expiryDate': '2025-03-15',
      'alertDate': '2025-03-10',
      'description': '소비기한 주의',
    };

    group('생성', () {
      test('필수 필드로 ProductExpirationRegisterForm이 올바르게 생성된다', () {
        final form = ProductExpirationRegisterForm(
          accountCode: 'ACC101',
          accountName: '그린유통D',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.accountCode, 'ACC101');
        expect(form.accountName, '그린유통D');
        expect(form.productCode, 'P001');
        expect(form.productName, '진라면');
        expect(form.expiryDate, testExpiryDate);
        expect(form.alertDate, testAlertDate);
        expect(form.description, '');
      });

      test('description 필드와 함께 ProductExpirationRegisterForm이 올바르게 생성된다', () {
        expect(testForm.accountCode, 'ACC101');
        expect(testForm.accountName, '그린유통D');
        expect(testForm.productCode, 'P001');
        expect(testForm.productName, '진라면');
        expect(testForm.expiryDate, testExpiryDate);
        expect(testForm.alertDate, testAlertDate);
        expect(testForm.description, '소비기한 주의');
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final copied = testForm.copyWith(
          productCode: 'P002',
          description: '신규 등록',
        );

        expect(copied.accountCode, testForm.accountCode);
        expect(copied.productCode, 'P002');
        expect(copied.expiryDate, testForm.expiryDate);
        expect(copied.alertDate, testForm.alertDate);
        expect(copied.description, '신규 등록');
      });

      test('모든 필드를 변경하여 복사할 수 있다', () {
        final newExpiryDate = DateTime(2025, 4, 20);
        final newAlertDate = DateTime(2025, 4, 15);

        final copied = testForm.copyWith(
          accountCode: 'ACC102',
          accountName: '다른거래처',
          productCode: 'P002',
          productName: '케첩',
          expiryDate: newExpiryDate,
          alertDate: newAlertDate,
          description: '긴급 등록',
        );

        expect(copied.accountCode, 'ACC102');
        expect(copied.accountName, '다른거래처');
        expect(copied.productCode, 'P002');
        expect(copied.productName, '케첩');
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

        expect(result['accountCode'], 'ACC101');
        expect(result['accountName'], '그린유통D');
        expect(result['productCode'], 'P001');
        expect(result['productName'], '진라면');
        expect(result['expiryDate'], '2025-03-15');
        expect(result['alertDate'], '2025-03-10');
        expect(result['description'], '소비기한 주의');
      });

      test('날짜가 YYYY-MM-DD 형식으로 직렬화된다', () {
        final result = testForm.toJson();

        expect(result['expiryDate'], matches(r'^\d{4}-\d{2}-\d{2}$'));
        expect(result['alertDate'], matches(r'^\d{4}-\d{2}-\d{2}$'));
      });

      test('description이 빈 문자열인 경우도 직렬화된다', () {
        final form = ProductExpirationRegisterForm(
          accountCode: 'ACC101',
          accountName: '그린유통D',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        final result = form.toJson();

        expect(result['description'], '');
      });
    });

    group('isValid', () {
      test('accountCode가 비어있지 않고 productCode가 비어있지 않으면 유효하다', () {
        final form = ProductExpirationRegisterForm(
          accountCode: 'ACC001',
          accountName: '거래처명',
          productCode: 'P001',
          productName: '제품명',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.isValid, isTrue);
      });

      test('accountCode가 빈 문자열이면 유효하지 않다', () {
        final form = ProductExpirationRegisterForm(
          accountCode: '',
          accountName: '거래처명',
          productCode: 'P001',
          productName: '제품명',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.isValid, isFalse);
      });

      test('productCode가 빈 문자열이면 유효하지 않다', () {
        final form = ProductExpirationRegisterForm(
          accountCode: 'ACC101',
          accountName: '거래처명',
          productCode: '',
          productName: '제품명',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.isValid, isFalse);
      });

      test('accountCode와 productCode가 모두 빈 문자열이면 유효하지 않다', () {
        final form = ProductExpirationRegisterForm(
          accountCode: '',
          accountName: '',
          productCode: '',
          productName: '',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.isValid, isFalse);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 ProductExpirationRegisterForm은 동일하다', () {
        final form1 = ProductExpirationRegisterForm(
          accountCode: 'ACC101',
          accountName: '그린유통D',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '소비기한 주의',
        );

        final form2 = ProductExpirationRegisterForm(
          accountCode: 'ACC101',
          accountName: '그린유통D',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '소비기한 주의',
        );

        expect(form1, form2);
      });

      test('다른 값을 가진 두 ProductExpirationRegisterForm은 동일하지 않다', () {
        final other = ProductExpirationRegisterForm(
          accountCode: 'ACC102',
          accountName: '다른거래처',
          productCode: 'P002',
          productName: '케첩',
          expiryDate: DateTime(2025, 4, 20),
          alertDate: DateTime(2025, 4, 15),
          description: '긴급',
        );

        expect(testForm, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 ProductExpirationRegisterForm은 같은 hashCode를 가진다', () {
        final form1 = ProductExpirationRegisterForm(
          accountCode: 'ACC101',
          accountName: '그린유통D',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '소비기한 주의',
        );

        final form2 = ProductExpirationRegisterForm(
          accountCode: 'ACC101',
          accountName: '그린유통D',
          productCode: 'P001',
          productName: '진라면',
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '소비기한 주의',
        );

        expect(form1.hashCode, form2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환한다', () {
        final result = testForm.toString();

        expect(result, contains('ProductExpirationRegisterForm'));
        expect(result, contains('accountCode: ACC101'));
        expect(result, contains('productCode: P001'));
        expect(result, contains('description: 소비기한 주의'));
      });
    });
  });

  group('ProductExpirationUpdateForm', () {
    final testExpiryDate = DateTime(2025, 3, 15);
    final testAlertDate = DateTime(2025, 3, 10);

    final testForm = ProductExpirationUpdateForm(
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
      test('필수 필드로 ProductExpirationUpdateForm이 올바르게 생성된다', () {
        final form = ProductExpirationUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        expect(form.expiryDate, testExpiryDate);
        expect(form.alertDate, testAlertDate);
        expect(form.description, '');
      });

      test('description 필드와 함께 ProductExpirationUpdateForm이 올바르게 생성된다', () {
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
        final form = ProductExpirationUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
        );

        final result = form.toJson();

        expect(result['description'], '');
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 ProductExpirationUpdateForm은 동일하다', () {
        final form1 = ProductExpirationUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '수정된 설명',
        );

        final form2 = ProductExpirationUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '수정된 설명',
        );

        expect(form1, form2);
      });

      test('다른 값을 가진 두 ProductExpirationUpdateForm은 동일하지 않다', () {
        final other = ProductExpirationUpdateForm(
          expiryDate: DateTime(2025, 4, 20),
          alertDate: DateTime(2025, 4, 15),
          description: '다른 설명',
        );

        expect(testForm, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 ProductExpirationUpdateForm은 같은 hashCode를 가진다', () {
        final form1 = ProductExpirationUpdateForm(
          expiryDate: testExpiryDate,
          alertDate: testAlertDate,
          description: '수정된 설명',
        );

        final form2 = ProductExpirationUpdateForm(
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

        expect(result, contains('ProductExpirationUpdateForm'));
        expect(result, contains('description: 수정된 설명'));
      });
    });
  });
}
