import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/repositories/inspection_repository.dart';
import 'package:mobile/domain/usecases/get_inspection_list_usecase.dart';

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

InspectionListItem _createTestItem({
  int id = 1,
  InspectionCategory category = InspectionCategory.OWN,
  String storeName = '롯데마트 사상',
  int storeId = 2001,
  DateTime? inspectionDate,
  String fieldType = '시식',
  String fieldTypeCode = 'FT02',
}) {
  return InspectionListItem(
    id: id,
    category: category,
    storeName: storeName,
    storeId: storeId,
    inspectionDate: inspectionDate ?? DateTime(2020, 8, 18),
    fieldType: fieldType,
    fieldTypeCode: fieldTypeCode,
  );
}

void main() {
  late _MockInspectionRepository repository;
  late GetInspectionListUseCase useCase;

  setUp(() {
    repository = _MockInspectionRepository();
    useCase = GetInspectionListUseCase(repository);
  });

  group('GetInspectionListUseCase', () {
    test('기본 필터로 현장 점검 목록을 조회한다', () async {
      // Given
      final filter = InspectionFilter(
        fromDate: DateTime(2020, 8, 1),
        toDate: DateTime(2020, 8, 31),
      );
      final expectedList = [
        _createTestItem(id: 1, category: InspectionCategory.OWN),
        _createTestItem(id: 2, category: InspectionCategory.COMPETITOR),
      ];
      repository.listResult = expectedList;

      // When
      final result = await useCase.call(filter);

      // Then
      expect(result, expectedList);
      expect(result.length, 2);
    });

    test('거래처 필터로 현장 점검 목록을 조회한다', () async {
      // Given
      final filter = InspectionFilter(
        storeId: 2001,
        fromDate: DateTime(2020, 8, 1),
        toDate: DateTime(2020, 8, 31),
      );
      final expectedList = [
        _createTestItem(id: 1, storeId: 2001, storeName: '롯데마트 사상'),
      ];
      repository.listResult = expectedList;

      // When
      final result = await useCase.call(filter);

      // Then
      expect(result, expectedList);
      expect(result.length, 1);
      expect(result[0].storeId, 2001);
    });

    test('분류 필터로 현장 점검 목록을 조회한다', () async {
      // Given
      final filter = InspectionFilter(
        category: InspectionCategory.OWN,
        fromDate: DateTime(2020, 8, 1),
        toDate: DateTime(2020, 8, 31),
      );
      final expectedList = [
        _createTestItem(id: 1, category: InspectionCategory.OWN),
        _createTestItem(id: 2, category: InspectionCategory.OWN),
      ];
      repository.listResult = expectedList;

      // When
      final result = await useCase.call(filter);

      // Then
      expect(result, expectedList);
      expect(result.every((item) => item.category == InspectionCategory.OWN), true);
    });

    test('빈 목록이 반환될 수 있다', () async {
      // Given
      final filter = InspectionFilter(
        fromDate: DateTime(2020, 8, 1),
        toDate: DateTime(2020, 8, 31),
      );
      repository.listResult = [];

      // When
      final result = await useCase.call(filter);

      // Then
      expect(result, isEmpty);
    });

    test('날짜 범위가 유효하지 않으면 예외를 던진다 (시작일 > 종료일)', () async {
      // Given
      final filter = InspectionFilter(
        fromDate: DateTime(2020, 8, 31),
        toDate: DateTime(2020, 8, 1),
      );

      // When & Then
      expect(
        () => useCase.call(filter),
        throwsException,
      );
    });

    test('날짜 범위가 동일하면 예외를 던지지 않는다 (시작일 == 종료일)', () async {
      // Given
      final filter = InspectionFilter(
        fromDate: DateTime(2020, 8, 1),
        toDate: DateTime(2020, 8, 1),
      );
      repository.listResult = [];

      // When
      final result = await useCase.call(filter);

      // Then
      expect(result, isEmpty);
    });

    test('Repository에서 예외가 발생하면 전파된다', () async {
      // Given
      final filter = InspectionFilter(
        fromDate: DateTime(2020, 8, 1),
        toDate: DateTime(2020, 8, 31),
      );
      repository.error = Exception('Network error');

      // When & Then
      expect(
        () => useCase.call(filter),
        throwsException,
      );
    });
  });
}
