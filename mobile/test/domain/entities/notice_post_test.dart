import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice_category.dart';
import 'package:mobile/domain/entities/notice_post.dart';

void main() {
  group('NoticePost', () {
    final testDate = DateTime(2020, 8, 17);

    final testPost = NoticePost(
      id: 5,
      category: NoticeCategory.branch,
      categoryName: '지점공지',
      title: '마진선조장 공지사항',
      createdAt: testDate,
    );

    test('엔티티가 올바르게 생성된다', () {
      // When & Then
      expect(testPost.id, 5);
      expect(testPost.category, NoticeCategory.branch);
      expect(testPost.categoryName, '지점공지');
      expect(testPost.title, '마진선조장 공지사항');
      expect(testPost.createdAt, testDate);
    });

    test('copyWith이 올바르게 동작한다', () {
      // When
      final copied = testPost.copyWith(
        title: '변경된 제목',
        category: NoticeCategory.company,
      );

      // Then
      expect(copied.title, '변경된 제목');
      expect(copied.category, NoticeCategory.company);
      expect(copied.id, testPost.id); // 변경되지 않은 필드 유지
    });

    test('toJson과 fromJson이 정확히 동작한다', () {
      // When
      final json = testPost.toJson();
      final restored = NoticePost.fromJson(json);

      // Then
      expect(restored, testPost);
      expect(json['id'], 5);
      expect(json['category'], 'BRANCH');
      expect(json['categoryName'], '지점공지');
    });

    test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
      // Given
      final post1 = NoticePost(
        id: 1,
        category: NoticeCategory.company,
        categoryName: '회사공지',
        title: 'Test',
        createdAt: testDate,
      );
      final post2 = NoticePost(
        id: 1,
        category: NoticeCategory.company,
        categoryName: '회사공지',
        title: 'Test',
        createdAt: testDate,
      );

      // When & Then
      expect(post1, post2);
      expect(post1.hashCode, post2.hashCode);
    });
  });

  group('NoticePostPage', () {
    final testPosts = [
      NoticePost(
        id: 1,
        category: NoticeCategory.company,
        categoryName: '회사공지',
        title: 'Test 1',
        createdAt: DateTime(2020, 8, 1),
      ),
      NoticePost(
        id: 2,
        category: NoticeCategory.branch,
        categoryName: '지점공지',
        title: 'Test 2',
        createdAt: DateTime(2020, 8, 2),
      ),
    ];

    final testPage = NoticePostPage(
      content: testPosts,
      totalCount: 5,
      totalPages: 1,
      currentPage: 1,
      size: 10,
    );

    test('페이지 엔티티가 올바르게 생성된다', () {
      // When & Then
      expect(testPage.content.length, 2);
      expect(testPage.totalCount, 5);
      expect(testPage.totalPages, 1);
      expect(testPage.currentPage, 1);
      expect(testPage.size, 10);
    });

    test('copyWith이 올바르게 동작한다', () {
      // When
      final copied = testPage.copyWith(currentPage: 2);

      // Then
      expect(copied.currentPage, 2);
      expect(copied.totalCount, testPage.totalCount);
    });

    test('toJson과 fromJson이 정확히 동작한다', () {
      // When
      final json = testPage.toJson();
      final restored = NoticePostPage.fromJson(json);

      // Then
      expect(restored, testPage);
      expect(restored.content.length, 2);
    });
  });
}
