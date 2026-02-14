import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/pages/my_schedule_detail_page.dart';
import 'package:mobile/presentation/providers/my_schedule_provider.dart';
import 'package:mobile/domain/entities/daily_schedule_info.dart';
import 'package:mobile/domain/entities/monthly_schedule_day.dart';
import 'package:mobile/domain/entities/schedule_store_detail.dart';
import 'package:mobile/domain/repositories/my_schedule_repository.dart';

/// 테스트용 Mock Repository
class MockMyScheduleRepository implements MyScheduleRepository {
  DailyScheduleInfo? dailySchedule;
  Exception? exceptionToThrow;

  @override
  Future<List<MonthlyScheduleDay>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    throw UnimplementedError();
  }

  @override
  Future<DailyScheduleInfo> getDailySchedule(DateTime date) async {
    await Future.delayed(const Duration(milliseconds: 10));
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return dailySchedule!;
  }
}

void main() {
  late MockMyScheduleRepository mockRepository;

  setUp(() {
    mockRepository = MockMyScheduleRepository();
  });

  group('MyScheduleDetailPage', () {
    testWidgets('페이지가 렌더링된다', (tester) async {
      // Given
      mockRepository.dailySchedule = DailyScheduleInfo(
        date: '2026년 02월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: const ReportProgress(
          completed: 0,
          total: 2,
          workType: '진열',
        ),
        stores: const [],
      );

      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: MyScheduleDetailPage(
              selectedDate: DateTime(2026, 2, 4),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then
      expect(find.text('최금주'), findsOneWidget);
    });

    testWidgets('데이터 로드 후 정상적으로 표시된다', (tester) async {
      // Given
      mockRepository.dailySchedule = DailyScheduleInfo(
        date: '2026년 02월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: const ReportProgress(
          completed: 0,
          total: 2,
          workType: '진열',
        ),
        stores: const [],
      );

      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: MyScheduleDetailPage(
              selectedDate: DateTime(2026, 2, 4),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then: 데이터가 표시됨
      expect(find.text('2026년 02월 04일(화)'), findsWidgets);
    });

    testWidgets('탭 바가 표시된다', (tester) async {
      // Given
      mockRepository.dailySchedule = DailyScheduleInfo(
        date: '2026년 02월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: const ReportProgress(
          completed: 0,
          total: 2,
          workType: '진열',
        ),
        stores: const [],
      );

      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: MyScheduleDetailPage(
              selectedDate: DateTime(2026, 2, 4),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then
      expect(find.text('일정'), findsOneWidget);
      expect(find.text('등록'), findsOneWidget);
    });

    testWidgets('일정 탭에 날짜와 보고 진행 정보를 표시한다', (tester) async {
      // Given
      mockRepository.dailySchedule = DailyScheduleInfo(
        date: '2026년 02월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: const ReportProgress(
          completed: 1,
          total: 3,
          workType: '진열',
        ),
        stores: const [],
      );

      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: MyScheduleDetailPage(
              selectedDate: DateTime(2026, 2, 4),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then
      expect(find.text('2026년 02월 04일(화)'), findsWidgets);
      expect(find.text('1 / 3 보고 완료 (진열)'), findsWidgets);
    });

    testWidgets('일정 탭에 조원명과 사원번호를 표시한다', (tester) async {
      // Given
      mockRepository.dailySchedule = DailyScheduleInfo(
        date: '2026년 02월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: const ReportProgress(
          completed: 0,
          total: 2,
          workType: '진열',
        ),
        stores: const [],
      );

      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: MyScheduleDetailPage(
              selectedDate: DateTime(2026, 2, 4),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then
      expect(find.text('최금주 (20030117)'), findsOneWidget);
    });

    testWidgets('일정 탭에 거래처 목록을 표시한다', (tester) async {
      // Given
      mockRepository.dailySchedule = DailyScheduleInfo(
        date: '2026년 02월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: const ReportProgress(
          completed: 0,
          total: 2,
          workType: '진열',
        ),
        stores: const [
          ScheduleStoreDetail(
            storeId: 1,
            storeName: '이마트',
            workType1: '진열',
            workType2: '전담',
            workType3: '순회',
            isRegistered: true,
          ),
          ScheduleStoreDetail(
            storeId: 2,
            storeName: '롯데마트',
            workType1: '진열',
            workType2: '전담',
            workType3: '격고',
            isRegistered: false,
          ),
        ],
      );

      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: MyScheduleDetailPage(
              selectedDate: DateTime(2026, 2, 4),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then
      expect(find.text('이마트'), findsOneWidget);
      expect(find.text('롯데마트'), findsOneWidget);
    });

    testWidgets('등록 탭으로 전환할 수 있다', (tester) async {
      // Given
      mockRepository.dailySchedule = DailyScheduleInfo(
        date: '2026년 02월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: const ReportProgress(
          completed: 0,
          total: 2,
          workType: '진열',
        ),
        stores: const [],
      );

      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: MyScheduleDetailPage(
              selectedDate: DateTime(2026, 2, 4),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Then: 등록 탭 버튼 찾기
      final registrationTab = find.text('등록');
      expect(registrationTab, findsOneWidget);

      // When: 등록 탭 탭
      await tester.tap(registrationTab);
      await tester.pumpAndSettle();

      // Then: 등록 전 필터 체크박스가 표시됨
      expect(find.byType(Checkbox), findsOneWidget);
    });

    testWidgets('등록 탭에서 등록 전 필터를 토글할 수 있다', (tester) async {
      // Given
      mockRepository.dailySchedule = DailyScheduleInfo(
        date: '2026년 02월 04일(화)',
        memberName: '최금주',
        employeeNumber: '20030117',
        reportProgress: const ReportProgress(
          completed: 0,
          total: 2,
          workType: '진열',
        ),
        stores: const [
          ScheduleStoreDetail(
            storeId: 1,
            storeName: '이마트',
            workType1: '진열',
            workType2: '전담',
            workType3: '순회',
            isRegistered: true,
          ),
          ScheduleStoreDetail(
            storeId: 2,
            storeName: '롯데마트',
            workType1: '진열',
            workType2: '전담',
            workType3: '격고',
            isRegistered: false,
          ),
        ],
      );

      // When
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            myScheduleRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: MyScheduleDetailPage(
              selectedDate: DateTime(2026, 2, 4),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // When: 등록 탭으로 전환
      await tester.tap(find.text('등록'));
      await tester.pumpAndSettle();

      // Then: 필터 OFF 상태에서는 모든 거래처 표시
      expect(find.text('이마트'), findsOneWidget);
      expect(find.text('롯데마트'), findsOneWidget);

      // When: 등록 전 필터 체크
      await tester.tap(find.byType(Checkbox));
      await tester.pumpAndSettle();

      // Then: 등록 전 항목만 표시
      expect(find.text('이마트'), findsNothing);
      expect(find.text('롯데마트'), findsOneWidget);
    });
  });
}
