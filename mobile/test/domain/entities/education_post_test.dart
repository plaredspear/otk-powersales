import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_post.dart';

void main() {
  group('EducationPost', () {
    final testCreatedAt = DateTime.parse('2020-08-10T00:00:00.000Z');

    final testPost = EducationPost(
      id: 9,
      title: '진짬뽕 시식 매뉴얼',
      createdAt: testCreatedAt,
    );

    final testJson = {
      'id': 9,
      'title': '진짬뽕 시식 매뉴얼',
      'createdAt': '2020-08-10T00:00:00.000Z',
    };

    group('생성', () {
      test('EducationPost 엔티티가 올바르게 생성된다', () {
        expect(testPost.id, 9);
        expect(testPost.title, '진짬뽕 시식 매뉴얼');
        expect(testPost.createdAt, testCreatedAt);
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final copied = testPost.copyWith(title: '미숫가루 시식매뉴얼');

        expect(copied.id, testPost.id);
        expect(copied.title, '미숫가루 시식매뉴얼');
        expect(copied.createdAt, testPost.createdAt);
      });
    });

    group('toJson', () {
      test('올바른 JSON Map을 반환한다', () {
        final result = testPost.toJson();

        expect(result['id'], 9);
        expect(result['title'], '진짬뽕 시식 매뉴얼');
        expect(result['createdAt'], isA<String>());
      });
    });

    group('fromJson', () {
      test('JSON Map에서 올바르게 생성된다', () {
        final result = EducationPost.fromJson(testJson);

        expect(result.id, 9);
        expect(result.title, '진짬뽕 시식 매뉴얼');
        expect(result.createdAt, testCreatedAt);
      });
    });

    group('round trip', () {
      test('toJson -> fromJson 변환이 일관성 있다', () {
        final json = testPost.toJson();
        final restored = EducationPost.fromJson(json);

        expect(restored, testPost);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 EducationPost는 동일하다', () {
        final post1 = EducationPost(
          id: 9,
          title: '진짬뽕 시식 매뉴얼',
          createdAt: testCreatedAt,
        );
        final post2 = EducationPost(
          id: 9,
          title: '진짬뽕 시식 매뉴얼',
          createdAt: testCreatedAt,
        );

        expect(post1, post2);
      });

      test('다른 값을 가진 두 EducationPost는 동일하지 않다', () {
        final other = EducationPost(
          id: 7,
          title: '미숫가루 시식매뉴얼',
          createdAt: DateTime.parse('2020-08-06T00:00:00.000Z'),
        );

        expect(testPost, isNot(other));
      });
    });
  });

  group('EducationPostPage', () {
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

    group('생성', () {
      test('EducationPostPage 엔티티가 올바르게 생성된다', () {
        expect(testPage.content.length, 2);
        expect(testPage.totalCount, 4);
        expect(testPage.totalPages, 1);
        expect(testPage.currentPage, 1);
        expect(testPage.size, 10);
      });

      test('빈 페이지를 생성할 수 있다', () {
        final emptyPage = EducationPostPage.empty();

        expect(emptyPage.content, isEmpty);
        expect(emptyPage.totalCount, 0);
        expect(emptyPage.totalPages, 0);
        expect(emptyPage.currentPage, 1);
        expect(emptyPage.size, 10);
      });
    });

    group('page state', () {
      test('isLastPage returns true when current page equals total pages', () {
        expect(testPage.isLastPage, true);
      });

      test('isLastPage returns false when current page is less than total pages', () {
        final page = testPage.copyWith(currentPage: 1, totalPages: 3);
        expect(page.isLastPage, false);
      });

      test('isFirstPage returns true when current page is 1', () {
        expect(testPage.isFirstPage, true);
      });

      test('isFirstPage returns false when current page is greater than 1', () {
        final page = testPage.copyWith(currentPage: 2);
        expect(page.isFirstPage, false);
      });
    });

    group('round trip', () {
      test('toJson -> fromJson 변환이 일관성 있다', () {
        final json = testPage.toJson();
        final restored = EducationPostPage.fromJson(json);

        expect(restored, testPage);
      });
    });
  });
}
