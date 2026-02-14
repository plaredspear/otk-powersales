import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_schedule_info.dart';
import 'package:mobile/domain/entities/monthly_schedule_day.dart';
import 'package:mobile/domain/entities/schedule_store_detail.dart';
import 'package:mobile/domain/repositories/my_schedule_repository.dart';
import 'package:mobile/presentation/providers/my_schedule_provider.dart';
import 'package:mobile/presentation/providers/my_schedule_state.dart';

/// 테스트용 Mock MyScheduleRepository
class MockMyScheduleRepository implements MyScheduleRepository {
  List<MonthlyScheduleDay>? monthlySchedule;
  DailyScheduleInfo? dailySchedule;
  Exception? exceptionToThrow;

  @override
  Future<List<MonthlyScheduleDay>> getMonthlySchedule(
    int year,
    int month,
  ) async {
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return monthlySchedule!;
  }

  @override
  Future<DailyScheduleInfo> getDailySchedule(DateTime date) async {
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return dailySchedule!;
  }
}

void main() {
  group('MyScheduleCalendarState', () {
    test('초기 상태가 올바르게 설정된다', () {
      final state = MyScheduleCalendarState.initial();

      expect(state.workDays, isEmpty);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.currentYear, DateTime.now().year);
      expect(state.currentMonth, DateTime.now().month);
    });

    test('toLoading으로 로딩 상태로 전환된다', () {
      final state = MyScheduleCalendarState.initial().toLoading();

      expect(state.isLoading, true);
      expect(state.errorMessage, isNull);
    });

    test('toData로 성공 상태로 전환된다', () {
      final workDays = [
        MonthlyScheduleDay(date: DateTime(2026, 2, 1), hasWork: true),
      ];

      final state = MyScheduleCalendarState.initial().toData(workDays);

      expect(state.workDays, workDays);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
    });

    test('toError로 에러 상태로 전환된다', () {
      final state = MyScheduleCalendarState.initial().toError('네트워크 오류');

      expect(state.errorMessage, '네트워크 오류');
      expect(state.isLoading, false);
      expect(state.isError, true);
    });

    test('toMonth로 다른 월로 이동한다', () {
      final state = MyScheduleCalendarState.initial().toMonth(2025, 12);

      expect(state.currentYear, 2025);
      expect(state.currentMonth, 12);
      expect(state.workDays, isEmpty);
    });
  });

  group('MyScheduleDetailState', () {
    test('초기 상태가 올바르게 설정된다', () {
      final state = MyScheduleDetailState.initial();

      expect(state.scheduleInfo, isNull);
      expect(state.isLoading, false);
      expect(state.showOnlyUnregistered, false);
      expect(state.errorMessage, isNull);
    });

    test('toggleFilter로 필터를 토글한다', () {
      final state = MyScheduleDetailState.initial();

      expect(state.showOnlyUnregistered, false);

      final toggled = state.toggleFilter();
      expect(toggled.showOnlyUnregistered, true);

      final toggledAgain = toggled.toggleFilter();
      expect(toggledAgain.showOnlyUnregistered, false);
    });

    test('filteredStores가 필터 적용 없이 전체 목록을 반환한다', () {
      final scheduleInfo = DailyScheduleInfo(
        date: '2026년 02월 04일(수)',
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

      final state = MyScheduleDetailState.initial().toData(scheduleInfo);

      expect(state.filteredStores.length, 2);
    });

    test('filteredStores가 등록 전 항목만 필터링한다', () {
      final scheduleInfo = DailyScheduleInfo(
        date: '2026년 02월 04일(수)',
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

      final state = MyScheduleDetailState.initial()
          .toData(scheduleInfo)
          .toggleFilter();

      expect(state.filteredStores.length, 1);
      expect(state.filteredStores[0].storeName, '롯데마트');
    });

    test('unregisteredCount가 미등록 거래처 수를 반환한다', () {
      final scheduleInfo = DailyScheduleInfo(
        date: '2026년 02월 04일(수)',
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

      final state = MyScheduleDetailState.initial().toData(scheduleInfo);

      expect(state.unregisteredCount, 1);
    });
  });
}
