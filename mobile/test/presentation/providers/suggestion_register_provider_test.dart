import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/suggestion_draft.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';
import 'package:mobile/domain/entities/suggestion_result.dart';
import 'package:mobile/domain/usecases/load_suggestion_draft_usecase.dart';
import 'package:mobile/domain/usecases/register_suggestion_usecase.dart';
import 'package:mobile/domain/usecases/save_suggestion_draft_usecase.dart';
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

class MockSaveSuggestionDraftUseCase implements SaveSuggestionDraftUseCase {
  bool shouldFail = false;
  String? failureMessage;
  SuggestionRegisterForm? capturedForm;

  @override
  Future<void> call(SuggestionRegisterForm? form) async {
    capturedForm = form;
    if (shouldFail) {
      throw Exception(failureMessage ?? '임시저장 실패');
    }
  }
}

class MockLoadSuggestionDraftUseCase implements LoadSuggestionDraftUseCase {
  SuggestionDraft? draft;
  bool shouldFail = false;

  @override
  Future<SuggestionDraft?> call() async {
    if (shouldFail) throw Exception('조회 실패');
    return draft;
  }
}

void main() {
  group('SuggestionRegisterNotifier', () {
    late SuggestionRegisterNotifier notifier;
    late MockRegisterSuggestionUseCase mockUseCase;
    late MockSaveSuggestionDraftUseCase mockSaveDraft;
    late MockLoadSuggestionDraftUseCase mockLoadDraft;

    setUp(() {
      mockUseCase = MockRegisterSuggestionUseCase();
      mockSaveDraft = MockSaveSuggestionDraftUseCase();
      mockLoadDraft = MockLoadSuggestionDraftUseCase();
      notifier = SuggestionRegisterNotifier(
        registerSuggestion: mockUseCase,
        saveDraft: mockSaveDraft,
        loadDraft: mockLoadDraft,
      );
    });

    group('초기 상태', () {
      test('초기 상태는 물류 클레임이다 (레거시 기본값 정합)', () {
        // When
        final state = notifier.state;

        // Then
        expect(state.isLoading, false);
        expect(state.isSubmitting, false);
        expect(state.errorMessage, null);
        expect(state.form.category, SuggestionCategory.logisticsClaim);
        expect(state.form.title, '');
        expect(state.form.content, '');
        expect(state.form.photos, isEmpty);
      });

      test('초기 상태는 isLogisticsClaim이 true이다', () {
        // When & Then
        expect(notifier.state.isLogisticsClaim, true);
        expect(notifier.state.isNewProduct, false);
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
        // Given - 신제품 제안 전환
        notifier.changeCategory(SuggestionCategory.newProduct);

        // When
        notifier.selectProduct('12345678', '진라면');

        // Then
        expect(notifier.state.form.productCode, null);
        expect(notifier.state.hasProduct, false);
      });

      test('물류 클레임 선택 시 대표 제품을 설정한다', () {
        // Given
        notifier.changeCategory(SuggestionCategory.logisticsClaim);

        // When
        notifier.selectProduct('12345678', '진라면');

        // Then
        expect(notifier.state.form.productCode, '12345678');
        expect(notifier.state.form.productName, '진라면');
        expect(notifier.state.hasProduct, true);
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
        notifier.changeCategory(SuggestionCategory.newProduct);
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
        // Given - 신제품 제안 + 제목 없음
        notifier.changeCategory(SuggestionCategory.newProduct);
        notifier.updateContent('제안 내용');

        // When
        await notifier.submit();

        // Then
        expect(notifier.state.errorMessage, '제목을 입력해주세요');
        expect(notifier.state.successMessage, null);
      });

      test('필수 항목 누락 시 에러가 발생한다 (내용)', () async {
        // Given - 신제품 제안 + 내용 없음
        notifier.changeCategory(SuggestionCategory.newProduct);
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
        notifier.changeCategory(SuggestionCategory.newProduct);
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
        expect(notifier.state.category, SuggestionCategory.logisticsClaim);
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
        notifier.changeCategory(SuggestionCategory.newProduct);
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

    group('물류 클레임 카테고리 분기', () {
      test('기존제품 ↔ 물류 클레임 전환 시 대표 제품은 유지된다 (레거시 정합)', () {
        // Given
        notifier.changeCategory(SuggestionCategory.existingProduct);
        notifier.selectProduct('12345678', '진라면');
        expect(notifier.state.form.productCode, '12345678');

        // When
        notifier.changeCategory(SuggestionCategory.logisticsClaim);

        // Then — 대표 제품은 공통 개념이라 유지
        expect(notifier.state.isLogisticsClaim, true);
        expect(notifier.state.form.productCode, '12345678');
        expect(notifier.state.form.productName, '진라면');
      });

      test('물류 클레임에서 신제품으로 전환하면 분기 입력이 초기화된다', () {
        // Given
        notifier.changeCategory(SuggestionCategory.logisticsClaim);
        notifier.selectProduct('12345678', '진라면');
        notifier.selectAccount(
          accountId: 100,
          accountName: '오뚜기 농협',
          sapAccountCode: 'SAP-0001',
        );
        notifier.updateClaimType('취급부주의 제품 파손');
        notifier.updateClaimDate(DateTime(2026, 5, 22));
        notifier.updateCarNumber('12가1234');

        // When
        notifier.changeCategory(SuggestionCategory.newProduct);

        // Then — 신제품 제안은 제품 + 물류 클레임 필드 모두 제거
        expect(notifier.state.isNewProduct, true);
        expect(notifier.state.form.productCode, null);
        expect(notifier.state.form.productName, null);
        expect(notifier.state.form.accountId, null);
        expect(notifier.state.form.accountName, null);
        expect(notifier.state.form.sapAccountCode, null);
        expect(notifier.state.form.claimType, null);
        expect(notifier.state.form.claimDate, null);
        expect(notifier.state.form.carNumber, null);
      });

      test('selectAccount는 물류 클레임 카테고리에서만 동작한다', () {
        // Given — 신제품 카테고리
        notifier.changeCategory(SuggestionCategory.newProduct);
        notifier.selectAccount(
          accountId: 100,
          accountName: '오뚜기 농협',
        );

        // Then — 무시됨
        expect(notifier.state.form.accountId, null);

        // When — 물류 클레임 전환 후 호출
        notifier.changeCategory(SuggestionCategory.logisticsClaim);
        notifier.selectAccount(
          accountId: 100,
          accountName: '오뚜기 농협',
          sapAccountCode: 'SAP-0001',
        );

        // Then
        expect(notifier.state.form.accountId, 100);
        expect(notifier.state.form.accountName, '오뚜기 농협');
        expect(notifier.state.form.sapAccountCode, 'SAP-0001');
      });

      test('updateClaimType / updateClaimDate / updateCarNumber 가 form 에 반영된다', () {
        // Given
        notifier.changeCategory(SuggestionCategory.logisticsClaim);

        // When
        notifier.updateClaimType('취급부주의 제품 파손');
        notifier.updateClaimDate(DateTime(2026, 5, 22));
        notifier.updateCarNumber('12가1234');

        // Then
        expect(notifier.state.form.claimType, '취급부주의 제품 파손');
        expect(notifier.state.form.claimDate, DateTime(2026, 5, 22));
        expect(notifier.state.form.carNumber, '12가1234');
      });
    });

    group('임시저장 (draft)', () {
      test('saveDraft 는 현재 폼을 usecase 로 전달하고 성공 시 true', () async {
        // Given
        notifier.changeCategory(SuggestionCategory.newProduct);
        notifier.updateTitle('임시 제목');

        // When
        final ok = await notifier.saveDraft();

        // Then
        expect(ok, true);
        expect(mockSaveDraft.capturedForm, isNotNull);
        expect(mockSaveDraft.capturedForm!.title, '임시 제목');
        expect(notifier.state.errorMessage, null);
        expect(notifier.state.hasDraft, true);
      });

      test('saveDraft 실패 시 false 와 에러 메시지', () async {
        // Given
        mockSaveDraft.shouldFail = true;
        mockSaveDraft.failureMessage = '서버 오류';

        // When
        final ok = await notifier.saveDraft();

        // Then
        expect(ok, false);
        expect(notifier.state.errorMessage, '서버 오류');
      });

      test('loadDraftIfExists 는 draft 없으면 null 반환', () async {
        // Given
        mockLoadDraft.draft = null;

        // When
        final draft = await notifier.loadDraftIfExists();

        // Then
        expect(draft, null);
        expect(notifier.state.hasDraft, false);
      });

      test('loadDraftIfExists 조회 실패는 null 로 흡수된다', () async {
        // Given
        mockLoadDraft.shouldFail = true;

        // When
        final draft = await notifier.loadDraftIfExists();

        // Then
        expect(draft, null);
      });

      test('applyDraft 는 물류 클레임 draft 를 폼에 prefill 한다', () {
        // Given
        final draft = SuggestionDraft(
          category: 'LOGISTICS_CLAIM',
          title: '물류 클레임 제목',
          content: '상세 내용',
          productCode: '12345678',
          productName: '진라면',
          accountId: 100,
          accountName: '오뚜기 농협',
          sapAccountCode: 'SAP-0001',
          claimType: '취급부주의 제품 파손',
          claimDate: DateTime(2026, 5, 22),
          carNumber: '12가1234',
          photos: [File('p0.jpg')],
        );

        // When
        notifier.applyDraft(draft);

        // Then
        final form = notifier.state.form;
        expect(form.category, SuggestionCategory.logisticsClaim);
        expect(form.title, '물류 클레임 제목');
        expect(form.content, '상세 내용');
        expect(form.productCode, '12345678');
        expect(form.accountId, 100);
        expect(form.sapAccountCode, 'SAP-0001');
        expect(form.claimType, '취급부주의 제품 파손');
        expect(form.claimDate, DateTime(2026, 5, 22));
        expect(form.carNumber, '12가1234');
        expect(form.photos.length, 1);
        expect(notifier.state.hasDraft, true);
      });

      test('applyDraft 는 신제품 draft 의 물류 전용 필드를 무시한다', () {
        // Given — category 가 신제품이면 accountId 등은 채우지 않는다
        final draft = SuggestionDraft(
          category: 'NEW_PRODUCT',
          title: '신제품 제목',
          accountId: 100,
          claimType: '취급부주의 제품 파손',
        );

        // When
        notifier.applyDraft(draft);

        // Then
        final form = notifier.state.form;
        expect(form.category, SuggestionCategory.newProduct);
        expect(form.title, '신제품 제목');
        expect(form.accountId, null);
        expect(form.claimType, null);
      });
    });
  });
}
