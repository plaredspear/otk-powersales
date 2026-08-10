import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/dio_provider.dart';
import '../../data/datasources/home_api_datasource.dart';
import '../../data/datasources/home_remote_datasource.dart';
import '../../data/repositories/home_repository_impl.dart';
import '../../domain/repositories/home_repository.dart';
import '../../domain/usecases/get_home_data.dart';
import 'home_state.dart';

// ============================================
// 1. Dependency Providers (DataSource, Repository, UseCase)
// ============================================

/// Home Remote DataSource Provider
final homeRemoteDataSourceProvider = Provider<HomeRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return HomeApiDataSource(dio);
});

/// Home Repository Provider
final homeRepositoryProvider = Provider<HomeRepository>((ref) {
  final remoteDataSource = ref.watch(homeRemoteDataSourceProvider);
  return HomeRepositoryImpl(remoteDataSource: remoteDataSource);
});

/// GetHomeData UseCase Provider
final getHomeDataUseCaseProvider = Provider<GetHomeData>((ref) {
  final repository = ref.watch(homeRepositoryProvider);
  return GetHomeData(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 홈 화면 상태 관리 Notifier
///
/// 홈 데이터의 로딩, 성공, 에러 상태를 관리한다.
class HomeNotifier extends StateNotifier<HomeState> {
  HomeNotifier(this._getHomeData, {DateTime Function()? now})
      : _now = now ?? DateTime.now,
        super(HomeState.initial());

  final GetHomeData _getHomeData;

  /// 현재 시각 공급자 — 테스트에서 시간 경과/날짜 변경을 주입하기 위해 분리.
  final DateTime Function() _now;

  /// 포그라운드 복귀 시 재조회를 건너뛰는 최소 간격.
  ///
  /// 잠깐 다른 앱을 보고 돌아오는 흔한 패턴에서 매번 요청이 나가는 것을 막는다.
  /// 이 간격 안이라도 날짜가 바뀌었으면 [refreshIfStale] 이 무조건 재조회한다.
  static const Duration staleThreshold = Duration(minutes: 5);

  /// 마지막으로 홈 데이터 조회를 성공한 시각 (조회 이력 없으면 null).
  DateTime? _lastFetchedAt;

  DateTime? get lastFetchedAt => _lastFetchedAt;

  /// 홈 데이터 조회
  Future<void> fetchHomeData() async {
    state = state.toLoading();

    try {
      final homeData = await _getHomeData();
      _lastFetchedAt = _now();
      state = state.toData(homeData);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 홈 데이터 새로고침 (Pull-to-refresh)
  Future<void> refresh() async {
    await fetchHomeData();
  }

  /// 데이터가 낡았을 때만 재조회 (포그라운드 복귀용).
  ///
  /// 홈은 오늘 일정·출근 집계 등 **당일 기준** 데이터를 보여주는데,
  /// `homeProvider` 는 autoDispose 가 아니라 앱을 켜 둔 동안 상태가 계속 살아 있다.
  /// 그래서 갱신 훅이 없으면 web 에서 행사 일정을 확정해도, 심지어 자정을 넘겨
  /// 날짜가 바뀌어도 화면이 로그인 시점 데이터에 고정된다.
  ///
  /// 재조회 조건은 ① 아직 한 번도 조회한 적 없음 ② 마지막 조회 이후
  /// [staleThreshold] 경과 ③ 마지막 조회 이후 날짜가 바뀜 — 셋 중 하나.
  /// 조회하지 않았으면 false 를 반환한다.
  Future<bool> refreshIfStale() async {
    if (!_isStale()) return false;
    await fetchHomeData();
    return true;
  }

  bool _isStale() {
    final lastFetchedAt = _lastFetchedAt;
    if (lastFetchedAt == null) return true;

    final now = _now();
    if (now.difference(lastFetchedAt) >= staleThreshold) return true;

    // 날짜 경계를 넘었으면 경과 시간과 무관하게 재조회 — 자정 직후 복귀 시
    // 어제 일정이 그대로 남아 있는 것을 막는다.
    return lastFetchedAt.year != now.year ||
        lastFetchedAt.month != now.month ||
        lastFetchedAt.day != now.day;
  }
}

// ============================================
// 3. StateNotifier Provider Definition
// ============================================

/// Home StateNotifier Provider
final homeProvider =
    StateNotifierProvider<HomeNotifier, HomeState>((ref) {
  final useCase = ref.watch(getHomeDataUseCaseProvider);
  return HomeNotifier(useCase);
});
