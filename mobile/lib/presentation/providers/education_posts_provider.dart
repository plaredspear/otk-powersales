import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/core/network/dio_provider.dart';
import 'package:mobile/data/datasources/education_api_datasource.dart';
import 'package:mobile/data/datasources/education_remote_datasource.dart';
import 'package:mobile/data/repositories/education_repository_impl.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/domain/repositories/education_repository.dart';
import 'package:mobile/domain/usecases/get_education_posts_usecase.dart';
import 'package:mobile/presentation/providers/education_posts_state.dart';

/// 교육 원격 데이터소스 Provider (실 API)
final educationRemoteDataSourceProvider =
    Provider<EducationRemoteDataSource>((ref) {
  return EducationApiDataSource(ref.watch(dioProvider));
});

/// 교육 Repository Provider (실 API — EducationController)
final educationRepositoryProvider = Provider<EducationRepository>((ref) {
  return EducationRepositoryImpl(
    remoteDataSource: ref.watch(educationRemoteDataSourceProvider),
  );
});

/// 교육 게시물 목록 조회 UseCase Provider
final getEducationPostsUseCaseProvider = Provider<GetEducationPostsUseCase>((ref) {
  final repository = ref.watch(educationRepositoryProvider);
  return GetEducationPostsUseCase(repository);
});

/// 교육 게시물 목록 Notifier
///
/// 카테고리는 생성 시점에 고정된다 (provider family key). 하나의 Notifier 가 카테고리를 갈아끼우면
/// 이전 카테고리의 목록이 다음 화면 첫 프레임에 그대로 노출되므로, 카테고리별로 인스턴스를 분리한다.
class EducationPostsNotifier extends StateNotifier<EducationPostsState> {
  EducationPostsNotifier(this._getEducationPosts, EducationCategory category)
      : super(EducationPostsState.initial(category)) {
    // 초기 로드
    fetchPosts();
  }

  final GetEducationPostsUseCase _getEducationPosts;

  /// 게시물 목록 조회
  Future<void> fetchPosts() async {
    state = state.toLoading();
    try {
      final postPage = await _getEducationPosts(
        category: state.category,
        search: state.searchKeyword,
        page: state.currentPage,
        size: 20, // 레거시 edu/list pageSize=20 정합
      );
      state = state.toData(postPage);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 검색
  Future<void> search(String? keyword) async {
    state = state.withSearchKeyword(keyword);
    await fetchPosts();
  }

  /// 검색어 초기화
  Future<void> clearSearch() async {
    await search(null);
  }

  /// 페이지 변경
  Future<void> changePage(int page) async {
    if (state.currentPage == page) return;
    state = state.withPage(page);
    await fetchPosts();
  }

  /// 다음 페이지
  Future<void> nextPage() async {
    if (state.isLastPage) return;
    await changePage(state.currentPage + 1);
  }

  /// 이전 페이지
  Future<void> previousPage() async {
    if (state.isFirstPage) return;
    await changePage(state.currentPage - 1);
  }

  /// 새로고침
  Future<void> refresh() async {
    await fetchPosts();
  }
}

/// 교육 게시물 목록 Provider (카테고리별 family)
///
/// 카테고리를 family key 로 두어 카테고리마다 독립된 상태를 갖는다 — 시식 매뉴얼을 보고 APP 매뉴얼로
/// 이동했을 때 이전 카테고리의 목록이 잠깐 노출되던 문제를 구조적으로 차단한다 (단일 provider 를 공유하면
/// 화면 첫 프레임이 직전 카테고리의 상태를 그린다).
///
/// autoDispose: 목록 화면을 벗어나면 상태를 버려 재진입 시 항상 최신 목록을 받는다 (검색어/페이지도 초기화).
final educationPostsProvider = StateNotifierProvider.autoDispose
    .family<EducationPostsNotifier, EducationPostsState, EducationCategory>(
        (ref, category) {
  final useCase = ref.watch(getEducationPostsUseCaseProvider);
  return EducationPostsNotifier(useCase, category);
});
