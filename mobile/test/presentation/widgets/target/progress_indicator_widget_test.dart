import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/progress.dart';
import 'package:mobile/presentation/widgets/target/progress_indicator_widget.dart';

void main() {
  group('ProgressIndicatorWidget Tests', () {
    testWidgets('가로형 프로그레스 바가 렌더링된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 800,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressIndicatorWidget(
              progress: progress,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(find.byType(ProgressIndicatorWidget), findsOneWidget);
      expect(find.text('80.0%'), findsOneWidget);
    });

    testWidgets('백분율 텍스트가 표시된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 1200,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressIndicatorWidget(
              progress: progress,
              showPercentage: true,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(find.text('120.0%'), findsOneWidget);
    });

    testWidgets('백분율 텍스트를 숨길 수 있다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 500,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressIndicatorWidget(
              progress: progress,
              showPercentage: false,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(find.text('50.0%'), findsNothing);
    });

    testWidgets('진도율 초과 시 녹색으로 표시된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 1200,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressIndicatorWidget(
              progress: progress,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(progress.color, equals(Colors.green));
      expect(progress.status, equals(ProgressStatus.exceeded));
    });

    testWidgets('진도율 부족 시 빨강색으로 표시된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 700,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressIndicatorWidget(
              progress: progress,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(progress.color, equals(Colors.red));
      expect(progress.status, equals(ProgressStatus.insufficient));
    });

    testWidgets('진도율 달성 시 파랑색으로 표시된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 1000,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressIndicatorWidget(
              progress: progress,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(progress.color, equals(Colors.blue));
      expect(progress.status, equals(ProgressStatus.achieved));
    });

    testWidgets('애니메이션 효과가 동작한다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 800,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressIndicatorWidget(
              progress: progress,
              animated: true,
              animationDuration: 500,
            ),
          ),
        ),
      );

      // Initial state (animation not started)
      await tester.pump();

      // Mid-animation
      await tester.pump(const Duration(milliseconds: 250));

      // Animation complete
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('80.0%'), findsOneWidget);
    });

    testWidgets('커스텀 높이를 설정할 수 있다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 600,
      );
      const customHeight = 32.0;

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressIndicatorWidget(
              progress: progress,
              height: customHeight,
              animated: false,
            ),
          ),
        ),
      );

      // Assert - 위젯이 렌더링되었는지 확인
      expect(find.byType(ProgressIndicatorWidget), findsOneWidget);
      expect(find.text('60.0%'), findsOneWidget);
    });
  });

  group('CircularProgressIndicatorWidget Tests', () {
    testWidgets('원형 프로그레스 바가 렌더링된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 750,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CircularProgressIndicatorWidget(
              progress: progress,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(find.byType(CircularProgressIndicatorWidget), findsOneWidget);
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      expect(find.text('75.0%'), findsOneWidget);
    });

    testWidgets('백분율 텍스트가 중앙에 표시된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 900,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CircularProgressIndicatorWidget(
              progress: progress,
              showPercentage: true,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(find.text('90.0%'), findsOneWidget);
    });

    testWidgets('백분율 텍스트를 숨길 수 있다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 400,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CircularProgressIndicatorWidget(
              progress: progress,
              showPercentage: false,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(find.text('40.0%'), findsNothing);
    });

    testWidgets('진도율에 따라 색상이 변경된다', (tester) async {
      // Arrange - 초과
      final exceededProgress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 1100,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CircularProgressIndicatorWidget(
              progress: exceededProgress,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      expect(exceededProgress.color, equals(Colors.green));
    });

    testWidgets('애니메이션 효과가 동작한다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 850,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CircularProgressIndicatorWidget(
              progress: progress,
              animated: true,
              animationDuration: 500,
            ),
          ),
        ),
      );

      // Initial state
      await tester.pump();

      // Mid-animation
      await tester.pump(const Duration(milliseconds: 250));

      // Animation complete
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('85.0%'), findsOneWidget);
    });

    testWidgets('커스텀 크기를 설정할 수 있다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 600,
      );
      const customSize = 100.0;

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CircularProgressIndicatorWidget(
              progress: progress,
              size: customSize,
              animated: false,
            ),
          ),
        ),
      );

      // Assert - 위젯이 렌더링되었는지 확인
      expect(find.byType(CircularProgressIndicatorWidget), findsOneWidget);
      expect(find.text('60.0%'), findsOneWidget);
    });

    testWidgets('커스텀 strokeWidth를 설정할 수 있다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 700,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CircularProgressIndicatorWidget(
              progress: progress,
              strokeWidth: 12.0,
              animated: false,
            ),
          ),
        ),
      );

      // Assert
      final circularProgress = tester.widget<CircularProgressIndicator>(
        find.byType(CircularProgressIndicator),
      );
      expect(circularProgress.strokeWidth, equals(12.0));
    });
  });

  group('ProgressBadge Tests', () {
    testWidgets('배지가 렌더링된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 950,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressBadge(progress: progress),
          ),
        ),
      );

      // Assert
      expect(find.byType(ProgressBadge), findsOneWidget);
      expect(find.text('95.0'), findsOneWidget);
    });

    testWidgets('아이콘을 표시할 수 있다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 1200,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressBadge(
              progress: progress,
              showIcon: true,
            ),
          ),
        ),
      );

      // Assert
      expect(find.byIcon(Icons.trending_up), findsOneWidget);
      expect(find.text('120.0'), findsNothing);
    });

    testWidgets('진도율 초과 시 trending_up 아이콘이 표시된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 1300,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressBadge(
              progress: progress,
              showIcon: true,
            ),
          ),
        ),
      );

      // Assert
      expect(find.byIcon(Icons.trending_up), findsOneWidget);
    });

    testWidgets('진도율 달성 시 check_circle 아이콘이 표시된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 1000,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressBadge(
              progress: progress,
              showIcon: true,
            ),
          ),
        ),
      );

      // Assert
      expect(find.byIcon(Icons.check_circle), findsOneWidget);
    });

    testWidgets('진도율 부족 시 trending_down 아이콘이 표시된다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 600,
      );

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressBadge(
              progress: progress,
              showIcon: true,
            ),
          ),
        ),
      );

      // Assert
      expect(find.byIcon(Icons.trending_down), findsOneWidget);
    });

    testWidgets('커스텀 크기를 설정할 수 있다', (tester) async {
      // Arrange
      final progress = Progress.calculate(
        targetAmount: 1000,
        actualAmount: 800,
      );
      const customSize = 80.0;

      // Act
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressBadge(
              progress: progress,
              size: customSize,
            ),
          ),
        ),
      );

      // Assert - 위젯이 렌더링되었는지 확인
      expect(find.byType(ProgressBadge), findsOneWidget);
      expect(find.text('80.0'), findsOneWidget);
    });
  });
}
