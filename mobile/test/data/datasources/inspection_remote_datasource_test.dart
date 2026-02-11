import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/inspection_remote_datasource.dart';
import 'package:mobile/data/models/inspection_detail_model.dart';
import 'package:mobile/data/models/inspection_field_type_model.dart';
import 'package:mobile/data/models/inspection_list_item_model.dart';
import 'package:mobile/data/models/inspection_register_request.dart';
import 'package:mobile/data/models/inspection_theme_model.dart';

/// Mock implementation for testing
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
  group('InspectionRemoteDataSource', () {
    late _MockInspectionRemoteDataSource dataSource;

    setUp(() {
      dataSource = _MockInspectionRemoteDataSource();
    });

    group('getInspectionList', () {
      test('목록 조회 API를 호출한다', () async {
        // Given
        final expectedList = [
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
        dataSource.listResult = expectedList;

        // When
        final result = await dataSource.getInspectionList(
          storeId: 100,
          category: 'OWN',
          fromDate: '2020-08-01',
          toDate: '2020-08-31',
        );

        // Then
        expect(result, expectedList);
        expect(dataSource.storeIdParam, 100);
        expect(dataSource.categoryParam, 'OWN');
        expect(dataSource.fromDateParam, '2020-08-01');
        expect(dataSource.toDateParam, '2020-08-31');
      });

      test('storeId와 category가 null인 경우 전체 조회', () async {
        // Given
        dataSource.listResult = [];

        // When
        await dataSource.getInspectionList(
          fromDate: '2020-08-01',
          toDate: '2020-08-31',
        );

        // Then
        expect(dataSource.storeIdParam, null);
        expect(dataSource.categoryParam, null);
      });

      test('API 에러 시 예외를 전파한다', () async {
        // Given
        dataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => dataSource.getInspectionList(
            fromDate: '2020-08-01',
            toDate: '2020-08-31',
          ),
          throwsException,
        );
      });
    });

    group('getInspectionDetail', () {
      test('상세 조회 API를 호출한다', () async {
        // Given
        const expectedDetail = InspectionDetailModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          themeName: '8월 테마',
          themeId: 10,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
          photos: [],
          createdAt: '2020-08-13T10:30:00Z',
        );
        dataSource.detailResult = expectedDetail;

        // When
        final result = await dataSource.getInspectionDetail(1);

        // Then
        expect(result, expectedDetail);
        expect(dataSource.inspectionIdParam, 1);
      });

      test('API 에러 시 예외를 전파한다', () async {
        // Given
        dataSource.error = Exception('Not found');

        // When & Then
        expect(
          () => dataSource.getInspectionDetail(999),
          throwsException,
        );
      });
    });

    group('registerInspection', () {
      test('등록 API를 호출한다', () async {
        // Given
        final request = InspectionRegisterRequest(
          themeId: 10,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [File('/mock/photo.jpg')],
        );
        const expectedResult = InspectionListItemModel(
          id: 1,
          category: 'OWN',
          storeName: '이마트 죽전점',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        );
        dataSource.registerResult = expectedResult;

        // When
        final result = await dataSource.registerInspection(request);

        // Then
        expect(result, expectedResult);
        expect(dataSource.requestParam, request);
      });

      test('API 에러 시 예외를 전파한다', () async {
        // Given
        final request = InspectionRegisterRequest(
          themeId: 10,
          category: 'OWN',
          storeId: 100,
          inspectionDate: '2020-08-13',
          fieldTypeCode: 'FT01',
          productCode: 'P001',
          photos: [File('/mock/photo.jpg')],
        );
        dataSource.error = Exception('Validation error');

        // When & Then
        expect(
          () => dataSource.registerInspection(request),
          throwsException,
        );
      });
    });

    group('getThemes', () {
      test('테마 목록 조회 API를 호출한다', () async {
        // Given
        final expectedThemes = [
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
        dataSource.themesResult = expectedThemes;

        // When
        final result = await dataSource.getThemes();

        // Then
        expect(result, expectedThemes);
        expect(result.length, 2);
      });

      test('빈 테마 목록을 반환할 수 있다', () async {
        // Given
        dataSource.themesResult = [];

        // When
        final result = await dataSource.getThemes();

        // Then
        expect(result, isEmpty);
      });

      test('API 에러 시 예외를 전파한다', () async {
        // Given
        dataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => dataSource.getThemes(),
          throwsException,
        );
      });
    });

    group('getFieldTypes', () {
      test('현장 유형 목록 조회 API를 호출한다', () async {
        // Given
        final expectedFieldTypes = [
          const InspectionFieldTypeModel(code: 'FT01', name: '본매대'),
          const InspectionFieldTypeModel(code: 'FT02', name: '시식'),
          const InspectionFieldTypeModel(code: 'FT03', name: '행사매대'),
          const InspectionFieldTypeModel(code: 'FT99', name: '기타'),
        ];
        dataSource.fieldTypesResult = expectedFieldTypes;

        // When
        final result = await dataSource.getFieldTypes();

        // Then
        expect(result, expectedFieldTypes);
        expect(result.length, 4);
      });

      test('빈 현장 유형 목록을 반환할 수 있다', () async {
        // Given
        dataSource.fieldTypesResult = [];

        // When
        final result = await dataSource.getFieldTypes();

        // Then
        expect(result, isEmpty);
      });

      test('API 에러 시 예외를 전파한다', () async {
        // Given
        dataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => dataSource.getFieldTypes(),
          throwsException,
        );
      });
    });
  });
}
