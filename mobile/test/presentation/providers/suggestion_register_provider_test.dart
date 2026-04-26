import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';
import 'package:mobile/domain/entities/suggestion_result.dart';
import 'package:mobile/domain/usecases/register_suggestion_usecase.dart';
import 'package:mobile/presentation/providers/suggestion_register_provider.dart';

// Mock UseCase
class MockRegisterSuggestionUseCase implements RegisterSuggestionUseCase {
  bool shouldFail = false;
  String? failureMessage;

  @override
  Future<SuggestionRegisterResult> call(SuggestionRegisterForm form) async {
    await Future.delayed(const Duration(milliseconds: 50));
    if (shouldFail) {
      throw Exception(failureMessage ?? '등록 실패');
    }
    return SuggestionRegisterResult(
      id: 1,
      category: form.category,
      categoryName: form.category.displayName,
      productCode: form.productCode,
      productName: form.productName,
      title: form.title,
      createdAt: DateTime.now(),
    );
  }
}

void main() {
  group('SuggestionRegisterNotifier', () {
    late SuggestionRegisterNotifier notifier;
    late MockRegisterSuggestionUseCase mockUseCase;

    setUp(() {
      mockUseCase = MockRegisterSuggestionUseCase();
      notifier = SuggestionRegisterNotifier(
        registerSuggestion: mockUseCase,
      );
    });

    group('초기 상태', () {
      test('초기 상태는 신제품 제안이다', () {
        // When
        final state = notifier.state;

        // Then
        expect(state.isLoading, false);
        expect(state.isSubmitting, false);
        expect(state.errorMessage, null);
        expect(state.form.category, SuggestionCategory.newProduct);
        expect(state.form.title, '');
        expect(state.form.content, '');
        expect(state.form.photos, isEmpty);
      });

      test('초기 상태는 isNewProduct가 true이다', () {
        // When & Then
        expect(notifier.state.isNewProduct, true);
        expect(notifier.state.isExistingProduct, false);
      });
    });

    group('분류 변경', () {
      test('신제품에서 기존제품으로 변경한다', () {
        // When
        notifier.changeCategory(SuggestionCategory.existingProduct);

        // Then
        expect(notifier.state.category, SuggestionCategory.existingProduct);
        expect(notifier.state.isExistingProduct, true);
        expect(notifier.state.isNewProduct, false);
      });

      test('기존제품에서 신제품으로 변경하면 제품 정보가 초기화된다', () {
        // Given
        notifier.changeCategory(SuggestionCategory.existingProduct);
        notifier.selectProduct('12345678', '진라면');
        expect(notifier.state.hasProduct, true);

        // When
        notifier.changeCategory(SuggestionCategory.newProduct);

        // Then
        expect(notifier.state.category, SuggestionCategory.newProduct);
        expect(notifier.state.hasProduct, false);
        expect(notifier.state.form.productCode, null);
        expect(notifier.state.form.productName, null);
        expect(notifier.state.selectedProductName, null);
      });
    });

    group('제품 선택', () {
      test('기존제품 선택 시 제품을 설정한다', () {
        // Given
        notifier.changeCategory(SuggestionCategory.existingProduct);

        // When
        notifier.selectProduct('12345678', '진라면');

        // Then
        expect(notifier.state.form.productCode, '12345678');
        expect(notifier.state.form.productName, '진라면');
        expect(notifier.state.selectedProductName, '진라면');
        expect(notifier.state.hasProduct, true);
      });

      test('신제품 선택 시 제품 선택이 무시된다', () {
        // Given - 초기 상태는 신제품

        // When
        notifier.selectProduct('12345678', '진라면');

        // Then
        expect(notifier.state.form.productCode, null);
        expect(notifier.state.hasProduct, false);
      });
    });

    group('입력값 변경', () {
      test('제목을 변경한다', () {
        // When
        notifier.updateTitle('저당 라면 시리즈 제안');

        // Then
        expect(notifier.state.form.title, '저당 라면 시리즈 제안');
      });

      test('내용을 변경한다', () {
        // When
        notifier.updateContent('건강을 생각하는 저당 라면을 출시하면 좋겠습니다.');

        // Then
        expect(notifier.state.form.content, '건강을 생각하는 저당 라면을 출시하면 좋겠습니다.');
      });
    });

    group('사진 관리', () {
      test('사진을 추가한다', () {
        // Given
        final photo1 = File('test1.jpg');

        // When
        notifier.addPhoto(photo1);

        // Then
        expect(notifier.state.form.photos.length, 1);
        expect(notifier.state.form.photos[0].path, 'test1.jpg');
        expect(notifier.state.hasPhotos, true);
      });

      test('사진을 2장까지 추가한다', () {
        // Given
        final photo1 = File('test1.jpg');
        final photo2 = File('test2.jpg');

        // When
        notifier.addPhoto(photo1);
        notifier.addPhoto(photo2);

        // Then
        expect(notifier.state.form.photos.length, 2);
      });

      test('사진이 2장을 초과하면 에러가 발생한다', () {
        // Given
        notifier.addPhoto(File('test1.jpg'));
        notifier.addPhoto(File('test2.jpg'));

        // When
        notifier.addPhoto(File('test3.jpg'));

        // Then
        expect(notifier.state.form.photos.length, 2);
        expect(notifier.state.errorMessage, '사진은 최대 2장까지 첨부 가능합니다');
      });

      test('사진을 삭제한다', () {
        // Given
        notifier.addPhoto(File('test1.jpg'));
        notifier.addPhoto(File('test2.jpg'));
        expect(notifier.state.form.photos.length, 2);

        // When
        notifier.removePhoto(0);

        // Then
        expect(notifier.state.form.photos.length, 1);
        expect(notifier.state.form.photos[0].path, 'test2.jpg');
      });
    });

    group('제안 등록', () {
      test('유효한 신제품 제안을 등록한다', () async {
        // Given
        notifier.updateTitle('신제품 제안');
        notifier.updateContent('제안 내용');

        // When
        await notifier.submit();

        // Then
        expect(notifier.state.isSubmitting, false);
        expect(notifier.state.successMessage, '제안이 등록되었습니다');
        expect(notifier.state.errorMessage, null);
      });

      test('유효한 기존제품 제안을 등록한다', () async {
        // Given
        notifier.changeCategory(SuggestionCategory.existingProduct);
        notifier.selectProduct('12345678', '진라면');
        notifier.updateTitle('기존제품 개선 제안');
        notifier.updateContent('제안 내용');

        // When
        await notifier.submit();

        // Then
        expect(notifier.state.isSubmitting, false);
        expect(notifier.state.successMessage, '제안이 등록되었습니다');
      });

      test('필수 항목 누락 시 에러가 발생한다 (제목)', () async {
        // Given - 제목 없음
        notifier.updateContent('제안 내용');

        // When
        await notifier.submit();

        // Then
        expect(notifier.state.errorMessage, '제목을 입력해주세요');
        expect(notifier.state.successMessage, null);
      });

      test('필수 항목 누락 시 에러가 발생한다 (내용)', () async {
        // Given - 내용 없음
        notifier.updateTitle('신제품 제안');

        // When
        await notifier.submit();

        // Then
        expect(notifier.state.errorMessage, '제안 내용을 입력해주세요');
      });

      test('기존제품 선택 시 제품 미선택하면 에러가 발생한다', () async {
        // Given
        notifier.changeCategory(SuggestionCategory.existingProduct);
        notifier.updateTitle('기존제품 개선 제안');
        notifier.updateContent('제안 내용');
        // 제품 미선택

        // When
        await notifier.submit();

        // Then
        expect(notifier.state.errorMessage, '제품을 선택해주세요');
      });

      test('UseCase에서 에러 발생 시 에러 상태가 된다', () async {
        // Given
        notifier.updateTitle('신제품 제안');
        notifier.updateContent('제안 내용');
        mockUseCase.shouldFail = true;
        mockUseCase.failureMessage = '네트워크 오류';

        // When
        await notifier.submit();

        // Then
        expect(notifier.state.isSubmitting, false);
        expect(notifier.state.errorMessage, '네트워크 오류');
        expect(notifier.state.successMessage, null);
      });
    });

    group('상태 초기화', () {
      test('reset()으로 폼을 초기화한다', () {
        // Given
        notifier.updateTitle('제목');
        notifier.updateContent('내용');
        notifier.addPhoto(File('test.jpg'));

        // When
        notifier.reset();

        // Then
        expect(notifier.state.form.title, '');
        expect(notifier.state.form.content, '');
        expect(notifier.state.form.photos, isEmpty);
        expect(notifier.state.category, SuggestionCategory.newProduct);
      });

      test('clearError()로 에러를 지운다', () {
        // Given
        notifier.addPhoto(File('test1.jpg'));
        notifier.addPhoto(File('test2.jpg'));
        notifier.addPhoto(File('test3.jpg')); // 에러 발생
        expect(notifier.state.errorMessage, isNotNull);

        // When
        notifier.clearError();

        // Then
        expect(notifier.state.errorMessage, null);
      });

      test('clearSuccess()로 성공 메시지를 지운다', () async {
        // Given
        notifier.updateTitle('제목');
        notifier.updateContent('내용');
        await notifier.submit();
        expect(notifier.state.successMessage, isNotNull);

        // When
        notifier.clearSuccess();

        // Then
        expect(notifier.state.successMessage, null);
      });
    });
  });
}
