import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/domain/entities/claim_category.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/domain/entities/claim_form.dart';
import 'package:mobile/domain/entities/claim_form_data.dart';
import 'package:mobile/domain/entities/claim_result.dart';
import 'package:mobile/domain/repositories/claim_repository.dart';
import 'package:mobile/domain/usecases/get_claim_form_data_usecase.dart';
import 'package:mobile/domain/usecases/register_claim_usecase.dart';
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

    group('loadFormData', () {
      test('폼 초기화 데이터를 로드한다', () async {
        // Given
        fakeRepository.formDataToReturn = _sampleFormData;

        final notifier = container.read(claimRegisterProvider.notifier);

        // When
        await notifier.loadFormData();

        // Then
        final state = container.read(claimRegisterProvider);
        expect(state.formData, isNotNull);
        expect(state.formData!.categories.length, 2);
        expect(state.formData!.purchaseMethods.length, 2);
        expect(state.formData!.requestTypes.length, 2);
        expect(state.loading, false);
        expect(state.error, isNull);
      });

      test('로드 실패 시 에러를 설정한다', () async {
        // Given
        fakeRepository.shouldThrowError = true;

        final notifier = container.read(claimRegisterProvider.notifier);

        // When
        await notifier.loadFormData();

        // Then
        final state = container.read(claimRegisterProvider);
        expect(state.formData, isNull);
        expect(state.loading, false);
        expect(state.error, isNotNull);
      });
    });

    group('폼 필드 업데이트', () {
      test('거래처를 선택한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.selectStore(1025, '미광종합물류');

        final state = container.read(claimRegisterProvider);
        expect(state.form, isNotNull);
        expect(state.form!.storeId, 1025);
        expect(state.form!.storeName, '미광종합물류');
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
        notifier.selectSubcategory(101, '벌레');

        // When: 종류1 변경
        notifier.selectCategory(1, '이물');

        // Then: 종류2 초기화됨
        final state = container.read(claimRegisterProvider);
        expect(state.form!.categoryId, 1);
        expect(state.form!.categoryName, '이물');
        expect(state.form!.subcategoryId, 0);
        expect(state.form!.subcategoryName, '');
      });

      test('클레임 종류2를 선택한다', () {
        final notifier = container.read(claimRegisterProvider.notifier);

        notifier.selectSubcategory(101, '벌레');

        final state = container.read(claimRegisterProvider);
        expect(state.form!.subcategoryId, 101);
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
        notifier.selectStore(1025, '미광종합물류');
        notifier.selectProduct('12345678', '맛있는부대찌개라양념140G');
        notifier.selectDateType(ClaimDateType.expiryDate);
        notifier.selectDate(DateTime(2026, 2, 20));
        notifier.selectCategory(1, '이물');
        notifier.selectSubcategory(101, '벌레');
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
        notifier.selectStore(1025, '미광종합물류');
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
        notifier.selectStore(1025, '미광종합물류');
        notifier.selectProduct('12345678', '맛있는부대찌개라양념140G');
        notifier.selectDateType(ClaimDateType.expiryDate);
        notifier.selectDate(DateTime(2026, 2, 20));
        notifier.selectCategory(1, '이물');
        notifier.selectSubcategory(101, '벌레');
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
      notifier.selectStore(1025, '미광종합물류');
      notifier.selectProduct('12345678', '맛있는부대찌개라양념140G');

      // When
      notifier.clearForm();

      // Then
      final state = container.read(claimRegisterProvider);
      expect(state.form, isNull);
      expect(state.error, isNull);
    });
  });
}

// ──────────────────────────────────────────────────────────────────
// Fake Repository
// ──────────────────────────────────────────────────────────────────

class FakeClaimRepository implements ClaimRepository {
  int registerClaimCalls = 0;
  int getFormDataCalls = 0;

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
  Future<ClaimFormData> getFormData() async {
    getFormDataCalls++;

    if (shouldThrowError) {
      throw Exception('폼 데이터 로드 실패');
    }

    return formDataToReturn!;
  }
}

// ──────────────────────────────────────────────────────────────────
// Sample test data
// ──────────────────────────────────────────────────────────────────

final _sampleFormData = ClaimFormData(
  categories: [
    ClaimCategory(
      id: 1,
      name: '이물',
      subcategories: [
        const ClaimSubcategory(id: 101, name: '벌레'),
        const ClaimSubcategory(id: 102, name: '금속'),
      ],
    ),
    ClaimCategory(
      id: 2,
      name: '변질/변패',
      subcategories: [
        const ClaimSubcategory(id: 201, name: '맛 변질'),
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
  storeName: '미광종합물류',
  storeId: 1025,
  productName: '맛있는부대찌개라양념140G',
  productCode: '12345678',
  createdAt: DateTime(2026, 2, 11, 10, 30),
);
