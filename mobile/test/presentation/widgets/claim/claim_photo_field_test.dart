import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/claim/claim_photo_field.dart';

void main() {
  group('ClaimPhotoField', () {
    testWidgets('사진 미첨부 시 선택 버튼을 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimPhotoField(
              label: '불량 사진',
              photo: null,
              onPhotoSelected: (_) {},
              onPhotoRemoved: () {},
              isRequired: true,
            ),
          ),
        ),
      );

      // Then: 라벨 표시 (필수 표시 포함)
      expect(find.text('불량 사진 *'), findsOneWidget);

      // Then: 사진 선택 버튼 표시
      expect(find.text('사진 선택'), findsOneWidget);
      expect(find.byIcon(Icons.add_photo_alternate), findsOneWidget);
    });

    testWidgets('선택 사항인 경우 * 표시가 없다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimPhotoField(
              label: '구매 영수증',
              photo: null,
              onPhotoSelected: (_) {},
              onPhotoRemoved: () {},
              isRequired: false,
            ),
          ),
        ),
      );

      // Then: 라벨에 * 없음
      expect(find.text('구매 영수증'), findsOneWidget);
      expect(find.text('구매 영수증 *'), findsNothing);
    });

    testWidgets('사진 첨부 시 미리보기와 삭제 버튼을 표시한다', (tester) async {
      final testPhoto = File('test_photo.jpg');

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimPhotoField(
              label: '불량 사진',
              photo: testPhoto,
              onPhotoSelected: (_) {},
              onPhotoRemoved: () {},
            ),
          ),
        ),
      );

      // Then: 이미지 표시 (Image.file)
      expect(find.byType(Image), findsOneWidget);

      // Then: 삭제 버튼 표시
      expect(find.byIcon(Icons.close), findsOneWidget);

      // Then: 선택 버튼 미표시
      expect(find.text('사진 선택'), findsNothing);
    });

    testWidgets('삭제 버튼 탭 시 콜백이 호출된다', (tester) async {
      final testPhoto = File('test_photo.jpg');
      bool removeCalled = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ClaimPhotoField(
              label: '불량 사진',
              photo: testPhoto,
              onPhotoSelected: (_) {},
              onPhotoRemoved: () {
                removeCalled = true;
              },
            ),
          ),
        ),
      );

      // When: 삭제 버튼 탭
      await tester.tap(find.byIcon(Icons.close));
      await tester.pump();

      // Then
      expect(removeCalled, true);
    });
  });
}
