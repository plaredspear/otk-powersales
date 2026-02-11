import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/domain/entities/claim_form.dart';
import 'package:mobile/domain/entities/claim_form_data.dart';
import 'package:mobile/domain/entities/claim_result.dart';
import 'package:mobile/domain/repositories/claim_repository.dart';
import 'package:mobile/domain/usecases/register_claim_usecase.dart';

// Mock Repository
class MockClaimRepository implements ClaimRepository {
  ClaimRegisterResult? _resultToReturn;
  Exception? _exceptionToThrow;

  void setResult(ClaimRegisterResult result) {
    _resultToReturn = result;
    _exceptionToThrow = null;
  }

  void setException(Exception exception) {
    _exceptionToThrow = exception;
    _resultToReturn = null;
  }

  @override
  Future<ClaimRegisterResult> registerClaim(ClaimRegisterForm form) async {
    if (_exceptionToThrow != null) {
      throw _exceptionToThrow!;
    }
    return _resultToReturn!;
  }

  @override
  Future<ClaimFormData> getFormData() async {
    throw UnimplementedError();
  }
}

void main() {
  group('RegisterClaimUseCase', () {
    late MockClaimRepository mockRepository;
    late RegisterClaimUseCase useCase;

    final testDefectPhoto = File('test_defect.jpg');
    final testLabelPhoto = File('test_label.jpg');

    setUp(() {
      mockRepository = MockClaimRepository();
      useCase = RegisterClaimUseCase(mockRepository);
    });

    ClaimRegisterForm createValidForm() {
      return ClaimRegisterForm(
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
    }

    test('유효한 폼으로 클레임 등록이 성공한다', () async {
      // Given
      final form = createValidForm();
      final expectedResult = ClaimRegisterResult(
        id: 100,
        storeName: '미광종합물류',
        storeId: 1025,
        productName: '맛있는부대찌개라양념140G',
        productCode: '12345678',
        createdAt: DateTime(2026, 2, 11, 10, 30),
      );
      mockRepository.setResult(expectedResult);

      // When
      final result = await useCase(form);

      // Then
      expect(result, expectedResult);
    });

    test('유효하지 않은 폼은 ClaimValidationException을 발생시킨다', () async {
      // Given: storeId가 0인 유효하지 않은 폼
      final invalidForm = ClaimRegisterForm(
        storeId: 0, // 유효하지 않음
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

      // When & Then
      expect(
        () => useCase(invalidForm),
        throwsA(isA<ClaimValidationException>()),
      );
    });

    test('여러 필드가 유효하지 않으면 모든 에러를 포함한다', () async {
      // Given: 여러 필드가 유효하지 않은 폼
      final invalidForm = ClaimRegisterForm(
        storeId: 0, // 유효하지 않음
        storeName: '', // 유효하지 않음
        productCode: '', // 유효하지 않음
        productName: '',
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

      // When & Then
      try {
        await useCase(invalidForm);
        fail('Should throw ClaimValidationException');
      } catch (e) {
        expect(e, isA<ClaimValidationException>());
        final exception = e as ClaimValidationException;
        expect(exception.errors.length, greaterThanOrEqualTo(3));
        expect(exception.errors, contains('거래처를 선택해주세요'));
        expect(exception.errors, contains('거래처명이 비어있습니다'));
        expect(exception.errors, contains('제품을 선택해주세요'));
      }
    });

    test('구매 정보가 있지만 구매 방법이 없으면 예외를 발생시킨다', () async {
      // Given
      final invalidForm = ClaimRegisterForm(
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
        purchaseAmount: 5000, // 구매 금액 있음
        purchaseMethodCode: null, // 구매 방법 없음 (필수)
      );

      // When & Then
      try {
        await useCase(invalidForm);
        fail('Should throw ClaimValidationException');
      } catch (e) {
        expect(e, isA<ClaimValidationException>());
        final exception = e as ClaimValidationException;
        expect(exception.errors, contains('구매 방법을 선택해주세요'));
      }
    });

    test('Repository에서 예외가 발생하면 그대로 전파한다', () async {
      // Given
      final form = createValidForm();
      final testException = Exception('Network error');
      mockRepository.setException(testException);

      // When & Then
      expect(
        () => useCase(form),
        throwsA(testException),
      );
    });
  });

  group('ClaimValidationException', () {
    test('에러 메시지를 올바르게 생성한다', () {
      // Given
      const errors = ['에러1', '에러2', '에러3'];
      const exception = ClaimValidationException(errors);

      // Then
      expect(exception.errors, errors);
      expect(exception.message, '에러1, 에러2, 에러3');
    });

    test('toString이 올바르게 동작한다', () {
      // Given
      const errors = ['에러1', '에러2'];
      const exception = ClaimValidationException(errors);

      // When
      final str = exception.toString();

      // Then
      expect(str, 'ClaimValidationException: 에러1, 에러2');
    });
  });
}
