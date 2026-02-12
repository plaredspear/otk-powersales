import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/entities/education_post.dart';
import 'package:mobile/domain/entities/education_post_detail.dart';
import 'package:mobile/domain/repositories/education_repository.dart';
import 'package:mobile/domain/usecases/get_education_post_detail_usecase.dart';

/// 테스트용 Mock EducationRepository
class MockEducationRepository implements EducationRepository {
  EducationPostDetail? postDetail;
  Exception? exceptionToThrow;
  int callCount = 0;
  int? lastPostId;

  @override
  Future<EducationPostDetail> getPostDetail(int postId) async {
    callCount++;
    lastPostId = postId;

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return postDetail!;
  }

  @override
  Future<EducationPostPage> getPosts({
    required EducationCategory category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    throw UnimplementedError();
  }
}

void main() {
  group('GetEducationPostDetailUseCase', () {
    late GetEducationPostDetailUseCase useCase;
    late MockEducationRepository mockRepository;

    final testDetail = EducationPostDetail(
      id: 9,
      category: EducationCategory.tastingManual,
      categoryName: '시식 매뉴얼',
      title: '진짬뽕 시식 매뉴얼',
      content: '진짬뽕 시식 매뉴얼 본문 내용...',
      createdAt: DateTime.parse('2020-08-10T00:00:00.000Z'),
      images: [
        EducationImage(
          id: 1,
          url: 'https://example.com/image.jpg',
          sortOrder: 1,
        ),
      ],
      attachments: [
        EducationAttachment(
          id: 1,
          fileName: '진짬뽕_시식_가이드.pdf',
          fileUrl: 'https://example.com/files/guide.pdf',
          fileSize: 2048576,
        ),
      ],
    );

    setUp(() {
      mockRepository = MockEducationRepository();
      useCase = GetEducationPostDetailUseCase(mockRepository);
    });

    group('정상 케이스', () {
      test('게시물 상세를 반환한다', () async {
        mockRepository.postDetail = testDetail;

        final result = await useCase(9);

        expect(result, testDetail);
        expect(result.id, 9);
        expect(result.title, '진짬뽕 시식 매뉴얼');
        expect(result.images.length, 1);
        expect(result.attachments.length, 1);
      });

      test('Repository에 올바른 postId를 전달한다', () async {
        mockRepository.postDetail = testDetail;

        await useCase(9);

        expect(mockRepository.lastPostId, 9);
      });

      test('Repository를 정확히 1회 호출한다', () async {
        mockRepository.postDetail = testDetail;

        await useCase(9);

        expect(mockRepository.callCount, 1);
      });

      test('이미지와 첨부파일이 없는 게시물을 반환할 수 있다', () async {
        mockRepository.postDetail = testDetail.copyWith(
          images: [],
          attachments: [],
        );

        final result = await useCase(7);

        expect(result.images, isEmpty);
        expect(result.attachments, isEmpty);
      });
    });

    group('유효성 검증', () {
      test('postId가 0이면 ArgumentError를 발생시킨다', () async {
        expect(
          () => useCase(0),
          throwsA(isA<ArgumentError>()),
        );
      });

      test('postId가 음수이면 ArgumentError를 발생시킨다', () async {
        expect(
          () => useCase(-1),
          throwsA(isA<ArgumentError>()),
        );
      });
    });

    group('에러 처리', () {
      test('Repository에서 Exception 발생 시 그대로 전파한다', () async {
        mockRepository.exceptionToThrow = Exception('POST_NOT_FOUND');

        expect(
          () => useCase(999),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('POST_NOT_FOUND'),
            ),
          ),
        );
      });

      test('네트워크 오류 시 Exception을 전파한다', () async {
        mockRepository.exceptionToThrow = Exception('네트워크 연결 실패');

        expect(
          () => useCase(9),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('네트워크 연결 실패'),
            ),
          ),
        );
      });
    });
  });
}
