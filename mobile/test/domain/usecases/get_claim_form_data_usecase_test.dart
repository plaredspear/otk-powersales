import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_category.dart';
import 'package:mobile/domain/entities/claim_code.dart';
import 'package:mobile/domain/entities/claim_form.dart';
import 'package:mobile/domain/entities/claim_form_data.dart';
import 'package:mobile/domain/entities/claim_result.dart';
import 'package:mobile/domain/repositories/claim_repository.dart';
import 'package:mobile/domain/usecases/get_claim_form_data_usecase.dart';

// Mock Repository
class MockClaimRepository implements ClaimRepository {
  ClaimFormData? _dataToReturn;
  Exception? _exceptionToThrow;

  void setData(ClaimFormData data) {
    _dataToReturn = data;
    _exceptionToThrow = null;
  }

  void setException(Exception exception) {
    _exceptionToThrow = exception;
    _dataToReturn = null;
  }

  @override
  Future<ClaimFormData> getFormData() async {
    if (_exceptionToThrow != null) {
      throw _exceptionToThrow!;
    }
    return _dataToReturn!;
  }

  @override
  Future<ClaimRegisterResult> registerClaim(ClaimRegisterForm form) async {
    throw UnimplementedError();
  }
}

void main() {
  group('GetClaimFormDataUseCase', () {
    late MockClaimRepository mockRepository;
    late GetClaimFormDataUseCase useCase;

    setUp(() {
      mockRepository = MockClaimRepository();
      useCase = GetClaimFormDataUseCase(mockRepository);
    });

    test('폼 초기화 데이터 조회가 성공한다', () async {
      // Given
      final expectedData = ClaimFormData(
        categories: const [
          ClaimCategory(
            id: 1,
            name: '이물',
            subcategories: [
              ClaimSubcategory(id: 101, name: '벌레'),
              ClaimSubcategory(id: 102, name: '금속'),
            ],
          ),
          ClaimCategory(
            id: 2,
            name: '변질/변패',
            subcategories: [
              ClaimSubcategory(id: 201, name: '맛 변질'),
            ],
          ),
        ],
        purchaseMethods: const [
          PurchaseMethod(code: 'PM01', name: '대형마트'),
          PurchaseMethod(code: 'PM02', name: '편의점'),
        ],
        requestTypes: const [
          ClaimRequestType(code: 'RT01', name: '교환'),
          ClaimRequestType(code: 'RT02', name: '환불'),
        ],
      );
      mockRepository.setData(expectedData);

      // When
      final result = await useCase();

      // Then
      expect(result, expectedData);
      expect(result.categories.length, 2);
      expect(result.purchaseMethods.length, 2);
      expect(result.requestTypes.length, 2);
    });

    test('빈 데이터를 반환할 수 있다', () async {
      // Given
      const emptyData = ClaimFormData(
        categories: [],
        purchaseMethods: [],
        requestTypes: [],
      );
      mockRepository.setData(emptyData);

      // When
      final result = await useCase();

      // Then
      expect(result, emptyData);
      expect(result.categories, isEmpty);
      expect(result.purchaseMethods, isEmpty);
      expect(result.requestTypes, isEmpty);
    });

    test('Repository에서 예외가 발생하면 그대로 전파한다', () async {
      // Given
      final testException = Exception('Network error');
      mockRepository.setException(testException);

      // When & Then
      expect(
        () => useCase(),
        throwsA(testException),
      );
    });
  });
}
