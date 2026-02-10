import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/attendance/attendance_status_counter.dart';

void main() {
  group('AttendanceStatusCounter 위젯 테스트', () {
    testWidgets('카운트 텍스트를 렌더링한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AttendanceStatusCounter(
              registeredCount: 2,
              totalCount: 5,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.text('2/5'), findsOneWidget);
    });

    testWidgets('탭하면 onTap 콜백이 호출된다', (tester) async {
      bool tapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AttendanceStatusCounter(
              registeredCount: 2,
              totalCount: 5,
              onTap: () {
                tapped = true;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.byType(AttendanceStatusCounter));
      await tester.pump();

      expect(tapped, true);
    });

    testWidgets('완료 상태(5/5)에서 check_circle 아이콘을 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AttendanceStatusCounter(
              registeredCount: 5,
              totalCount: 5,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.check_circle), findsOneWidget);
      expect(find.byIcon(Icons.check_circle_outline), findsNothing);
    });

    testWidgets('미완료 상태에서 check_circle_outline 아이콘을 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AttendanceStatusCounter(
              registeredCount: 2,
              totalCount: 5,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.check_circle_outline), findsOneWidget);
      expect(find.byIcon(Icons.check_circle), findsNothing);
    });

    testWidgets('0/0 상태에서 check_circle_outline 아이콘을 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AttendanceStatusCounter(
              registeredCount: 0,
              totalCount: 0,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.check_circle_outline), findsOneWidget);
      expect(find.byIcon(Icons.check_circle), findsNothing);
    });

    testWidgets('등록 수가 전체 수를 초과하는 경우에도 완료 상태로 표시한다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AttendanceStatusCounter(
              registeredCount: 6,
              totalCount: 5,
              onTap: () {},
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.check_circle), findsOneWidget);
      expect(find.text('6/5'), findsOneWidget);
    });
  });
}
