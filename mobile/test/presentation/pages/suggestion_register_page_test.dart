import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/suggestion_remote_datasource.dart';
import 'package:mobile/data/models/suggestion_register_request.dart';
import 'package:mobile/data/models/suggestion_register_result_model.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';
import 'package:mobile/presentation/pages/suggestion_register_page.dart';
import 'package:mobile/presentation/providers/suggestion_register_provider.dart';

/// Mock SuggestionRemoteDataSource
class MockSuggestionRemoteDataSource implements SuggestionRemoteDataSource {
  @override
  Future<SuggestionRegisterResultModel> registerSuggestion(
    SuggestionRegisterRequest request,
  ) async {
    return SuggestionRegisterResultModel(
      id: 1,
      category: 'NEW',
      categoryName: '신제품 제안',
      title: 'Test Title',
      createdAt: DateTime.now().toIso8601String(),
    );
  }
}

void main() {
  group('SuggestionRegisterPage', () {
    testWidgets('페이지가 렌더링된다', (tester) async {
      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            suggestionRemoteDataSourceProvider.overrideWithValue(
              MockSuggestionRemoteDataSource(),
            ),
          ],
          child: const MaterialApp(
            home: SuggestionRegisterPage(),
          ),
        ),
      );

      // Then
      expect(find.text('제안하기'), findsOneWidget);
      expect(find.text('분류 *'), findsOneWidget);
      expect(find.text('제품'), findsOneWidget);
      expect(find.text('제목 *'), findsOneWidget);
      expect(find.text('내용 *'), findsOneWidget);
      expect(find.text('사진 (최대 2장)'), findsOneWidget);
      expect(find.text('제출'), findsOneWidget);
    });

    testWidgets('신제품 제안 선택 시 제품 필드가 비활성화된다', (tester) async {
      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            suggestionRemoteDataSourceProvider.overrideWithValue(
              MockSuggestionRemoteDataSource(),
            ),
          ],
          child: const MaterialApp(
            home: SuggestionRegisterPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then - 초기 상태는 신제품 제안
      expect(find.text('신제품 제안 시 선택 불필요'), findsOneWidget);
    });

    testWidgets('기존제품 선택 시 제품 필드가 활성화된다', (tester) async {
      // Given
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            suggestionRemoteDataSourceProvider.overrideWithValue(
              MockSuggestionRemoteDataSource(),
            ),
          ],
          child: const MaterialApp(
            home: SuggestionRegisterPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // When - 기존제품 라디오 버튼 탭
      await tester.tap(find.text('기존제품 상품가치향상'));
      await tester.pumpAndSettle();

      // Then - 제품 선택 필드 활성화
      expect(find.text('제품 선택'), findsOneWidget);
      expect(find.text('신제품 제안 시 선택 불필요'), findsNothing);
    });

    testWidgets('제목 입력 시 상태가 업데이트된다', (tester) async {
      // Given
      final container = ProviderContainer(
        overrides: [
          suggestionRemoteDataSourceProvider.overrideWithValue(
            MockSuggestionRemoteDataSource(),
          ),
        ],
      );
      addTearDown(container.dispose);

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: const MaterialApp(
            home: SuggestionRegisterPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // When - 제목 입력
      await tester.enterText(
        find.widgetWithText(TextField, '제목을 입력하세요'),
        '신제품 아이디어',
      );
      await tester.pumpAndSettle();

      // Then - 상태 확인
      final state = container.read(suggestionRegisterProvider);
      expect(state.form.title, '신제품 아이디어');
    });

    testWidgets('내용 입력 시 상태가 업데이트된다', (tester) async {
      // Given
      final container = ProviderContainer(
        overrides: [
          suggestionRemoteDataSourceProvider.overrideWithValue(
            MockSuggestionRemoteDataSource(),
          ),
        ],
      );
      addTearDown(container.dispose);

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: const MaterialApp(
            home: SuggestionRegisterPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // When - 내용 입력
      await tester.enterText(
        find.widgetWithText(TextField, '제안 내용을 상세하게 입력하세요'),
        '이런 제품이 있으면 좋겠습니다',
      );
      await tester.pumpAndSettle();

      // Then - 상태 확인
      final state = container.read(suggestionRegisterProvider);
      expect(state.form.content, '이런 제품이 있으면 좋겠습니다');
    });

    testWidgets('제출 버튼이 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            suggestionRemoteDataSourceProvider.overrideWithValue(
              MockSuggestionRemoteDataSource(),
            ),
          ],
          child: const MaterialApp(
            home: SuggestionRegisterPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then
      expect(find.text('제출'), findsOneWidget);
      expect(find.byType(ElevatedButton), findsOneWidget);
    });
  });
}
