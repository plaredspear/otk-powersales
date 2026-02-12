import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/entities/education_post.dart';
import 'package:mobile/domain/entities/education_post_detail.dart';
import 'package:mobile/domain/repositories/education_repository.dart';
import 'package:mobile/domain/usecases/get_education_posts_usecase.dart';

/// 테스트용 Mock EducationRepository
class MockEducationRepository implements EducationRepository {
  EducationPostPage? postPage;
  Exception? exceptionToThrow;
  int callCount = 0;

  // 호출 파라미터 기록
  EducationCategory? lastCategory;
  String? lastSearch;
  int? lastPage;
  int? lastSize;

  @override
  Future<EducationPostPage> getPosts({
    required EducationCategory category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    callCount++;
    lastCategory = category;
    lastSearch = search;
    lastPage = page;
    lastSize = size;

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return postPage!;
  }

  @override
  Future<EducationPostDetail> getPostDetail(int postId) async {
    throw UnimplementedError();
  }
}

void main() {
  group('GetEducationPostsUseCase', () {
    late GetEducationPostsUseCase useCase;
    late MockEducationRepository mockRepository;

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
      mockRepository = MockEducationRepository();
      useCase = GetEducationPostsUseCase(mockRepository);
    });

    group('정상 케이스', () {
      test('카테고리별 게시물 목록을 반환한다', () async {
        mockRepository.postPage = testPage;

        final result = await useCase(category: EducationCategory.tastingManual);

        expect(result, testPage);
        expect(result.content.length, 2);
        expect(result.totalCount, 4);
      });

      test('Repository에 올바른 파라미터를 전달한다', () async {
        mockRepository.postPage = testPage;

        await useCase(
          category: EducationCategory.tastingManual,
          search: '진짬뽕',
          page: 2,
          size: 20,
        );

        expect(mockRepository.lastCategory, EducationCategory.tastingManual);
        expect(mockRepository.lastSearch, '진짬뽕');
        expect(mockRepository.lastPage, 2);
        expect(mockRepository.lastSize, 20);
      });

      test('Repository를 정확히 1회 호출한다', () async {
        mockRepository.postPage = testPage;

        await useCase(category: EducationCategory.tastingManual);

        expect(mockRepository.callCount, 1);
      });

      test('검색 없이 카테고리만으로 조회할 수 있다', () async {
        mockRepository.postPage = testPage;

        await useCase(
          category: EducationCategory.csSafety,
          page: 1,
          size: 10,
        );

        expect(mockRepository.lastCategory, EducationCategory.csSafety);
        expect(mockRepository.lastSearch, isNull);
      });

      test('빈 목록을 반환할 수 있다', () async {
        mockRepository.postPage = EducationPostPage.empty();

        final result = await useCase(category: EducationCategory.evaluation);

        expect(result.content, isEmpty);
        expect(result.totalCount, 0);
      });
    });

    group('유효성 검증', () {
      test('페이지 번호가 1보다 작으면 ArgumentError를 발생시킨다', () async {
        expect(
          () => useCase(
            category: EducationCategory.tastingManual,
            page: 0,
          ),
          throwsA(isA<ArgumentError>()),
        );
      });

      test('페이지 크기가 1보다 작으면 ArgumentError를 발생시킨다', () async {
        expect(
          () => useCase(
            category: EducationCategory.tastingManual,
            size: 0,
          ),
          throwsA(isA<ArgumentError>()),
        );
      });

      test('페이지 크기가 100보다 크면 ArgumentError를 발생시킨다', () async {
        expect(
          () => useCase(
            category: EducationCategory.tastingManual,
            size: 101,
          ),
          throwsA(isA<ArgumentError>()),
        );
      });
    });

    group('에러 처리', () {
      test('Repository에서 Exception 발생 시 그대로 전파한다', () async {
        mockRepository.exceptionToThrow = Exception('네트워크 오류');

        expect(
          () => useCase(category: EducationCategory.tastingManual),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('네트워크 오류'),
            ),
          ),
        );
      });
    });
  });
}
