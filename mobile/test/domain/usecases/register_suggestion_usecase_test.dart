import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';
import 'package:mobile/domain/entities/suggestion_result.dart';
import 'package:mobile/domain/repositories/suggestion_repository.dart';
import 'package:mobile/domain/usecases/register_suggestion_usecase.dart';

class _MockSuggestionRepository implements SuggestionRepository {
  SuggestionRegisterResult? result;
  Exception? error;

  @override
  Future<SuggestionRegisterResult> registerSuggestion(
      SuggestionRegisterForm form) async {
    if (error != null) throw error!;
    return result!;
  }
}

File _createMockFile(String path) {
  return File(path);
}

SuggestionRegisterForm _createValidNewProductForm() {
  return SuggestionRegisterForm(
    category: SuggestionCategory.newProduct,
    title: '저당 라면 시리즈 출시 제안',
    content: '건강을 생각하는 저당 라면 시리즈를 출시하면 좋을 것 같습니다.',
  );
}

SuggestionRegisterForm _createValidExistingProductForm() {
  return SuggestionRegisterForm(
    category: SuggestionCategory.existingProduct,
    productCode: '12345678',
    productName: '진라면',
    title: '진라면 매운맛 개선 제안',
    content: '매운맛을 더 강화하면 좋을 것 같습니다.',
  );
}

void main() {
  late _MockSuggestionRepository repository;
  late RegisterSuggestionUseCase useCase;

  setUp(() {
    repository = _MockSuggestionRepository();
    useCase = RegisterSuggestionUseCase(repository);
  });

  group('RegisterSuggestionUseCase', () {
    group('정상 케이스', () {
      test('유효한 신제품 제안 폼으로 등록에 성공한다', () async {
        // Given
        final form = _createValidNewProductForm();
        final expectedResult = SuggestionRegisterResult(
          id: 50,
          category: SuggestionCategory.newProduct,
          categoryName: '신제품 제안',
          title: '저당 라면 시리즈 출시 제안',
          createdAt: DateTime(2026, 2, 11, 11, 0, 0),
        );
        repository.result = expectedResult;

        // When
        final result = await useCase.call(form);

        // Then
        expect(result, expectedResult);
        expect(result.category, SuggestionCategory.newProduct);
        expect(result.productCode, null);
        expect(result.productName, null);
      });

      test('유효한 기존제품 제안 폼으로 등록에 성공한다', () async {
        // Given
        final form = _createValidExistingProductForm();
        final expectedResult = SuggestionRegisterResult(
          id: 51,
          category: SuggestionCategory.existingProduct,
          categoryName: '기존제품 상품가치향상',
          productCode: '12345678',
          productName: '진라면',
          title: '진라면 매운맛 개선 제안',
          createdAt: DateTime(2026, 2, 11, 14, 30, 0),
        );
        repository.result = expectedResult;

        // When
        final result = await useCase.call(form);

        // Then
        expect(result, expectedResult);
        expect(result.category, SuggestionCategory.existingProduct);
        expect(result.productCode, '12345678');
        expect(result.productName, '진라면');
      });

      test('사진을 포함한 신제품 제안 폼으로 등록에 성공한다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.newProduct,
          title: '신제품 제안',
          content: '제안 내용',
          photos: [
            _createMockFile('/path/to/photo1.jpg'),
            _createMockFile('/path/to/photo2.jpg'),
          ],
        );
        final expectedResult = SuggestionRegisterResult(
          id: 52,
          category: SuggestionCategory.newProduct,
          categoryName: '신제품 제안',
          title: '신제품 제안',
          createdAt: DateTime(2026, 2, 11, 15, 0, 0),
        );
        repository.result = expectedResult;

        // When
        final result = await useCase.call(form);

        // Then
        expect(result, expectedResult);
      });

      test('사진을 포함한 기존제품 제안 폼으로 등록에 성공한다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.existingProduct,
          productCode: '87654321',
          productName: '너구리',
          title: '너구리 개선 제안',
          content: '제안 내용',
          photos: [_createMockFile('/path/to/photo1.jpg')],
        );
        final expectedResult = SuggestionRegisterResult(
          id: 53,
          category: SuggestionCategory.existingProduct,
          categoryName: '기존제품 상품가치향상',
          productCode: '87654321',
          productName: '너구리',
          title: '너구리 개선 제안',
          createdAt: DateTime(2026, 2, 11, 16, 0, 0),
        );
        repository.result = expectedResult;

        // When
        final result = await useCase.call(form);

        // Then
        expect(result, expectedResult);
      });
    });

    group('오류 케이스 - 필수 항목 누락', () {
      test('제목이 없으면 예외를 던진다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.newProduct,
          title: '', // Invalid
          content: '제안 내용',
        );

        // When & Then
        expect(
          () => useCase.call(form),
          throwsA(
            predicate((e) =>
                e is Exception && e.toString().contains('제목을 입력해주세요')),
          ),
        );
      });

      test('제안 내용이 없으면 예외를 던진다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.newProduct,
          title: '제목',
          content: '', // Invalid
        );

        // When & Then
        expect(
          () => useCase.call(form),
          throwsA(
            predicate((e) =>
                e is Exception && e.toString().contains('제안 내용을 입력해주세요')),
          ),
        );
      });
    });

    group('오류 케이스 - 조건부 필수 항목 누락', () {
      test('기존제품 선택 시 제품이 없으면 예외를 던진다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.existingProduct,
          // productCode, productName missing
          title: '제목',
          content: '제안 내용',
        );

        // When & Then
        expect(
          () => useCase.call(form),
          throwsA(
            predicate((e) =>
                e is Exception && e.toString().contains('제품을 선택해주세요')),
          ),
        );
      });

      test('기존제품 선택 시 제품 코드만 있고 제품명이 없으면 예외를 던진다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.existingProduct,
          productCode: '12345678',
          productName: null, // Missing
          title: '제목',
          content: '제안 내용',
        );

        // When & Then
        expect(
          () => useCase.call(form),
          throwsA(
            predicate((e) =>
                e is Exception && e.toString().contains('제품을 선택해주세요')),
          ),
        );
      });
    });

    group('오류 케이스 - 사진 제한', () {
      test('사진이 2장을 초과하면 예외를 던진다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.newProduct,
          title: '제목',
          content: '제안 내용',
          photos: [
            _createMockFile('/path/to/photo1.jpg'),
            _createMockFile('/path/to/photo2.jpg'),
            _createMockFile('/path/to/photo3.jpg'), // Exceeds limit
          ],
        );

        // When & Then
        expect(
          () => useCase.call(form),
          throwsA(
            predicate((e) =>
                e is Exception &&
                e.toString().contains('사진은 최대 2장까지 첨부 가능합니다')),
          ),
        );
      });
    });

    group('Repository 에러 처리', () {
      test('Repository에서 예외가 발생하면 전파된다', () async {
        // Given
        final form = _createValidNewProductForm();
        repository.error = Exception('Network error');

        // When & Then
        expect(
          () => useCase.call(form),
          throwsA(
            predicate((e) =>
                e is Exception && e.toString().contains('Network error')),
          ),
        );
      });

      test('Repository에서 서버 에러가 발생하면 전파된다', () async {
        // Given
        final form = _createValidExistingProductForm();
        repository.error = Exception('Server error: 500');

        // When & Then
        expect(
          () => useCase.call(form),
          throwsException,
        );
      });
    });
  });
}
