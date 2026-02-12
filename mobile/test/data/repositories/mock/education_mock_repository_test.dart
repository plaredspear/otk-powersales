import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/entities/education_post.dart';
import 'package:mobile/domain/entities/education_post_detail.dart';
import 'package:mobile/data/repositories/mock/education_mock_repository.dart';

void main() {
  group('EducationMockRepository', () {
    late EducationMockRepository repository;

    setUp(() {
      repository = EducationMockRepository();
    });

    group('getPosts', () {
      test('시식 매뉴얼 카테고리의 게시물 목록을 반환한다', () async {
        final result = await repository.getPosts(
          category: EducationCategory.tastingManual,
        );

        expect(result.content, isNotEmpty);
        expect(result.totalCount, greaterThan(0));
      });

      test('CS/안전 카테고리의 게시물 목록을 반환한다', () async {
        final result = await repository.getPosts(
          category: EducationCategory.csSafety,
        );

        expect(result.content, isNotEmpty);
        expect(result.totalCount, greaterThan(0));
      });

      test('검색 키워드로 필터링할 수 있다', () async {
        final result = await repository.getPosts(
          category: EducationCategory.tastingManual,
          search: '진짬뽕',
        );

        expect(result.content.every((post) => post.title.contains('진짬뽕')), true);
      });

      test('검색 결과가 없으면 빈 목록을 반환한다', () async {
        final result = await repository.getPosts(
          category: EducationCategory.tastingManual,
          search: '존재하지않는키워드',
        );

        expect(result.content, isEmpty);
        expect(result.totalCount, 0);
      });

      test('페이지네이션이 올바르게 동작한다', () async {
        final result = await repository.getPosts(
          category: EducationCategory.tastingManual,
          page: 1,
          size: 2,
        );

        expect(result.content.length, lessThanOrEqualTo(2));
        expect(result.currentPage, 1);
        expect(result.size, 2);
      });

      test('페이지 번호가 범위를 벗어나면 빈 목록을 반환한다', () async {
        final result = await repository.getPosts(
          category: EducationCategory.tastingManual,
          page: 999,
          size: 10,
        );

        expect(result.content, isEmpty);
      });

      test('커스텀 데이터를 사용할 수 있다', () async {
        final customPosts = [
          EducationPost(
            id: 100,
            title: '테스트 게시물',
            createdAt: DateTime.now(),
          ),
        ];
        repository.customPosts = {
          EducationCategory.tastingManual: customPosts,
        };

        final result = await repository.getPosts(
          category: EducationCategory.tastingManual,
        );

        expect(result.content, customPosts);
      });

      test('Exception을 throw할 수 있다', () async {
        repository.exceptionToThrow = Exception('네트워크 오류');

        expect(
          () => repository.getPosts(category: EducationCategory.tastingManual),
          throwsA(isA<Exception>()),
        );
      });
    });

    group('getPostDetail', () {
      test('존재하는 게시물의 상세를 반환한다', () async {
        final result = await repository.getPostDetail(9);

        expect(result.id, 9);
        expect(result.title, isNotEmpty);
        expect(result.content, isNotEmpty);
      });

      test('이미지와 첨부파일을 포함한 상세를 반환한다', () async {
        final result = await repository.getPostDetail(9);

        expect(result.images, isNotEmpty);
        expect(result.attachments, isNotEmpty);
      });

      test('이미지와 첨부파일이 없는 상세를 반환할 수 있다', () async {
        final result = await repository.getPostDetail(7);

        expect(result.images, isEmpty);
        expect(result.attachments, isEmpty);
      });

      test('존재하지 않는 게시물은 Exception을 throw한다', () async {
        expect(
          () => repository.getPostDetail(999),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('POST_NOT_FOUND'),
            ),
          ),
        );
      });

      test('커스텀 데이터를 사용할 수 있다', () async {
        final customDetail = EducationPostDetail(
          id: 100,
          category: EducationCategory.tastingManual,
          categoryName: '시식 매뉴얼',
          title: '테스트 게시물',
          content: '테스트 내용',
          createdAt: DateTime.now(),
          images: [],
          attachments: [],
        );
        repository.customPostDetails = {100: customDetail};

        final result = await repository.getPostDetail(100);

        expect(result, customDetail);
      });

      test('Exception을 throw할 수 있다', () async {
        repository.exceptionToThrow = Exception('네트워크 오류');

        expect(
          () => repository.getPostDetail(9),
          throwsA(isA<Exception>()),
        );
      });
    });
  });
}
