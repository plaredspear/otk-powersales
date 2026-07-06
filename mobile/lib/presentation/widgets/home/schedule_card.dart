import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/attendance_summary.dart';
import '../../../domain/entities/schedule.dart';

/// 일정 카드 위젯
///
/// 홈 화면의 #1 영역: 오늘 일정을 표시한다.
/// - 일정 없음: "오늘 등록된 스케줄이 없습니다." + 비활성 "등록" 버튼
/// - 출근 카운트 배지: "X/N" 형태로 출근 현황 표시
///
/// 일정 표시는 근무형태(workType = 고정/순회/격고)에 따라 레거시 home.jsp와
/// 동일하게 분기한다:
/// - 고정근무자 (home.jsp:546): 출근등록 여부와 무관하게 모든 일정을
///   "거래처명 (근무구분/상시임시/근무형태)" 형식으로 항상 표시.
/// - 순회/격고근무자 (home.jsp:558):
///   · 출근 전(registeredCount == 0) → 일정을 숨기고 "출근 후 등록을 누르세요."
///   · 출근 후 → 첫 일정(list[0])만 표시 (home.jsp:575)
///
/// 조장(LEADER) 뷰 (레거시 home.jsp 정합):
/// - 날짜 → "N명 중, M명 등록 완료" → 풀폭 "일정 관리" navy 버튼
/// - 팀원 목록/등록 버튼 미표시 (팀원 상세는 "일정 관리" 진입 후 페이지에서 확인)
/// - 레거시 home.jsp:509 근태영역 조건이 `eq '조장'` 정확 일치이므로 지점장(ADMIN)·
///   부서장(USER)은 조장뷰가 아닌 본인 일정 뷰로 떨어진다.
class ScheduleCard extends StatelessWidget {
  /// 오늘 일정 목록
  final List<Schedule> schedules;

  /// 현재 날짜 문자열 (예: '2026-02-07')
  final String currentDate;

  /// 출근 현황 집계
  final AttendanceSummary attendanceSummary;

  /// 사용자 역할 ("USER", "LEADER", "ADMIN")
  final String userRole;

  /// "등록" 버튼 탭 콜백
  final VoidCallback? onRegisterTap;

  /// 일정 아이템 탭 콜백
  final void Function(Schedule schedule)? onScheduleTap;

  /// 조장/지점장 뷰 "일정 관리" 버튼 탭 콜백
  ///
  /// 레거시 home.jsp 근태보고 영역의 조장 전용 "일정 관리" 버튼
  /// (`${ctx}/employee/mgnSchedule`)에 대응한다.
  final VoidCallback? onScheduleManageTap;

  const ScheduleCard({
    super.key,
    required this.schedules,
    required this.currentDate,
    required this.attendanceSummary,
    this.userRole = 'USER',
    this.onRegisterTap,
    this.onScheduleTap,
    this.onScheduleManageTap,
  });

  /// 조장 뷰 여부 (레거시 `eq '조장'` 정확 일치 — 지점장/부서장 제외)
  bool get _isLeaderView => userRole == 'LEADER';

  /// 순회/격고 근무자 여부 (레거시 home.jsp:558 `workingType eq '순회' || '격고'`).
  ///
  /// 한 사람의 오늘 일정은 모두 동일한 근무형태(workType)이므로 첫 일정 기준으로
  /// 판정한다. 순회/격고는 출근 전 일정을 숨기고, 출근 후에도 첫 일정만 노출한다.
  bool get _isRotationWorker {
    if (schedules.isEmpty) return false;
    final type = schedules.first.workType;
    return type == '순회' || type == '격고';
  }

  @override
  Widget build(BuildContext context) {
    final totalCount = attendanceSummary.totalCount;
    final registeredCount = attendanceSummary.registeredCount;

    return Container(
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
        boxShadow: AppSpacing.cardShadow,
      ),
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.homeGutter),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 날짜 헤더 + 링크 + 출근 카운트 배지
            Row(
              children: [
                Expanded(
                  child: Text(
                    _formatDate(currentDate),
                    style: AppTypography.legacyTitleXXL,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                if (!_isLeaderView && totalCount > 0) ...[
                  const SizedBox(width: AppSpacing.sm),
                  _buildAttendanceBadge(registeredCount, totalCount),
                ],
              ],
            ),

            // 조장 뷰: 팀 출근 현황 텍스트 + 풀폭 "일정 관리" 버튼
            // (레거시 home.jsp 정합 — 날짜/현황 아래 navy 버튼, 팀원 목록은 '일정 관리' 페이지에 있음)
            if (_isLeaderView) ...[
              const SizedBox(height: AppSpacing.sm),
              Text(
                '$totalCount명 중, $registeredCount명 등록 완료',
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              _buildScheduleManageButton(),
            ] else ...[
              const SizedBox(height: AppSpacing.md),

              // 본문 영역 (레거시 home.jsp 근무형태 분기 정합)
              if (totalCount == 0)
                // 휴무
                _buildEmptySchedule()
              else if (_isRotationWorker && registeredCount == 0)
                // 순회/격고 · 출근 전: 일정 숨김 (home.jsp:570)
                _buildUnregisteredMessage()
              else if (_isRotationWorker)
                // 순회/격고 · 출근 후: 첫 일정만 표시 (home.jsp:575)
                _buildScheduleList(firstOnly: true)
              else
                // 고정: 출근 여부와 무관하게 전체 일정 표시 (home.jsp:546)
                _buildScheduleList(),

              // 등록 버튼 (일반 사원만) - 레거시 navy/slate/disabled
              const SizedBox(height: AppSpacing.md),
              _buildRegisterButton(totalCount, registeredCount),
            ],
            const SizedBox(height: AppSpacing.sm),
          ],
        ),
      ),
    );
  }

  /// 조장/지점장 "일정 관리" 버튼 (레거시 home.jsp 풀폭 navy 버튼 대응)
  Widget _buildScheduleManageButton() {
    return SizedBox(
      width: double.infinity,
      height: AppSpacing.buttonHeight, // 44
      child: Material(
        color: AppColors.legacyNavy,
        borderRadius: BorderRadius.circular(AppSpacing.homeButtonRadius),
        child: InkWell(
          borderRadius: BorderRadius.circular(AppSpacing.homeButtonRadius),
          onTap: onScheduleManageTap,
          child: Center(
            child: Text(
              '일정 관리',
              style: AppTypography.legacyButton.copyWith(
                color: AppColors.white,
              ),
            ),
          ),
        ),
      ),
    );
  }

  /// 출근 카운트 배지 (레거시 pill: 78×28, radius 50)
  Widget _buildAttendanceBadge(int registeredCount, int totalCount) {
    return Container(
      width: 78,
      height: 28,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: const Color(0x05000000), // rgba(0,0,0,0.02)
        border: Border.all(color: const Color(0x1A000000)), // rgba(0,0,0,0.1)
        borderRadius: BorderRadius.circular(AppSpacing.homePillRadius),
      ),
      child: Text(
        '$registeredCount/$totalCount',
        style: AppTypography.legacyBody,
      ),
    );
  }

  /// 등록 버튼 (레거시 height 44, radius 8, navy/slate/disabled)
  Widget _buildRegisterButton(int totalCount, int registeredCount) {
    final isComplete = totalCount > 0 && registeredCount == totalCount;
    final isDisabled = totalCount == 0;
    final enabled = !isDisabled && !isComplete;

    final Color background;
    final Color textColor;
    if (isDisabled) {
      // 휴무/일정 없음
      background = AppColors.surface; // #F7F7F7
      textColor = AppColors.legacyPlaceholder;
    } else if (isComplete) {
      background = AppColors.legacySlate;
      textColor = AppColors.white;
    } else {
      background = AppColors.legacyNavy;
      textColor = AppColors.white;
    }

    return SizedBox(
      width: double.infinity,
      height: AppSpacing.buttonHeight, // 44
      child: Material(
        color: background,
        borderRadius: BorderRadius.circular(AppSpacing.homeButtonRadius),
        child: InkWell(
          borderRadius: BorderRadius.circular(AppSpacing.homeButtonRadius),
          onTap: enabled ? onRegisterTap : null,
          child: Center(
            child: Text(
              _buttonText(totalCount, registeredCount),
              style: AppTypography.legacyButton.copyWith(color: textColor),
            ),
          ),
        ),
      ),
    );
  }

  /// 일정 없음 UI
  Widget _buildEmptySchedule() {
    return Column(
      children: [
        const SizedBox(height: AppSpacing.xs),
        Text(
          '오늘 등록된 스케줄이 없습니다.',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      ],
    );
  }

  /// 순회/격고 근무자의 출근 전 안내 (레거시 home.jsp:571)
  Widget _buildUnregisteredMessage() {
    return Column(
      children: [
        const SizedBox(height: AppSpacing.xs),
        Text(
          '출근 후 등록을 누르세요.',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      ],
    );
  }

  /// 일정 목록 UI
  ///
  /// [firstOnly] 가 true 이면 첫 일정만 표시한다(순회/격고 출근 후, home.jsp:575).
  Widget _buildScheduleList({bool firstOnly = false}) {
    final items = firstOnly ? schedules.take(1) : schedules;
    return Column(
      children: items.map((schedule) {
        return _buildScheduleItem(schedule);
      }).toList(),
    );
  }

  /// 일정 아이템 UI
  ///
  /// 레거시 home.jsp:549 `${name} (${wc1}/${wc2}/${wc3})` 정합:
  /// "거래처명 (근무구분/상시임시/근무형태)" 텍스트 + 우측 출근 상태.
  Widget _buildScheduleItem(Schedule schedule) {
    return InkWell(
      onTap: onScheduleTap != null ? () => onScheduleTap!(schedule) : null,
      borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          vertical: AppSpacing.sm,
        ),
        child: Row(
          children: [
            // 거래처명 (근무구분/상시임시/근무형태)
            Expanded(
              child: Text(
                _formatScheduleLabel(schedule),
                style: AppTypography.bodyMedium,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: AppSpacing.sm),

            // 출근 상태
            Text(
              schedule.isCommuteRegistered ? '출근완료' : '미등록',
              style: AppTypography.labelMedium.copyWith(
                color: schedule.isCommuteRegistered
                    ? AppColors.success
                    : AppColors.textSecondary,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 일정 라벨을 "거래처명 (근무구분/상시임시/근무형태)" 형식으로 조합한다.
  ///
  /// 레거시 home.jsp:549 `${name} (${wc1}/${wc2}/${wc3})` 정합.
  /// - wc1 workCategory(진열/행사)
  /// - wc2 workCategory2(진열=상시/임시, 행사=전담/진열겸임)
  /// - wc3 workType(고정/순회/격고)
  /// 레거시처럼 세 슬롯을 항상 노출하며, 빈 슬롯은 `-` 로 표시한다.
  /// (예: "가락 알파마트(주) (진열/상시/고정)", "가락 알파마트(주) (진열/-/고정)")
  String _formatScheduleLabel(Schedule schedule) {
    final name = schedule.accountName ?? '(미지정)';
    String slot(String? value) =>
        (value != null && value.isNotEmpty) ? value : '-';
    final tokens = [
      slot(schedule.workCategory),
      slot(schedule.workCategory2),
      slot(schedule.workType),
    ];
    return '$name (${tokens.join('/')})';
  }

  /// 버튼 텍스트 결정
  String _buttonText(int totalCount, int registeredCount) {
    if (totalCount == 0 || registeredCount == 0) return '등록';
    if (registeredCount == totalCount) return '등록 완료';
    return '다음 등록';
  }

  /// 날짜 문자열을 "MM월 dd일 (요일)" 형식으로 변환
  String _formatDate(String dateStr) {
    try {
      final date = DateTime.parse(dateStr);
      const weekdays = ['월', '화', '수', '목', '금', '토', '일'];
      final weekday = weekdays[date.weekday - 1];
      final month = date.month.toString().padLeft(2, '0');
      final day = date.day.toString().padLeft(2, '0');
      return '$month월 $day일 ($weekday)';
    } catch (_) {
      return dateStr;
    }
  }
}
