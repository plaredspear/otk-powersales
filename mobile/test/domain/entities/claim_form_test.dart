import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/domain/entities/claim_form.dart';

void main() {
  group('ClaimRegisterForm Entity', () {
    final testDefectPhoto = File('test_defect.jpg');
    final testLabelPhoto = File('test_label.jpg');
    final testReceiptPhoto = File('test_receipt.jpg');

    ClaimRegisterForm createValidForm({
      int? storeId,
      String? storeName,
      String? productCode,
      String? productName,
      ClaimDateType? dateType,
      DateTime? date,
      int? categoryId,
      String? categoryName,
      int? subcategoryId,
      String? subcategoryName,
      String? defectDescription,
      int? defectQuantity,
      File? defectPhoto,
      File? labelPhoto,
      int? purchaseAmount,
      String? purchaseMethodCode,
      String? purchaseMethodName,
      File? receiptPhoto,
      String? requestTypeCode,
      String? requestTypeName,
    }) {
      return ClaimRegisterForm(
        storeId: storeId ?? 1025,
        storeName: storeName ?? '미광종합물류',
        productCode: productCode ?? '12345678',
        productName: productName ?? '맛있는부대찌개라양념140G',
        dateType: dateType ?? ClaimDateType.expiryDate,
        date: date ?? DateTime(2026, 2, 20),
        categoryId: categoryId ?? 1,
        categoryName: categoryName ?? '이물',
        subcategoryId: subcategoryId ?? 101,
        subcategoryName: subcategoryName ?? '벌레',
        defectDescription: defectDescription ?? '제품에서 벌레가 발견되었습니다',
        defectQuantity: defectQuantity ?? 1,
        defectPhoto: defectPhoto ?? testDefectPhoto,
        labelPhoto: labelPhoto ?? testLabelPhoto,
        purchaseAmount: purchaseAmount,
        purchaseMethodCode: purchaseMethodCode,
        purchaseMethodName: purchaseMethodName,
        receiptPhoto: receiptPhoto,
        requestTypeCode: requestTypeCode,
        requestTypeName: requestTypeName,
      );
    }

    group('생성 테스트', () {
      test('필수 필드로 엔티티가 올바르게 생성된다', () {
        // Given & When
        final form = createValidForm();

        // Then
        expect(form.storeId, 1025);
        expect(form.storeName, '미광종합물류');
        expect(form.productCode, '12345678');
        expect(form.productName, '맛있는부대찌개라양념140G');
        expect(form.dateType, ClaimDateType.expiryDate);
        expect(form.date, DateTime(2026, 2, 20));
        expect(form.categoryId, 1);
        expect(form.subcategoryId, 101);
        expect(form.defectDescription, '제품에서 벌레가 발견되었습니다');
        expect(form.defectQuantity, 1);
      });

      test('구매 정보를 포함한 엔티티가 생성된다', () {
        // Given & When
        final form = createValidForm(
          purchaseAmount: 5000,
          purchaseMethodCode: 'PM01',
          purchaseMethodName: '대형마트',
          receiptPhoto: testReceiptPhoto,
        );

        // Then
        expect(form.purchaseAmount, 5000);
        expect(form.purchaseMethodCode, 'PM01');
        expect(form.purchaseMethodName, '대형마트');
        expect(form.receiptPhoto, testReceiptPhoto);
      });

      test('요청사항을 포함한 엔티티가 생성된다', () {
        // Given & When
        final form = createValidForm(
          requestTypeCode: 'RT01',
          requestTypeName: '교환',
        );

        // Then
        expect(form.requestTypeCode, 'RT01');
        expect(form.requestTypeName, '교환');
      });
    });

    group('유효성 검증 테스트', () {
      test('유효한 폼은 isValid가 true를 반환한다', () {
        // Given
        final form = createValidForm();

        // Then
        expect(form.isValid, true);
        expect(form.validate(), isEmpty);
      });

      test('storeId가 0 이하면 유효하지 않다', () {
        // Given
        final form = createValidForm(storeId: 0);

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('거래처를 선택해주세요'));
      });

      test('storeName이 비어있으면 유효하지 않다', () {
        // Given
        final form = createValidForm(storeName: '');

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('거래처명이 비어있습니다'));
      });

      test('productCode가 비어있으면 유효하지 않다', () {
        // Given
        final form = createValidForm(productCode: '');

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('제품을 선택해주세요'));
      });

      test('categoryId가 0 이하면 유효하지 않다', () {
        // Given
        final form = createValidForm(categoryId: 0);

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('클레임 종류를 선택해주세요'));
      });

      test('subcategoryId가 0 이하면 유효하지 않다', () {
        // Given
        final form = createValidForm(subcategoryId: 0);

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('클레임 세부 종류를 선택해주세요'));
      });

      test('defectDescription이 비어있으면 유효하지 않다', () {
        // Given
        final form = createValidForm(defectDescription: '');

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('불량 내역을 입력해주세요'));
      });

      test('defectQuantity가 0 이하면 유효하지 않다', () {
        // Given
        final form = createValidForm(defectQuantity: 0);

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('불량 수량을 입력해주세요 (1개 이상)'));
      });

      test('구매 금액 입력 시 구매 방법 코드가 없으면 유효하지 않다', () {
        // Given
        final form = createValidForm(
          purchaseAmount: 5000,
          purchaseMethodCode: null,
          purchaseMethodName: '대형마트',
          receiptPhoto: testReceiptPhoto,
        );

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('구매 방법을 선택해주세요'));
      });

      test('구매 금액 입력 시 구매 방법명이 없으면 유효하지 않다', () {
        // Given
        final form = createValidForm(
          purchaseAmount: 5000,
          purchaseMethodCode: 'PM01',
          purchaseMethodName: null,
          receiptPhoto: testReceiptPhoto,
        );

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('구매 방법명이 비어있습니다'));
      });

      test('구매 금액 입력 시 영수증 사진이 없으면 유효하지 않다', () {
        // Given
        final form = createValidForm(
          purchaseAmount: 5000,
          purchaseMethodCode: 'PM01',
          purchaseMethodName: '대형마트',
          receiptPhoto: null,
        );

        // Then
        expect(form.isValid, false);
        expect(form.validate(), contains('구매 영수증 사진을 첨부해주세요'));
      });

      test('구매 금액이 0이면 조건부 필수 검증을 하지 않는다', () {
        // Given
        final form = createValidForm(
          purchaseAmount: 0,
          purchaseMethodCode: null,
          receiptPhoto: null,
        );

        // Then
        expect(form.isValid, true);
        expect(form.validate(), isEmpty);
      });

      test('여러 필드가 유효하지 않으면 모든 에러를 반환한다', () {
        // Given
        final form = createValidForm(
          storeId: 0,
          productCode: '',
          defectQuantity: 0,
        );

        // Then
        expect(form.isValid, false);
        final errors = form.validate();
        expect(errors.length, greaterThanOrEqualTo(3));
        expect(errors, contains('거래처를 선택해주세요'));
        expect(errors, contains('제품을 선택해주세요'));
        expect(errors, contains('불량 수량을 입력해주세요 (1개 이상)'));
      });
    });

    group('hasPurchaseInfo getter', () {
      test('구매 금액이 있으면 true를 반환한다', () {
        // Given
        final form = createValidForm(purchaseAmount: 5000);

        // Then
        expect(form.hasPurchaseInfo, true);
      });

      test('구매 금액이 0이면 false를 반환한다', () {
        // Given
        final form = createValidForm(purchaseAmount: 0);

        // Then
        expect(form.hasPurchaseInfo, false);
      });

      test('구매 금액이 null이면 false를 반환한다', () {
        // Given
        final form = createValidForm(purchaseAmount: null);

        // Then
        expect(form.hasPurchaseInfo, false);
      });
    });

    group('hasRequestType getter', () {
      test('요청사항 코드가 있으면 true를 반환한다', () {
        // Given
        final form = createValidForm(requestTypeCode: 'RT01');

        // Then
        expect(form.hasRequestType, true);
      });

      test('요청사항 코드가 빈 문자열이면 false를 반환한다', () {
        // Given
        final form = createValidForm(requestTypeCode: '');

        // Then
        expect(form.hasRequestType, false);
      });

      test('요청사항 코드가 null이면 false를 반환한다', () {
        // Given
        final form = createValidForm(requestTypeCode: null);

        // Then
        expect(form.hasRequestType, false);
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다', () {
        // Given
        final original = createValidForm();

        // When
        final copied = original.copyWith(
          defectQuantity: 5,
          defectDescription: '변경된 불량 내역',
        );

        // Then
        expect(copied.defectQuantity, 5);
        expect(copied.defectDescription, '변경된 불량 내역');
        expect(copied.storeId, original.storeId);
        expect(original.defectQuantity, 1); // 원본 불변성 확인
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
        // Given
        final form1 = createValidForm();
        final form2 = createValidForm();

        // Then
        expect(form1, form2);
        expect(form1.hashCode, form2.hashCode);
      });

      test('다른 값을 가진 엔티티는 다르게 비교된다', () {
        // Given
        final form1 = createValidForm();
        final form2 = createValidForm(defectQuantity: 10);

        // Then
        expect(form1, isNot(form2));
      });
    });

    test('toString이 올바른 형식으로 출력된다', () {
      // Given
      final form = createValidForm();

      // When
      final str = form.toString();

      // Then
      expect(str, contains('ClaimRegisterForm'));
      expect(str, contains('storeId: 1025'));
      expect(str, contains('storeName: 미광종합물류'));
    });
  });
}
