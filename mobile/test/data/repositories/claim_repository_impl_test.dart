import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/claim_remote_datasource.dart';
import 'package:mobile/data/models/claim_category_model.dart';
import 'package:mobile/data/models/claim_code_model.dart';
import 'package:mobile/data/models/claim_form_data_model.dart';
import 'package:mobile/data/models/claim_register_request.dart';
import 'package:mobile/data/models/claim_register_result_model.dart';
import 'package:mobile/data/repositories/claim_repository_impl.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/domain/entities/claim_form.dart';
import 'package:mobile/domain/entities/claim_form_data.dart';
import 'package:mobile/domain/entities/claim_result.dart';

void main() {
  late ClaimRepositoryImpl repository;
  late FakeClaimRemoteDataSource fakeDataSource;

  setUp(() {
    fakeDataSource = FakeClaimRemoteDataSource();
    repository = ClaimRepositoryImpl(fakeDataSource);
  });

  group('ClaimRepositoryImpl', () {
    group('registerClaim', () {
      test('클레임 등록이 성공한다', () async {
        // Given
        final form = ClaimRegisterForm(
          storeId: 1025,
          storeName: '미광종합물류',
          productCode: '12345678',
          productName: '맛있는부대찌개라양념140G',
          dateType: ClaimDateType.expiryDate,
          date: DateTime(2026, 2, 20),
          categoryId: 1,
          categoryName: '이물',
          subcategoryId: 101,
          subcategoryName: '벌레',
          defectDescription: '제품에서 벌레가 발견되었습니다',
          defectQuantity: 1,
          defectPhoto: File('test_defect.jpg'),
          labelPhoto: File('test_label.jpg'),
        );

        fakeDataSource.registerResultToReturn = const ClaimRegisterResultModel(
          id: 100,
          storeName: '미광종합물류',
          storeId: 1025,
          productName: '맛있는부대찌개라양념140G',
          productCode: '12345678',
          createdAt: '2026-02-11T10:30:00',
        );

        // When
        final result = await repository.registerClaim(form);

        // Then
        expect(fakeDataSource.registerClaimCalls, 1);
        expect(result, isA<ClaimRegisterResult>());
        expect(result.id, 100);
        expect(result.storeName, '미광종합물류');
        expect(result.storeId, 1025);
        expect(result.productName, '맛있는부대찌개라양념140G');
        expect(result.productCode, '12345678');
        expect(result.createdAt, DateTime(2026, 2, 11, 10, 30));
      });

      test('DataSource로 올바른 요청이 전달된다', () async {
        // Given
        final form = ClaimRegisterForm(
          storeId: 1025,
          storeName: '미광종합물류',
          productCode: '12345678',
          productName: '맛있는부대찌개라양념140G',
          dateType: ClaimDateType.manufactureDate,
          date: DateTime(2026, 1, 15),
          categoryId: 2,
          categoryName: '변질/변패',
          subcategoryId: 201,
          subcategoryName: '맛 변질',
          defectDescription: '맛이 이상합니다',
          defectQuantity: 5,
          defectPhoto: File('test_defect.jpg'),
          labelPhoto: File('test_label.jpg'),
          purchaseAmount: 5000,
          purchaseMethodCode: 'PM01',
          purchaseMethodName: '대형마트',
          receiptPhoto: File('test_receipt.jpg'),
          requestTypeCode: 'RT01',
          requestTypeName: '교환',
        );

        fakeDataSource.registerResultToReturn = const ClaimRegisterResultModel(
          id: 200,
          storeName: '미광종합물류',
          storeId: 1025,
          productName: '맛있는부대찌개라양념140G',
          productCode: '12345678',
          createdAt: '2026-02-11T14:20:30',
        );

        // When
        await repository.registerClaim(form);

        // Then
        final captured = fakeDataSource.lastRegisterRequest!;
        expect(captured.storeId, 1025);
        expect(captured.productCode, '12345678');
        expect(captured.dateType, 'MANUFACTURE_DATE');
        expect(captured.date, '2026-01-15');
        expect(captured.categoryId, 2);
        expect(captured.subcategoryId, 201);
        expect(captured.defectDescription, '맛이 이상합니다');
        expect(captured.defectQuantity, 5);
        expect(captured.purchaseAmount, 5000);
        expect(captured.purchaseMethodCode, 'PM01');
        expect(captured.requestTypeCode, 'RT01');
      });
    });

    group('getFormData', () {
      test('폼 데이터 조회가 성공한다', () async {
        // Given
        fakeDataSource.formDataToReturn = ClaimFormDataModel(
          categories: [
            ClaimCategoryModel(
              id: 1,
              name: '이물',
              subcategories: [
                const ClaimSubcategoryModel(id: 101, name: '벌레'),
                const ClaimSubcategoryModel(id: 102, name: '금속'),
              ],
            ),
            ClaimCategoryModel(
              id: 2,
              name: '변질/변패',
              subcategories: [
                const ClaimSubcategoryModel(id: 201, name: '맛 변질'),
              ],
            ),
          ],
          purchaseMethods: const [
            PurchaseMethodModel(code: 'PM01', name: '대형마트'),
            PurchaseMethodModel(code: 'PM02', name: '온라인'),
          ],
          requestTypes: const [
            ClaimRequestTypeModel(code: 'RT01', name: '교환'),
            ClaimRequestTypeModel(code: 'RT02', name: '환불'),
          ],
        );

        // When
        final result = await repository.getFormData();

        // Then
        expect(fakeDataSource.getFormDataCalls, 1);
        expect(result, isA<ClaimFormData>());
        expect(result.categories.length, 2);
        expect(result.categories[0].name, '이물');
        expect(result.categories[0].subcategories.length, 2);
        expect(result.categories[1].name, '변질/변패');
        expect(result.purchaseMethods.length, 2);
        expect(result.requestTypes.length, 2);
      });

      test('빈 폼 데이터를 처리할 수 있다', () async {
        // Given
        fakeDataSource.formDataToReturn = ClaimFormDataModel(
          categories: [],
          purchaseMethods: const [],
          requestTypes: const [],
        );

        // When
        final result = await repository.getFormData();

        // Then
        expect(result, isA<ClaimFormData>());
        expect(result.categories, isEmpty);
        expect(result.purchaseMethods, isEmpty);
        expect(result.requestTypes, isEmpty);
      });
    });
  });
}

// ──────────────────────────────────────────────────────────────────
// Fake DataSource
// ──────────────────────────────────────────────────────────────────

class FakeClaimRemoteDataSource implements ClaimRemoteDataSource {
  // ─── Call counters ─────────────────────────────────────────────
  int registerClaimCalls = 0;
  int getFormDataCalls = 0;

  // ─── Return values ─────────────────────────────────────────────
  ClaimRegisterResultModel? registerResultToReturn;
  ClaimFormDataModel? formDataToReturn;

  // ─── Captured parameters ───────────────────────────────────────
  ClaimRegisterRequest? lastRegisterRequest;

  @override
  Future<ClaimRegisterResultModel> registerClaim(
    ClaimRegisterRequest request,
  ) async {
    registerClaimCalls++;
    lastRegisterRequest = request;
    return registerResultToReturn!;
  }

  @override
  Future<ClaimFormDataModel> getFormData() async {
    getFormDataCalls++;
    return formDataToReturn!;
  }
}
