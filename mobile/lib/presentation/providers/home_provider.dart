import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/repositories/mock/home_mock_repository.dart';
import '../../domain/repositories/home_repository.dart';
import '../../domain/usecases/get_home_data.dart';
import 'home_state.dart';

/// Home Repository Provider
final homeRepositoryProvider = Provider<HomeRepository>((ref) {
  return HomeMockRepository();
});

/// GetHomeData UseCase Provider
final getHomeDataUseCaseProvider = Provider<GetHomeData>((ref) {
  final repository = ref.watch(homeRepositoryProvider);
  return GetHomeData(repository);
});

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

/// Home StateNotifier Provider
final homeProvider =
    StateNotifierProvider<HomeNotifier, HomeState>((ref) {
  final useCase = ref.watch(getHomeDataUseCaseProvider);
  return HomeNotifier(useCase);
});
