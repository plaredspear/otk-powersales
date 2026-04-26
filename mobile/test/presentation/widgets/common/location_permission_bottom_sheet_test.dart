import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/common/location_permission_bottom_sheet.dart';

void main() {
  group('LocationPermissionBottomSheet', () {
    group('denied 모드 (openSettings: false)', () {
      testWidgets('제목과 설명 텍스트가 올바르게 표시된다', (tester) async {
        await tester.pumpWidget(
          _buildTestApp(openSettings: false),
        );

        // 바텀시트 열기
        await tester.tap(find.text('Open'));
        await tester.pumpAndSettle();

        expect(find.text('위치 권한이 필요합니다'), findsOneWidget);
        expect(find.text('출근등록을 위해 위치 정보\n접근을 허용해 주세요'), findsOneWidget);
        expect(find.text('권한 허용하기'), findsOneWidget);
        expect(find.text('취소'), findsOneWidget);
      });

      testWidgets('"권한 허용하기" 탭 시 true 반환', (tester) async {
        bool? result;
        await tester.pumpWidget(
          _buildTestApp(
            openSettings: false,
            onResult: (r) => result = r,
          ),
        );

        await tester.tap(find.text('Open'));
        await tester.pumpAndSettle();

        await tester.tap(find.text('권한 허용하기'));
        await tester.pumpAndSettle();

        expect(result, true);
      });

      testWidgets('"취소" 탭 시 null 반환', (tester) async {
        bool? result;
        bool resultSet = false;
        await tester.pumpWidget(
          _buildTestApp(
            openSettings: false,
            onResult: (r) {
              result = r;
              resultSet = true;
            },
          ),
        );

        await tester.tap(find.text('Open'));
        await tester.pumpAndSettle();

        await tester.tap(find.text('취소'));
        await tester.pumpAndSettle();

        expect(resultSet, true);
        expect(result, null);
      });
    });

    group('deniedForever / serviceDisabled 모드 (openSettings: true)', () {
      testWidgets('설정 유도 텍스트가 올바르게 표시된다', (tester) async {
        await tester.pumpWidget(
          _buildTestApp(openSettings: true),
        );

        await tester.tap(find.text('Open'));
        await tester.pumpAndSettle();

        expect(find.text('위치 권한이 필요합니다'), findsOneWidget);
        expect(find.text('설정에서 위치 권한을 허용해 주세요'), findsOneWidget);
        expect(find.text('설정으로 이동'), findsOneWidget);
        expect(find.text('취소'), findsOneWidget);
      });

      testWidgets('"설정으로 이동" 탭 시 true 반환', (tester) async {
        bool? result;
        await tester.pumpWidget(
          _buildTestApp(
            openSettings: true,
            onResult: (r) => result = r,
          ),
        );

        await tester.tap(find.text('Open'));
        await tester.pumpAndSettle();

        await tester.tap(find.text('설정으로 이동'));
        await tester.pumpAndSettle();

        expect(result, true);
      });
    });
  });
}

Widget _buildTestApp({
  required bool openSettings,
  void Function(bool?)? onResult,
}) {
  return MaterialApp(
    home: Scaffold(
      body: Builder(
        builder: (context) => ElevatedButton(
          onPressed: () async {
            final result = await LocationPermissionBottomSheet.show(
              context,
              openSettings: openSettings,
            );
            onResult?.call(result);
          },
          child: const Text('Open'),
        ),
      ),
    ),
  );
}
