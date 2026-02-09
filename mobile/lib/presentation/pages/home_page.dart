import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../providers/home_provider.dart';
import '../providers/home_state.dart';
import '../widgets/common/error_view.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/home/schedule_card.dart';
import '../widgets/home/expiry_alert_card.dart';
import '../widgets/home/notice_carousel.dart';
import '../widgets/home/product_search_bar.dart';
import '../widgets/home/quick_menu_grid.dart';

/// 홈 화면
///
/// 오늘 일정, 유통기한 알림, 공지사항, 제품 검색, 빠른 메뉴를 표시한다.
/// Pull-to-refresh로 전체 데이터를 새로고침할 수 있다.
class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  @override
  void initState() {
    super.initState();
    // 화면 로드 시 홈 데이터 조회
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(homeProvider.notifier).fetchHomeData();
    });
  }

  /// Pull-to-refresh 핸들러
  Future<void> _onRefresh() async {
    await ref.read(homeProvider.notifier).refresh();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(homeProvider);

    return Scaffold(
      appBar: _buildAppBar(),
      body: _buildBody(state),
    );
  }

  /// AppBar (로고 + 햄버거 메뉴)
  PreferredSizeWidget _buildAppBar() {
    return AppBar(
      title: Text(
        '오뚜기 파워세일즈',
        style: AppTypography.headlineLarge.copyWith(
          color: AppColors.otokiRed,
        ),
      ),
      centerTitle: false,
      actions: [
        IconButton(
          icon: const Icon(Icons.menu),
          onPressed: () {
            // TODO: 전체 메뉴 Drawer 열기 (후속 작업)
          },
          tooltip: '전체 메뉴',
        ),
      ],
    );
  }

  /// 본문 영역
  Widget _buildBody(HomeState state) {
    // 초기 상태 또는 로딩 중 (데이터 없음)
    if (state.homeData == null && !state.isError) {
      return const LoadingIndicator(
        message: '홈 데이터를 불러오는 중...',
      );
    }

    // 에러 상태 (데이터 없음)
    if (state.isError && state.homeData == null) {
      return ErrorView(
        message: '데이터를 불러올 수 없습니다',
        description: state.errorMessage,
        onRetry: () {
          ref.read(homeProvider.notifier).fetchHomeData();
        },
      );
    }

    // 데이터 있음 → 화면 렌더링
    return RefreshIndicator(
      onRefresh: _onRefresh,
      color: AppColors.primary,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        child: _buildContent(state),
      ),
    );
  }

  /// 홈 화면 콘텐츠
  Widget _buildContent(HomeState state) {
    final homeData = state.homeData!;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // #1 일정 영역
          Padding(
            padding: AppSpacing.screenHorizontal,
            child: ScheduleCard(
              schedules: homeData.todaySchedules,
              currentDate: homeData.currentDate,
              onRegisterTap: () {
                // TODO: 일정 등록 화면으로 이동 (후속 작업)
              },
              onScheduleTap: (schedule) {
                // TODO: 일정 상세 화면으로 이동 (후속 작업)
              },
            ),
          ),
          const SizedBox(height: AppSpacing.lg),

          // #2 유통기한 알림
          Padding(
            padding: AppSpacing.screenHorizontal,
            child: ExpiryAlertCard(
              expiryAlert: homeData.expiryAlert,
              onTap: () {
                // TODO: 유통기한 관리 화면으로 이동 (후속 작업)
              },
            ),
          ),
          const SizedBox(height: AppSpacing.lg),

          // #3 공지 영역 (가로 스크롤)
          _buildSectionHeader('공지사항'),
          const SizedBox(height: AppSpacing.sm),
          NoticeCarousel(
            notices: homeData.notices,
            onNoticeTap: (notice) {
              // TODO: 공지 상세 화면으로 이동 (후속 작업)
            },
          ),
          const SizedBox(height: AppSpacing.xl),

          // #4 제품 검색 바
          Padding(
            padding: AppSpacing.screenHorizontal,
            child: ProductSearchBar(
              onTap: () {
                // TODO: 제품 검색 화면으로 이동 (후속 작업)
              },
            ),
          ),
          const SizedBox(height: AppSpacing.xl),

          // #5 빠른 메뉴
          Padding(
            padding: AppSpacing.screenHorizontal,
            child: QuickMenuGrid(
              onMenuTap: (item) {
                // TODO: 해당 기능 화면으로 이동 (후속 작업)
              },
            ),
          ),
          const SizedBox(height: AppSpacing.xxxl),
        ],
      ),
    );
  }

  /// 섹션 헤더
  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: AppSpacing.screenHorizontal,
      child: Text(
        title,
        style: AppTypography.headlineSmall,
      ),
    );
  }
}
