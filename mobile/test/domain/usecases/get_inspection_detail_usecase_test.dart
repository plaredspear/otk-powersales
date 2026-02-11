import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/repositories/inspection_repository.dart';
import 'package:mobile/domain/usecases/get_inspection_detail_usecase.dart';

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

InspectionDetail _createTestDetail({
  int id = 1,
  InspectionCategory category = InspectionCategory.OWN,
  String storeName = '롯데마트 사상',
  int storeId = 2001,
  String themeName = '테마',
  int themeId = 10,
  DateTime? inspectionDate,
  String fieldType = '시식',
  String fieldTypeCode = 'FT02',
  String? description,
  String? productCode,
  String? productName,
  List<InspectionPhoto>? photos,
  DateTime? createdAt,
}) {
  return InspectionDetail(
    id: id,
    category: category,
    storeName: storeName,
    storeId: storeId,
    themeName: themeName,
    themeId: themeId,
    inspectionDate: inspectionDate ?? DateTime(2020, 8, 18),
    fieldType: fieldType,
    fieldTypeCode: fieldTypeCode,
    description: description,
    productCode: productCode,
    productName: productName,
    photos: photos ?? [],
    createdAt: createdAt ?? DateTime(2020, 8, 18, 10, 30, 0),
  );
}

void main() {
  late _MockInspectionRepository repository;
  late GetInspectionDetailUseCase useCase;

  setUp(() {
    repository = _MockInspectionRepository();
    useCase = GetInspectionDetailUseCase(repository);
  });

  group('GetInspectionDetailUseCase', () {
    test('유효한 ID로 자사 점검 상세를 조회한다', () async {
      // Given
      final expectedDetail = _createTestDetail(
        id: 1,
        category: InspectionCategory.OWN,
        productCode: '12345678',
        productName: '제품명',
      );
      repository.detailResult = expectedDetail;

      // When
      final result = await useCase.call(1);

      // Then
      expect(result, expectedDetail);
      expect(result.id, 1);
      expect(result.category, InspectionCategory.OWN);
      expect(result.productCode, '12345678');
    });

    test('유효한 ID로 경쟁사 점검 상세를 조회한다', () async {
      // Given
      final expectedDetail = _createTestDetail(
        id: 2,
        category: InspectionCategory.COMPETITOR,
        storeName: '이마트',
      );
      repository.detailResult = expectedDetail;

      // When
      final result = await useCase.call(2);

      // Then
      expect(result, expectedDetail);
      expect(result.id, 2);
      expect(result.category, InspectionCategory.COMPETITOR);
    });

    test('ID가 0 이하이면 예외를 던진다', () async {
      // When & Then
      expect(
        () => useCase.call(0),
        throwsException,
      );
      expect(
        () => useCase.call(-1),
        throwsException,
      );
    });

    test('Repository에서 예외가 발생하면 전파된다', () async {
      // Given
      repository.error = Exception('Not found');

      // When & Then
      expect(
        () => useCase.call(1),
        throwsException,
      );
    });
  });
}
