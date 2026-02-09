import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/expiry_alert.dart';
import 'package:mobile/domain/entities/notice.dart';
import 'package:mobile/domain/entities/schedule.dart';
import 'package:mobile/domain/repositories/home_repository.dart';
import 'package:mobile/domain/usecases/get_home_data.dart';
import 'package:mobile/presentation/providers/home_provider.dart';
import 'package:mobile/presentation/providers/home_state.dart';

/// 테스트용 Mock HomeRepository
class MockHomeRepository implements HomeRepository {
  HomeData? homeData;
  Exception? exceptionToThrow;
  int callCount = 0;

  @override
  Future<HomeData> getHomeData() async {
    callCount++;
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return homeData!;
  }
}

void main() {
  group('HomeState', () {
    test('초기 상태가 올바르게 설정된다', () {
      final state = HomeState.initial();

      expect(state.homeData, isNull);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.isLoaded, false);
      expect(state.isError, false);
    });

    test('toLoading으로 로딩 상태로 전환된다', () {
      final state = HomeState.initial().toLoading();

      expect(state.isLoading, true);
      expect(state.errorMessage, isNull);
    });

    test('toData로 성공 상태로 전환된다', () {
      final homeData = HomeData(
        todaySchedules: const [],
        expiryAlert: null,
        notices: const [],
        currentDate: '2026-02-07',
      );

      final state = HomeState.initial().toData(homeData);

      expect(state.homeData, homeData);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.isLoaded, true);
    });

    test('toError로 에러 상태로 전환된다', () {
      final state = HomeState.initial().toError('네트워크 오류');

      expect(state.errorMessage, '네트워크 오류');
      expect(state.isLoading, false);
      expect(state.isError, true);
      expect(state.isLoaded, false);
    });

    test('toLoading은 기존 데이터를 유지한다', () {
      final homeData = HomeData(
        todaySchedules: const [],
        expiryAlert: null,
        notices: const [],
        currentDate: '2026-02-07',
      );

      final state = HomeState.initial().toData(homeData).toLoading();

      expect(state.homeData, homeData);
      expect(state.isLoading, true);
    });

    test('toError는 기존 데이터를 유지한다', () {
      final homeData = HomeData(
        todaySchedules: const [],
        expiryAlert: null,
        notices: const [],
        currentDate: '2026-02-07',
      );

      final state = HomeState.initial().toData(homeData).toError('서버 오류');

      expect(state.homeData, homeData);
      expect(state.errorMessage, '서버 오류');
    });

    test('copyWith로 상태를 복사할 수 있다', () {
      final state = HomeState.initial().copyWith(isLoading: true);

      expect(state.isLoading, true);
      expect(state.homeData, isNull);
    });
  });

  group('HomeNotifier', () {
    late MockHomeRepository mockRepository;
    late GetHomeData useCase;
    late HomeNotifier notifier;

    final testHomeData = HomeData(
      todaySchedules: const [
        Schedule(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        ),
      ],
      expiryAlert: const ExpiryAlert(
        branchName: '부산1지점',
        employeeName: '최금주',
        employeeId: '20030117',
        expiryCount: 1,
      ),
      notices: [
        Notice(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: DateTime.parse('2026-02-05T10:00:00.000Z'),
        ),
      ],
      currentDate: '2026-02-07',
    );

    setUp(() {
      mockRepository = MockHomeRepository();
      useCase = GetHomeData(mockRepository);
      notifier = HomeNotifier(useCase);
    });

    test('초기 상태는 HomeState.initial()이다', () {
      expect(notifier.state.homeData, isNull);
      expect(notifier.state.isLoading, false);
      expect(notifier.state.errorMessage, isNull);
    });

    test('fetchHomeData 성공 시 데이터가 로딩된다', () async {
      mockRepository.homeData = testHomeData;

      await notifier.fetchHomeData();

      expect(notifier.state.homeData, testHomeData);
      expect(notifier.state.isLoading, false);
      expect(notifier.state.errorMessage, isNull);
      expect(notifier.state.isLoaded, true);
    });

    test('fetchHomeData 성공 시 일정 데이터가 올바르다', () async {
      mockRepository.homeData = testHomeData;

      await notifier.fetchHomeData();

      expect(notifier.state.homeData!.todaySchedules.length, 1);
      expect(
        notifier.state.homeData!.todaySchedules.first.storeName,
        '이마트 부산점',
      );
    });

    test('fetchHomeData 성공 시 유통기한 알림이 올바르다', () async {
      mockRepository.homeData = testHomeData;

      await notifier.fetchHomeData();

      expect(notifier.state.homeData!.expiryAlert, isNotNull);
      expect(
        notifier.state.homeData!.expiryAlert!.branchName,
        '부산1지점',
      );
    });

    test('fetchHomeData 성공 시 공지사항이 올바르다', () async {
      mockRepository.homeData = testHomeData;

      await notifier.fetchHomeData();

      expect(notifier.state.homeData!.notices.length, 1);
      expect(
        notifier.state.homeData!.notices.first.title,
        '2월 영업 목표 달성 현황',
      );
    });

    test('fetchHomeData 실패 시 에러 상태로 전환된다', () async {
      mockRepository.exceptionToThrow = Exception('네트워크 오류');

      await notifier.fetchHomeData();

      expect(notifier.state.isError, true);
      expect(notifier.state.errorMessage, contains('네트워크 오류'));
      expect(notifier.state.isLoading, false);
    });

    test('refresh는 fetchHomeData를 호출한다', () async {
      mockRepository.homeData = testHomeData;

      await notifier.refresh();

      expect(mockRepository.callCount, 1);
      expect(notifier.state.isLoaded, true);
    });

    test('빈 일정으로 데이터가 로딩된다', () async {
      mockRepository.homeData = HomeData(
        todaySchedules: const [],
        expiryAlert: null,
        notices: const [],
        currentDate: '2026-02-07',
      );

      await notifier.fetchHomeData();

      expect(notifier.state.homeData!.todaySchedules, isEmpty);
      expect(notifier.state.homeData!.expiryAlert, isNull);
      expect(notifier.state.homeData!.notices, isEmpty);
    });
  });

  group('HomeProvider (ProviderContainer)', () {
    late ProviderContainer container;

    setUp(() {
      container = ProviderContainer();
    });

    tearDown(() {
      container.dispose();
    });

    test('초기 상태가 올바르게 설정된다', () {
      final state = container.read(homeProvider);

      expect(state.homeData, isNull);
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
    });

    test('fetchHomeData 호출 시 로딩 상태로 전환된다', () async {
      final notifier = container.read(homeProvider.notifier);

      final future = notifier.fetchHomeData();

      // 첫 프레임에서 로딩 상태 확인
      await Future.microtask(() {});
      final loadingState = container.read(homeProvider);
      expect(loadingState.isLoading, true);

      await future;
    });

    test('fetchHomeData 성공 시 Mock 데이터가 로딩된다', () async {
      final notifier = container.read(homeProvider.notifier);

      await notifier.fetchHomeData();

      final state = container.read(homeProvider);

      expect(state.isLoading, false);
      expect(state.homeData, isNotNull);
      expect(state.homeData!.todaySchedules, isNotEmpty);
      expect(state.homeData!.expiryAlert, isNotNull);
      expect(state.homeData!.notices, isNotEmpty);
    });

    test('refresh로 데이터가 새로고침된다', () async {
      final notifier = container.read(homeProvider.notifier);

      await notifier.fetchHomeData();
      await notifier.refresh();

      final state = container.read(homeProvider);

      expect(state.isLoaded, true);
      expect(state.homeData, isNotNull);
    });
  });
}
