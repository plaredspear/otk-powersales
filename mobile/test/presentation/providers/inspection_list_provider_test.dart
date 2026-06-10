import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_detail.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/domain/repositories/inspection_repository.dart';
import 'package:mobile/domain/usecases/get_inspection_list_usecase.dart';
import 'package:mobile/presentation/providers/inspection_list_provider.dart';
import 'package:mobile/presentation/providers/inspection_list_state.dart';

/// Mock Repository for testing
class _MockInspectionRepository implements InspectionRepository {
  List<InspectionListItem>? mockItems;
  Exception? mockError;

  // Capture call parameters
  InspectionFilter? lastFilter;

  @override
  Future<List<InspectionListItem>> getInspectionList(
    InspectionFilter filter,
  ) async {
    lastFilter = filter;
    // Simulate API delay for loading state test
    await Future.delayed(const Duration(milliseconds: 100));
    if (mockError != null) throw mockError!;
    return mockItems ?? [];
  }

  @override
  Future<InspectionDetail> getInspectionDetail(int id) async {
    throw UnimplementedError();
  }

  @override
  Future<InspectionListItem> registerInspection(InspectionRegisterForm form) async {
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
  group('InspectionListNotifier', () {
    late _MockInspectionRepository mockRepository;
    late ProviderContainer container;

    setUp(() {
      mockRepository = _MockInspectionRepository();
      container = ProviderContainer(
        overrides: [
          inspectionRepositoryProvider.overrideWithValue(mockRepository),
        ],
      );
    });

    tearDown(() {
      container.dispose();
    });

    test('초기 상태는 initial()이다', () {
      // Given & When
      final state = container.read(inspectionListProvider);

      // Then
      expect(state.isLoading, false);
      expect(state.items, isEmpty);
      expect(state.hasSearched, false);
      expect(state.selectedAccountId, null);
      expect(state.selectedCategory, null);
    });

    test('selectAccount는 거래처를 선택한다', () {
      // Given
      final notifier = container.read(inspectionListProvider.notifier);

      // When
      notifier.selectAccount(100, '이마트 죽전점');

      // Then
      final state = container.read(inspectionListProvider);
      expect(state.selectedAccountId, 100);
      expect(state.selectedAccountName, '이마트 죽전점');
    });

    test('selectAccount에 null을 전달하면 거래처 필터가 초기화된다', () {
      // Given
      final notifier = container.read(inspectionListProvider.notifier);
      notifier.selectAccount(100, '이마트 죽전점');

      // When
      notifier.selectAccount(null, null);

      // Then
      final state = container.read(inspectionListProvider);
      expect(state.selectedAccountId, null);
      expect(state.selectedAccountName, null);
    });

    test('selectCategory는 분류를 선택한다', () {
      // Given
      final notifier = container.read(inspectionListProvider.notifier);

      // When
      notifier.selectCategory(InspectionCategory.OWN);

      // Then
      final state = container.read(inspectionListProvider);
      expect(state.selectedCategory, InspectionCategory.OWN);
    });

    test('selectCategory에 null을 전달하면 분류 필터가 초기화된다', () {
      // Given
      final notifier = container.read(inspectionListProvider.notifier);
      notifier.selectCategory(InspectionCategory.OWN);

      // When
      notifier.selectCategory(null);

      // Then
      final state = container.read(inspectionListProvider);
      expect(state.selectedCategory, null);
    });

    test('updateFromDate는 시작일을 변경한다', () {
      // Given
      final notifier = container.read(inspectionListProvider.notifier);
      final newDate = DateTime(2020, 8, 1);

      // When
      notifier.updateFromDate(newDate);

      // Then
      final state = container.read(inspectionListProvider);
      expect(state.fromDate, newDate);
    });

    test('updateToDate는 종료일을 변경한다', () {
      // Given
      final notifier = container.read(inspectionListProvider.notifier);
      final newDate = DateTime(2020, 8, 31);

      // When
      notifier.updateToDate(newDate);

      // Then
      final state = container.read(inspectionListProvider);
      expect(state.toDate, newDate);
    });

    test('searchInspections는 성공 시 데이터 상태로 전환한다', () async {
      // Given
      final mockItems = [
        InspectionListItem(
          id: 1,
          category: InspectionCategory.OWN,
          accountName: '이마트 죽전점',
          accountId: 100,
          inspectionDate: DateTime(2020, 8, 13),
          fieldType: '본매대',
          fieldTypeCode: 'FT01',
        ),
      ];
      mockRepository.mockItems = mockItems;
      final notifier = container.read(inspectionListProvider.notifier);

      // When
      await notifier.searchInspections();

      // Then
      final state = container.read(inspectionListProvider);
      expect(state.isLoading, false);
      expect(state.items, mockItems);
      expect(state.hasSearched, true);
      expect(state.errorMessage, null);
    });

    test('searchInspections는 필터를 UseCase에 전달한다', () async {
      // Given
      mockRepository.mockItems = [];
      final notifier = container.read(inspectionListProvider.notifier);
      notifier.selectAccount(100, '이마트');
      notifier.selectCategory(InspectionCategory.OWN);
      notifier.updateFromDate(DateTime(2020, 8, 25));
      notifier.updateToDate(DateTime(2020, 8, 31));

      // When
      await notifier.searchInspections();

      // Then
      expect(mockRepository.lastFilter, isNotNull);
      expect(mockRepository.lastFilter!.accountId, 100);
      expect(mockRepository.lastFilter!.category, InspectionCategory.OWN);
      expect(mockRepository.lastFilter!.fromDate, DateTime(2020, 8, 25));
      expect(mockRepository.lastFilter!.toDate, DateTime(2020, 8, 31));
    });

    test('updateFromDate는 종료일이 7일을 초과하면 종료일을 시작일+7일로 보정한다', () {
      // Given — 레거시 daterangepicker maxSpan {days: 7} 정합
      final notifier = container.read(inspectionListProvider.notifier);
      notifier.updateToDate(DateTime(2020, 8, 31));

      // When — 시작일을 31일보다 30일 이른 날짜로 변경
      notifier.updateFromDate(DateTime(2020, 8, 1));

      // Then — 종료일이 시작일+7일로 자동 보정
      final state = container.read(inspectionListProvider);
      expect(state.fromDate, DateTime(2020, 8, 1));
      expect(state.toDate, DateTime(2020, 8, 8));
    });

    test('updateToDate는 시작일이 7일을 초과하면 시작일을 종료일-7일로 보정한다', () {
      // Given — 레거시 daterangepicker maxSpan {days: 7} 정합
      final notifier = container.read(inspectionListProvider.notifier);
      notifier.updateFromDate(DateTime(2020, 8, 1));

      // When — 종료일을 1일보다 30일 늦은 날짜로 변경
      notifier.updateToDate(DateTime(2020, 8, 31));

      // Then — 시작일이 종료일-7일로 자동 보정
      final state = container.read(inspectionListProvider);
      expect(state.fromDate, DateTime(2020, 8, 24));
      expect(state.toDate, DateTime(2020, 8, 31));
    });

    test('searchInspections는 에러 시 에러 상태로 전환한다', () async {
      // Given
      mockRepository.mockError = Exception('네트워크 오류');
      final notifier = container.read(inspectionListProvider.notifier);

      // When
      await notifier.searchInspections();

      // Then
      final state = container.read(inspectionListProvider);
      expect(state.isLoading, false);
      expect(state.errorMessage, '네트워크 오류');
    });

    test('clearError는 에러 메시지를 초기화한다', () async {
      // Given
      mockRepository.mockError = Exception('네트워크 오류');
      final notifier = container.read(inspectionListProvider.notifier);
      await notifier.searchInspections();

      // When
      notifier.clearError();

      // Then
      final state = container.read(inspectionListProvider);
      expect(state.errorMessage, null);
    });

    test('searchInspections 호출 중에는 로딩 상태가 된다', () async {
      // Given
      mockRepository.mockItems = [];
      final notifier = container.read(inspectionListProvider.notifier);

      // When
      final searchFuture = notifier.searchInspections();

      // Then (로딩 중)
      await Future.microtask(() {});
      expect(container.read(inspectionListProvider).isLoading, true);

      // Then (완료 후)
      await searchFuture;
      expect(container.read(inspectionListProvider).isLoading, false);
    });
  });
}
