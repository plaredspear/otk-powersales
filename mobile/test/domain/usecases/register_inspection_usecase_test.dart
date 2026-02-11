import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/repositories/inspection_repository.dart';
import 'package:mobile/domain/usecases/register_inspection_usecase.dart';

class _MockInspectionRepository implements InspectionRepository {
  List<InspectionListItem>? listResult;
  InspectionDetail? detailResult;
  InspectionListItem? itemResult;
  List<InspectionTheme>? themesResult;
  List<InspectionFieldType>? fieldTypesResult;
  Exception? error;

  @override
  Future<List<InspectionListItem>> getInspectionList(InspectionFilter filter) async {
    if (error != null) throw error!;
    return listResult!;
  }

  @override
  Future<InspectionDetail> getInspectionDetail(int id) async {
    if (error != null) throw error!;
    return detailResult!;
  }

  @override
  Future<InspectionListItem> registerInspection(InspectionRegisterForm form) async {
    if (error != null) throw error!;
    return itemResult!;
  }

  @override
  Future<List<InspectionTheme>> getThemes() async {
    if (error != null) throw error!;
    return themesResult!;
  }

  @override
  Future<List<InspectionFieldType>> getFieldTypes() async {
    if (error != null) throw error!;
    return fieldTypesResult!;
  }
}

File _createMockFile(String path) {
  return File(path);
}

InspectionRegisterForm _createValidOwnForm() {
  return InspectionRegisterForm(
    themeId: 10,
    category: InspectionCategory.OWN,
    storeId: 3001,
    inspectionDate: DateTime(2020, 8, 19),
    fieldTypeCode: 'FT01',
    productCode: '12345678',
    photos: [_createMockFile('/path/to/photo1.jpg')],
  );
}

InspectionRegisterForm _createValidCompetitorForm() {
  return InspectionRegisterForm(
    themeId: 11,
    category: InspectionCategory.COMPETITOR,
    storeId: 2001,
    inspectionDate: DateTime(2020, 8, 25),
    fieldTypeCode: 'FT02',
    competitorName: '경쟁사1',
    competitorActivity: '활동1',
    competitorTasting: false,
    photos: [_createMockFile('/path/to/photo1.jpg')],
  );
}

void main() {
  late _MockInspectionRepository repository;
  late RegisterInspectionUseCase useCase;

  setUp(() {
    repository = _MockInspectionRepository();
    useCase = RegisterInspectionUseCase(repository);
  });

  group('RegisterInspectionUseCase', () {
    test('유효한 자사 점검 폼으로 등록에 성공한다', () async {
      // Given
      final form = _createValidOwnForm();
      final expectedResult = InspectionListItem(
        id: 1,
        category: InspectionCategory.OWN,
        storeName: '이마트',
        storeId: 3001,
        inspectionDate: DateTime(2020, 8, 19),
        fieldType: '본매대',
        fieldTypeCode: 'FT01',
      );
      repository.itemResult = expectedResult;

      // When
      final result = await useCase.call(form);

      // Then
      expect(result, expectedResult);
      expect(result.category, InspectionCategory.OWN);
    });

    test('유효한 경쟁사 점검 폼으로 등록에 성공한다', () async {
      // Given
      final form = _createValidCompetitorForm();
      final expectedResult = InspectionListItem(
        id: 2,
        category: InspectionCategory.COMPETITOR,
        storeName: '롯데마트',
        storeId: 2001,
        inspectionDate: DateTime(2020, 8, 25),
        fieldType: '시식',
        fieldTypeCode: 'FT02',
      );
      repository.itemResult = expectedResult;

      // When
      final result = await useCase.call(form);

      // Then
      expect(result, expectedResult);
      expect(result.category, InspectionCategory.COMPETITOR);
    });

    test('필수 항목이 없으면 예외를 던진다', () async {
      // Given
      final form = InspectionRegisterForm(
        themeId: 0, // Invalid
        category: InspectionCategory.OWN,
        storeId: 0, // Invalid
        inspectionDate: DateTime(2020, 8, 19),
        fieldTypeCode: '', // Invalid
        photos: [], // Invalid
      );

      // When & Then
      expect(
        () => useCase.call(form),
        throwsException,
      );
    });

    test('자사 점검에서 제품 코드가 없으면 예외를 던진다', () async {
      // Given
      final form = InspectionRegisterForm(
        themeId: 10,
        category: InspectionCategory.OWN,
        storeId: 3001,
        inspectionDate: DateTime(2020, 8, 19),
        fieldTypeCode: 'FT01',
        // productCode missing
        photos: [_createMockFile('/path/to/photo1.jpg')],
      );

      // When & Then
      expect(
        () => useCase.call(form),
        throwsException,
      );
    });

    test('경쟁사 점검에서 필수 정보가 없으면 예외를 던진다', () async {
      // Given
      final form = InspectionRegisterForm(
        themeId: 11,
        category: InspectionCategory.COMPETITOR,
        storeId: 2001,
        inspectionDate: DateTime(2020, 8, 25),
        fieldTypeCode: 'FT02',
        // competitorName, competitorActivity, competitorTasting missing
        photos: [_createMockFile('/path/to/photo1.jpg')],
      );

      // When & Then
      expect(
        () => useCase.call(form),
        throwsException,
      );
    });

    test('Repository에서 예외가 발생하면 전파된다', () async {
      // Given
      final form = _createValidOwnForm();
      repository.error = Exception('Network error');

      // When & Then
      expect(
        () => useCase.call(form),
        throwsException,
      );
    });
  });
}
