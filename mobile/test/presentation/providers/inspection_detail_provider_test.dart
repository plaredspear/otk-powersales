import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/domain/repositories/inspection_repository.dart';
import 'package:mobile/domain/usecases/get_inspection_detail_usecase.dart';
import 'package:mobile/presentation/providers/inspection_detail_provider.dart';
import 'package:mobile/presentation/providers/inspection_detail_state.dart';
import 'package:mobile/presentation/providers/inspection_list_provider.dart';

/// Mock Repository for testing
class _MockInspectionRepository implements InspectionRepository {
  InspectionDetail? mockDetail;
  Exception? mockError;

  @override
  Future<InspectionDetail> getInspectionDetail(int id) async {
    // Simulate API delay
    await Future.delayed(const Duration(milliseconds: 100));
    if (mockError != null) throw mockError!;
    if (mockDetail == null) throw Exception('점검을 찾을 수 없습니다');
    return mockDetail!;
  }

  @override
  Future<List<InspectionListItem>> getInspectionList(
    InspectionFilter filter,
  ) async {
    throw UnimplementedError();
  }

  @override
  Future<InspectionListItem> registerInspection(
    InspectionRegisterForm form,
  ) async {
    throw UnimplementedError();
  }

  @override
  Future<List<InspectionTheme>> getThemes() async {
    throw UnimplementedError();
  }

  @override
  Future<List<InspectionFieldType>> getFieldTypes() async {
    throw UnimplementedError();
  }
}

void main() {
  group('InspectionDetailNotifier', () {
    late _MockInspectionRepository mockRepository;
    late GetInspectionDetailUseCase useCase;
    late InspectionDetailNotifier notifier;

    setUp(() {
      mockRepository = _MockInspectionRepository();
      useCase = GetInspectionDetailUseCase(mockRepository);
      notifier = InspectionDetailNotifier(getInspectionDetail: useCase);
    });

    test('초기 상태는 initial()이다', () {
      // Then
      expect(notifier.state.isLoading, false);
      expect(notifier.state.errorMessage, null);
      expect(notifier.state.detail, null);
      expect(notifier.state.hasData, false);
    });

    test('loadDetail 성공 시 데이터 상태로 전환한다', () async {
      // Given
      final mockDetail = InspectionDetail(
        id: 1,
        category: InspectionCategory.OWN,
        storeName: '이마트 죽전점',
        storeId: 100,
        themeName: '8월 테마',
        themeId: 10,
        inspectionDate: DateTime(2020, 8, 13),
        fieldType: '본매대',
        fieldTypeCode: 'FT01',
        description: '냉장고 앞 본매대',
        productCode: 'P001',
        productName: '진라면',
        photos: const [],
        createdAt: DateTime(2020, 8, 13, 10, 30),
      );
      mockRepository.mockDetail = mockDetail;

      // When
      await notifier.loadDetail(1);

      // Then
      expect(notifier.state.isLoading, false);
      expect(notifier.state.detail, mockDetail);
      expect(notifier.state.errorMessage, null);
      expect(notifier.state.hasData, true);
    });

    test('loadDetail 에러 시 에러 상태로 전환한다', () async {
      // Given
      mockRepository.mockError = Exception('네트워크 오류');

      // When
      await notifier.loadDetail(1);

      // Then
      expect(notifier.state.isLoading, false);
      expect(notifier.state.detail, null);
      expect(notifier.state.errorMessage, '네트워크 오류');
      expect(notifier.state.hasData, false);
    });

    test('loadDetail 호출 중에는 로딩 상태가 된다', () async {
      // Given
      mockRepository.mockDetail = InspectionDetail(
        id: 1,
        category: InspectionCategory.OWN,
        storeName: '이마트',
        storeId: 100,
        themeName: '8월 테마',
        themeId: 10,
        inspectionDate: DateTime(2020, 8, 13),
        fieldType: '본매대',
        fieldTypeCode: 'FT01',
        photos: const [],
        createdAt: DateTime(2020, 8, 13, 10, 30),
      );

      // When
      final loadFuture = notifier.loadDetail(1);

      // Then (로딩 중)
      await Future.microtask(() {});
      expect(notifier.state.isLoading, true);

      // Then (완료 후)
      await loadFuture;
      expect(notifier.state.isLoading, false);
    });

    test('clearError는 에러 메시지를 초기화한다', () async {
      // Given
      mockRepository.mockError = Exception('네트워크 오류');
      await notifier.loadDetail(1);
      expect(notifier.state.errorMessage, '네트워크 오류');

      // When
      notifier.clearError();

      // Then
      expect(notifier.state.errorMessage, null);
    });

    test('loadDetail은 자사 점검 상세를 올바르게 로드한다', () async {
      // Given
      final mockDetail = InspectionDetail(
        id: 1,
        category: InspectionCategory.OWN,
        storeName: '이마트 죽전점',
        storeId: 100,
        themeName: '8월 테마',
        themeId: 10,
        inspectionDate: DateTime(2020, 8, 13),
        fieldType: '본매대',
        fieldTypeCode: 'FT01',
        description: '냉장고 앞 본매대',
        productCode: 'P001',
        productName: '진라면',
        photos: const [],
        createdAt: DateTime(2020, 8, 13, 10, 30),
      );
      mockRepository.mockDetail = mockDetail;

      // When
      await notifier.loadDetail(1);

      // Then
      expect(notifier.state.detail, mockDetail);
      expect(notifier.state.isOwn, true);
      expect(notifier.state.isCompetitor, false);
      expect(notifier.state.hasTasting, false);
    });

    test('loadDetail은 경쟁사 점검 상세를 올바르게 로드한다', () async {
      // Given
      final mockDetail = InspectionDetail(
        id: 2,
        category: InspectionCategory.COMPETITOR,
        storeName: '홈플러스',
        storeId: 200,
        themeName: '8월 테마',
        themeId: 10,
        inspectionDate: DateTime(2020, 8, 14),
        fieldType: '시식',
        fieldTypeCode: 'FT02',
        competitorName: '농심',
        competitorActivity: '시식 행사',
        competitorTasting: true,
        competitorProductName: '신라면 블랙',
        competitorProductPrice: 5000,
        competitorSalesQuantity: 50,
        photos: const [],
        createdAt: DateTime(2020, 8, 14, 10, 30),
      );
      mockRepository.mockDetail = mockDetail;

      // When
      await notifier.loadDetail(2);

      // Then
      expect(notifier.state.detail, mockDetail);
      expect(notifier.state.isOwn, false);
      expect(notifier.state.isCompetitor, true);
      expect(notifier.state.hasTasting, true);
    });
  });

}
