import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/presentation/providers/inspection_register_state.dart';

void main() {
  group('InspectionRegisterState', () {
    test('initial() 상태가 올바르게 생성된다', () {
      // Given & When
      final state = InspectionRegisterState.initial();

      // Then
      expect(state.isLoading, false);
      expect(state.errorMessage, null);
      expect(state.form, isNotNull);
      expect(state.form!.category, InspectionCategory.OWN);
      expect(state.form!.themeId, 0);
      expect(state.form!.storeId, 0);
      expect(state.form!.fieldTypeCode, '');
      expect(state.form!.photos, isEmpty);
      expect(state.themes, isEmpty);
      expect(state.fieldTypes, isEmpty);
      expect(state.stores, isEmpty);
    });

    test('toLoading()은 로딩 상태로 전환한다', () {
      // Given
      final state = InspectionRegisterState.initial();

      // When
      final loadingState = state.toLoading();

      // Then
      expect(loadingState.isLoading, true);
      expect(loadingState.errorMessage, null);
    });

    test('toError()는 에러 상태로 전환한다', () {
      // Given
      final state = InspectionRegisterState.initial();
      const errorMessage = '등록 실패';

      // When
      final errorState = state.toError(errorMessage);

      // Then
      expect(errorState.isLoading, false);
      expect(errorState.errorMessage, errorMessage);
    });

    test('toLoaded()는 데이터 로드 완료 상태로 전환한다', () {
      // Given
      final state = InspectionRegisterState.initial().toLoading();
      final themes = [
        InspectionTheme(
          id: 1,
          name: '테마1',
          startDate: DateTime(2020, 1, 1),
          endDate: DateTime(2020, 12, 31),
        ),
      ];
      final fieldTypes = [
        const InspectionFieldType(code: 'FT01', name: '본매대'),
      ];
      final stores = {100: '이마트 죽전점'};

      // When
      final loadedState = state.toLoaded(
        themes: themes,
        fieldTypes: fieldTypes,
        stores: stores,
      );

      // Then
      expect(loadedState.isLoading, false);
      expect(loadedState.errorMessage, null);
      expect(loadedState.themes, themes);
      expect(loadedState.fieldTypes, fieldTypes);
      expect(loadedState.stores, stores);
    });

    test('isOwn getter가 올바르게 동작한다', () {
      // Given
      final state = InspectionRegisterState.initial();

      // When & Then
      expect(state.isOwn, true);
      expect(state.isCompetitor, false);
    });

    test('isCompetitor getter가 올바르게 동작한다', () {
      // Given
      final form = InspectionRegisterForm(
        themeId: 1,
        category: InspectionCategory.COMPETITOR,
        storeId: 100,
        inspectionDate: DateTime(2020, 8, 13),
        fieldTypeCode: 'FT01',
        photos: const [],
      );
      final state = InspectionRegisterState.initial().copyWith(form: form);

      // When & Then
      expect(state.isOwn, false);
      expect(state.isCompetitor, true);
    });

    test('selectedTheme getter가 올바르게 동작한다', () {
      // Given
      final theme = InspectionTheme(
        id: 1,
        name: '8월 테마',
        startDate: DateTime(2020, 8, 1),
        endDate: DateTime(2020, 8, 31),
      );
      final form = InspectionRegisterForm(
        themeId: 1,
        category: InspectionCategory.OWN,
        storeId: 100,
        inspectionDate: DateTime(2020, 8, 13),
        fieldTypeCode: 'FT01',
        photos: const [],
      );
      final state = InspectionRegisterState.initial().copyWith(
        form: form,
        themes: [theme],
      );

      // When & Then
      expect(state.selectedTheme, theme);
    });

    test('selectedStoreName getter가 올바르게 동작한다', () {
      // Given
      final form = InspectionRegisterForm(
        themeId: 1,
        category: InspectionCategory.OWN,
        storeId: 100,
        inspectionDate: DateTime(2020, 8, 13),
        fieldTypeCode: 'FT01',
        photos: const [],
      );
      final state = InspectionRegisterState.initial().copyWith(
        form: form,
        stores: {100: '이마트 죽전점'},
      );

      // When & Then
      expect(state.selectedStoreName, '이마트 죽전점');
    });

    test('selectedFieldType getter가 올바르게 동작한다', () {
      // Given
      final fieldType = const InspectionFieldType(code: 'FT01', name: '본매대');
      final form = InspectionRegisterForm(
        themeId: 1,
        category: InspectionCategory.OWN,
        storeId: 100,
        inspectionDate: DateTime(2020, 8, 13),
        fieldTypeCode: 'FT01',
        photos: const [],
      );
      final state = InspectionRegisterState.initial().copyWith(
        form: form,
        fieldTypes: [fieldType],
      );

      // When & Then
      expect(state.selectedFieldType, fieldType);
    });

    test('photoCount getter가 올바르게 동작한다', () {
      // Given
      final form = InspectionRegisterForm(
        themeId: 1,
        category: InspectionCategory.OWN,
        storeId: 100,
        inspectionDate: DateTime(2020, 8, 13),
        fieldTypeCode: 'FT01',
        photos: [File('photo1.jpg')],
      );
      final state = InspectionRegisterState.initial().copyWith(form: form);

      // When & Then
      expect(state.photoCount, 1);
    });

    test('canAddPhoto getter가 최대 2장 제한을 확인한다', () {
      // Given
      final form1 = InspectionRegisterForm(
        themeId: 1,
        category: InspectionCategory.OWN,
        storeId: 100,
        inspectionDate: DateTime(2020, 8, 13),
        fieldTypeCode: 'FT01',
        photos: [File('photo1.jpg')],
      );
      final state1 = InspectionRegisterState.initial().copyWith(form: form1);

      final form2 = InspectionRegisterForm(
        themeId: 1,
        category: InspectionCategory.OWN,
        storeId: 100,
        inspectionDate: DateTime(2020, 8, 13),
        fieldTypeCode: 'FT01',
        photos: [File('photo1.jpg'), File('photo2.jpg')],
      );
      final state2 = InspectionRegisterState.initial().copyWith(form: form2);

      // When & Then
      expect(state1.canAddPhoto, true); // 1장 -> 추가 가능
      expect(state2.canAddPhoto, false); // 2장 -> 추가 불가
    });

    test('validate()는 폼 유효성을 검증한다', () {
      // Given: 빈 폼
      final state = InspectionRegisterState.initial();

      // When
      final result = state.validate();

      // Then: 필수 항목 미입력으로 실패
      expect(result.isValid, false);
      expect(result.errors, isNotEmpty);
    });

    test('copyWith()로 특정 필드만 변경할 수 있다', () {
      // Given
      final state = InspectionRegisterState.initial();
      const newError = '에러 발생';

      // When
      final newState = state.copyWith(errorMessage: newError);

      // Then
      expect(newState.errorMessage, newError);
      expect(newState.isLoading, state.isLoading);
      expect(newState.form, state.form);
    });

    test('copyWith(clearError: true)로 에러를 초기화할 수 있다', () {
      // Given
      final state = InspectionRegisterState.initial().copyWith(
        errorMessage: '에러 발생',
      );

      // When
      final newState = state.copyWith(clearError: true);

      // Then
      expect(newState.errorMessage, null);
    });

    test('copyWith(clearProductName: true)로 제품명을 초기화할 수 있다', () {
      // Given
      final state = InspectionRegisterState.initial().copyWith(
        selectedProductName: '진라면',
      );

      // When
      final newState = state.copyWith(clearProductName: true);

      // Then
      expect(newState.selectedProductName, null);
    });
  });
}
