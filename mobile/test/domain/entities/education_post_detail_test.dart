import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/entities/education_post_detail.dart';

void main() {
  group('EducationImage', () {
    final testImage = EducationImage(
      id: 1,
      url: 'https://example.com/image.jpg',
      sortOrder: 1,
    );

    test('생성', () {
      expect(testImage.id, 1);
      expect(testImage.url, 'https://example.com/image.jpg');
      expect(testImage.sortOrder, 1);
    });

    test('round trip', () {
      final json = testImage.toJson();
      final restored = EducationImage.fromJson(json);

      expect(restored, testImage);
    });
  });

  group('EducationAttachment', () {
    final testAttachment = EducationAttachment(
      id: 1,
      fileName: '진짬뽕_시식_가이드.pdf',
      fileUrl: 'https://example.com/files/guide.pdf',
      fileSize: 2048576, // 2 MB
    );

    test('생성', () {
      expect(testAttachment.id, 1);
      expect(testAttachment.fileName, '진짬뽕_시식_가이드.pdf');
      expect(testAttachment.fileUrl, 'https://example.com/files/guide.pdf');
      expect(testAttachment.fileSize, 2048576);
    });

    test('fileSizeFormatted returns MB for large files', () {
      expect(testAttachment.fileSizeFormatted, '2.0 MB');
    });

    test('fileSizeFormatted returns KB for medium files', () {
      final attachment = testAttachment.copyWith(fileSize: 512000); // 500 KB
      expect(attachment.fileSizeFormatted, '500.0 KB');
    });

    test('fileSizeFormatted returns B for small files', () {
      final attachment = testAttachment.copyWith(fileSize: 100); // 100 B
      expect(attachment.fileSizeFormatted, '100 B');
    });

    test('round trip', () {
      final json = testAttachment.toJson();
      final restored = EducationAttachment.fromJson(json);

      expect(restored, testAttachment);
    });
  });

  group('EducationPostDetail', () {
    final testImages = [
      EducationImage(
        id: 1,
        url: 'https://example.com/image1.jpg',
        sortOrder: 1,
      ),
      EducationImage(
        id: 2,
        url: 'https://example.com/image2.jpg',
        sortOrder: 2,
      ),
    ];

    final testAttachments = [
      EducationAttachment(
        id: 1,
        fileName: 'guide.pdf',
        fileUrl: 'https://example.com/files/guide.pdf',
        fileSize: 2048576,
      ),
    ];

    final testDetail = EducationPostDetail(
      id: 9,
      category: EducationCategory.tastingManual,
      categoryName: '시식 매뉴얼',
      title: '진짬뽕 시식 매뉴얼',
      content: '진짬뽕 시식 매뉴얼 본문 내용...',
      createdAt: DateTime.parse('2020-08-10T00:00:00.000Z'),
      images: testImages,
      attachments: testAttachments,
    );

    test('생성', () {
      expect(testDetail.id, 9);
      expect(testDetail.category, EducationCategory.tastingManual);
      expect(testDetail.categoryName, '시식 매뉴얼');
      expect(testDetail.title, '진짬뽕 시식 매뉴얼');
      expect(testDetail.content, '진짬뽕 시식 매뉴얼 본문 내용...');
      expect(testDetail.images.length, 2);
      expect(testDetail.attachments.length, 1);
    });

    test('hasImages returns true when images exist', () {
      expect(testDetail.hasImages, true);
    });

    test('hasImages returns false when images are empty', () {
      final detail = testDetail.copyWith(images: []);
      expect(detail.hasImages, false);
    });

    test('hasAttachments returns true when attachments exist', () {
      expect(testDetail.hasAttachments, true);
    });

    test('hasAttachments returns false when attachments are empty', () {
      final detail = testDetail.copyWith(attachments: []);
      expect(detail.hasAttachments, false);
    });

    test('round trip', () {
      final json = testDetail.toJson();
      final restored = EducationPostDetail.fromJson(json);

      expect(restored, testDetail);
    });
  });
}
