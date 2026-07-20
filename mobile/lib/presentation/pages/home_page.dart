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
import '../widgets/attendance/attendance_status_popup.dart';
import '../../domain/entities/attendance_status.dart';
import '../../domain/repositories/home_repository.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../../app_router.dart';
import '../providers/safety_check_provider.dart';

/// 홈 화면
///
/// 오늘 일정, 소비기한 알림, 공지사항, 제품 검색, 빠른 메뉴를 표시한다.
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

  /// 오늘의 등록 현황 팝업 (레거시 home.jsp `#popPlace3`)
  ///
  /// 레거시는 배지·팝업이 홈에서 이미 렌더된 동일한 `list` 를 공유한다
  /// (별도 AJAX 없음). 신규도 홈 응답의 `todaySchedules` 를 그대로 사용해야
  /// 배지 카운트(= todaySchedules 집계)와 팝업 목록이 어긋나지 않는다.
  /// `/attendance/status` 는 TMS 원본 기준이라 진열 확정마스터 기반인 홈 집계와
  /// 건수가 달라질 수 있으므로 여기서는 쓰지 않는다.
  void _showAttendanceStatus(HomeData homeData) {
    final statusList = homeData.todaySchedules
        .map(
          (schedule) => AttendanceStatus(
            scheduleId: schedule.displayWorkScheduleId ?? schedule.scheduleId,
            accountName: schedule.accountName ?? '(미지정)',
            workCategory: schedule.workCategory,
            status: schedule.isCommuteRegistered ? 'REGISTERED' : 'PENDING',
            secondWorkType: schedule.secondWorkType,
          ),
        )
        .toList();

    AttendanceStatusPopup.show(
      context,
      statusList: statusList,
      totalCount: homeData.attendanceSummary.totalCount,
      registeredCount: homeData.attendanceSummary.registeredCount,
      currentDate: homeData.currentDate,
    );
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
    //
    // pull-to-refresh(iOS bounce)로 콘텐츠가 위/아래로 밀릴 때
    // safe area 전체가 올바른 색으로 채워지도록, 스크롤 뒤에
    // 화면 전체를 덮는 정적 배경(위=노랑 헤더 / 아래=흰 그라데이션 끝)을 깐다.
    // 이 배경은 오버스크롤로 콘텐츠가 밀렸을 때만 드러난다.
    return Stack(
      children: [
        const Positioned.fill(
          child: DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  AppColors.legacyYellow,
                  AppColors.homeBgGradientEnd,
                ],
              ),
            ),
          ),
        ),
        RefreshIndicator(
          onRefresh: _onRefresh,
          color: AppColors.legacyNavy,
          backgroundColor: Colors.white,
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            child: _buildContent(state, userRole),
          ),
        ),
      ],
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
        // 카드는 노란 영역 위에 떠 있고, 카드 아래 작은 노란 strip에
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

              // #1 스케줄 카드
              // 출근/근태 대상(여사원/조장)만 노출. 지점장 등은 카드 자체를 숨김.
              if (homeData.attendanceApplicable)
                Padding(
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
                    // 레거시 home.jsp:567 배지 → 오늘의 등록 현황 팝업(#popPlace3)
                    onAttendanceStatusTap: () =>
                        throttledTap(() => _showAttendanceStatus(homeData)),
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

              // 카드 아래 노란 strip (프로필 아바타 상단이 걸치는 여백)
              const SizedBox(height: AppSpacing.homeCardProfileGap),
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
              // #2 프로필 / 소비기한 알림
              // 프로필 행(아바타+텍스트)을 통째로 끌어올려 세로 정렬을 유지한 채
              // ① 노란 영역에 겹친다. 키가 큰 아바타 상단만 노랑에 걸치고
              // 텍스트는 그라데이션(흰색) 위에 놓인다.
              Transform.translate(
                offset: const Offset(0, -AppSpacing.homeProfileRaise),
                child: Padding(
                  padding: horizontalGutter,
                  child: ExpiryAlertCard(
                    expiryAlert: homeData.expiryAlert,
                    // 조장(LEADER)·지점장(ADMIN)은 소비기한 임박제품 라인을 숨긴다.
                    showExpiryCount:
                        userRole != 'LEADER' && userRole != 'ADMIN',
                    onTap: () {
                      AppRouter.navigateTo(context, AppRouter.productExpiration);
                    },
                  ),
                ),
              ),
              // 끌어올림으로 생긴 시각적 간격 보정
              const SizedBox(height: AppSpacing.sm),

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
                  userRole: userRole,
                  onMenuTap: (item) {
                    throttledTap(() => _handleQuickMenuTap(item));
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

  /// AppBar 영역 (로고 + 햄버거 메뉴) - 레거시 CSS padding:14 0 0 20
  /// 높이 = 레거시 헤더(116) − 카드 겹침(55) = 카드 위에 보이는 노란 헤더 영역.
  Widget _buildAppBar() {
    return SizedBox(
      height: AppSpacing.homeHeaderHeight - AppSpacing.homeCardOverlap,
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
    // 조장(LEADER)만 안전점검 없이 바로 출근등록.
    // 레거시 home.jsp 는 조장만 safetyCheck 미세팅(바로 출근)이고, 지점장/부서장은
    // else 분기에서 safetyCheck 가 세팅되어 여사원과 동일하게 안전점검을 선행한다.
    if (userRole == 'LEADER') {
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
  void _handleQuickMenuTap(QuickMenuItem item) {
    if (item.label == '활동 등록') {
      ActivityRegistrationPopup.show(
        context,
        onMenuTap: _handleActivityMenuTap,
      );
    } else if (item.label == '행사매출\n등록') {
      // 행사매출 등록 항목은 여사원(USER)에게만 노출된다(조장·지점장은 그리드에서 제외).
      AppRouter.navigateTo(context, AppRouter.promotionDailySalesEntry);
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
      AppRouter.navigateTo(context, item.route!, arguments: item.arguments);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${item.label} 기능은 준비 중입니다')),
      );
    }
  }

}
