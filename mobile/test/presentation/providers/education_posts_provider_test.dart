import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/entities/education_post.dart';
import 'package:mobile/domain/entities/education_post_detail.dart';
import 'package:mobile/domain/repositories/education_repository.dart';
import 'package:mobile/domain/usecases/get_education_posts_usecase.dart';
import 'package:mobile/presentation/providers/education_posts_provider.dart';

/// 카테고리별로 다른 목록을 돌려주는 Mock — 상태 공유가 일어나면 제목으로 즉시 드러난다.
class _MockEducationRepository implements EducationRepository {
  /// 호출된 (카테고리, 검색어, 페이지) 이력
  final List<({EducationCategory category, String? search, int page})> calls = [];

  @override
  Future<EducationPostPage> getPosts({
    required EducationCategory category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    calls.add((category: category, search: search, page: page));
    return EducationPostPage(
      content: [
        EducationPost(
          id: '${category.code}-1',
          title: '${category.displayName} 게시물',
          createdAt: DateTime(2026, 8, 11),
        ),
      ],
      totalCount: 1,
      totalPages: 1,
      currentPage: page,
      size: size,
    );
  }

  @override
  Future<EducationPostDetail> getPostDetail(String postId) {
    throw UnimplementedError();
  }
}

void main() {
  late _MockEducationRepository repository;
  late ProviderContainer container;

  setUp(() {
    repository = _MockEducationRepository();
    container = ProviderContainer(
      overrides: [
        getEducationPostsUseCaseProvider
            .overrideWithValue(GetEducationPostsUseCase(repository)),
      ],
    );
  });

  tearDown(() => container.dispose());

  /// family provider 는 autoDispose 라 listen 없이는 즉시 폐기된다 — 구독을 유지한다.
  ProviderSubscription<dynamic> keepAlive(EducationCategory category) =>
      container.listen(educationPostsProvider(category), (_, __) {});

  group('카테고리별 상태 격리', () {
    test('서로 다른 카테고리는 상태를 공유하지 않는다', () async {
      keepAlive(EducationCategory.tastingManual);
      keepAlive(EducationCategory.appManual);
      await Future.delayed(Duration.zero);

      final tasting =
          container.read(educationPostsProvider(EducationCategory.tastingManual));
      final app =
          container.read(educationPostsProvider(EducationCategory.appManual));

      expect(tasting.category, EducationCategory.tastingManual);
      expect(app.category, EducationCategory.appManual);
      expect(tasting.posts.single.title, '시식 매뉴얼 게시물');
      expect(app.posts.single.title, 'APP 매뉴얼 게시물');
    });

    test('각 카테고리는 자기 카테고리로만 조회한다', () async {
      keepAlive(EducationCategory.tastingManual);
      keepAlive(EducationCategory.appManual);
      await Future.delayed(Duration.zero);

      expect(
        repository.calls.map((c) => c.category),
        containsAll([EducationCategory.tastingManual, EducationCategory.appManual]),
      );
    });

    test('한 카테고리의 검색이 다른 카테고리 목록에 영향을 주지 않는다', () async {
      keepAlive(EducationCategory.tastingManual);
      keepAlive(EducationCategory.appManual);
      await Future.delayed(Duration.zero);

      await container
          .read(educationPostsProvider(EducationCategory.tastingManual).notifier)
          .search('키워드');

      final app =
          container.read(educationPostsProvider(EducationCategory.appManual));
      expect(app.searchKeyword, isNull);
      expect(app.posts.single.title, 'APP 매뉴얼 게시물');

      // 검색은 요청한 카테고리에만 실린다
      final searchCalls = repository.calls.where((c) => c.search != null);
      expect(searchCalls, hasLength(1));
      expect(searchCalls.single.category, EducationCategory.tastingManual);
    });

    test('한 카테고리의 페이지 이동이 다른 카테고리에 전이되지 않는다', () async {
      keepAlive(EducationCategory.tastingManual);
      keepAlive(EducationCategory.appManual);
      await Future.delayed(Duration.zero);

      await container
          .read(educationPostsProvider(EducationCategory.tastingManual).notifier)
          .changePage(2);

      expect(
        container
            .read(educationPostsProvider(EducationCategory.appManual))
            .currentPage,
        1,
      );
    });
  });

  group('초기 상태', () {
    test('생성 즉시 자기 카테고리로 목록을 조회한다', () async {
      keepAlive(EducationCategory.csSafety);
      await Future.delayed(Duration.zero);

      expect(repository.calls.single.category, EducationCategory.csSafety);
      expect(repository.calls.single.page, 1);
      expect(repository.calls.single.search, isNull);
    });

    test('카테고리는 family key 이므로 상태의 카테고리와 항상 일치한다', () async {
      for (final category in EducationCategory.values) {
        keepAlive(category);
      }
      await Future.delayed(Duration.zero);

      for (final category in EducationCategory.values) {
        expect(
          container.read(educationPostsProvider(category)).category,
          category,
        );
      }
    });
  });

  group('autoDispose', () {
    test('구독이 끊기면 상태를 버리고 재진입 시 다시 조회한다', () async {
      final subscription = keepAlive(EducationCategory.appManual);
      await Future.delayed(Duration.zero);
      expect(repository.calls, hasLength(1));

      subscription.close();
      await Future.delayed(Duration.zero);

      keepAlive(EducationCategory.appManual);
      await Future.delayed(Duration.zero);

      // 폐기 후 재구독 → 새 Notifier 가 생성되어 다시 조회한다 (stale 목록 노출 방지)
      expect(repository.calls, hasLength(2));
      expect(
        container.read(educationPostsProvider(EducationCategory.appManual)).currentPage,
        1,
      );
    });
  });
}
