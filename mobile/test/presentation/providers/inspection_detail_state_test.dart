import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/presentation/providers/inspection_detail_state.dart';

void main() {
  group('InspectionDetailState', () {
    test('초기 상태가 올바르게 생성된다', () {
      // When
      final state = InspectionDetailState.initial();

      // Then
      expect(state.isLoading, false);
      expect(state.errorMessage, null);
      expect(state.detail, null);
      expect(state.hasData, false);
    });

    test('toLoading은 로딩 상태로 전환한다', () {
      // Given
      final state = InspectionDetailState.initial().copyWith(
        errorMessage: '이전 에러',
      );

      // When
      final loadingState = state.toLoading();

      // Then
      expect(loadingState.isLoading, true);
      expect(loadingState.errorMessage, null);
    });

    test('toError는 에러 상태로 전환한다', () {
      // Given
      final state = InspectionDetailState.initial().copyWith(
        isLoading: true,
      );

      // When
      final errorState = state.toError('네트워크 오류');

      // Then
      expect(errorState.isLoading, false);
      expect(errorState.errorMessage, '네트워크 오류');
    });

    test('hasData는 detail이 있을 때 true를 반환한다', () {
      // Given
      final detail = InspectionDetail(
        id: 1,
        category: InspectionCategory.OWN,
        storeName: '이마트',
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
      final state = InspectionDetailState.initial().copyWith(detail: detail);

      // Then
      expect(state.hasData, true);
    });

    test('isOwn은 자사 점검일 때 true를 반환한다', () {
      // Given
      final detail = InspectionDetail(
        id: 1,
        category: InspectionCategory.OWN,
        storeName: '이마트',
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
      final state = InspectionDetailState.initial().copyWith(detail: detail);

      // Then
      expect(state.isOwn, true);
      expect(state.isCompetitor, false);
    });

    test('isCompetitor는 경쟁사 점검일 때 true를 반환한다', () {
      // Given
      final detail = InspectionDetail(
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
      final state = InspectionDetailState.initial().copyWith(detail: detail);

      // Then
      expect(state.isCompetitor, true);
      expect(state.isOwn, false);
    });

    test('hasTasting은 경쟁사 점검에서 시식=true일 때 true를 반환한다', () {
      // Given
      final detail = InspectionDetail(
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
      final state = InspectionDetailState.initial().copyWith(detail: detail);

      // Then
      expect(state.hasTasting, true);
    });

    test('hasTasting은 경쟁사 점검에서 시식=false일 때 false를 반환한다', () {
      // Given
      final detail = InspectionDetail(
        id: 2,
        category: InspectionCategory.COMPETITOR,
        storeName: '홈플러스',
        storeId: 200,
        themeName: '8월 테마',
        themeId: 10,
        inspectionDate: DateTime(2020, 8, 14),
        fieldType: '행사매대',
        fieldTypeCode: 'FT03',
        competitorName: '농심',
        competitorActivity: '행사 진행',
        competitorTasting: false,
        photos: const [],
        createdAt: DateTime(2020, 8, 14, 10, 30),
      );
      final state = InspectionDetailState.initial().copyWith(detail: detail);

      // Then
      expect(state.hasTasting, false);
    });

    test('hasTasting은 자사 점검일 때 false를 반환한다', () {
      // Given
      final detail = InspectionDetail(
        id: 1,
        category: InspectionCategory.OWN,
        storeName: '이마트',
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
      final state = InspectionDetailState.initial().copyWith(detail: detail);

      // Then
      expect(state.hasTasting, false);
    });

    test('copyWith는 선택적으로 필드를 업데이트한다', () {
      // Given
      final state = InspectionDetailState.initial();

      // When
      final updated = state.copyWith(
        isLoading: true,
        errorMessage: '에러',
      );

      // Then
      expect(updated.isLoading, true);
      expect(updated.errorMessage, '에러');
      expect(updated.detail, null); // 변경되지 않음
    });
  });
}

