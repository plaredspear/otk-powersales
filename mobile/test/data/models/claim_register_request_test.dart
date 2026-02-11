import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/claim_register_request.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/domain/entities/claim_form.dart';

void main() {
  group('ClaimRegisterRequest', () {
    final testDefectPhoto = File('test_defect.jpg');
    final testLabelPhoto = File('test_label.jpg');
    final testReceiptPhoto = File('test_receipt.jpg');

    test('fromEntity가 올바르게 동작한다 (필수 필드만)', () {
      // Given
      final form = ClaimRegisterForm(
        storeId: 1025,
        storeName: '미광종합물류',
        productCode: '12345678',
        productName: '맛있는부대찌개라양념140G',
        dateType: ClaimDateType.expiryDate,
        date: DateTime(2026, 2, 20),
        categoryId: 1,
        categoryName: '이물',
        subcategoryId: 101,
        subcategoryName: '벌레',
        defectDescription: '제품에서 벌레가 발견되었습니다',
        defectQuantity: 1,
        defectPhoto: testDefectPhoto,
        labelPhoto: testLabelPhoto,
      );

      // When
      final request = ClaimRegisterRequest.fromEntity(form);

      // Then
      expect(request.storeId, 1025);
      expect(request.productCode, '12345678');
      expect(request.dateType, 'EXPIRY_DATE');
      expect(request.date, '2026-02-20');
      expect(request.categoryId, 1);
      expect(request.subcategoryId, 101);
      expect(request.defectDescription, '제품에서 벌레가 발견되었습니다');
      expect(request.defectQuantity, 1);
      expect(request.purchaseAmount, null);
      expect(request.purchaseMethodCode, null);
      expect(request.receiptPhoto, null);
      expect(request.requestTypeCode, null);
    });

    test('fromEntity가 올바르게 동작한다 (선택 필드 포함)', () {
      // Given
      final form = ClaimRegisterForm(
        storeId: 1025,
        storeName: '미광종합물류',
        productCode: '12345678',
        productName: '맛있는부대찌개라양념140G',
        dateType: ClaimDateType.manufactureDate,
        date: DateTime(2026, 1, 15),
        categoryId: 2,
        categoryName: '변질/변패',
        subcategoryId: 201,
        subcategoryName: '맛 변질',
        defectDescription: '맛이 이상합니다',
        defectQuantity: 5,
        defectPhoto: testDefectPhoto,
        labelPhoto: testLabelPhoto,
        purchaseAmount: 5000,
        purchaseMethodCode: 'PM01',
        purchaseMethodName: '대형마트',
        receiptPhoto: testReceiptPhoto,
        requestTypeCode: 'RT01',
        requestTypeName: '교환',
      );

      // When
      final request = ClaimRegisterRequest.fromEntity(form);

      // Then
      expect(request.dateType, 'MANUFACTURE_DATE');
      expect(request.date, '2026-01-15');
      expect(request.purchaseAmount, 5000);
      expect(request.purchaseMethodCode, 'PM01');
      expect(request.receiptPhoto, testReceiptPhoto);
      expect(request.requestTypeCode, 'RT01');
    });

    test('날짜가 YYYY-MM-DD 형식으로 변환된다', () {
      // Given
      final form = ClaimRegisterForm(
        storeId: 1025,
        storeName: '미광종합물류',
        productCode: '12345678',
        productName: '맛있는부대찌개라양념140G',
        dateType: ClaimDateType.expiryDate,
        date: DateTime(2026, 12, 31, 15, 30, 45), // 시간 포함
        categoryId: 1,
        categoryName: '이물',
        subcategoryId: 101,
        subcategoryName: '벌레',
        defectDescription: '테스트',
        defectQuantity: 1,
        defectPhoto: testDefectPhoto,
        labelPhoto: testLabelPhoto,
      );

      // When
      final request = ClaimRegisterRequest.fromEntity(form);

      // Then
      expect(request.date, '2026-12-31'); // 시간 부분 제거됨
    });
  });
}
