import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/inspection/inspection_photo_picker.dart';

void main() {
  group('InspectionPhotoPicker Widget', () {
    late File testPhoto1;
    late File testPhoto2;

    setUp(() {
      testPhoto1 = File('test1.jpg');
      testPhoto2 = File('test2.jpg');
    });

    Widget buildTestWidget({
      List<File>? photos,
      VoidCallback? onAddPhoto,
      ValueChanged<int>? onRemovePhoto,
    }) {
      return MaterialApp(
        home: Scaffold(
          body: InspectionPhotoPicker(
            photos: photos ?? const [],
            onAddPhoto: onAddPhoto ?? () {},
            onRemovePhoto: onRemovePhoto ?? (_) {},
          ),
        ),
      );
    }

    testWidgets('사진 섹션 헤더가 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget());

      // Then: RichText가 렌더링됨 (헤더 포함)
      expect(find.byType(RichText), findsWidgets);
    });

    testWidgets('사진이 없을 때 사진 추가 버튼만 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(buildTestWidget(photos: []));

      // Then
      expect(find.text('사진 추가'), findsOneWidget);
      expect(find.byIcon(Icons.add_a_photo), findsOneWidget);
    });

    testWidgets('사진 1장 선택 시 사진과 추가 버튼이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(photos: [testPhoto1]),
      );

      // Then: 사진 1개 (삭제 버튼으로 확인) + 추가 버튼
      expect(find.byIcon(Icons.close), findsOneWidget); // 사진의 삭제 버튼
      expect(find.text('사진 추가'), findsOneWidget);
    });

    testWidgets('사진 2장 선택 시 추가 버튼이 표시되지 않는다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(photos: [testPhoto1, testPhoto2]),
      );

      // Then: 사진 2개 (삭제 버튼 2개로 확인), 추가 버튼 없음
      expect(find.byIcon(Icons.close), findsNWidgets(2)); // 사진의 삭제 버튼 2개
      expect(find.text('사진 추가'), findsNothing);
    });

    testWidgets('사진에 삭제 버튼이 표시된다', (tester) async {
      // Given & When
      await tester.pumpWidget(
        buildTestWidget(photos: [testPhoto1]),
      );

      // Then
      expect(find.byIcon(Icons.close), findsOneWidget);
    });

    testWidgets('사진 추가 버튼 탭 시 콜백이 호출된다', (tester) async {
      // Given
      var tapped = false;
      await tester.pumpWidget(
        buildTestWidget(
          photos: [],
          onAddPhoto: () => tapped = true,
        ),
      );

      // When
      await tester.tap(find.text('사진 추가'));
      await tester.pumpAndSettle();

      // Then
      expect(tapped, true);
    });

    testWidgets('사진 삭제 버튼 탭 시 콜백이 호출된다', (tester) async {
      // Given
      int? removedIndex;
      await tester.pumpWidget(
        buildTestWidget(
          photos: [testPhoto1],
          onRemovePhoto: (index) => removedIndex = index,
        ),
      );

      // When
      await tester.tap(find.byIcon(Icons.close));
      await tester.pumpAndSettle();

      // Then
      expect(removedIndex, 0);
    });

    testWidgets('두 번째 사진 삭제 시 올바른 인덱스가 전달된다', (tester) async {
      // Given
      int? removedIndex;
      await tester.pumpWidget(
        buildTestWidget(
          photos: [testPhoto1, testPhoto2],
          onRemovePhoto: (index) => removedIndex = index,
        ),
      );

      // When: 두 번째 사진의 삭제 버튼 탭
      final deleteButtons = find.byIcon(Icons.close);
      await tester.tap(deleteButtons.at(1));
      await tester.pumpAndSettle();

      // Then
      expect(removedIndex, 1);
    });

    testWidgets('canAddPhoto getter가 올바르게 동작한다', (tester) async {
      // Given: Widget을 찾기 위해 Key 추가
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InspectionPhotoPicker(
              key: const Key('picker'),
              photos: const [],
              onAddPhoto: () {},
              onRemovePhoto: (_) {},
            ),
          ),
        ),
      );

      // When
      final picker = tester.widget<InspectionPhotoPicker>(
        find.byKey(const Key('picker')),
      );

      // Then: 사진 0장 -> 추가 가능
      expect(picker.canAddPhoto, true);
    });

    testWidgets('canAddPhoto getter가 2장일 때 false를 반환한다', (tester) async {
      // Given
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: InspectionPhotoPicker(
              key: const Key('picker'),
              photos: [testPhoto1, testPhoto2],
              onAddPhoto: () {},
              onRemovePhoto: (_) {},
            ),
          ),
        ),
      );

      // When
      final picker = tester.widget<InspectionPhotoPicker>(
        find.byKey(const Key('picker')),
      );

      // Then: 사진 2장 -> 추가 불가
      expect(picker.canAddPhoto, false);
    });
  });
}
