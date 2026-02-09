import '../../domain/repositories/home_repository.dart';

/// 홈 화면 상태
///
/// 홈 화면의 데이터 로딩 상태를 관리한다.
/// - initial: 데이터 요청 전 초기 상태
/// - loading: API 호출 중
/// - loaded: 데이터 로딩 완료
/// - error: API 호출 실패
class HomeState {
  /// 홈 데이터 (loaded 상태에서만 값이 있음)
  final HomeData? homeData;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지 (error 상태에서만 값이 있음)
  final String? errorMessage;

  const HomeState({
    this.homeData,
    this.isLoading = false,
    this.errorMessage,
  });

  /// 초기 상태
  factory HomeState.initial() {
    return const HomeState();
  }

  /// 로딩 상태로 전환
  HomeState toLoading() {
    return HomeState(
      homeData: homeData,
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 성공 상태로 전환
  HomeState toData(HomeData data) {
    return HomeState(
      homeData: data,
      isLoading: false,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  HomeState toError(String message) {
    return HomeState(
      homeData: homeData,
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 데이터 로딩 완료 여부
  bool get isLoaded => homeData != null && !isLoading && errorMessage == null;

  /// 에러 상태 여부
  bool get isError => errorMessage != null;

  /// copyWith
  HomeState copyWith({
    HomeData? homeData,
    bool? isLoading,
    String? errorMessage,
  }) {
    return HomeState(
      homeData: homeData ?? this.homeData,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
    );
  }
}
