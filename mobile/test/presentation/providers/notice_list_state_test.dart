import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice_category.dart';
import 'package:mobile/domain/entities/notice_post.dart';
import 'package:mobile/presentation/providers/notice_list_state.dart';

void main() {
  group('NoticeListState', () {
    final testPosts = [
      NoticePost(
        id: 1,
        category: NoticeCategory.company,
        categoryName: '회사공지',
        title: 'Test',
        createdAt: DateTime(2020, 8, 1),
      ),
    ];

    test('초기 상태가 올바르게 생성된다', () {
      // When
      final state = NoticeListState.initial();

      // Then
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.posts, isEmpty);
      expect(state.totalCount, 0);
      expect(state.currentPage, 1);
      expect(state.hasSearched, false);
    });

    test('toLoading이 로딩 상태로 전환한다', () {
      // Given
      final state = NoticeListState.initial();

      // When
      final loadingState = state.toLoading();

      // Then
      expect(loadingState.isLoading, true);
      expect(loadingState.errorMessage, isNull);
    });

    test('toError가 에러 상태로 전환한다', () {
      // Given
      final state = NoticeListState.initial();

      // When
      final errorState = state.toError('Network error');

      // Then
      expect(errorState.isLoading, false);
      expect(errorState.errorMessage, 'Network error');
    });

    test('hasResults가 올바르게 동작한다', () {
      // Given
      final emptyState = NoticeListState.initial();
      final filledState = emptyState.copyWith(posts: testPosts);

      // When & Then
      expect(emptyState.hasResults, false);
      expect(filledState.hasResults, true);
    });

    test('isEmpty가 올바르게 동작한다', () {
      // Given
      final notSearchedState = NoticeListState.initial();
      final searchedEmptyState = notSearchedState.copyWith(
        hasSearched: true,
        posts: [],
      );
      final searchedFilledState = searchedEmptyState.copyWith(posts: testPosts);

      // When & Then
      expect(notSearchedState.isEmpty, false); // 검색 전
      expect(searchedEmptyState.isEmpty, true); // 검색 후 결과 없음
      expect(searchedFilledState.isEmpty, false); // 검색 후 결과 있음
    });

    test('hasNextPage가 올바르게 동작한다', () {
      // Given
      final state = NoticeListState.initial().copyWith(
        currentPage: 1,
        totalPages: 3,
      );

      // When & Then
      expect(state.hasNextPage, true);
      expect(state.copyWith(currentPage: 3).hasNextPage, false);
    });

    test('hasPreviousPage가 올바르게 동작한다', () {
      // Given
      final state = NoticeListState.initial().copyWith(
        currentPage: 2,
      );

      // When & Then
      expect(state.hasPreviousPage, true);
      expect(state.copyWith(currentPage: 1).hasPreviousPage, false);
    });

    test('hasActiveFilter가 올바르게 동작한다', () {
      // Given
      final noFilterState = NoticeListState.initial();
      final categoryFilterState = noFilterState.copyWith(
        selectedCategory: NoticeCategory.company,
      );
      final searchFilterState = noFilterState.copyWith(
        searchKeyword: 'test',
      );

      // When & Then
      expect(noFilterState.hasActiveFilter, false);
      expect(categoryFilterState.hasActiveFilter, true);
      expect(searchFilterState.hasActiveFilter, true);
    });

    test('copyWith이 올바르게 동작한다', () {
      // Given
      final state = NoticeListState.initial();

      // When
      final newState = state.copyWith(
        isLoading: true,
        posts: testPosts,
        selectedCategory: NoticeCategory.company,
      );

      // Then
      expect(newState.isLoading, true);
      expect(newState.posts, testPosts);
      expect(newState.selectedCategory, NoticeCategory.company);
      expect(newState.currentPage, state.currentPage); // 변경 안 된 필드 유지
    });

    test('copyWith에서 필터 초기화가 올바르게 동작한다', () {
      // Given
      final state = NoticeListState.initial().copyWith(
        selectedCategory: NoticeCategory.company,
        searchKeyword: 'test',
      );

      // When
      final clearedState = state.copyWith(
        clearCategoryFilter: true,
        clearSearchKeyword: true,
      );

      // Then
      expect(clearedState.selectedCategory, isNull);
      expect(clearedState.searchKeyword, isNull);
    });
  });
}
