import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/repositories/inspection_repository.dart';
import 'package:mobile/domain/usecases/get_field_types_usecase.dart';

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

void main() {
  late _MockInspectionRepository repository;
  late GetFieldTypesUseCase useCase;

  setUp(() {
    repository = _MockInspectionRepository();
    useCase = GetFieldTypesUseCase(repository);
  });

  group('GetFieldTypesUseCase', () {
    test('현장 유형 목록을 조회한다', () async {
      // Given
      final expectedFieldTypes = [
        InspectionFieldType(code: 'FT01', name: '본매대'),
        InspectionFieldType(code: 'FT02', name: '시식'),
        InspectionFieldType(code: 'FT03', name: '행사매대'),
        InspectionFieldType(code: 'FT99', name: '기타'),
      ];
      repository.fieldTypesResult = expectedFieldTypes;

      // When
      final result = await useCase.call();

      // Then
      expect(result, expectedFieldTypes);
      expect(result.length, 4);
    });

    test('빈 현장 유형 목록이 반환될 수 있다', () async {
      // Given
      repository.fieldTypesResult = [];

      // When
      final result = await useCase.call();

      // Then
      expect(result, isEmpty);
    });

    test('Repository에서 예외가 발생하면 전파된다', () async {
      // Given
      repository.error = Exception('Network error');

      // When & Then
      expect(
        () => useCase.call(),
        throwsException,
      );
    });
  });
}
