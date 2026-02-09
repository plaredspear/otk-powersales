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
      backgroundColor: AppColors.background,
      body: _buildBody(state),
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

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // #0 노란 배경 헤더 (AppBar + 빨간 라인 + 노란 확장)
        _buildYellowHeader(),

        // #1 스케줄 카드 (노란 영역과 약 30% 중첩 → 음수 margin)
        Transform.translate(
          offset: const Offset(0, -40),
          child: Padding(
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
        ),

        // 중첩 offset 만큼 간격 보정
        const SizedBox(height: 0),

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
    );
  }

  /// 노란 배경 헤더 영역 (SafeArea + AppBar + 빨간 라인 + 확장)
  Widget _buildYellowHeader() {
    final topPadding = MediaQuery.of(context).padding.top;

    return Container(
      color: AppColors.otokiYellow,
      child: Column(
        children: [
          // SafeArea 상단 여백
          SizedBox(height: topPadding),
          // AppBar 영역
          SizedBox(
            height: AppSpacing.appBarHeight,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  RichText(
                    text: TextSpan(
                      children: [
                        TextSpan(
                          text: '오뚜기 ',
                          style: AppTypography.headlineLarge.copyWith(
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                            color: AppColors.textPrimary,
                          ),
                        ),
                        TextSpan(
                          text: '파워세일즈',
                          style: AppTypography.headlineLarge.copyWith(
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                            color: AppColors.otokiRed,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Spacer(),
                  IconButton(
                    icon: const Icon(Icons.menu, color: AppColors.textPrimary),
                    onPressed: () {
                      // TODO: 전체 메뉴 Drawer 열기 (후속 작업)
                    },
                    tooltip: '전체 메뉴',
                  ),
                ],
              ),
            ),
          ),
          // 노란 확장 영역 (스케줄 카드가 위에서 겹치는 영역)
          const SizedBox(height: 24),
        ],
      ),
    );
  }
}
