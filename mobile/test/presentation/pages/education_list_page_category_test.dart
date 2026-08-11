import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/entities/education_post.dart';
import 'package:mobile/domain/entities/education_post_detail.dart';
import 'package:mobile/domain/repositories/education_repository.dart';
import 'package:mobile/domain/usecases/get_education_posts_usecase.dart';
import 'package:mobile/presentation/pages/education_list_page.dart';
import 'package:mobile/presentation/providers/education_posts_provider.dart';

/// 카테고리마다 구분되는 목록을 돌려주는 Mock.
class _MockEducationRepository implements EducationRepository {
  @override
  Future<EducationPostPage> getPosts({
    required EducationCategory category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
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

/// 교육 목록 화면 카테고리 격리 테스트.
///
/// 시식 매뉴얼을 열어 본 뒤 APP 매뉴얼로 이동하면 시식 매뉴얼 목록이 잠시 노출되던 회귀를 막는다
/// (원인: 전역 단일 provider 를 공유해 화면 첫 프레임이 직전 카테고리 상태를 그렸다).
void main() {
  Widget wrap(EducationCategory category) {
    return ProviderScope(
      overrides: [
        getEducationPostsUseCaseProvider.overrideWithValue(
          GetEducationPostsUseCase(_MockEducationRepository()),
        ),
      ],
      child: MaterialApp(home: EducationListPage(category: category)),
    );
  }

  testWidgets('첫 프레임에 다른 카테고리의 내용이 보이지 않는다', (tester) async {
    // 1) 시식 매뉴얼 화면을 열어 목록까지 로드한다
    await tester.pumpWidget(wrap(EducationCategory.tastingManual));
    await tester.pumpAndSettle();
    expect(find.text('시식 매뉴얼 게시물'), findsOneWidget);

    // 2) APP 매뉴얼 화면으로 교체 — 첫 프레임(settle 이전)을 검사한다
    await tester.pumpWidget(wrap(EducationCategory.appManual));
    await tester.pump();

    expect(
      find.text('시식 매뉴얼 게시물'),
      findsNothing,
      reason: '이전 카테고리 목록이 첫 프레임에 노출되면 안 된다',
    );
    expect(
      find.text('시식 매뉴얼'),
      findsNothing,
      reason: 'AppBar 제목도 이전 카테고리로 남으면 안 된다',
    );

    // 3) 로드 완료 후에는 자기 카테고리 내용만 보인다
    await tester.pumpAndSettle();
    expect(find.text('APP 매뉴얼 게시물'), findsOneWidget);
    expect(find.text('시식 매뉴얼 게시물'), findsNothing);
  });

  testWidgets('AppBar 제목은 전달받은 카테고리를 즉시 반영한다', (tester) async {
    await tester.pumpWidget(wrap(EducationCategory.appManual));
    await tester.pump();

    expect(find.text('APP 매뉴얼'), findsOneWidget);
  });
}
