import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/domain/entities/my_store.dart';
import 'package:mobile/domain/repositories/my_store_repository.dart';
import 'package:mobile/domain/usecases/get_field_types_usecase.dart';
import 'package:mobile/domain/usecases/get_my_stores.dart';
import 'package:mobile/domain/usecases/get_themes_usecase.dart';
import 'package:mobile/domain/usecases/register_inspection_usecase.dart';
import 'package:mobile/presentation/providers/inspection_register_provider.dart';

// Mock classes
class MockGetThemesUseCase implements GetThemesUseCase {
  @override
  Future<List<InspectionTheme>> call() async {
    await Future.delayed(const Duration(milliseconds: 100));
    return [
      InspectionTheme(
        id: 1,
        name: '8월 테마',
        startDate: DateTime(2020, 8, 1),
        endDate: DateTime(2020, 8, 31),
      ),
    ];
  }
}

class MockGetFieldTypesUseCase implements GetFieldTypesUseCase {
  @override
  Future<List<InspectionFieldType>> call() async {
    await Future.delayed(const Duration(milliseconds: 100));
    return [
      const InspectionFieldType(code: 'FT01', name: '본매대'),
    ];
  }
}

class MockGetMyStores implements GetMyStores {
  @override
  Future<MyStoreListResult> call() async {
    await Future.delayed(const Duration(milliseconds: 100));
    return const MyStoreListResult(
      stores: [
        MyStore(
          storeId: 100,
          storeName: '이마트 죽전점',
          storeCode: 'S100',
          address: '서울시 강남구',
          representativeName: '홍길동',
        ),
      ],
      totalCount: 1,
    );
  }
}

class MockRegisterInspectionUseCase implements RegisterInspectionUseCase {
  bool shouldFail = false;

  @override
  Future<InspectionListItem> call(InspectionRegisterForm form) async {
    await Future.delayed(const Duration(milliseconds: 100));
    if (shouldFail) {
      throw Exception('등록 실패');
    }
    // 등록 성공 시 생성된 항목 반환
    return InspectionListItem(
      id: 1,
      category: form.category,
      storeName: '이마트 죽전점',
      storeId: form.storeId,
      inspectionDate: form.inspectionDate,
      fieldType: '본매대',
      fieldTypeCode: form.fieldTypeCode,
    );
  }
}

void main() {
  group('InspectionRegisterNotifier', () {
    late InspectionRegisterNotifier notifier;
    late MockRegisterInspectionUseCase mockRegisterUseCase;

    setUp(() {
      mockRegisterUseCase = MockRegisterInspectionUseCase();
      notifier = InspectionRegisterNotifier(
        getThemes: MockGetThemesUseCase(),
        getFieldTypes: MockGetFieldTypesUseCase(),
        getMyStores: MockGetMyStores(),
        registerInspection: mockRegisterUseCase,
      );
    });

    test('초기 상태는 initial()이다', () {
      // When
      final state = notifier.state;

      // Then
      expect(state.isLoading, false);
      expect(state.form, isNotNull);
      expect(state.themes, isEmpty);
      expect(state.fieldTypes, isEmpty);
      expect(state.stores, isEmpty);
    });

    test('initialize()는 데이터를 로드한다', () async {
      // When
      await notifier.initialize();

      // Then
      expect(notifier.state.isLoading, false);
      expect(notifier.state.themes.length, 1);
      expect(notifier.state.fieldTypes.length, 1);
      expect(notifier.state.stores.length, 1);
      expect(notifier.state.stores[100], '이마트 죽전점');
    });

    test('selectTheme()는 테마를 선택한다', () async {
      // Given
      await notifier.initialize();
      final theme = notifier.state.themes.first;

      // When
      notifier.selectTheme(theme);

      // Then
      expect(notifier.state.form!.themeId, theme.id);
      expect(notifier.state.selectedTheme, theme);
    });

    test('changeCategory()는 분류를 변경하고 관련 필드를 초기화한다', () async {
      // Given
      await notifier.initialize();
      notifier.selectProduct('P001', '진라면'); // 자사 제품 선택

      // When
      notifier.changeCategory(InspectionCategory.COMPETITOR);

      // Then
      expect(notifier.state.category, InspectionCategory.COMPETITOR);
      expect(notifier.state.form!.productCode, null);
      expect(notifier.state.selectedProductName, null);
    });

    test('selectStore()는 거래처를 선택한다', () async {
      // Given
      await notifier.initialize();

      // When
      notifier.selectStore(100, '이마트 죽전점');

      // Then
      expect(notifier.state.form!.storeId, 100);
      expect(notifier.state.selectedStoreName, '이마트 죽전점');
    });

    test('updateInspectionDate()는 점검일을 변경한다', () async {
      // Given
      await notifier.initialize();
      final newDate = DateTime(2020, 8, 13);

      // When
      notifier.updateInspectionDate(newDate);

      // Then
      expect(notifier.state.form!.inspectionDate, newDate);
    });

    test('selectFieldType()는 현장 유형을 선택한다', () async {
      // Given
      await notifier.initialize();
      final fieldType = notifier.state.fieldTypes.first;

      // When
      notifier.selectFieldType(fieldType);

      // Then
      expect(notifier.state.form!.fieldTypeCode, fieldType.code);
      expect(notifier.state.selectedFieldType, fieldType);
    });

    test('updateDescription()는 설명을 입력한다 (자사)', () async {
      // Given
      await notifier.initialize();
      const description = '냉장고 앞 본매대';

      // When
      notifier.updateDescription(description);

      // Then
      expect(notifier.state.form!.description, description);
    });

    test('selectProduct()는 제품을 선택한다 (자사)', () async {
      // Given
      await notifier.initialize();

      // When
      notifier.selectProduct('P001', '진라면');

      // Then
      expect(notifier.state.form!.productCode, 'P001');
      expect(notifier.state.selectedProductName, '진라면');
    });

    test('updateCompetitorName()는 경쟁사명을 입력한다 (경쟁사)', () async {
      // Given
      await notifier.initialize();
      notifier.changeCategory(InspectionCategory.COMPETITOR);

      // When
      notifier.updateCompetitorName('농심');

      // Then
      expect(notifier.state.form!.competitorName, '농심');
    });

    test('updateCompetitorActivity()는 경쟁사 활동 내용을 입력한다 (경쟁사)', () async {
      // Given
      await notifier.initialize();
      notifier.changeCategory(InspectionCategory.COMPETITOR);

      // When
      notifier.updateCompetitorActivity('시식 행사');

      // Then
      expect(notifier.state.form!.competitorActivity, '시식 행사');
    });

    test('updateCompetitorTasting()는 시식 여부를 변경한다 (경쟁사)', () async {
      // Given
      await notifier.initialize();
      notifier.changeCategory(InspectionCategory.COMPETITOR);

      // When
      notifier.updateCompetitorTasting(true);

      // Then
      expect(notifier.state.form!.competitorTasting, true);
      expect(notifier.state.hasTasting, true);
    });

    test('updateCompetitorTasting(false)는 시식 관련 필드를 초기화한다', () async {
      // Given
      await notifier.initialize();
      notifier.changeCategory(InspectionCategory.COMPETITOR);
      notifier.updateCompetitorTasting(true);
      notifier.updateCompetitorProductName('신라면 블랙');
      notifier.updateCompetitorProductPrice(5000);
      notifier.updateCompetitorSalesQuantity(50);

      // When
      notifier.updateCompetitorTasting(false);

      // Then
      expect(notifier.state.form!.competitorTasting, false);
      expect(notifier.state.form!.competitorProductName, null);
      expect(notifier.state.form!.competitorProductPrice, null);
      expect(notifier.state.form!.competitorSalesQuantity, null);
    });

    test('updateCompetitorProductName()는 경쟁사 상품명을 입력한다 (시식=예)', () async {
      // Given
      await notifier.initialize();
      notifier.changeCategory(InspectionCategory.COMPETITOR);
      notifier.updateCompetitorTasting(true);

      // When
      notifier.updateCompetitorProductName('신라면 블랙');

      // Then
      expect(notifier.state.form!.competitorProductName, '신라면 블랙');
    });

    test('updateCompetitorProductPrice()는 제품 가격을 입력한다 (시식=예)', () async {
      // Given
      await notifier.initialize();
      notifier.changeCategory(InspectionCategory.COMPETITOR);
      notifier.updateCompetitorTasting(true);

      // When
      notifier.updateCompetitorProductPrice(5000);

      // Then
      expect(notifier.state.form!.competitorProductPrice, 5000);
    });

    test('updateCompetitorSalesQuantity()는 판매 수량을 입력한다 (시식=예)', () async {
      // Given
      await notifier.initialize();
      notifier.changeCategory(InspectionCategory.COMPETITOR);
      notifier.updateCompetitorTasting(true);

      // When
      notifier.updateCompetitorSalesQuantity(50);

      // Then
      expect(notifier.state.form!.competitorSalesQuantity, 50);
    });

    test('addPhoto()는 사진을 추가한다', () async {
      // Given
      await notifier.initialize();
      final photo = File('test.jpg');

      // When
      notifier.addPhoto(photo);

      // Then
      expect(notifier.state.photoCount, 1);
      expect(notifier.state.form!.photos.first, photo);
    });

    test('addPhoto()는 최대 2장까지만 추가할 수 있다', () async {
      // Given
      await notifier.initialize();
      notifier.addPhoto(File('test1.jpg'));
      notifier.addPhoto(File('test2.jpg'));

      // When
      notifier.addPhoto(File('test3.jpg')); // 3번째 시도

      // Then
      expect(notifier.state.photoCount, 2); // 2장만 유지
    });

    test('removePhoto()는 사진을 삭제한다', () async {
      // Given
      await notifier.initialize();
      notifier.addPhoto(File('test1.jpg'));
      notifier.addPhoto(File('test2.jpg'));

      // When
      notifier.removePhoto(0); // 첫 번째 사진 삭제

      // Then
      expect(notifier.state.photoCount, 1);
      expect(notifier.state.form!.photos.first.path, 'test2.jpg');
    });

    test('submit()는 유효성 검증에 실패하면 false를 반환한다', () async {
      // Given
      await notifier.initialize();
      // 필수 항목 미입력

      // When
      final result = await notifier.submit();

      // Then
      expect(result, false);
      expect(notifier.state.errorMessage, isNotNull);
    });

    test('submit()는 유효한 폼을 등록한다', () async {
      // Given
      await notifier.initialize();
      notifier.selectTheme(notifier.state.themes.first);
      notifier.selectStore(100, '이마트 죽전점');
      notifier.selectFieldType(notifier.state.fieldTypes.first);
      notifier.selectProduct('P001', '진라면');
      notifier.addPhoto(File('test.jpg'));

      // When
      final result = await notifier.submit();

      // Then
      expect(result, true);
      expect(notifier.state.errorMessage, null);
    });

    test('submit()는 API 실패 시 false를 반환한다', () async {
      // Given
      mockRegisterUseCase.shouldFail = true;
      await notifier.initialize();
      notifier.selectTheme(notifier.state.themes.first);
      notifier.selectStore(100, '이마트 죽전점');
      notifier.selectFieldType(notifier.state.fieldTypes.first);
      notifier.selectProduct('P001', '진라면');
      notifier.addPhoto(File('test.jpg'));

      // When
      final result = await notifier.submit();

      // Then
      expect(result, false);
      expect(notifier.state.errorMessage, '등록 실패');
    });

    test('clearError()는 에러 메시지를 초기화한다', () async {
      // Given
      await notifier.initialize();
      await notifier.submit(); // 유효성 검증 실패로 에러 발생

      // When
      notifier.clearError();

      // Then
      expect(notifier.state.errorMessage, null);
    });
  });
}
