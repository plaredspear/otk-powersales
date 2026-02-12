import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice_category.dart';
import 'package:mobile/domain/entities/notice_post_detail.dart';
import 'package:mobile/presentation/providers/notice_detail_state.dart';

void main() {
  group('NoticeDetailState', () {
    final testDetail = NoticePostDetail(
      id: 1,
      category: NoticeCategory.company,
      categoryName: '회사공지',
      title: 'Test',
      content: 'Content',
      createdAt: DateTime(2020, 8, 1),
      images: [],
    );

    test('초기 상태가 올바르게 생성된다', () {
      // When
      final state = NoticeDetailState.initial();

      // Then
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.detail, isNull);
    });

    test('toLoading이 로딩 상태로 전환한다', () {
      // Given
      final state = NoticeDetailState.initial();

      // When
      final loadingState = state.toLoading();

      // Then
      expect(loadingState.isLoading, true);
      expect(loadingState.errorMessage, isNull);
    });

    test('toError가 에러 상태로 전환한다', () {
      // Given
      final state = NoticeDetailState.initial();

      // When
      final errorState = state.toError('Network error');

      // Then
      expect(errorState.isLoading, false);
      expect(errorState.errorMessage, 'Network error');
    });

    test('toSuccess가 성공 상태로 전환한다', () {
      // Given
      final state = NoticeDetailState.initial();

      // When
      final successState = state.toSuccess(testDetail);

      // Then
      expect(successState.isLoading, false);
      expect(successState.errorMessage, isNull);
      expect(successState.detail, testDetail);
    });

    test('hasData가 올바르게 동작한다', () {
      // Given
      final emptyState = NoticeDetailState.initial();
      final filledState = emptyState.toSuccess(testDetail);

      // When & Then
      expect(emptyState.hasData, false);
      expect(filledState.hasData, true);
    });

    test('copyWith이 올바르게 동작한다', () {
      // Given
      final state = NoticeDetailState.initial();

      // When
      final newState = state.copyWith(
        isLoading: true,
        detail: testDetail,
      );

      // Then
      expect(newState.isLoading, true);
      expect(newState.detail, testDetail);
    });
  });
}
