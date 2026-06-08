import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/domain/entities/claim_category.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/domain/entities/claim_detail.dart';
import 'package:mobile/domain/entities/claim_draft.dart';
import 'package:mobile/domain/entities/claim_form.dart';
import 'package:mobile/domain/entities/claim_form_data.dart';
import 'package:mobile/domain/entities/claim_form_entry.dart';
import 'package:mobile/domain/entities/claim_list_item.dart';
import 'package:mobile/domain/entities/claim_result.dart';
import 'package:mobile/domain/repositories/claim_repository.dart';
import 'package:mobile/presentation/providers/claim_register_provider.dart';

void main() {
  late ProviderContainer container;
  late FakeClaimRepository fakeRepository;

  setUp(() {
    fakeRepository = FakeClaimRepository();
    container = ProviderContainer(
      overrides: [
        claimRepositoryProvider.overrideWithValue(fakeRepository),
      ],
    );
  });

  tearDown(() {
    container.dispose();
  });

  group('ClaimRegisterNotifier', () {
    test('초기 상태는 빈 상태이다', () {
      final state = container.read(claimRegisterProvider);

      expect(state.form, isNull);
      expect(state.formData, isNull);
      expect(state.loading, false);
      expect(state.error, isNull);
    });

    group('loadForm', () {
      test('진입 폼 메타데이터를 state 에 채우고 임시저장 없으면 null 을 반환한다', () async {
        // Given
        fakeRepository.formDataToReturn = _sampleFormData;

        final notifier = container.read(claimRegisterProvider.notifier);

        // When
        final draft = await notifier.loadForm();

        // Then
        expect(draft, isNull);
        expect(fakeRepository.getFormCalls, 1);
        final state = container.read(claimRegisterProvider);
        expect(state.formData, isNotNull);
        expect(state.formData!.categories.length, 2);
        expect(state.formData!.purchaseMethods.length, 2);
        expect(state.formData!.requestTypes.length, 2);
        expect(state.loading, false);
        expect(state.error, isNull);
      });

      test('임시저장이 있으면 메타데이터를 채우고 draft 를 반환한다', () async {
        // Given
        fakeRepository.formDataToReturn = _sampleFormData;
        fakeRepository.draftToReturn = const ClaimDraft(
          accountId: 1025,
          accountName: '미광종합물류',
          claimType1: 'A',
        );

        final notifier = container.read(claimRegisterProvider.notifier);

        // When
        final draft = await notifier.loadForm();

        // Then
        expect(draft, isNotNull);
        expect(draft!.accountName, '미광종합물류');
        expect(container.read(claimRegisterProvider).formData, isNotNull);
      });

      test('로드 실패 시 에러를 설정한다', () async {
        // Given
        fakeRepository.shouldThrowError = true;

        final notifier = container.read(claimRegisterProvider.notifier);

        // When
        final draft = await notifier.loadForm();

        // Then
        expect(draft, isNull);
        final state = container.read(claimRegisterProvider);
        expect(state.formData, isNull);
        expect(state.loading, false);
        expect(state.error, isNotNull);
      });
    });

    group('폼 필드 업데이트', () {
      test('거래처를 선택한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.selectAccount(1025, '미광종합물류');

        final state = container.read(claimRegisterProvider);
        expect(state.form, isNotNull);
        expect(state.form!.accountId, 1025);
        expect(state.form!.accountName, '미광종합물류');
      });

      test('제품을 선택한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.selectProduct('12345678', '맛있는부대찌개라양념140G');

        final state = container.read(claimRegisterProvider);
        expect(state.form!.productCode, '12345678');
        expect(state.form!.productName, '맛있는부대찌개라양념140G');
      });

      test('기한 종류를 선택한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.selectDateType(ClaimDateType.manufactureDate);

        final state = container.read(claimRegisterProvider);
        expect(state.form!.dateType, ClaimDateType.manufactureDate);
      });

      test('기한 날짜를 선택한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        final date = DateTime(2026, 2, 20);
        notifier.selectDate(date);

        final state = container.read(claimRegisterProvider);
        expect(state.form!.date, date);
      });

      test('클레임 종류1을 선택하면 종류2가 초기화된다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        // Given: 종류2 먼저 설정
        notifier.selectSubcategory('AA', '벌레');

        // When: 종류1 변경
        notifier.selectCategory('A', '이물');

        // Then: 종류2 초기화됨
        final state = container.read(claimRegisterProvider);
        expect(state.form!.categoryId, 'A');
        expect(state.form!.categoryName, '이물');
        expect(state.form!.subcategoryId, '');
        expect(state.form!.subcategoryName, '');
      });

      test('클레임 종류2를 선택한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.selectSubcategory('AA', '벌레');

        final state = container.read(claimRegisterProvider);
        expect(state.form!.subcategoryId, 'AA');
        expect(state.form!.subcategoryName, '벌레');
      });

      test('불량 내역을 입력한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.updateDefectDescription('제품에서 벌레가 발견되었습니다');

        final state = container.read(claimRegisterProvider);
        expect(state.form!.defectDescription, '제품에서 벌레가 발견되었습니다');
      });

      test('불량 수량을 입력한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.updateDefectQuantity(5);

        final state = container.read(claimRegisterProvider);
        expect(state.form!.defectQuantity, 5);
      });

      test('불량 사진을 첨부한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        final photo = File('test_defect.jpg');
        notifier.attachDefectPhoto(photo);

        final state = container.read(claimRegisterProvider);
        expect(state.form!.defectPhoto.path, 'test_defect.jpg');
      });

      test('일부인 사진을 첨부한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        final photo = File('test_label.jpg');
        notifier.attachLabelPhoto(photo);

        final state = container.read(claimRegisterProvider);
        expect(state.form!.labelPhoto.path, 'test_label.jpg');
      });

      test('구매 금액을 입력한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.updatePurchaseAmount(5000);

        final state = container.read(claimRegisterProvider);
        expect(state.form!.purchaseAmount, 5000);
      });

      test('구매 방법을 선택한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.selectPurchaseMethod('PM01', '대형마트');

        final state = container.read(claimRegisterProvider);
        expect(state.form!.purchaseMethodCode, 'PM01');
        expect(state.form!.purchaseMethodName, '대형마트');
      });

      test('구매 영수증 사진을 첨부한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        final photo = File('test_receipt.jpg');
        notifier.attachReceiptPhoto(photo);

        final state = container.read(claimRegisterProvider);
        expect(state.form!.receiptPhoto?.path, 'test_receipt.jpg');
      });

      test('요청사항을 선택한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.selectRequestType('RT01', '교환');

        final state = container.read(claimRegisterProvider);
        expect(state.form!.requestTypeCode, 'RT01');
        expect(state.form!.requestTypeName, '교환');
      });
    });

    group('registerClaim', () {
      test('유효한 폼으로 클레임을 등록한다', () async {
        // Given
        fakeRepository.registerResultToReturn = _sampleRegisterResult;

        final notifier = container.read(claimRegisterProvider.notifier);

        // 필수 필드 모두 입력
        notifier.selectAccount(1025, '미광종합물류');
        notifier.selectProduct('12345678', '맛있는부대찌개라양념140G');
        notifier.selectDateType(ClaimDateType.expiryDate);
        notifier.selectDate(DateTime(2026, 2, 20));
        notifier.selectCategory('A', '이물');
        notifier.selectSubcategory('AA', '벌레');
        notifier.updateDefectDescription('제품에서 벌레가 발견되었습니다');
        notifier.updateDefectQuantity(1);
        notifier.attachDefectPhoto(File('test_defect.jpg'));
        notifier.attachLabelPhoto(File('test_label.jpg'));

        // When
        final result = await notifier.registerClaim();

        // Then
        expect(result, true);
        expect(fakeRepository.registerClaimCalls, 1);

        // 등록 성공 후 폼 초기화
        final state = container.read(claimRegisterProvider);
        expect(state.form, isNull);
        expect(state.loading, false);
        expect(state.error, isNull);
      });

      test('폼이 없으면 등록 실패', () async {
        final notifier = container.read(claimRegisterProvider.notifier);

        // When: 폼 없이 등록 시도
        final result = await notifier.registerClaim();

        // Then
        expect(result, false);
        final state = container.read(claimRegisterProvider);
        expect(state.error, '폼 데이터가 없습니다');
      });

      test('필수 항목 누락 시 등록 실패', () async {
        final notifier = container.read(claimRegisterProvider.notifier);

        // Given: 일부 필수 항목만 입력 (거래처, 제품만)
        notifier.selectAccount(1025, '미광종합물류');
        notifier.selectProduct('12345678', '맛있는부대찌개라양념140G');

        // When
        final result = await notifier.registerClaim();

        // Then
        expect(result, false);
        final state = container.read(claimRegisterProvider);
        expect(state.error, isNotNull);
      });

      test('등록 실패 시 에러를 설정한다', () async {
        // Given
        fakeRepository.shouldThrowError = true;

        final notifier = container.read(claimRegisterProvider.notifier);

        // 모든 필수 필드 입력
        notifier.selectAccount(1025, '미광종합물류');
        notifier.selectProduct('12345678', '맛있는부대찌개라양념140G');
        notifier.selectDateType(ClaimDateType.expiryDate);
        notifier.selectDate(DateTime(2026, 2, 20));
        notifier.selectCategory('A', '이물');
        notifier.selectSubcategory('AA', '벌레');
        notifier.updateDefectDescription('제품에서 벌레가 발견되었습니다');
        notifier.updateDefectQuantity(1);
        notifier.attachDefectPhoto(File('test_defect.jpg'));
        notifier.attachLabelPhoto(File('test_label.jpg'));

        // When
        final result = await notifier.registerClaim();

        // Then
        expect(result, false);
        final state = container.read(claimRegisterProvider);
        expect(state.loading, false);
        expect(state.error, isNotNull);
      });
    });

    test('clearForm은 폼을 초기화한다', () {
      final notifier = container.read(claimRegisterProvider.notifier);

      // Given: 폼 데이터 입력
      notifier.selectAccount(1025, '미광종합물류');
      notifier.selectProduct('12345678', '맛있는부대찌개라양념140G');

      // When
      notifier.clearForm();

      // Then
      final state = container.read(claimRegisterProvider);
      expect(state.form, isNull);
      expect(state.error, isNull);
    });

    group('임시저장 (draft)', () {
      test('saveDraft 성공 시 true 를 반환하고 repository 를 호출한다', () async {
        final notifier = container.read(claimRegisterProvider.notifier);
        notifier.selectAccount(1025, '미광종합물류');

        final result = await notifier.saveDraft();

        expect(result, true);
        expect(fakeRepository.saveDraftCalls, 1);
      });

      test('saveDraft 실패 시 false 를 반환하고 에러를 설정한다', () async {
        fakeRepository.shouldThrowError = true;
        final notifier = container.read(claimRegisterProvider.notifier);

        final result = await notifier.saveDraft();

        expect(result, false);
        expect(container.read(claimRegisterProvider).error, isNotNull);
      });

      test('applyDraft 는 임시저장 값을 폼에 반영하고 종류명을 metadata 로 해석한다', () async {
        fakeRepository.formDataToReturn = _sampleFormData;
        final notifier = container.read(claimRegisterProvider.notifier);
        await notifier.loadForm();

        notifier.applyDraft(const ClaimDraft(
          accountId: 1025,
          accountName: '미광종합물류',
          productCode: '12345678',
          productName: '맛있는부대찌개',
          dateType: 'MANUFACTURE_DATE',
          date: '2026-06-01',
          claimType1: 'A',
          claimType2: 'AA',
          defectDescription: '벌레 발견',
          defectQuantity: 3,
          purchaseAmount: 5000,
          purchaseMethodCode: 'PM01',
          requestTypeCode: 'RT01',
        ));

        final form = container.read(claimRegisterProvider).form!;
        expect(form.accountId, 1025);
        expect(form.productName, '맛있는부대찌개');
        expect(form.dateType, ClaimDateType.manufactureDate);
        expect(form.categoryId, 'A');
        expect(form.categoryName, '이물'); // form-data 해석
        expect(form.subcategoryName, '벌레');
        expect(form.defectDescription, '벌레 발견');
        expect(form.defectQuantity, 3);
        expect(form.purchaseMethodName, '대형마트');
        expect(form.requestTypeName, '교환');
      });
    });
  });
}

// ──────────────────────────────────────────────────────────────────
// Fake Repository
// ──────────────────────────────────────────────────────────────────

class FakeClaimRepository implements ClaimRepository {
  int registerClaimCalls = 0;
  int getFormCalls = 0;

  ClaimRegisterResult? registerResultToReturn;
  ClaimFormData? formDataToReturn;

  bool shouldThrowError = false;

  @override
  Future<ClaimRegisterResult> registerClaim(ClaimRegisterForm form) async {
    registerClaimCalls++;

    if (shouldThrowError) {
      throw Exception('등록 실패');
    }

    return registerResultToReturn!;
  }

  @override
  Future<ClaimFormEntry> getForm() async {
    getFormCalls++;

    if (shouldThrowError) {
      throw Exception('폼 로드 실패');
    }

    return ClaimFormEntry(formData: formDataToReturn!, draft: draftToReturn);
  }

  @override
  Future<List<ClaimListItem>> getClaims({
    String? startDate,
    String? endDate,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<ClaimDetail> getClaimDetail(int claimId) async {
    throw UnimplementedError();
  }

  int saveDraftCalls = 0;
  int deleteDraftCalls = 0;
  ClaimDraft? draftToReturn;

  @override
  Future<void> saveDraft(ClaimRegisterForm? form) async {
    saveDraftCalls++;
    if (shouldThrowError) {
      throw Exception('임시저장 실패');
    }
  }

  @override
  Future<void> deleteDraft() async {
    deleteDraftCalls++;
  }
}

// ──────────────────────────────────────────────────────────────────
// Sample test data
// ──────────────────────────────────────────────────────────────────

final _sampleFormData = ClaimFormData(
  categories: [
    ClaimCategory(
      id: 'A',
      name: '이물',
      subcategories: [
        const ClaimSubcategory(id: 'AA', name: '벌레'),
        const ClaimSubcategory(id: 'AB', name: '금속'),
      ],
    ),
    ClaimCategory(
      id: 'B',
      name: '변질/변패',
      subcategories: [
        const ClaimSubcategory(id: 'BA', name: '맛 변질'),
      ],
    ),
  ],
  purchaseMethods: const [
    PurchaseMethod(code: 'PM01', name: '대형마트'),
    PurchaseMethod(code: 'PM02', name: '온라인'),
  ],
  requestTypes: const [
    ClaimRequestType(code: 'RT01', name: '교환'),
    ClaimRequestType(code: 'RT02', name: '환불'),
  ],
);

final _sampleRegisterResult = ClaimRegisterResult(
  id: 100,
  accountName: '미광종합물류',
  accountId: 1025,
  productName: '맛있는부대찌개라양념140G',
  productCode: '12345678',
  createdAt: DateTime(2026, 2, 11, 10, 30),
);
