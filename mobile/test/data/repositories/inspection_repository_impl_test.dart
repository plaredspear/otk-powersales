import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/inspection_remote_datasource.dart';
import 'package:mobile/data/models/inspection_detail_model.dart';
import 'package:mobile/data/models/inspection_field_type_model.dart';
import 'package:mobile/data/models/inspection_list_item_model.dart';
import 'package:mobile/data/models/inspection_register_request.dart';
import 'package:mobile/data/models/inspection_theme_model.dart';
import 'package:mobile/data/repositories/inspection_repository_impl.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';

/// Mock DataSource for testing
class _MockInspectionRemoteDataSource implements InspectionRemoteDataSource {
  List<InspectionListItemModel>? listResult;
  InspectionDetailModel? detailResult;
  InspectionListItemModel? registerResult;
  List<InspectionThemeModel>? themesResult;
  List<InspectionFieldTypeModel>? fieldTypesResult;
  Exception? error;

  // Capture method call parameters
  int? storeIdParam;
  String? categoryParam;
  String? fromDateParam;
  String? toDateParam;
  int? inspectionIdParam;
  InspectionRegisterRequest? requestParam;

  @override
  Future<List<InspectionListItemModel>> getInspectionList({
    int? storeId,
    String? category,
    required String fromDate,
    required String toDate,
  }) async {
    storeIdParam = storeId;
    categoryParam = category;
    fromDateParam = fromDate;
    toDateParam = toDate;

    if (error != null) throw error!;
    return listResult!;
  }

  @override
  Future<InspectionDetailModel> getInspectionDetail(int inspectionId) async {
    inspectionIdParam = inspectionId;

    if (error != null) throw error!;
    return detailResult!;
  }

  @override
  Future<InspectionListItemModel> registerInspection(
    InspectionRegisterRequest request,
  ) async {
    requestParam = request;

    if (error != null) throw error!;
    return registerResult!;
  }

  @override
  Future<List<InspectionThemeModel>> getThemes() async {
    if (error != null) throw error!;
    return themesResult!;
  }

  @override
  Future<List<InspectionFieldTypeModel>> getFieldTypes() async {
    if (error != null) throw error!;
    return fieldTypesResult!;
  }
}

void main() {
  group('InspectionRepositoryImpl', () {
    late _MockInspectionRemoteDataSource mockDataSource;
    late InspectionRepositoryImpl repository;

    setUp(() {
      mockDataSource = _MockInspectionRemoteDataSource();
      repository = InspectionRepositoryImpl(mockDataSource);
    });

    group('getInspectionList', () {
      test('필터를 API 파라미터로 변환하여 목록을 조회한다', () async {
        // Given
        final filter = InspectionFilter(
          storeId: 100,
          category: InspectionCategory.OWN,
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );
        mockDataSource.listResult = [
          const InspectionListItemModel(
            id: 1,
            category: 'OWN',
            storeName: '이마트 죽전점',
            storeId: 100,
            inspectionDate: '2020-08-13',
            fieldType: '본매대',
            fieldTypeCode: 'FT01',
          ),
        ];

        // When
        final result = await repository.getInspectionList(filter);

        // Then
        expect(result.length, 1);
        expect(result[0].id, 1);
        expect(result[0].category, InspectionCategory.OWN);
        expect(result[0].storeName, '이마트 죽전점');
        expect(mockDataSource.storeIdParam, 100);
        expect(mockDataSource.categoryParam, 'OWN');
        expect(mockDataSource.fromDateParam, '2020-08-01');
        expect(mockDataSource.toDateParam, '2020-08-31');
      });

      test('storeId와 category가 null인 필터를 처리한다', () async {
        // Given
        final filter = InspectionFilter(
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );
        mockDataSource.listResult = [];

        // When
        await repository.getInspectionList(filter);

        // Then
        expect(mockDataSource.storeIdParam, null);
        expect(mockDataSource.categoryParam, null);
      });

      test('빈 목록을 반환할 수 있다', () async {
        // Given
        final filter = InspectionFilter(
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );
        mockDataSource.listResult = [];

        // When
        final result = await repository.getInspectionList(filter);

        // Then
        expect(result, isEmpty);
      });

      test('DataSource 에러를 전파한다', () async {
        // Given
        final filter = InspectionFilter(
          fromDate: DateTime(2020, 8, 1),
          toDate: DateTime(2020, 8, 31),
        );
        mockDataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => repository.getInspectionList(filter),
          throwsException,
        );
      });
    });

    group('getInspectionDetail', () {
      test('상세 정보를 조회하고 Entity로 변환한다', () async {
        // Given
        const mockModel = InspectionDetailModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          productCode: 'P001',
          productName: '진라면',
          photos: [],
          createdAt: '2020-08-13T10:30:00Z',
        );
        mockDataSource.detailResult = mockModel;

        // When
        final result = await repository.getInspectionDetail(1);

        // Then
        expect(result.id, 1);
        expect(result.category, InspectionCategory.OWN);
        expect(result.storeName, '이마트 죽전점');
        expect(result.description, '냉장고 앞 본매대');
        expect(mockDataSource.inspectionIdParam, 1);
      });

      test('DataSource 에러를 전파한다', () async {
        // Given
        mockDataSource.error = Exception('Not found');

        // When & Then
        expect(
          () => repository.getInspectionDetail(1),
          throwsException,
        );
      });
    });

    group('registerInspection', () {
      test('폼을 Request로 변환하여 등록한다', () async {
        // Given
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldTypeCode: 'FT01',
          description: '냉장고 앞 본매대',
          productCode: 'P001',
          photos: [File('/mock/photo.jpg')],
        );
        const mockModel = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );
        mockDataSource.registerResult = mockModel;

        // When
        final result = await repository.registerInspection(form);

        // Then
        expect(result.id, 1);
        expect(result.category, InspectionCategory.OWN);
        expect(result.storeName, '이마트 죽전점');
        expect(mockDataSource.requestParam, isNotNull);
        expect(mockDataSource.requestParam!.themeId, 10);
        expect(mockDataSource.requestParam!.category, 'OWN');
        expect(mockDataSource.requestParam!.storeId, 100);
        expect(mockDataSource.requestParam!.inspectionDate, '2020-08-13');
      });

      test('경쟁사 폼을 Request로 변환하여 등록한다', () async {
        // Given
        final form = InspectionRegisterForm(
          themeId: 20,
          category: InspectionCategory.COMPETITOR,
          storeId: 200,
          inspectionDate: DateTime(2020, 8, 14),
          fieldTypeCode: 'FT02',
          competitorName: '농심',
          competitorActivity: '시식 행사 진행 중',
          competitorTasting: true,
          competitorProductName: '신라면 블랙',
          competitorProductPrice: 5000,
          competitorSalesQuantity: 50,
          photos: [File('/mock/photo.jpg')],
        );
        const mockModel = InspectionListItemModel(
          id: 2,
          category: 'COMPETITOR',
          storeName: '홈플러스 강남점',
          storeId: 200,
          inspectionDate: '2020-08-14',
          fieldType: '시식',
          fieldTypeCode: 'FT02',
        );
        mockDataSource.registerResult = mockModel;

        // When
        final result = await repository.registerInspection(form);

        // Then
        expect(result.id, 2);
        expect(result.category, InspectionCategory.COMPETITOR);
        expect(mockDataSource.requestParam!.category, 'COMPETITOR');
        expect(mockDataSource.requestParam!.competitorName, '농심');
        expect(mockDataSource.requestParam!.competitorTasting, true);
      });

      test('DataSource 에러를 전파한다', () async {
        // Given
        final form = InspectionRegisterForm(
          themeId: 10,
          category: InspectionCategory.OWN,
          storeId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [File('/mock/photo.jpg')],
        );
        mockDataSource.error = Exception('Validation error');

        // When & Then
        expect(
          () => repository.registerInspection(form),
          throwsException,
        );
      });
    });

    group('getThemes', () {
      test('테마 목록을 조회하고 Entity로 변환한다', () async {
        // Given
        mockDataSource.themesResult = [
          const InspectionThemeModel(
            id: 10,
            name: '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)',
            startDate: '2020-08-01',
            endDate: '2020-08-31',
          ),
          const InspectionThemeModel(
            id: 11,
            name: '8월 테마(영업지원실_영업지원1팀)',
            startDate: '2020-08-01',
            endDate: '2020-08-31',
          ),
        ];

        // When
        final result = await repository.getThemes();

        // Then
        expect(result.length, 2);
        expect(result[0].id, 10);
        expect(result[0].name, '롯데마트 탕국찌개 행사 사진 취합 건(영업지원1팀)');
        expect(result[0].startDate, DateTime(2020, 8, 1));
        expect(result[0].endDate, DateTime(2020, 8, 31));
      });

      test('빈 테마 목록을 반환할 수 있다', () async {
        // Given
        mockDataSource.themesResult = [];

        // When
        final result = await repository.getThemes();

        // Then
        expect(result, isEmpty);
      });

      test('DataSource 에러를 전파한다', () async {
        // Given
        mockDataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => repository.getThemes(),
          throwsException,
        );
      });
    });

    group('getFieldTypes', () {
      test('현장 유형 목록을 조회하고 Entity로 변환한다', () async {
        // Given
        mockDataSource.fieldTypesResult = [
          const InspectionFieldTypeModel(code: 'FT01', name: '본매대'),
          const InspectionFieldTypeModel(code: 'FT02', name: '시식'),
          const InspectionFieldTypeModel(code: 'FT03', name: '행사매대'),
          const InspectionFieldTypeModel(code: 'FT99', name: '기타'),
        ];

        // When
        final result = await repository.getFieldTypes();

        // Then
        expect(result.length, 4);
        expect(result[0].code, 'FT01');
        expect(result[0].name, '본매대');
        expect(result[1].code, 'FT02');
        expect(result[1].name, '시식');
      });

      test('빈 현장 유형 목록을 반환할 수 있다', () async {
        // Given
        mockDataSource.fieldTypesResult = [];

        // When
        final result = await repository.getFieldTypes();

        // Then
        expect(result, isEmpty);
      });

      test('DataSource 에러를 전파한다', () async {
        // Given
        mockDataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => repository.getFieldTypes(),
          throwsException,
        );
      });
    });
  });
}
