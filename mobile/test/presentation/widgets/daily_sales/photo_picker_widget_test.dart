import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/daily_sales/photo_picker_widget.dart';

void main() {
  group('PhotoPickerWidget', () {
    testWidgets('사진이 없을 때 선택 버튼들이 렌더링된다', (tester) async {
      File? selectedPhoto;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PhotoPickerWidget(
              photo: null,
              onPhotoChanged: (photo) => selectedPhoto = photo,
            ),
          ),
        ),
      );

      // 라벨 확인
      expect(find.text('사진 첨부'), findsOneWidget);

      // 안내 메시지 확인
      expect(find.text('사진을 추가해주세요'), findsOneWidget);

      // 카메라/갤러리 버튼 확인
      expect(find.text('촬영'), findsOneWidget);
      expect(find.text('갤러리'), findsOneWidget);

      // 미리보기는 없어야 함
      expect(find.byType(Image), findsNothing);
    });

    testWidgets('사진이 있을 때 미리보기가 렌더링된다', (tester) async {
      final testPhoto = File('/test/photo.jpg');

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PhotoPickerWidget(
              photo: testPhoto,
              onPhotoChanged: (_) {},
            ),
          ),
        ),
      );

      // 라벨 확인
      expect(find.text('사진 첨부'), findsOneWidget);

      // 선택 버튼들은 없어야 함
      expect(find.text('촬영'), findsNothing);
      expect(find.text('갤러리'), findsNothing);

      // 삭제 버튼 확인
      expect(find.byIcon(Icons.close), findsOneWidget);

      // 재선택 버튼 확인
      expect(find.text('다시 선택'), findsOneWidget);
    });

    testWidgets('삭제 버튼을 탭하면 onPhotoChanged(null)이 호출된다', (tester) async {
      final testPhoto = File('/test/photo.jpg');
      File? changedPhoto = testPhoto;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PhotoPickerWidget(
              photo: testPhoto,
              onPhotoChanged: (photo) => changedPhoto = photo,
            ),
          ),
        ),
      );

      // 삭제 버튼 탭
      await tester.tap(find.byIcon(Icons.close));
      await tester.pumpAndSettle();

      // onPhotoChanged가 null로 호출되었는지 확인
      expect(changedPhoto, isNull);
    });

    testWidgets('재선택 버튼을 탭하면 선택 다이얼로그가 표시된다', (tester) async {
      final testPhoto = File('/test/photo.jpg');

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PhotoPickerWidget(
              photo: testPhoto,
              onPhotoChanged: (_) {},
            ),
          ),
        ),
      );

      // 재선택 버튼 탭
      await tester.tap(find.text('다시 선택'));
      await tester.pumpAndSettle();

      // 다이얼로그 확인
      expect(find.text('사진 선택'), findsOneWidget);
      expect(find.text('카메라로 촬영'), findsOneWidget);
      expect(find.text('갤러리에서 선택'), findsOneWidget);
    });

    testWidgets('선택 다이얼로그에서 카메라를 선택하면 다이얼로그가 닫힌다', (tester) async {
      final testPhoto = File('/test/photo.jpg');

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PhotoPickerWidget(
              photo: testPhoto,
              onPhotoChanged: (_) {},
            ),
          ),
        ),
      );

      // 재선택 버튼 탭
      await tester.tap(find.text('다시 선택'));
      await tester.pumpAndSettle();

      // 카메라 선택
      await tester.tap(find.text('카메라로 촬영'));
      await tester.pumpAndSettle();

      // 다이얼로그가 닫혔는지 확인
      expect(find.text('사진 선택'), findsNothing);
    });

    testWidgets('선택 다이얼로그에서 갤러리를 선택하면 다이얼로그가 닫힌다', (tester) async {
      final testPhoto = File('/test/photo.jpg');

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PhotoPickerWidget(
              photo: testPhoto,
              onPhotoChanged: (_) {},
            ),
          ),
        ),
      );

      // 재선택 버튼 탭
      await tester.tap(find.text('다시 선택'));
      await tester.pumpAndSettle();

      // 갤러리 선택
      await tester.tap(find.text('갤러리에서 선택'));
      await tester.pumpAndSettle();

      // 다이얼로그가 닫혔는지 확인
      expect(find.text('사진 선택'), findsNothing);
    });

    testWidgets('커스텀 ImagePicker를 주입할 수 있다', (tester) async {
      // NOTE: 실제 ImagePicker 동작은 플랫폼 종속적이므로
      // 여기서는 주입 가능성만 확인합니다.
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PhotoPickerWidget(
              photo: null,
              onPhotoChanged: (_) {},
              imagePicker: null, // 테스트에서는 null 가능
            ),
          ),
        ),
      );

      expect(find.byType(PhotoPickerWidget), findsOneWidget);
    });
  });
}
