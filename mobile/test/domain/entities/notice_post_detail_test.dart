import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice_category.dart';
import 'package:mobile/domain/entities/notice_post_detail.dart';

void main() {
  group('NoticeImage', () {
    final testImage = NoticeImage(
      id: 1,
      url: 'https://example.com/image1.jpg',
      sortOrder: 1,
    );

    test('이미지 엔티티가 올바르게 생성된다', () {
      // When & Then
      expect(testImage.id, 1);
      expect(testImage.url, 'https://example.com/image1.jpg');
      expect(testImage.sortOrder, 1);
    });

    test('copyWith이 올바르게 동작한다', () {
      // When
      final copied = testImage.copyWith(url: 'https://example.com/image2.jpg');

      // Then
      expect(copied.url, 'https://example.com/image2.jpg');
      expect(copied.id, testImage.id);
    });

    test('toJson과 fromJson이 정확히 동작한다', () {
      // When
      final json = testImage.toJson();
      final restored = NoticeImage.fromJson(json);

      // Then
      expect(restored, testImage);
    });
  });

  group('NoticePostDetail', () {
    final testDate = DateTime(2020, 8, 9);
    final testImages = [
      NoticeImage(
        id: 1,
        url: 'https://example.com/image1.jpg',
        sortOrder: 1,
      ),
      NoticeImage(
        id: 2,
        url: 'https://example.com/image2.jpg',
        sortOrder: 2,
      ),
    ];

    final testDetail = NoticePostDetail(
      id: 4,
      category: NoticeCategory.company,
      categoryName: '회사공지',
      title: '진라면 포장지 변경',
      content: '업 무 연 락\n진라면 디자인 변경...',
      createdAt: testDate,
      images: testImages,
    );

    test('상세 엔티티가 올바르게 생성된다', () {
      // When & Then
      expect(testDetail.id, 4);
      expect(testDetail.category, NoticeCategory.company);
      expect(testDetail.title, '진라면 포장지 변경');
      expect(testDetail.content, '업 무 연 락\n진라면 디자인 변경...');
      expect(testDetail.images.length, 2);
    });

    test('이미지가 없는 상세도 생성 가능하다', () {
      // When
      final detailWithoutImages = NoticePostDetail(
        id: 10,
        category: NoticeCategory.branch,
        categoryName: '지점공지',
        title: 'Test',
        content: 'Content',
        createdAt: testDate,
        images: [],
      );

      // Then
      expect(detailWithoutImages.images, isEmpty);
    });

    test('copyWith이 올바르게 동작한다', () {
      // When
      final copied = testDetail.copyWith(
        title: '변경된 제목',
        images: [],
      );

      // Then
      expect(copied.title, '변경된 제목');
      expect(copied.images, isEmpty);
      expect(copied.id, testDetail.id);
    });

    test('toJson과 fromJson이 정확히 동작한다 (이미지 포함)', () {
      // When
      final json = testDetail.toJson();
      final restored = NoticePostDetail.fromJson(json);

      // Then
      expect(restored, testDetail);
      expect(restored.images.length, 2);
      expect(restored.images[0].url, 'https://example.com/image1.jpg');
    });

    test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
      // Given
      final detail1 = NoticePostDetail(
        id: 1,
        category: NoticeCategory.company,
        categoryName: '회사공지',
        title: 'Test',
        content: 'Content',
        createdAt: testDate,
        images: testImages,
      );
      final detail2 = NoticePostDetail(
        id: 1,
        category: NoticeCategory.company,
        categoryName: '회사공지',
        title: 'Test',
        content: 'Content',
        createdAt: testDate,
        images: testImages,
      );

      // When & Then
      expect(detail1, detail2);
      expect(detail1.hashCode, detail2.hashCode);
    });
  });
}
