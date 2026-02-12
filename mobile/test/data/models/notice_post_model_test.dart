import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/notice_post_model.dart';
import 'package:mobile/domain/entities/notice_category.dart';

void main() {
  group('NoticePostModel', () {
    final testJson = {
      'id': 5,
      'category': 'BRANCH',
      'categoryName': '지점공지',
      'title': '마진선조장 공지사항',
      'createdAt': '2020-08-17T00:00:00',
    };

    final testModel = NoticePostModel(
      id: 5,
      category: 'BRANCH',
      categoryName: '지점공지',
      title: '마진선조장 공지사항',
      createdAt: '2020-08-17T00:00:00',
    );

    test('fromJson이 올바르게 동작한다', () {
      // When
      final model = NoticePostModel.fromJson(testJson);

      // Then
      expect(model.id, 5);
      expect(model.category, 'BRANCH');
      expect(model.categoryName, '지점공지');
      expect(model.title, '마진선조장 공지사항');
      expect(model.createdAt, '2020-08-17T00:00:00');
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
      expect(entity.id, 5);
      expect(entity.category, NoticeCategory.branch);
      expect(entity.categoryName, '지점공지');
      expect(entity.title, '마진선조장 공지사항');
      expect(entity.createdAt, DateTime(2020, 8, 17));
    });

    test('fromEntity가 올바르게 변환한다', () {
      // Given
      final entity = testModel.toEntity();

      // When
      final model = NoticePostModel.fromEntity(entity);

      // Then
      expect(model.id, entity.id);
      expect(model.category, 'BRANCH');
      expect(model.title, entity.title);
    });

    test('fromJson -> toEntity -> fromEntity -> toJson 변환이 일관되다', () {
      // When
      final model = NoticePostModel.fromJson(testJson);
      final entity = model.toEntity();
      final model2 = NoticePostModel.fromEntity(entity);
      final json = model2.toJson();

      // Then
      expect(json['id'], testJson['id']);
      expect(json['category'], testJson['category']);
      expect(json['title'], testJson['title']);
    });
  });

  group('NoticePostPageModel', () {
    final testJson = {
      'content': [
        {
          'id': 1,
          'category': 'COMPANY',
          'categoryName': '회사공지',
          'title': 'Test 1',
          'createdAt': '2020-08-01T00:00:00',
        },
        {
          'id': 2,
          'category': 'BRANCH',
          'categoryName': '지점공지',
          'title': 'Test 2',
          'createdAt': '2020-08-02T00:00:00',
        },
      ],
      'totalCount': 5,
      'totalPages': 1,
      'currentPage': 1,
      'size': 10,
    };

    test('fromJson이 올바르게 동작한다 (중첩 구조)', () {
      // When
      final model = NoticePostPageModel.fromJson(testJson);

      // Then
      expect(model.content.length, 2);
      expect(model.content[0].id, 1);
      expect(model.content[1].id, 2);
      expect(model.totalCount, 5);
      expect(model.currentPage, 1);
    });

    test('toJson이 올바르게 동작한다 (중첩 구조)', () {
      // Given
      final model = NoticePostPageModel.fromJson(testJson);

      // When
      final json = model.toJson();

      // Then
      expect(json['content'], isA<List>());
      expect((json['content'] as List).length, 2);
      expect(json['totalCount'], 5);
    });

    test('toEntity가 올바르게 변환한다', () {
      // Given
      final model = NoticePostPageModel.fromJson(testJson);

      // When
      final entity = model.toEntity();

      // Then
      expect(entity.content.length, 2);
      expect(entity.content[0].category, NoticeCategory.company);
      expect(entity.content[1].category, NoticeCategory.branch);
      expect(entity.totalCount, 5);
    });

    test('fromEntity가 올바르게 변환한다', () {
      // Given
      final model = NoticePostPageModel.fromJson(testJson);
      final entity = model.toEntity();

      // When
      final model2 = NoticePostPageModel.fromEntity(entity);

      // Then
      expect(model2.content.length, 2);
      expect(model2.totalCount, 5);
    });
  });
}
