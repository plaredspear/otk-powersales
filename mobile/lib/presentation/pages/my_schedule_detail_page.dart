import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/daily_schedule_info.dart';
import '../../domain/entities/schedule_account_detail.dart';
import '../providers/my_schedule_provider.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/my_schedule/schedule_account_item.dart';

/// 마이페이지 일정 상세 화면
///
/// 선택한 날짜의 일정 정보를 표시하며, "일정" 및 "등록" 두 탭으로 구성됩니다.
class MyScheduleDetailPage extends ConsumerStatefulWidget {
  final DateTime selectedDate;

  const MyScheduleDetailPage({
    super.key,
    required this.selectedDate,
  });

  @override
  ConsumerState<MyScheduleDetailPage> createState() =>
      _MyScheduleDetailPageState();
}

class _MyScheduleDetailPageState extends ConsumerState<MyScheduleDetailPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);

    // 선택한 날짜의 일정 상세 데이터 로드
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(myScheduleDetailProvider.notifier)
          .loadDailySchedule(widget.selectedDate);
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(myScheduleDetailProvider);

    // 에러 리스닝
    ref.listen(myScheduleDetailProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
      }
    });

    final workingType = state.scheduleInfo?.workingType;
    final isDayOff = workingType == '대휴' || workingType == '연차';

    return Scaffold(
      appBar: AppBar(
        title: Text(state.scheduleInfo?.memberName ?? '일정 상세'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
        bottom: isDayOff
            ? null
            : TabBar(
                controller: _tabController,
                tabs: const [
                  Tab(text: '일정'),
                  Tab(text: '등록'),
                ],
                // 레거시 .tab_menu: 활성/비활성 모두 검정 글씨, 활성은 파란 밑줄만 구분
                labelColor: AppColors.black,
                unselectedLabelColor: AppColors.black,
                labelStyle: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
                unselectedLabelStyle: const TextStyle(fontSize: 16),
                indicatorColor: AppColors.otokiBlue,
                indicatorWeight: AppSpacing.tabIndicatorWeight,
              ),
      ),
      body: state.isLoading
          ? const Center(child: LoadingIndicator())
          : state.errorMessage != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        state.errorMessage!,
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.error,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: AppSpacing.md),
                      ElevatedButton(
                        onPressed: () {
                          ref
                              .read(myScheduleDetailProvider.notifier)
                              .loadDailySchedule(widget.selectedDate);
                        },
                        child: const Text('다시 시도'),
                      ),
                    ],
                  ),
                )
              : isDayOff
                  ? _buildDayOffScreen(state)
                  : TabBarView(
                      controller: _tabController,
                      children: [
                        _buildScheduleTab(state),
                        _buildRegistrationTab(state),
                      ],
                    ),
    );
  }

  /// 대휴/연차 안내 화면
  Widget _buildDayOffScreen(state) {
    final info = state.scheduleInfo;
    if (info == null) {
      return const Center(child: Text('일정 정보가 없습니다'));
    }

    final isSubstituteHoliday = info.workingType == '대휴';
    final icon = isSubstituteHoliday ? Icons.beach_access : Icons.event_busy;
    final color =
        isSubstituteHoliday ? AppColors.otokiBlue : AppColors.secondary;
    final message =
        isSubstituteHoliday ? '대휴가 예정된 날입니다' : '연차가 예정된 날입니다';

    return Column(
      children: [
        // 날짜 헤더 (일정/등록 탭과 동일한 레거시 18px/800 정합)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 22),
          child: Align(
            alignment: Alignment.centerLeft,
            child: Text(
              info.date,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w800,
                color: AppColors.black,
                height: 1.1,
              ),
            ),
          ),
        ),
        // 아이콘 + 메시지
        Expanded(
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(icon, size: 64, color: color),
                const SizedBox(height: AppSpacing.md),
                Text(
                  message,
                  style: AppTypography.bodyLarge.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  /// 날짜 헤더 + 보고완료 카운터 (레거시 myDaily.jsp .schedule_top 정합)
  Widget _buildDateHeader(DailyScheduleInfo info) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // 날짜 (레거시 .date_wrap h2: 18px / weight 800)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 22),
          child: Text(
            info.date,
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: AppColors.black,
              height: 1.1,
            ),
          ),
        ),
        // 보고완료 카운터 (레거시 .text_info: bg #F7F7F7, 중앙, 완료수=빨강)
        Container(
          width: double.infinity,
          height: 50,
          alignment: Alignment.center,
          color: AppColors.legacyCounterBg,
          child: Text.rich(
            TextSpan(
              style: const TextStyle(
                fontSize: 16,
                color: AppColors.black,
                height: 1.2,
              ),
              children: [
                TextSpan(
                  text: '${info.reportProgress.completed}',
                  style: const TextStyle(color: AppColors.legacyDanger),
                ),
                TextSpan(
                  text:
                      ' / ${info.reportProgress.total} 보고 완료 (${info.reportProgress.workType})',
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  /// 부차 항목(레거시 미노출 일정) 안내 다이얼로그
  void _showSecondaryScheduleInfo() {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('참고 일정'),
        content: const Text(
          '진열과 행사가 같은 날에 겹칠 때, 기존 시스템은 둘 중 한쪽만 보여주고 '
          '나머지는 화면에서 생략했습니다.\n\n'
          '방문할 매장을 놓치지 않도록 생략되던 일정도 함께 표시하되, '
          '위쪽 목록과 구분해 여기에 모아 둡니다. 위의 보고 완료 건수에는 '
          '포함되지 않습니다.\n\n'
          '참고 일정도 동일하게 등록할 수 있으며, 출근을 등록하면 위쪽 목록으로 올라갑니다.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  /// 부차 항목 그룹 헤더 (구분선 + 라벨 + info 아이콘)
  Widget _buildSecondaryGroupHeader() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.only(
        left: 20,
        right: 12,
        top: AppSpacing.md,
        bottom: AppSpacing.xs,
      ),
      decoration: const BoxDecoration(
        // 주 일정과 명확히 분리하는 상단 구분선
        border: Border(
          top: BorderSide(color: AppColors.divider),
        ),
      ),
      child: Row(
        children: [
          Text(
            '참고 일정',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
              fontWeight: FontWeight.w600,
            ),
          ),
          IconButton(
            onPressed: _showSecondaryScheduleInfo,
            icon: const Icon(Icons.info_outline),
            iconSize: 18,
            color: AppColors.textSecondary,
            visualDensity: VisualDensity.compact,
            padding: const EdgeInsets.all(4),
            constraints: const BoxConstraints(),
            tooltip: '참고 일정 안내',
          ),
        ],
      ),
    );
  }

  /// 주 일정 → (구분선 + 참고 일정 헤더) → 부차 항목 순으로 목록을 구성한다.
  ///
  /// 백엔드가 isLegacyVisible 내림차순으로 정렬해 내려주므로 순서를 그대로 쓰되,
  /// 경계에 헤더를 삽입한다.
  Widget _buildAccountList(
    List<ScheduleAccountDetail> accounts, {
    required bool showRegistrationStatus,
  }) {
    final primary = accounts.where((a) => a.isLegacyVisible).toList();
    final secondary = accounts.where((a) => !a.isLegacyVisible).toList();

    // 헤더 1개를 부차 항목 앞에 끼워 넣는다 (부차 항목이 없으면 헤더도 없음)
    final itemCount =
        primary.length + (secondary.isEmpty ? 0 : 1 + secondary.length);

    return ListView.builder(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
      itemCount: itemCount,
      itemBuilder: (context, index) {
        if (index < primary.length) {
          return _buildAccountRow(
            primary[index],
            showRegistrationStatus: showRegistrationStatus,
            isSecondary: false,
          );
        }
        if (index == primary.length) {
          return _buildSecondaryGroupHeader();
        }
        final account = secondary[index - primary.length - 1];
        return _buildAccountRow(
          account,
          showRegistrationStatus: showRegistrationStatus,
          isSecondary: true,
        );
      },
    );
  }

  Widget _buildAccountRow(
    ScheduleAccountDetail account, {
    required bool showRegistrationStatus,
    required bool isSecondary,
  }) {
    return ScheduleAccountItem(
      accountName: account.accountName,
      workType1: account.workType1,
      workType2: account.workType2,
      workType3: account.workType3,
      isRegistered: showRegistrationStatus ? account.isRegistered : null,
      showRegistrationStatus: showRegistrationStatus,
      isSecondary: isSecondary,
    );
  }

  /// 일정 탭 콘텐츠
  Widget _buildScheduleTab(state) {
    if (state.scheduleInfo == null) {
      return const Center(child: Text('일정 정보가 없습니다'));
    }

    final info = state.scheduleInfo!;

    return Column(
      children: [
        // 날짜 및 보고 진행 정보
        _buildDateHeader(info),

        // 조원명 헤더 (레거시 .board_list02 li p strong: 16px bold, 하단 구분선)
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
          decoration: const BoxDecoration(
            color: AppColors.background,
            border: Border(
              bottom: BorderSide(color: AppColors.divider),
            ),
          ),
          child: Text(
            '${info.memberName} (${info.employeeCode})',
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: AppColors.black,
            ),
          ),
        ),

        // 거래처 목록 (레거시: 행 사이 구분선 없이 6px 간격으로 나열)
        Expanded(
          child: _buildAccountList(
            info.accounts,
            showRegistrationStatus: false,
          ),
        ),
      ],
    );
  }

  /// 등록 탭 콘텐츠
  Widget _buildRegistrationTab(state) {
    if (state.scheduleInfo == null) {
      return const Center(child: Text('일정 정보가 없습니다'));
    }

    final info = state.scheduleInfo!;

    return Column(
      children: [
        // 날짜 및 보고 진행 정보
        _buildDateHeader(info),

        // 등록 전 필터
        Container(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.sm,
          ),
          decoration: BoxDecoration(
            border: Border(
              bottom: BorderSide(color: AppColors.divider),
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              Checkbox(
                value: state.showOnlyUnregistered,
                onChanged: (value) {
                  ref
                      .read(myScheduleDetailProvider.notifier)
                      .toggleUnregisteredFilter();
                },
                activeColor: AppColors.otokiBlue,
              ),
              Text(
                '등록 전',
                style: AppTypography.bodyMedium,
              ),
            ],
          ),
        ),

        // 조원명 배너 (레거시 .list_title: bg #7C91A7 slate, 흰 글씨 15px)
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
          color: AppColors.legacySlate,
          child: Text(
            '${info.memberName} (${info.reportProgress.workType})',
            style: const TextStyle(
              fontSize: 15,
              color: AppColors.white,
            ),
          ),
        ),

        // 거래처 목록 (필터 적용)
        Expanded(
          child: state.filteredAccounts.isEmpty
              ? Center(
                  child: Text(
                    state.showOnlyUnregistered
                        ? '등록 전 항목이 없습니다'
                        : '거래처가 없습니다',
                    style: AppTypography.bodyMedium.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                )
              // 참고 일정도 동일하게 등록할 수 있다 (구분만 두고 동작 제약 없음)
              : _buildAccountList(
                  state.filteredAccounts,
                  showRegistrationStatus: true,
                ),
        ),
      ],
    );
  }
}
