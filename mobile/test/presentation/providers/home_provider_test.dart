import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_summary.dart';
import 'package:mobile/domain/repositories/home_repository.dart';
import 'package:mobile/domain/usecases/get_home_data.dart';
import 'package:mobile/presentation/providers/home_provider.dart';

void main() {
  group('HomeNotifier', () {
    late FakeHomeRepository fakeRepository;
    late DateTime currentTime;

    /// 고정 시각을 주입한 Notifier 생성 — 테스트에서 `currentTime` 을 옮겨
    /// 포그라운드 복귀 시점의 경과 시간/날짜 변경을 재현한다.
    HomeNotifier createNotifier() {
      return HomeNotifier(
        GetHomeData(fakeRepository),
        now: () => currentTime,
      );
    }

    setUp(() {
      fakeRepository = FakeHomeRepository();
      currentTime = DateTime(2026, 8, 10, 9, 0);
    });

    group('fetchHomeData', () {
      test('조회 성공 시 homeData 를 설정해야 한다', () async {
        final notifier = createNotifier();

        await notifier.fetchHomeData();

        expect(notifier.state.homeData, isNotNull);
        expect(notifier.state.isLoading, false);
        expect(fakeRepository.callCount, 1);
      });

      test('조회 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('네트워크 오류');
        final notifier = createNotifier();

        await notifier.fetchHomeData();

        expect(notifier.state.isError, true);
        expect(notifier.state.homeData, isNull);
      });
    });

    group('refreshIfStale', () {
      test('조회 이력이 없으면 재조회해야 한다', () async {
        final notifier = createNotifier();

        final refreshed = await notifier.refreshIfStale();

        expect(refreshed, true);
        expect(fakeRepository.callCount, 1);
      });

      test('임계 시간 이내면 재조회하지 않아야 한다', () async {
        final notifier = createNotifier();
        await notifier.fetchHomeData();

        // 4분 경과 — 잠깐 다른 앱을 보고 돌아온 경우
        currentTime = currentTime.add(const Duration(minutes: 4));
        final refreshed = await notifier.refreshIfStale();

        expect(refreshed, false);
        expect(fakeRepository.callCount, 1);
      });

      test('임계 시간이 지나면 재조회해야 한다', () async {
        final notifier = createNotifier();
        await notifier.fetchHomeData();

        currentTime = currentTime.add(HomeNotifier.staleThreshold);
        final refreshed = await notifier.refreshIfStale();

        expect(refreshed, true);
        expect(fakeRepository.callCount, 2);
      });

      test('임계 시간 이내라도 날짜가 바뀌면 재조회해야 한다', () async {
        // 자정 1분 전 조회 → 자정 2분 후 복귀 (경과 3분 < 임계 5분)
        currentTime = DateTime(2026, 8, 10, 23, 59);
        final notifier = createNotifier();
        await notifier.fetchHomeData();

        currentTime = DateTime(2026, 8, 11, 0, 2);
        final refreshed = await notifier.refreshIfStale();

        expect(refreshed, true);
        expect(fakeRepository.callCount, 2);
      });

      test('재조회 후에는 마지막 조회 시각이 갱신되어 연속 호출이 차단되어야 한다',
          () async {
        final notifier = createNotifier();
        await notifier.fetchHomeData();

        currentTime = currentTime.add(const Duration(minutes: 10));
        await notifier.refreshIfStale();
        // 같은 시각에 곧바로 한 번 더 복귀 이벤트가 온 경우
        final secondRefresh = await notifier.refreshIfStale();

        expect(secondRefresh, false);
        expect(fakeRepository.callCount, 2);
      });

      test('조회 실패는 마지막 조회 시각을 갱신하지 않아 다음 복귀에서 재시도해야 한다',
          () async {
        fakeRepository.exceptionToThrow = Exception('네트워크 오류');
        final notifier = createNotifier();
        await notifier.fetchHomeData();

        fakeRepository.exceptionToThrow = null;
        final refreshed = await notifier.refreshIfStale();

        expect(refreshed, true);
        expect(notifier.state.homeData, isNotNull);
      });
    });
  });
}

class FakeHomeRepository implements HomeRepository {
  int callCount = 0;
  Exception? exceptionToThrow;

  @override
  Future<HomeData> getHomeData() async {
    callCount++;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return _sampleHomeData;
  }
}

const _sampleHomeData = HomeData(
  todaySchedules: [],
  attendanceSummary: AttendanceSummary(totalCount: 0, registeredCount: 0),
  attendanceApplicable: true,
  safetyCheckRequired: false,
  notices: [],
  currentDate: '2026-08-10',
);
