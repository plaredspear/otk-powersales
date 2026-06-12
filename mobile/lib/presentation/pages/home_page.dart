import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../providers/auth_provider.dart';
import '../providers/home_provider.dart';
import '../providers/home_state.dart';
import '../widgets/common/error_view.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/home/schedule_card.dart';
import '../widgets/home/expiry_alert_card.dart';
import '../widgets/home/notice_carousel.dart';
import '../widgets/home/product_search_bar.dart';
import '../widgets/home/quick_menu_grid.dart';
import '../widgets/home/activity_registration_popup.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../../app_router.dart';
import '../providers/safety_check_provider.dart';

/// 홈 화면
///
/// 오늘 일정, 유통기한 알림, 공지사항, 제품 검색, 빠른 메뉴를 표시한다.
/// Pull-to-refresh로 전체 데이터를 새로고침할 수 있다.
class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage>
    with ThrottledTapMixin {
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
    final authState = ref.watch(authProvider);
    final userRole = authState.user?.role ?? 'USER';

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarBrightness: Brightness.light,
        statusBarIconBrightness: Brightness.dark,
        systemNavigationBarColor: AppColors.homeBgGradientEnd,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
      child: Container(
        color: AppColors.legacyYellow,
        child: _buildBody(state, userRole),
      ),
    );
  }

  /// 본문 영역
  Widget _buildBody(HomeState state, String userRole) {
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

    // 데이터 있음 → 배경+콘텐츠가 함께 스크롤
    return RefreshIndicator(
      onRefresh: _onRefresh,
      color: AppColors.legacyNavy,
      backgroundColor: Colors.white,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        child: _buildContent(state, userRole),
      ),
    );
  }

  /// 홈 화면 콘텐츠
  ///
  /// 두 블록으로 분리해 관리한다(겹치지 않음):
  ///  ① 헤더 + 카드 영역  → 노란 배경(레거시 #header.main_header)
  ///  ②③ 프로필 + 본문 영역 → 회색→흰 그라데이션(레거시 .main_bg)
  /// 프로필 아바타만 ① 노란 영역 위로 끌어올려 상단을 겹치고, 텍스트는 흰색 위에 둔다.
  Widget _buildContent(HomeState state, String userRole) {
    final homeData = state.homeData!;
    final mediaPadding = MediaQuery.of(context).padding;
    final topPadding = mediaPadding.top;
    final bottomPadding = mediaPadding.bottom;
    const horizontalGutter = EdgeInsets.symmetric(
      horizontal: AppSpacing.homeGutter,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // ── ① 헤더 + 카드 영역 (노란 배경) ──
        // 카드는 -55 로 헤더를 겹쳐 솟고, 카드 아래 남는 노란 영역에
        // 프로필 아바타 상단이 걸친다(카드와 프로필은 겹치지 않음).
        ColoredBox(
          color: AppColors.legacyYellow,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // SafeArea 상단 여백 (status bar/노치 회피)
              SizedBox(height: topPadding),

              // AppBar 영역 (로고 + 햄버거 메뉴)
              _buildAppBar(),

              // #1 스케줄 카드 (Transform.translate -55 로 노란 헤더 겹침)
              // 출근/근태 대상(여사원/조장)만 노출. 지점장 등은 카드 자체를 숨김.
              if (homeData.attendanceApplicable)
                Transform.translate(
                  offset: const Offset(0, -AppSpacing.homeCardOverlap),
                  child: Padding(
                    padding: horizontalGutter,
                    child: ScheduleCard(
                      schedules: homeData.todaySchedules,
                      currentDate: homeData.currentDate,
                      attendanceSummary: homeData.attendanceSummary,
                      userRole: userRole,
                      onRegisterTap: () => throttledTapAsync(() async {
                        await _handleRegisterTap(userRole);
                        if (mounted) {
                          ref.read(homeProvider.notifier).refresh();
                        }
                      }),
                      onScheduleTap: (schedule) {
                        final date =
                            DateTime.tryParse(homeData.currentDate) ??
                                DateTime.now();
                        AppRouter.navigateTo(
                          context,
                          AppRouter.myScheduleDetail,
                          arguments: date,
                        );
                      },
                      // 조장/지점장: 레거시 home.jsp "일정 관리" → /employee/mgnSchedule
                      // 여사원 전체 모드의 월간 일정 캘린더(LeaderTeamMemberScheduleScreen)로 연결.
                      onScheduleManageTap: () {
                        AppRouter.navigateTo(
                          context,
                          AppRouter.leaderSchedule,
                        );
                      },
                    ),
                  ),
                ),
            ],
          ),
        ),

        // ── ②③ 프로필 + 본문 영역 (회색→흰 그라데이션) ──
        // 레거시 .main_bg:after linear-gradient(#f7f7f7 → #ffffff)
        DecoratedBox(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [
                AppColors.homeBgGradientStart,
                AppColors.homeBgGradientEnd,
              ],
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // #2 프로필 / 유통기한 알림
              // 아바타만 위로 끌어올려 ① 노란 영역에 상단을 겹치고,
              // 지점·이름·유통기한 텍스트는 그라데이션(흰색) 위에 둔다.
              Padding(
                padding: horizontalGutter,
                child: ExpiryAlertCard(
                  expiryAlert: homeData.expiryAlert,
                  avatarTopOverlap: AppSpacing.homeProfileAvatarRaise,
                  onTap: () {
                    AppRouter.navigateTo(context, AppRouter.productExpiration);
                  },
                ),
              ),
              const SizedBox(height: AppSpacing.lg),

              // #3 공지 영역 (가로 스크롤)
              NoticeCarousel(
                notices: homeData.notices,
                onNoticeTap: (notice) {
                  AppRouter.navigateTo(
                    context,
                    AppRouter.noticeDetail,
                    arguments: notice.id,
                  );
                },
                onViewAllTap: () {
                  AppRouter.navigateTo(context, AppRouter.notices);
                },
              ),
              const SizedBox(height: AppSpacing.xl),

              // #4 제품 검색 바
              Padding(
                padding: horizontalGutter,
                child: ProductSearchBar(
                  onTap: () {
                    AppRouter.navigateTo(context, AppRouter.productSearch);
                  },
                ),
              ),
              // 레거시(common.css): 검색창 ~ 메뉴 그리드 간격 20px
              const SizedBox(height: AppSpacing.xl),

              // #5 빠른 메뉴
              Padding(
                padding: horizontalGutter,
                child: QuickMenuGrid(
                  onMenuTap: (item) {
                    throttledTap(() => _handleQuickMenuTap(item, userRole));
                  },
                ),
              ),

              // 마지막 여백: home indicator/네비바 회피
              SizedBox(height: AppSpacing.xxxl + bottomPadding),
            ],
          ),
        ),
      ],
    );
  }

  /// AppBar 영역 (로고 + 햄버거 메뉴) - 레거시 CSS height:116, padding:14 0 0 20
  Widget _buildAppBar() {
    return SizedBox(
      height: AppSpacing.homeHeaderHeight,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(
          AppSpacing.homeGutter,
          14,
          AppSpacing.homeGutter,
          0,
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Image.asset(
              'assets/images/h1_logo.png',
              height: 28,
              fit: BoxFit.contain,
            ),
            const Spacer(),
            // 햄버거 메뉴 - 레거시 ico_nav.png 16dp
            Semantics(
              button: true,
              label: '전체 메뉴',
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => Scaffold.of(context).openEndDrawer(),
                child: SizedBox(
                  width: 24,
                  height: 24,
                  child: Center(
                    child: Image.asset(
                      'assets/images/ico_nav.png',
                      width: 16,
                      height: 16,
                      fit: BoxFit.contain,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 등록 버튼 탭 핸들러: 조장이면 바로 출근등록, 그 외 안전점검 상태 확인 후 분기
  Future<void> _handleRegisterTap(String userRole) async {
    // 조장(LEADER)/지점장(ADMIN)은 안전점검 없이 바로 출근등록
    if (userRole == 'LEADER' || userRole == 'ADMIN') {
      await AppRouter.navigateTo(context, AppRouter.attendance);
      return;
    }

    // 안전점검 상태 확인
    try {
      final todayStatus = await ref
          .read(getSafetyCheckTodayStatusUseCaseProvider)
          .call();

      if (!mounted) return;

      if (todayStatus.completed) {
        await AppRouter.navigateTo(context, AppRouter.attendance);
      } else {
        await AppRouter.navigateTo(context, AppRouter.safetyCheck);
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('안전점검 상태를 확인할 수 없습니다: $e'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  /// 빠른 메뉴 탭 핸들러
  void _handleQuickMenuTap(QuickMenuItem item, String userRole) {
    final isLeaderOrAdmin = userRole == 'LEADER' || userRole == 'ADMIN';

    if (item.label == '활동 등록') {
      ActivityRegistrationPopup.show(
        context,
        onMenuTap: _handleActivityMenuTap,
      );
    } else if (item.label == '행사매출\n등록') {
      if (isLeaderOrAdmin) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('행사 담당자만 행사매출을 등록할 수 있습니다.'),
            duration: Duration(seconds: 3),
          ),
        );
      } else {
        AppRouter.navigateTo(context, AppRouter.promotionDailySalesEntry);
      }
    } else if (item.route != null) {
      AppRouter.navigateTo(context, item.route!);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('준비 중입니다')),
      );
    }
  }

  /// 활동등록 팝업 메뉴 탭 핸들러
  void _handleActivityMenuTap(ActivityMenuItem item) {
    if (item.route != null) {
      AppRouter.navigateTo(context, item.route!);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${item.label} 기능은 준비 중입니다')),
      );
    }
  }

}
