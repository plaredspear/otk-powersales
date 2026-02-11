import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/inspection_field_type.dart';
import 'package:mobile/domain/entities/inspection_form.dart';
import 'package:mobile/domain/entities/inspection_list_item.dart';
import 'package:mobile/domain/entities/inspection_theme.dart';
import 'package:mobile/domain/entities/my_store.dart';
import 'package:mobile/domain/repositories/my_store_repository.dart';
import 'package:mobile/domain/usecases/get_field_types_usecase.dart';
import 'package:mobile/domain/usecases/get_my_stores.dart';
import 'package:mobile/domain/usecases/get_themes_usecase.dart';
import 'package:mobile/domain/usecases/register_inspection_usecase.dart';
import 'package:mobile/presentation/pages/inspection/inspection_register_page.dart';
import 'package:mobile/presentation/providers/inspection_register_provider.dart';
import 'package:mobile/presentation/providers/inspection_register_state.dart';

// Mock UseCases (재사용)
class MockGetThemesUseCase implements GetThemesUseCase {
  @override
  Future<List<InspectionTheme>> call() async {
    return [
      InspectionTheme(
        id: 1,
        name: '8월 테마',
        startDate: DateTime(2020, 8, 1),
        endDate: DateTime(2020, 8, 31),
      ),
    ];
  }
}

class MockGetFieldTypesUseCase implements GetFieldTypesUseCase {
  @override
  Future<List<InspectionFieldType>> call() async {
    return [
      const InspectionFieldType(code: 'FT01', name: '본매대'),
    ];
  }
}

class MockGetMyStores implements GetMyStores {
  @override
  Future<MyStoreListResult> call() async {
    return const MyStoreListResult(
      stores: [
        MyStore(
          storeId: 100,
          storeName: '이마트 죽전점',
          storeCode: 'S100',
          address: '서울시 강남구',
          representativeName: '홍길동',
        ),
      ],
      totalCount: 1,
    );
  }
}

class MockRegisterInspectionUseCase implements RegisterInspectionUseCase {
  @override
  Future<InspectionListItem> call(InspectionRegisterForm form) async {
    return InspectionListItem(
      id: 1,
      category: form.category,
      storeName: '이마트 죽전점',
      storeId: form.storeId,
      inspectionDate: form.inspectionDate,
      fieldType: '본매대',
      fieldTypeCode: form.fieldTypeCode,
    );
  }
}

void main() {
  group('InspectionRegisterPage', () {

    Widget buildTestWidget() {
      return ProviderScope(
        overrides: [
          inspectionRegisterProvider.overrideWith((ref) {
            return InspectionRegisterNotifier(
              getThemes: MockGetThemesUseCase(),
              getFieldTypes: MockGetFieldTypesUseCase(),
              getMyStores: MockGetMyStores(),
              registerInspection: MockRegisterInspectionUseCase(),
            );
          }),
        ],
        child: const MaterialApp(
          home: InspectionRegisterPage(),
        ),
      );
    }

    testWidgets('페이지가 렌더링된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.text('점검 등록'), findsOneWidget);
    });

    testWidgets('AppBar에 뒤로가기 버튼이 있다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.byIcon(Icons.arrow_back), findsOneWidget);
    });

    testWidgets('공통 폼이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then: 공통 폼의 필드들이 표시됨
      expect(find.text('테마 선택'), findsOneWidget);
      expect(find.text('자사'), findsOneWidget);
      expect(find.text('경쟁사'), findsOneWidget);
      expect(find.text('거래처 선택'), findsOneWidget);
    });

    testWidgets('자사 활동 정보 폼이 기본으로 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.text('자사 활동 정보'), findsOneWidget);
      expect(find.text('바코드'), findsOneWidget);
    });

    testWidgets('경쟁사 선택 시 경쟁사 활동 정보 폼이 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // When: 경쟁사 토글 선택
      await tester.tap(find.text('경쟁사'));
      await tester.pumpAndSettle();

      // Then
      expect(find.text('경쟁사 활동 정보'), findsOneWidget);
      expect(find.text('경쟁사명 *'), findsOneWidget);
    });

    testWidgets('사진 선택 위젯이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.text('사진 추가'), findsOneWidget);
    });

    testWidgets('하단에 임시저장과 전송 버튼이 있다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // Then
      expect(find.text('임시저장'), findsOneWidget);
      expect(find.text('전송'), findsOneWidget);
    });

    testWidgets('임시저장 버튼 탭 시 스낵바가 표시된다', (tester) async {
      // Given
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      // When
      await tester.tap(find.text('임시저장'));
      await tester.pumpAndSettle();

      // Then
      expect(find.text('임시 저장되었습니다'), findsOneWidget);
    });
  });
}
