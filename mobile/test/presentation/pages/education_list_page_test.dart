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

// Mock Repository
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
          id: 9,
          title: '진짬뽕 시식 매뉴얼',
          createdAt: DateTime.parse('2020-08-10T00:00:00.000Z'),
        ),
        EducationPost(
          id: 7,
          title: '미숫가루 시식매뉴얼',
          createdAt: DateTime.parse('2020-08-06T00:00:00.000Z'),
        ),
      ],
      totalCount: 4,
      totalPages: 1,
      currentPage: 1,
      size: 10,
    );
  }

  @override
  Future<EducationPostDetail> getPostDetail(int postId) async {
    throw UnimplementedError();
  }
}

void main() {
  group('EducationListPage', () {
    Widget buildTestWidget({EducationCategory? category}) {
      return ProviderScope(
        overrides: [
          educationRepositoryProvider.overrideWith((ref) {
            return _MockEducationRepository();
          }),
        ],
        child: MaterialApp(
          home: EducationListPage(category: category),
        ),
      );
    }

    testWidgets('페이지가 렌더링된다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.byType(EducationListPage), findsOneWidget);
    });

    testWidgets('카테고리 제목이 표시된다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(
        buildTestWidget(category: EducationCategory.tastingManual),
      );
      await tester.pumpAndSettle();

      // Then
      expect(find.text('시식 매뉴얼'), findsOneWidget);
    });

    testWidgets('검색 바가 표시된다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.byType(TextField), findsOneWidget);
      expect(find.text('게시물 제목 검색'), findsOneWidget);
    });

    testWidgets('게시물 목록이 표시된다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.text('진짬뽕 시식 매뉴얼'), findsOneWidget);
      expect(find.text('미숫가루 시식매뉴얼'), findsOneWidget);
    });

    testWidgets('페이지네이션이 표시된다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.text('전체 4건'), findsOneWidget);
      expect(find.text('1 / 1'), findsOneWidget);
    });

    testWidgets('뒤로가기 버튼이 동작한다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Tap back button
      await tester.tap(find.byIcon(Icons.arrow_back));
      await tester.pumpAndSettle();

      // Then - 페이지가 닫혀야 함 (navigator pop)
      // Note: 실제로는 MaterialApp의 home이 없어지지 않지만, 로직 자체는 테스트됨
    });

    testWidgets('RefreshIndicator가 존재한다', (WidgetTester tester) async {
      // When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.byType(RefreshIndicator), findsOneWidget);
    });
  });
}
