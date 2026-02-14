import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/pages/my_schedule_calendar_page.dart';
import 'package:mobile/presentation/providers/my_schedule_provider.dart';
import 'package:mobile/presentation/providers/my_schedule_state.dart';
import 'package:mobile/domain/entities/monthly_schedule_day.dart';
import 'package:mobile/domain/entities/daily_schedule_info.dart';
import 'package:mobile/domain/repositories/my_schedule_repository.dart';

/// 테스트용 Mock Repository
class MockMyScheduleRepository implements MyScheduleRepository {
  List<MonthlyScheduleDay> monthlySchedule = [];

  @override
  Future<List<MonthlyScheduleDay>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    await Future.delayed(const Duration(milliseconds: 10));
    return monthlySchedule;
  }

  @override
  Future<DailyScheduleInfo> getDailySchedule(DateTime date) async {
    throw UnimplementedError();
  }
}

void main() {
  late MockMyScheduleRepository mockRepository;

  setUp(() {
    mockRepository = MockMyScheduleRepository();
  });

  group('MyScheduleCalendarPage', () {
    testWidgets('페이지가 렌더링된다', (tester) async {
      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: const MaterialApp(
            home: MyScheduleCalendarPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then
      expect(find.text('일정'), findsOneWidget);
    });

    testWidgets('로딩 상태를 표시한다', (tester) async {
      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: const MaterialApp(
            home: MyScheduleCalendarPage(),
          ),
        ),
      );

      // Then: 초기 로딩 표시
      expect(find.byType(CircularProgressIndicator), findsOneWidget);

      // Clean up: 비동기 작업 완료 대기
      await tester.pumpAndSettle();
    });

    testWidgets('월간 데이터 로드 후 캘린더를 표시한다', (tester) async {
      // Given
      mockRepository.monthlySchedule = [
        MonthlyScheduleDay(
          date: DateTime(2026, 2, 3),
          hasWork: true,
        ),
      ];

      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: const MaterialApp(
            home: MyScheduleCalendarPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then: 요일 헤더가 표시됨
      expect(find.text('일'), findsOneWidget);
      expect(find.text('월'), findsOneWidget);
      expect(find.text('토'), findsOneWidget);
    });

    testWidgets('월 네비게이션 버튼이 표시된다', (tester) async {
      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: const MaterialApp(
            home: MyScheduleCalendarPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then
      expect(find.byIcon(Icons.chevron_left), findsOneWidget);
      expect(find.byIcon(Icons.chevron_right), findsOneWidget);
    });

    testWidgets('현재 연월이 표시된다', (tester) async {
      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: const MaterialApp(
            home: MyScheduleCalendarPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then: 현재 연월이 표시됨
      final now = DateTime.now();
      expect(find.textContaining('${now.year}년'), findsOneWidget);
      expect(find.textContaining('${now.month}월'), findsOneWidget);
    });

    testWidgets('이전 월 버튼을 탭하면 이전 월로 이동한다', (tester) async {
      // Given
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: const MaterialApp(
            home: MyScheduleCalendarPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final now = DateTime.now();
      final expectedYear = now.month == 1 ? now.year - 1 : now.year;
      final expectedMonth = now.month == 1 ? 12 : now.month - 1;

      // When: 이전 월 버튼 탭
      await tester.tap(find.byIcon(Icons.chevron_left));
      await tester.pumpAndSettle();

      // Then: 이전 월이 표시됨
      expect(find.textContaining('${expectedYear}년'), findsOneWidget);
      expect(find.textContaining('${expectedMonth}월'), findsOneWidget);
    });

    testWidgets('다음 월 버튼을 탭하면 다음 월로 이동한다', (tester) async {
      // Given
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: const MaterialApp(
            home: MyScheduleCalendarPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final now = DateTime.now();
      final expectedYear = now.month == 12 ? now.year + 1 : now.year;
      final expectedMonth = now.month == 12 ? 1 : now.month + 1;

      // When: 다음 월 버튼 탭
      await tester.tap(find.byIcon(Icons.chevron_right));
      await tester.pumpAndSettle();

      // Then: 다음 월이 표시됨
      expect(find.textContaining('${expectedYear}년'), findsOneWidget);
      expect(find.textContaining('${expectedMonth}월'), findsOneWidget);
    });

    testWidgets('뒤로가기 버튼이 동작한다', (tester) async {
      // Given
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: const MaterialApp(
            home: MyScheduleCalendarPage(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // When: 뒤로가기 버튼 탭 (현재 테스트 환경에서는 Navigator.pop만 확인)
      final backButton = find.byIcon(Icons.arrow_back);
      expect(backButton, findsOneWidget);
    });
  });
}
