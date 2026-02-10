import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';
import 'package:mobile/domain/entities/safety_check_submit_result.dart';
import 'package:mobile/domain/entities/safety_check_today_status.dart';
import 'package:mobile/domain/repositories/safety_check_repository.dart';
import 'package:mobile/domain/usecases/get_safety_check_items.dart';
import 'package:mobile/domain/usecases/submit_safety_check.dart';
import 'package:mobile/presentation/providers/safety_check_provider.dart';

/// 테스트용 Mock SafetyCheckRepository
class MockSafetyCheckRepository implements SafetyCheckRepository {
  List<SafetyCheckCategory>? categoriesToReturn;
  SafetyCheckSubmitResult? submitResultToReturn;
  Exception? exceptionToThrow;
  Exception? submitExceptionToThrow;

  int submitCallCount = 0;
  List<int>? lastSubmittedItemIds;

  @override
  Future<List<SafetyCheckCategory>> getItems() async {
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return categoriesToReturn!;
  }

  @override
  Future<SafetyCheckSubmitResult> submit(List<int> checkedItemIds) async {
    submitCallCount++;
    lastSubmittedItemIds = checkedItemIds;

    if (submitExceptionToThrow != null) {
      throw submitExceptionToThrow!;
    }
    return submitResultToReturn!;
  }

  @override
  Future<SafetyCheckTodayStatus> getTodayStatus() {
    throw UnimplementedError();
  }
}

void main() {
  // 테스트 데이터: 카테고리 1 (필수 2개), 카테고리 2 (필수 1개 + 선택 1개)
  final testCategories = [
    const SafetyCheckCategory(
      id: 1,
      name: '차량 점검',
      description: '운행 전 차량 상태를 점검합니다',
      items: [
        SafetyCheckItem(id: 1, label: '타이어 상태', sortOrder: 1, required: true),
        SafetyCheckItem(id: 2, label: '브레이크 작동', sortOrder: 2, required: true),
      ],
    ),
    const SafetyCheckCategory(
      id: 2,
      name: '개인 안전장비',
      description: '안전장비 착용 여부를 확인합니다',
      items: [
        SafetyCheckItem(id: 3, label: '안전화 착용', sortOrder: 1, required: true),
        SafetyCheckItem(id: 4, label: '장갑 착용', sortOrder: 2, required: false),
      ],
    ),
  ];

  final testSubmitResult = SafetyCheckSubmitResult(
    submissionId: 1,
    submittedAt: DateTime.parse('2026-02-08T09:00:00.000Z'),
    safetyCheckCompleted: true,
  );

  group('SafetyCheckNotifier', () {
    late MockSafetyCheckRepository mockRepository;
    late GetSafetyCheckItems getItemsUseCase;
    late SubmitSafetyCheck submitUseCase;
    late SafetyCheckNotifier notifier;

    setUp(() {
      mockRepository = MockSafetyCheckRepository();
      getItemsUseCase = GetSafetyCheckItems(mockRepository);
      submitUseCase = SubmitSafetyCheck(mockRepository);
      notifier = SafetyCheckNotifier(getItemsUseCase, submitUseCase);
    });

    test('초기 상태는 SafetyCheckState.initial()이다', () {
      expect(notifier.state.categories, isNull);
      expect(notifier.state.checkedItems, isEmpty);
      expect(notifier.state.isLoading, false);
      expect(notifier.state.isSubmitting, false);
      expect(notifier.state.isSubmitted, false);
      expect(notifier.state.errorMessage, isNull);
    });

    test('fetchItems 성공 시 카테고리가 로딩된다', () async {
      mockRepository.categoriesToReturn = testCategories;

      await notifier.fetchItems();

      expect(notifier.state.categories, testCategories);
      expect(notifier.state.isLoading, false);
      expect(notifier.state.errorMessage, isNull);
      expect(notifier.state.isLoaded, true);
    });

    test('fetchItems 성공 시 모든 체크 항목이 unchecked로 초기화된다', () async {
      mockRepository.categoriesToReturn = testCategories;

      await notifier.fetchItems();

      // 4개 항목이 모두 false로 초기화되어야 함
      expect(notifier.state.checkedItems.length, 4);
      expect(notifier.state.checkedItems[1], false);
      expect(notifier.state.checkedItems[2], false);
      expect(notifier.state.checkedItems[3], false);
      expect(notifier.state.checkedItems[4], false);
      expect(notifier.state.allRequiredChecked, false);
    });

    test('fetchItems 실패 시 에러 상태로 전환된다', () async {
      mockRepository.exceptionToThrow = Exception('네트워크 오류');

      await notifier.fetchItems();

      expect(notifier.state.isError, true);
      expect(notifier.state.errorMessage, contains('네트워크 오류'));
      expect(notifier.state.isLoading, false);
      expect(notifier.state.categories, isNull);
    });

    test('toggleItem으로 항목을 체크/해제할 수 있다', () async {
      mockRepository.categoriesToReturn = testCategories;
      await notifier.fetchItems();

      // 항목 1 체크
      notifier.toggleItem(1);
      expect(notifier.state.checkedItems[1], true);
      expect(notifier.state.allRequiredChecked, false);

      // 항목 1 다시 체크 해제
      notifier.toggleItem(1);
      expect(notifier.state.checkedItems[1], false);

      // 항목 2, 3 체크
      notifier.toggleItem(2);
      notifier.toggleItem(3);
      expect(notifier.state.checkedItems[2], true);
      expect(notifier.state.checkedItems[3], true);

      // 선택 항목 체크
      notifier.toggleItem(4);
      expect(notifier.state.checkedItems[4], true);
    });

    test('submit 성공 시 제출 완료 상태로 전환된다', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitResultToReturn = testSubmitResult;

      await notifier.fetchItems();

      // 모든 필수 항목 체크
      notifier.toggleItem(1);
      notifier.toggleItem(2);
      notifier.toggleItem(3);

      expect(notifier.state.allRequiredChecked, true);

      await notifier.submit();

      expect(notifier.state.isSubmitted, true);
      expect(notifier.state.isSubmitting, false);
      expect(notifier.state.errorMessage, isNull);
      expect(mockRepository.submitCallCount, 1);
      expect(mockRepository.lastSubmittedItemIds, containsAll([1, 2, 3]));
    });

    test('submit은 allRequiredChecked가 false이면 무시된다', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitResultToReturn = testSubmitResult;

      await notifier.fetchItems();

      // 필수 항목을 일부만 체크 (항목 1, 2만 체크, 3은 체크 안함)
      notifier.toggleItem(1);
      notifier.toggleItem(2);

      expect(notifier.state.allRequiredChecked, false);

      await notifier.submit();

      // submit이 호출되지 않아야 함
      expect(mockRepository.submitCallCount, 0);
      expect(notifier.state.isSubmitted, false);
      expect(notifier.state.isSubmitting, false);
    });

    test('submit 실패 시 에러 상태로 전환된다', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitExceptionToThrow = Exception('서버 오류');

      await notifier.fetchItems();

      // 모든 필수 항목 체크
      notifier.toggleItem(1);
      notifier.toggleItem(2);
      notifier.toggleItem(3);

      await notifier.submit();

      expect(notifier.state.isError, true);
      expect(notifier.state.errorMessage, contains('서버 오류'));
      expect(notifier.state.isSubmitted, false);
      expect(notifier.state.isSubmitting, false);
    });

    test('submit 중복(409) 시 제출 완료로 처리된다', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitExceptionToThrow =
          Exception('이미 안전점검을 완료했습니다');

      await notifier.fetchItems();

      // 모든 필수 항목 체크
      notifier.toggleItem(1);
      notifier.toggleItem(2);
      notifier.toggleItem(3);

      await notifier.submit();

      // 중복 제출 에러는 제출 완료로 처리
      expect(notifier.state.isSubmitted, true);
      expect(notifier.state.isError, false);
      expect(notifier.state.errorMessage, isNull);
    });

    test('fetchItems → toggleAll → submit 전체 흐름 테스트', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitResultToReturn = testSubmitResult;

      // 1. 초기 상태 확인
      expect(notifier.state.categories, isNull);

      // 2. 항목 로딩
      await notifier.fetchItems();
      expect(notifier.state.isLoaded, true);
      expect(notifier.state.checkedItems.length, 4);
      expect(notifier.state.allRequiredChecked, false);

      // 3. 모든 필수 항목 체크
      notifier.toggleItem(1); // 타이어 상태 (필수)
      notifier.toggleItem(2); // 브레이크 작동 (필수)
      notifier.toggleItem(3); // 안전화 착용 (필수)
      notifier.toggleItem(4); // 장갑 착용 (선택)

      expect(notifier.state.allRequiredChecked, true);
      expect(notifier.state.checkedItemIds.length, 4);

      // 4. 제출
      await notifier.submit();

      expect(notifier.state.isSubmitted, true);
      expect(mockRepository.submitCallCount, 1);
      expect(mockRepository.lastSubmittedItemIds, containsAll([1, 2, 3, 4]));
    });

    test('선택 항목만 체크된 경우 submit이 무시된다', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitResultToReturn = testSubmitResult;

      await notifier.fetchItems();

      // 선택 항목만 체크
      notifier.toggleItem(4);

      expect(notifier.state.allRequiredChecked, false);

      await notifier.submit();

      expect(mockRepository.submitCallCount, 0);
      expect(notifier.state.isSubmitted, false);
    });

    test('일부 필수 항목과 선택 항목이 체크된 경우 submit이 무시된다', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitResultToReturn = testSubmitResult;

      await notifier.fetchItems();

      // 일부 필수 항목 + 선택 항목 체크
      notifier.toggleItem(1); // 필수
      notifier.toggleItem(2); // 필수
      notifier.toggleItem(4); // 선택 (필수 항목 3은 체크 안됨)

      expect(notifier.state.allRequiredChecked, false);

      await notifier.submit();

      expect(mockRepository.submitCallCount, 0);
      expect(notifier.state.isSubmitted, false);
    });

    test('checkedItemIds는 체크된 항목만 반환한다', () async {
      mockRepository.categoriesToReturn = testCategories;

      await notifier.fetchItems();

      notifier.toggleItem(1);
      notifier.toggleItem(3);

      final checkedIds = notifier.state.checkedItemIds;

      expect(checkedIds.length, 2);
      expect(checkedIds, containsAll([1, 3]));
      expect(checkedIds, isNot(contains(2)));
      expect(checkedIds, isNot(contains(4)));
    });
  });

  group('SafetyCheck Provider (ProviderContainer)', () {
    late ProviderContainer container;

    setUp(() {
      container = ProviderContainer();
    });

    tearDown(() {
      container.dispose();
    });

    test('초기 상태가 올바르게 설정된다', () {
      final state = container.read(safetyCheckProvider);

      expect(state.categories, isNull);
      expect(state.checkedItems, isEmpty);
      expect(state.isLoading, false);
      expect(state.isSubmitting, false);
      expect(state.isSubmitted, false);
      expect(state.errorMessage, isNull);
    });

    test('ProviderContainer로 fetchItems 호출 시 Mock 데이터가 로딩된다', () async {
      final notifier = container.read(safetyCheckProvider.notifier);

      await notifier.fetchItems();

      final state = container.read(safetyCheckProvider);

      expect(state.isLoading, false);
      expect(state.categories, isNotNull);
      expect(state.categories!.length, 2);
      expect(state.checkedItems.length, 15);

      // 카테고리 1 확인 (안전예방 장비 착용: 7개)
      expect(state.categories![0].name, '안전예방 장비 착용');
      expect(state.categories![0].items.length, 7);

      // 카테고리 2 확인 (사고 예방: 8개)
      expect(state.categories![1].name, '사고 예방');
      expect(state.categories![1].items.length, 8);
    });

    test('fetchItems 호출 시 로딩 상태로 전환된다', () async {
      final notifier = container.read(safetyCheckProvider.notifier);

      final future = notifier.fetchItems();

      // 첫 프레임에서 로딩 상태 확인
      await Future.microtask(() {});
      final loadingState = container.read(safetyCheckProvider);
      expect(loadingState.isLoading, true);

      await future;
    });

    test('Provider를 통해 toggleItem과 submit을 실행할 수 있다', () async {
      final notifier = container.read(safetyCheckProvider.notifier);

      await notifier.fetchItems();

      // 항목 토글
      notifier.toggleItem(1);
      notifier.toggleItem(2);

      var state = container.read(safetyCheckProvider);
      expect(state.checkedItems[1], true);
      expect(state.checkedItems[2], true);

      // 모든 필수 항목 체크 (ID 1-15, 총 15개)
      for (int i = 3; i <= 15; i++) {
        notifier.toggleItem(i);
      }

      state = container.read(safetyCheckProvider);
      expect(state.allRequiredChecked, true);

      // 제출
      await notifier.submit();

      state = container.read(safetyCheckProvider);
      expect(state.isSubmitted, true);
    });
  });
}
