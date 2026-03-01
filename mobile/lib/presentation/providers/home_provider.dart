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
  HomeNotifier(this._getHomeData) : super(HomeState.initial());

  final GetHomeData _getHomeData;

  /// 홈 데이터 조회
  Future<void> fetchHomeData() async {
    state = state.toLoading();

    try {
      final homeData = await _getHomeData();
      state = state.toData(homeData);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 홈 데이터 새로고침 (Pull-to-refresh)
  Future<void> refresh() async {
    await fetchHomeData();
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
