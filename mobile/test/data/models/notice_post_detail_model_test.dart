import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/notice_post_detail_model.dart';
import 'package:mobile/domain/entities/notice_category.dart';

void main() {
  group('NoticeImageModel', () {
    final testJson = {
      'id': 1,
      'url': 'https://example.com/image1.jpg',
      'sortOrder': 1,
    };

    final testModel = NoticeImageModel(
      id: 1,
      url: 'https://example.com/image1.jpg',
      sortOrder: 1,
    );

    test('fromJson이 올바르게 동작한다', () {
      // When
      final model = NoticeImageModel.fromJson(testJson);

      // Then
      expect(model.id, 1);
      expect(model.url, 'https://example.com/image1.jpg');
      expect(model.sortOrder, 1);
    });

    test('toJson이 올바르게 동작한다', () {
      // When
      final json = testModel.toJson();

      // Then
      expect(json, testJson);
    });

    test('toEntity가 올바르게 변환한다', () {
      // When
      final entity = testModel.toEntity();

      // Then
      expect(entity.id, 1);
      expect(entity.url, 'https://example.com/image1.jpg');
      expect(entity.sortOrder, 1);
    });

    test('fromEntity가 올바르게 변환한다', () {
      // Given
      final entity = testModel.toEntity();

      // When
      final model = NoticeImageModel.fromEntity(entity);

      // Then
      expect(model.id, entity.id);
      expect(model.url, entity.url);
    });
  });

  group('NoticePostDetailModel', () {
    final testJson = {
      'id': 4,
      'category': 'COMPANY',
      'categoryName': '회사공지',
      'title': '진라면 포장지 변경',
      'content': '업 무 연 락\n진라면 디자인 변경...',
      'createdAt': '2020-08-09T00:00:00',
      'images': [
        {
          'id': 1,
          'url': 'https://example.com/image1.jpg',
          'sortOrder': 1,
        },
        {
          'id': 2,
          'url': 'https://example.com/image2.jpg',
          'sortOrder': 2,
        },
      ],
    };

    test('fromJson이 올바르게 동작한다 (이미지 포함)', () {
      // When
      final model = NoticePostDetailModel.fromJson(testJson);

      // Then
      expect(model.id, 4);
      expect(model.category, 'COMPANY');
      expect(model.title, '진라면 포장지 변경');
      expect(model.content, '업 무 연 락\n진라면 디자인 변경...');
      expect(model.images.length, 2);
      expect(model.images[0].url, 'https://example.com/image1.jpg');
    });

    test('fromJson이 이미지 없는 경우도 처리한다', () {
      // Given
      final jsonWithoutImages = {
        ...testJson,
        'images': [],
      };

      // When
      final model = NoticePostDetailModel.fromJson(jsonWithoutImages);

      // Then
      expect(model.images, isEmpty);
    });

    test('toJson이 올바르게 동작한다 (중첩 구조)', () {
      // Given
      final model = NoticePostDetailModel.fromJson(testJson);

      // When
      final json = model.toJson();

      // Then
      expect(json['id'], 4);
      expect(json['images'], isA<List>());
      expect((json['images'] as List).length, 2);
    });

    test('toEntity가 올바르게 변환한다', () {
      // Given
      final model = NoticePostDetailModel.fromJson(testJson);

      // When
      final entity = model.toEntity();

      // Then
      expect(entity.id, 4);
      expect(entity.category, NoticeCategory.company);
      expect(entity.title, '진라면 포장지 변경');
      expect(entity.createdAt, DateTime(2020, 8, 9));
      expect(entity.images.length, 2);
    });

    test('fromEntity가 올바르게 변환한다', () {
      // Given
      final model = NoticePostDetailModel.fromJson(testJson);
      final entity = model.toEntity();

      // When
      final model2 = NoticePostDetailModel.fromEntity(entity);

      // Then
      expect(model2.id, 4);
      expect(model2.category, 'COMPANY');
      expect(model2.images.length, 2);
    });

    test('fromJson -> toEntity -> fromEntity -> toJson 변환이 일관되다', () {
      // When
      final model = NoticePostDetailModel.fromJson(testJson);
      final entity = model.toEntity();
      final model2 = NoticePostDetailModel.fromEntity(entity);
      final json = model2.toJson();

      // Then
      expect(json['id'], testJson['id']);
      expect(json['category'], testJson['category']);
      expect(json['title'], testJson['title']);
      expect((json['images'] as List).length, 2);
    });
  });
}
