import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/alternative_holiday.dart';
import 'package:mobile/domain/repositories/alternative_holiday_repository.dart';
import 'package:mobile/presentation/providers/alt_holiday_provider.dart';

void main() {
  group('AltHolidayRequestNotifier', () {
    late AltHolidayRequestNotifier notifier;
    late FakeAlternativeHolidayRepository fakeRepo;

    setUp(() {
      fakeRepo = FakeAlternativeHolidayRepository();
      notifier = AltHolidayRequestNotifier(fakeRepo);
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.isSubmitted, false);
      expect(notifier.state.actualWorkDate, isNull);
      expect(notifier.state.targetAltHolidayDate, isNull);
      expect(notifier.state.canSubmit, false);
    });

    test('날짜 선택 시 상태가 업데이트되어야 한다', () {
      final saturday = DateTime(2026, 3, 7);
      final monday = DateTime(2026, 3, 9);

      notifier.selectActualWorkDate(saturday);
      expect(notifier.state.actualWorkDate, saturday);
      expect(notifier.state.canSubmit, false);

      notifier.selectTargetAltHolidayDate(monday);
      expect(notifier.state.targetAltHolidayDate, monday);
      expect(notifier.state.canSubmit, true);
    });

    test('신청 성공 시 isSubmitted가 true가 되어야 한다', () async {
      fakeRepo.createResult = _sampleHoliday;
      notifier.selectActualWorkDate(DateTime(2026, 3, 7));
      notifier.selectTargetAltHolidayDate(DateTime(2026, 3, 9));

      await notifier.submit();

      expect(notifier.state.isSubmitted, true);
      expect(notifier.state.isLoading, false);
    });

    test('신청 실패 시 에러 메시지가 설정되어야 한다', () async {
      fakeRepo.exceptionToThrow = Exception('대상일에 해당 사원의 근무 스케줄이 없습니다');
      notifier.selectActualWorkDate(DateTime(2026, 3, 7));
      notifier.selectTargetAltHolidayDate(DateTime(2026, 3, 9));

      await notifier.submit();

      expect(notifier.state.errorMessage, contains('근무 스케줄'));
      expect(notifier.state.isLoading, false);
      expect(notifier.state.isSubmitted, false);
    });

    test('공휴일 로드 성공 시 holidays가 업데이트되어야 한다', () async {
      fakeRepo.holidaysToReturn = [DateTime(2026, 1, 1), DateTime(2026, 3, 1)];

      await notifier.loadHolidays(2026);

      expect(notifier.state.holidays.length, 2);
    });
  });

  group('AltHolidayHistoryNotifier', () {
    late AltHolidayHistoryNotifier notifier;
    late FakeAlternativeHolidayRepository fakeRepo;

    setUp(() {
      fakeRepo = FakeAlternativeHolidayRepository();
      notifier = AltHolidayHistoryNotifier(fakeRepo);
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.items, isEmpty);
      expect(notifier.state.hasLoaded, false);
    });

    test('이력 조회 성공 시 items가 업데이트되어야 한다', () async {
      fakeRepo.historyToReturn = [_sampleHoliday, _sampleHolidayApproved];

      await notifier.loadHistory();

      expect(notifier.state.items.length, 2);
      expect(notifier.state.hasLoaded, true);
      expect(notifier.state.isLoading, false);
    });

    test('이력 없을 시 isEmpty가 true가 되어야 한다', () async {
      fakeRepo.historyToReturn = [];

      await notifier.loadHistory();

      expect(notifier.state.isEmpty, true);
    });

    test('이력 조회 실패 시 에러 메시지가 설정되어야 한다', () async {
      fakeRepo.exceptionToThrow = Exception('네트워크 오류');

      await notifier.loadHistory();

      expect(notifier.state.errorMessage, contains('네트워크'));
      expect(notifier.state.hasLoaded, true);
    });
  });
}

// ============================================
// Fake Repository
// ============================================

class FakeAlternativeHolidayRepository implements AlternativeHolidayRepository {
  AlternativeHoliday? createResult;
  List<AlternativeHoliday> historyToReturn = [];
  List<DateTime> holidaysToReturn = [];
  Exception? exceptionToThrow;

  @override
  Future<AlternativeHoliday> createAlternativeHoliday({
    required DateTime actualWorkDate,
    required DateTime targetAltHolidayDate,
  }) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return createResult!;
  }

  @override
  Future<List<AlternativeHoliday>> getAlternativeHolidays({
    DateTime? startDate,
    DateTime? endDate,
  }) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return historyToReturn;
  }

  @override
  Future<List<DateTime>> getHolidays(int year) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return holidaysToReturn;
  }
}

// ============================================
// Test Data
// ============================================

final _sampleHoliday = AlternativeHoliday(
  id: 1,
  actualWorkDate: DateTime(2026, 3, 7),
  targetAltHolidayDate: DateTime(2026, 3, 9),
  status: '신규',
  createdAt: DateTime(2026, 3, 9, 10, 30),
);

final _sampleHolidayApproved = AlternativeHoliday(
  id: 2,
  actualWorkDate: DateTime(2026, 3, 14),
  targetAltHolidayDate: DateTime(2026, 3, 16),
  confirmAltHolidayDate: DateTime(2026, 3, 16),
  status: '승인',
  createdAt: DateTime(2026, 3, 16, 9, 0),
);
