import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/notice_remote_datasource.dart';
import 'package:mobile/data/models/notice_post_model.dart';
import 'package:mobile/data/models/notice_post_detail_model.dart';
import 'package:mobile/data/repositories/notice_repository_impl.dart';
import 'package:mobile/domain/entities/notice_category.dart';

class _MockNoticeRemoteDataSource implements NoticeRemoteDataSource {
  NoticePostPageModel? pageResult;
  NoticePostDetailModel? detailResult;
  Exception? error;

  String? lastCategory;
  String? lastSearch;
  int? lastPage;
  int? lastSize;

  @override
  Future<NoticePostPageModel> getPosts({
    String? category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    lastCategory = category;
    lastSearch = search;
    lastPage = page;
    lastSize = size;

    if (error != null) throw error!;
    return pageResult!;
  }

  @override
  Future<NoticePostDetailModel> getPostDetail(int noticeId) async {
    if (error != null) throw error!;
    return detailResult!;
  }
}

void main() {
  group('NoticeRepositoryImpl', () {
    late _MockNoticeRemoteDataSource dataSource;
    late NoticeRepositoryImpl repository;

    setUp(() {
      dataSource = _MockNoticeRemoteDataSource();
      repository = NoticeRepositoryImpl(remoteDataSource: dataSource);
    });

    group('getPosts', () {
      test('공지사항 목록 조회가 성공한다', () async {
        // Given
        final testModels = [
          NoticePostModel(
            id: 1,
            category: 'COMPANY',
            categoryName: '회사공지',
            title: 'Test',
            createdAt: '2020-08-01T00:00:00',
          ),
        ];
        dataSource.pageResult = NoticePostPageModel(
          content: testModels,
          totalCount: 1,
          totalPages: 1,
          currentPage: 1,
          size: 10,
        );

        // When
        final result = await repository.getPosts();

        // Then
        expect(result.content.length, 1);
        expect(result.content[0].category, NoticeCategory.company);
        expect(result.totalCount, 1);
      });

      test('분류 필터가 올바르게 변환되어 전달된다', () async {
        // Given
        dataSource.pageResult = NoticePostPageModel(
          content: [],
          totalCount: 0,
          totalPages: 0,
          currentPage: 1,
          size: 10,
        );

        // When
        await repository.getPosts(category: NoticeCategory.company);

        // Then
        expect(dataSource.lastCategory, 'COMPANY');
      });

      test('분류가 null이면 null로 전달된다', () async {
        // Given
        dataSource.pageResult = NoticePostPageModel(
          content: [],
          totalCount: 0,
          totalPages: 0,
          currentPage: 1,
          size: 10,
        );

        // When
        await repository.getPosts(category: null);

        // Then
        expect(dataSource.lastCategory, isNull);
      });

      test('검색 키워드가 올바르게 전달된다', () async {
        // Given
        dataSource.pageResult = NoticePostPageModel(
          content: [],
          totalCount: 0,
          totalPages: 0,
          currentPage: 1,
          size: 10,
        );

        // When
        await repository.getPosts(search: '진라면');

        // Then
        expect(dataSource.lastSearch, '진라면');
      });

      test('페이지 정보가 올바르게 전달된다', () async {
        // Given
        dataSource.pageResult = NoticePostPageModel(
          content: [],
          totalCount: 0,
          totalPages: 0,
          currentPage: 2,
          size: 20,
        );

        // When
        await repository.getPosts(page: 2, size: 20);

        // Then
        expect(dataSource.lastPage, 2);
        expect(dataSource.lastSize, 20);
      });

      test('DataSource에서 에러가 발생하면 전파된다', () async {
        // Given
        dataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => repository.getPosts(),
          throwsException,
        );
      });
    });

    group('getPostDetail', () {
      test('공지사항 상세 조회가 성공한다', () async {
        // Given
        dataSource.detailResult = NoticePostDetailModel(
          id: 4,
          category: 'COMPANY',
          categoryName: '회사공지',
          title: '진라면 포장지 변경',
          content: '업 무 연 락...',
          createdAt: '2020-08-09T00:00:00',
          images: [],
        );

        // When
        final result = await repository.getPostDetail(4);

        // Then
        expect(result.id, 4);
        expect(result.category, NoticeCategory.company);
        expect(result.title, '진라면 포장지 변경');
      });

      test('이미지가 포함된 상세 조회가 성공한다', () async {
        // Given
        dataSource.detailResult = NoticePostDetailModel(
          id: 4,
          category: 'COMPANY',
          categoryName: '회사공지',
          title: '진라면 포장지 변경',
          content: '업 무 연 락...',
          createdAt: '2020-08-09T00:00:00',
          images: [
            NoticeImageModel(
              id: 1,
              url: 'https://example.com/image1.jpg',
              sortOrder: 1,
            ),
          ],
        );

        // When
        final result = await repository.getPostDetail(4);

        // Then
        expect(result.images.length, 1);
        expect(result.images[0].url, 'https://example.com/image1.jpg');
      });

      test('DataSource에서 에러가 발생하면 전파된다', () async {
        // Given
        dataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => repository.getPostDetail(1),
          throwsException,
        );
      });
    });
  });
}
