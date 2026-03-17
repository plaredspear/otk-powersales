import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/promotion.dart';
import 'package:mobile/domain/repositories/promotion_repository.dart';
import 'package:mobile/presentation/providers/promotion_list_provider.dart';
import 'package:mobile/presentation/providers/promotion_list_state.dart';

// ============================================
// Fake Repository
// ============================================

class _FakePromotionRepository implements PromotionRepository {
  PromotionListResult? listResult;
  PromotionDetail? detailResult;
  Exception? exceptionToThrow;

  /// 호출된 page 값을 기록
  final List<int> calledPages = [];

  @override
  Future<PromotionListResult> getPromotions({
    String? startDate,
    String? endDate,
    String? keyword,
    int page = 0,
    int size = 20,
  }) async {
    calledPages.add(page);
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return listResult!;
  }

  @override
  Future<PromotionDetail> getPromotion(int id) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return detailResult!;
  }
}

void main() {
  group('PromotionListNotifier', () {
    late _FakePromotionRepository fakeRepository;
    late PromotionListNotifier notifier;

    setUp(() {
      fakeRepository = _FakePromotionRepository();
      notifier = PromotionListNotifier(repository: fakeRepository);
    });

    // ----------------------------------------
    // 1. 초기 상태
    // ----------------------------------------
    test('초기 상태는 당월 1일~말일, 빈 목록이다', () {
      final now = DateTime.now();
      final expectedStart =
          '${now.year}-${now.month.toString().padLeft(2, '0')}-01';
      final lastDay = DateTime(now.year, now.month + 1, 0);
      final expectedEnd =
          '${lastDay.year}-${lastDay.month.toString().padLeft(2, '0')}-${lastDay.day.toString().padLeft(2, '0')}';

      expect(notifier.state.isLoading, false);
      expect(notifier.state.isLoadingMore, false);
      expect(notifier.state.errorMessage, isNull);
      expect(notifier.state.items, isEmpty);
      expect(notifier.state.hasSearched, false);
      expect(notifier.state.startDate, expectedStart);
      expect(notifier.state.endDate, expectedEnd);
      expect(notifier.state.keyword, '');
      expect(notifier.state.currentPage, 0);
      expect(notifier.state.isLastPage, false);
    });

    // ----------------------------------------
    // 2. searchPromotions 성공
    // ----------------------------------------
    test('searchPromotions 성공 시 items가 업데이트된다', () async {
      // Given
      fakeRepository.listResult = PromotionListResult(
        items: [_sampleItem1, _sampleItem2],
        totalElements: 2,
        totalPages: 1,
        isLast: true,
      );

      // When
      await notifier.searchPromotions();

      // Then
      expect(notifier.state.isLoading, false);
      expect(notifier.state.items, [_sampleItem1, _sampleItem2]);
      expect(notifier.state.totalElements, 2);
      expect(notifier.state.totalPages, 1);
      expect(notifier.state.currentPage, 0);
      expect(notifier.state.isLastPage, true);
      expect(notifier.state.hasSearched, true);
      expect(notifier.state.errorMessage, isNull);
    });

    // ----------------------------------------
    // 3. searchPromotions 실패
    // ----------------------------------------
    test('searchPromotions 실패 시 errorMessage가 설정된다', () async {
      // Given
      fakeRepository.exceptionToThrow = Exception('네트워크 오류');

      // When
      await notifier.searchPromotions();

      // Then
      expect(notifier.state.isLoading, false);
      expect(notifier.state.errorMessage, '네트워크 오류');
      expect(notifier.state.items, isEmpty);
    });

    // ----------------------------------------
    // 4. loadNextPage 성공 시 append
    // ----------------------------------------
    test('loadNextPage 성공 시 items에 append된다', () async {
      // Given: 첫 페이지 로드
      fakeRepository.listResult = PromotionListResult(
        items: [_sampleItem1],
        totalElements: 2,
        totalPages: 2,
        isLast: false,
      );
      await notifier.searchPromotions();

      // Given: 두 번째 페이지 데이터 설정
      fakeRepository.listResult = PromotionListResult(
        items: [_sampleItem2],
        totalElements: 2,
        totalPages: 2,
        isLast: true,
      );

      // When
      await notifier.loadNextPage();

      // Then
      expect(notifier.state.items, [_sampleItem1, _sampleItem2]);
      expect(notifier.state.currentPage, 1);
      expect(notifier.state.isLastPage, true);
      expect(notifier.state.isLoadingMore, false);
      expect(fakeRepository.calledPages, [0, 1]);
    });

    // ----------------------------------------
    // 5. loadNextPage - isLastPage이면 호출 안 됨
    // ----------------------------------------
    test('loadNextPage는 isLastPage일 때 호출되지 않는다', () async {
      // Given: 마지막 페이지인 상태
      fakeRepository.listResult = PromotionListResult(
        items: [_sampleItem1],
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      );
      await notifier.searchPromotions();
      fakeRepository.calledPages.clear();

      // When
      await notifier.loadNextPage();

      // Then: repository 호출 없음
      expect(fakeRepository.calledPages, isEmpty);
      expect(notifier.state.items.length, 1);
    });

    // ----------------------------------------
    // 6. loadNextPage - isLoadingMore일 때 중복 호출 방지
    // ----------------------------------------
    test('loadNextPage는 isLoadingMore일 때 중복 호출되지 않는다', () async {
      // Given: 첫 페이지 로드 후 다음 페이지가 있는 상태
      fakeRepository.listResult = PromotionListResult(
        items: [_sampleItem1],
        totalElements: 10,
        totalPages: 5,
        isLast: false,
      );
      await notifier.searchPromotions();
      fakeRepository.calledPages.clear();

      // Given: 느린 응답 시뮬레이션
      fakeRepository.listResult = PromotionListResult(
        items: [_sampleItem2],
        totalElements: 10,
        totalPages: 5,
        isLast: false,
      );

      // When: 동시에 두 번 호출
      final future1 = notifier.loadNextPage();
      final future2 = notifier.loadNextPage(); // 이미 loadingMore 상태
      await Future.wait([future1, future2]);

      // Then: repository는 한 번만 호출됨
      expect(fakeRepository.calledPages, [1]);
    });

    // ----------------------------------------
    // 7. updateDateRange 필터 변경
    // ----------------------------------------
    test('updateDateRange로 기간 필터가 변경된다', () {
      // When
      notifier.updateDateRange('2025-01-01', '2025-01-31');

      // Then
      expect(notifier.state.startDate, '2025-01-01');
      expect(notifier.state.endDate, '2025-01-31');
    });

    // ----------------------------------------
    // 8. updateKeyword 검색어 변경
    // ----------------------------------------
    test('updateKeyword로 검색어가 변경된다', () {
      // When
      notifier.updateKeyword('이마트');

      // Then
      expect(notifier.state.keyword, '이마트');
    });

    // ----------------------------------------
    // 9. clearError 동작
    // ----------------------------------------
    test('clearError는 에러 메시지를 초기화한다', () async {
      // Given
      fakeRepository.exceptionToThrow = Exception('서버 오류');
      await notifier.searchPromotions();
      expect(notifier.state.errorMessage, isNotNull);

      // When
      notifier.clearError();

      // Then
      expect(notifier.state.errorMessage, isNull);
    });

    // ----------------------------------------
    // 추가: initialize는 searchPromotions를 호출한다
    // ----------------------------------------
    test('initialize는 searchPromotions를 호출한다', () async {
      // Given
      fakeRepository.listResult = PromotionListResult(
        items: [_sampleItem1],
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      );

      // When
      await notifier.initialize();

      // Then
      expect(notifier.state.hasSearched, true);
      expect(notifier.state.items, [_sampleItem1]);
    });

    // ----------------------------------------
    // 추가: PromotionListState helper getters
    // ----------------------------------------
    test('isEmpty는 검색 후 결과 없을 때 true이다', () async {
      // Given
      fakeRepository.listResult = const PromotionListResult(
        items: [],
        totalElements: 0,
        totalPages: 0,
        isLast: true,
      );

      // When
      await notifier.searchPromotions();

      // Then
      expect(notifier.state.isEmpty, true);
      expect(notifier.state.hasNextPage, false);
    });

    test('isEmpty는 검색 전에는 false이다', () {
      expect(notifier.state.isEmpty, false);
    });
  });
}

// ============================================
// Test Data
// ============================================

const _sampleItem1 = PromotionItem(
  id: 1,
  promotionNumber: 'P-2025-001',
  promotionName: '이마트 죽전점 행사',
  promotionTypeName: '시식행사',
  accountName: '이마트 죽전점',
  startDate: '2025-03-01',
  endDate: '2025-03-15',
  category: '라면',
  standLocation: '1층 중앙',
  targetAmount: 5000000,
  actualAmount: 3500000,
  isClosed: false,
  myScheduleDate: '2025-03-10',
);

const _sampleItem2 = PromotionItem(
  id: 2,
  promotionNumber: 'P-2025-002',
  promotionName: '홈플러스 수지점 행사',
  promotionTypeName: '엔드매대',
  accountName: '홈플러스 수지점',
  startDate: '2025-03-05',
  endDate: '2025-03-20',
  category: '소스',
  standLocation: '2층 식품관',
  targetAmount: 3000000,
  actualAmount: 2800000,
  isClosed: true,
  myScheduleDate: null,
);
