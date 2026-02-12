import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/suggestion/suggestion_photo_field.dart';

void main() {
  group('SuggestionPhotoField Widget', () {
    testWidgets('사진이 없을 때 렌더링된다', (tester) async {
      // When
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionPhotoField(
              photos: const [],
              onAddPhoto: () {},
              onRemovePhoto: (_) {},
            ),
          ),
        ),
      );

      // Then
      expect(find.text('사진 (최대 2장)'), findsOneWidget);
      expect(find.text('사진 선택'), findsOneWidget);
      expect(find.text('사진을 첨부하면 제안 내용을 더 명확하게 전달할 수 있습니다'), findsOneWidget);
    });

    testWidgets('사진 1장이 표시된다', (tester) async {
      // Given
      final photo1 = File('test1.jpg');

      // When
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionPhotoField(
              photos: [photo1],
              onAddPhoto: () {},
              onRemovePhoto: (_) {},
            ),
          ),
        ),
      );

      // Then
      expect(find.byIcon(Icons.close), findsOneWidget); // 사진 1개 (삭제 버튼 1개)
      expect(find.text('사진 선택'), findsOneWidget); // 추가 버튼 있음
    });

    testWidgets('사진 2장이 표시되고 추가 버튼이 사라진다', (tester) async {
      // Given
      final photo1 = File('test1.jpg');
      final photo2 = File('test2.jpg');

      // When
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionPhotoField(
              photos: [photo1, photo2],
              onAddPhoto: () {},
              onRemovePhoto: (_) {},
            ),
          ),
        ),
      );

      // Then
      expect(find.byIcon(Icons.close), findsNWidgets(2)); // 사진 2개 (삭제 버튼 2개)
      expect(find.text('사진 선택'), findsNothing); // 추가 버튼 없음
    });

    testWidgets('사진 추가 버튼 클릭 시 콜백이 호출된다', (tester) async {
      // Given
      bool addPhotoPressed = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionPhotoField(
              photos: const [],
              onAddPhoto: () {
                addPhotoPressed = true;
              },
              onRemovePhoto: (_) {},
            ),
          ),
        ),
      );

      // When
      await tester.tap(find.text('사진 선택'));
      await tester.pumpAndSettle();

      // Then
      expect(addPhotoPressed, true);
    });

    testWidgets('사진 삭제 버튼 클릭 시 콜백이 호출된다', (tester) async {
      // Given
      final photo1 = File('test1.jpg');
      int? removedIndex;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SuggestionPhotoField(
              photos: [photo1],
              onAddPhoto: () {},
              onRemovePhoto: (index) {
                removedIndex = index;
              },
            ),
          ),
        ),
      );

      // When - 삭제 버튼(close 아이콘) 탭
      await tester.tap(find.byIcon(Icons.close));
      await tester.pumpAndSettle();

      // Then
      expect(removedIndex, 0);
    });
  });
}
