import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/entities/education_post.dart';
import 'package:mobile/domain/entities/education_post_detail.dart';
import 'package:mobile/domain/repositories/education_repository.dart';
import 'package:mobile/domain/usecases/get_education_posts_usecase.dart';
import 'package:mobile/presentation/providers/education_posts_provider.dart';

/// Mock Repository for testing
class _MockEducationRepository implements EducationRepository {
  EducationPostPage? mockPostPage;
  Exception? mockError;

  @override
  Future<EducationPostPage> getPosts({
    required EducationCategory category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    // Simulate API delay
    await Future.delayed(const Duration(milliseconds: 100));
    if (mockError != null) throw mockError!;
    if (mockPostPage == null) throw Exception('게시물을 찾을 수 없습니다');
    return mockPostPage!;
  }

  @override
  Future<EducationPostDetail> getPostDetail(int postId) async {
    throw UnimplementedError();
  }
}

void main() {
  group('EducationPostsNotifier', () {
    late _MockEducationRepository mockRepository;
    late GetEducationPostsUseCase useCase;
    late EducationPostsNotifier notifier;

    final testPosts = [
      EducationPost(
        id: 9,
        title: '진짬뽕 시식 매뉴얼',
        createdAt: DateTime.parse('2020-08-10T00:00:00.000Z'),
      ),
      EducationPost(
        id: 7,
        title: '미숫가루 시식매뉴얼',
        createdAt: DateTime.parse('2020-08-06T00:00:00.000Z'),
      ),
    ];

    final testPage = EducationPostPage(
      content: testPosts,
      totalCount: 4,
      totalPages: 1,
      currentPage: 1,
      size: 10,
    );

    setUp(() {
      mockRepository = _MockEducationRepository();
      useCase = GetEducationPostsUseCase(mockRepository);
      notifier = EducationPostsNotifier(useCase);
    });

    test('초기 상태는 tastingManual 카테고리이다', () {
      expect(notifier.state.category, EducationCategory.tastingManual);
      expect(notifier.state.currentPage, 1);
      expect(notifier.state.searchKeyword, null);
    });

    test('fetchPosts 성공 시 데이터 상태로 전환한다', () async {
      // Given
      mockRepository.mockPostPage = testPage;

      // When
      await notifier.fetchPosts();

      // Then
      expect(notifier.state.isLoaded, true);
      expect(notifier.state.postPage, testPage);
      expect(notifier.state.posts.length, 2);
      expect(notifier.state.totalCount, 4);
      expect(notifier.state.isError, false);
    });

    test('fetchPosts 실패 시 에러 상태로 전환한다', () async {
      // Given
      mockRepository.mockError = Exception('네트워크 오류');

      // When
      await notifier.fetchPosts();

      // Then
      expect(notifier.state.isError, true);
      expect(notifier.state.errorMessage, contains('네트워크 오류'));
      expect(notifier.state.isLoaded, false);
    });

    test('selectCategory는 카테고리를 변경하고 데이터를 조회한다', () async {
      // Given
      mockRepository.mockPostPage = testPage;

      // When
      await notifier.selectCategory(EducationCategory.csSafety);

      // Then
      expect(notifier.state.category, EducationCategory.csSafety);
      expect(notifier.state.currentPage, 1);
      expect(notifier.state.searchKeyword, null);
      expect(notifier.state.isLoaded, true);
    });

    test('selectCategory는 같은 카테고리 선택 시 조회하지 않는다', () async {
      // Given
      mockRepository.mockPostPage = testPage;
      await notifier.fetchPosts(); // 초기 로드
      final previousState = notifier.state;

      // When
      await notifier.selectCategory(EducationCategory.tastingManual);

      // Then
      expect(notifier.state, previousState);
    });

    test('search는 검색어를 설정하고 첫 페이지로 리셋한다', () async {
      // Given
      mockRepository.mockPostPage = testPage;

      // When
      await notifier.search('진짬뽕');

      // Then
      expect(notifier.state.searchKeyword, '진짬뽕');
      expect(notifier.state.currentPage, 1);
      expect(notifier.state.isLoaded, true);
    });

    test('clearSearch는 검색어를 초기화한다', () async {
      // Given
      mockRepository.mockPostPage = testPage;
      await notifier.search('진짬뽕');

      // When
      await notifier.clearSearch();

      // Then
      expect(notifier.state.searchKeyword, null);
      expect(notifier.state.currentPage, 1);
    });

    test('changePage는 페이지를 변경한다', () async {
      // Given
      mockRepository.mockPostPage = testPage.copyWith(currentPage: 2);

      // When
      await notifier.changePage(2);

      // Then
      expect(notifier.state.currentPage, 2);
      expect(notifier.state.isLoaded, true);
    });

    test('changePage는 같은 페이지 선택 시 조회하지 않는다', () async {
      // Given
      mockRepository.mockPostPage = testPage;
      await notifier.fetchPosts(); // 초기 로드
      final previousState = notifier.state;

      // When
      await notifier.changePage(1);

      // Then
      expect(notifier.state, previousState);
    });

    test('nextPage는 다음 페이지로 이동한다', () async {
      // Given
      mockRepository.mockPostPage = testPage.copyWith(
        currentPage: 1,
        totalPages: 3,
      );
      await notifier.fetchPosts();

      // When
      mockRepository.mockPostPage = testPage.copyWith(currentPage: 2);
      await notifier.nextPage();

      // Then
      expect(notifier.state.currentPage, 2);
    });

    test('nextPage는 마지막 페이지에서 호출 시 이동하지 않는다', () async {
      // Given
      mockRepository.mockPostPage = testPage.copyWith(
        currentPage: 3,
        totalPages: 3,
      );
      await notifier.fetchPosts();
      final previousState = notifier.state;

      // When
      await notifier.nextPage();

      // Then
      expect(notifier.state.currentPage, previousState.currentPage);
    });

    test('previousPage는 이전 페이지로 이동한다', () async {
      // Given
      mockRepository.mockPostPage = testPage.copyWith(currentPage: 2);
      await notifier.fetchPosts();
      await notifier.changePage(2);

      // When
      mockRepository.mockPostPage = testPage.copyWith(currentPage: 1);
      await notifier.previousPage();

      // Then
      expect(notifier.state.currentPage, 1);
    });

    test('previousPage는 첫 페이지에서 호출 시 이동하지 않는다', () async {
      // Given
      mockRepository.mockPostPage = testPage;
      await notifier.fetchPosts();
      final previousState = notifier.state;

      // When
      await notifier.previousPage();

      // Then
      expect(notifier.state.currentPage, previousState.currentPage);
    });

    test('refresh는 현재 상태로 데이터를 다시 조회한다', () async {
      // Given
      mockRepository.mockPostPage = testPage;
      await notifier.fetchPosts();

      // Modify data
      mockRepository.mockPostPage = testPage.copyWith(totalCount: 5);

      // When
      await notifier.refresh();

      // Then
      expect(notifier.state.totalCount, 5);
      expect(notifier.state.isLoaded, true);
    });
  });
}
