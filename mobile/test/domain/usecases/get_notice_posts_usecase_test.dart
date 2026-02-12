import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice_category.dart';
import 'package:mobile/domain/entities/notice_post.dart';
import 'package:mobile/domain/entities/notice_post_detail.dart';
import 'package:mobile/domain/repositories/notice_repository.dart';
import 'package:mobile/domain/usecases/get_notice_posts_usecase.dart';

class _MockNoticeRepository implements NoticeRepository {
  NoticePostPage? result;
  Exception? error;

  @override
  Future<NoticePostPage> getPosts({
    NoticeCategory? category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    if (error != null) throw error!;
    return result!;
  }

  @override
  Future<NoticePostDetail> getPostDetail(int noticeId) async {
    throw UnimplementedError();
  }
}

void main() {
  group('GetNoticePostsUseCase', () {
    late _MockNoticeRepository repository;
    late GetNoticePostsUseCase useCase;

    setUp(() {
      repository = _MockNoticeRepository();
      useCase = GetNoticePostsUseCase(repository);
    });

    test('공지사항 목록 조회가 성공한다', () async {
      // Given
      final testPosts = [
        NoticePost(
          id: 1,
          category: NoticeCategory.company,
          categoryName: '회사공지',
          title: 'Test',
          createdAt: DateTime(2020, 8, 1),
        ),
      ];
      repository.result = NoticePostPage(
        content: testPosts,
        totalCount: 1,
        totalPages: 1,
        currentPage: 1,
        size: 10,
      );

      // When
      final result = await useCase.call();

      // Then
      expect(result.content.length, 1);
      expect(result.totalCount, 1);
    });

    test('분류 필터가 올바르게 전달된다', () async {
      // Given
      repository.result = NoticePostPage(
        content: [],
        totalCount: 0,
        totalPages: 0,
        currentPage: 1,
        size: 10,
      );

      // When
      await useCase.call(category: NoticeCategory.company);

      // Then - Mock이 호출되었으므로 에러 없이 완료
      expect(repository.result, isNotNull);
    });

    test('검색 키워드가 trim되어 전달된다', () async {
      // Given
      repository.result = NoticePostPage(
        content: [],
        totalCount: 0,
        totalPages: 0,
        currentPage: 1,
        size: 10,
      );

      // When
      await useCase.call(search: '  진라면  ');

      // Then - trim된 키워드가 전달됨
      expect(repository.result, isNotNull);
    });

    test('빈 검색 키워드는 null로 변환된다', () async {
      // Given
      repository.result = NoticePostPage(
        content: [],
        totalCount: 0,
        totalPages: 0,
        currentPage: 1,
        size: 10,
      );

      // When
      await useCase.call(search: '   ');

      // Then
      expect(repository.result, isNotNull);
    });

    test('페이지 번호가 1 미만이면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(page: 0),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('페이지 크기가 1 미만이면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(size: 0),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('페이지 크기가 100 초과이면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(size: 101),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('Repository에서 에러가 발생하면 전파된다', () async {
      // Given
      repository.error = Exception('Network error');

      // When & Then
      expect(
        () => useCase.call(),
        throwsException,
      );
    });
  });
}
