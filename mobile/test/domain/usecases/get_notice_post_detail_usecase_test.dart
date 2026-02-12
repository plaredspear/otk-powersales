import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice_category.dart';
import 'package:mobile/domain/entities/notice_post.dart';
import 'package:mobile/domain/entities/notice_post_detail.dart';
import 'package:mobile/domain/repositories/notice_repository.dart';
import 'package:mobile/domain/usecases/get_notice_post_detail_usecase.dart';

class _MockNoticeRepository implements NoticeRepository {
  NoticePostDetail? result;
  Exception? error;

  @override
  Future<NoticePostPage> getPosts({
    NoticeCategory? category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<NoticePostDetail> getPostDetail(int noticeId) async {
    if (error != null) throw error!;
    return result!;
  }
}

void main() {
  group('GetNoticePostDetailUseCase', () {
    late _MockNoticeRepository repository;
    late GetNoticePostDetailUseCase useCase;

    setUp(() {
      repository = _MockNoticeRepository();
      useCase = GetNoticePostDetailUseCase(repository);
    });

    test('공지사항 상세 조회가 성공한다', () async {
      // Given
      repository.result = NoticePostDetail(
        id: 4,
        category: NoticeCategory.company,
        categoryName: '회사공지',
        title: '진라면 포장지 변경',
        content: '업 무 연 락\n진라면 디자인 변경...',
        createdAt: DateTime(2020, 8, 9),
        images: [],
      );

      // When
      final result = await useCase.call(4);

      // Then
      expect(result.id, 4);
      expect(result.title, '진라면 포장지 변경');
    });

    test('공지사항 ID가 0 이하이면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(0),
        throwsA(isA<ArgumentError>()),
      );
      expect(
        () => useCase.call(-1),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('Repository에서 에러가 발생하면 전파된다', () async {
      // Given
      repository.error = Exception('Network error');

      // When & Then
      expect(
        () => useCase.call(1),
        throwsException,
      );
    });
  });
}
