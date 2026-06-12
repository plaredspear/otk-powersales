import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../../domain/entities/promotion.dart';
import '../providers/promotion_detail_provider.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/promotion/promotion_amount_text.dart';
import '../widgets/promotion/promotion_employee_list.dart';

/// 행사 상세 페이지.
///
/// 레거시 Heroku `promotion/event/view.jsp` 정합 — 상단 헤더(`[행사유형] 행사명` + 기간)
/// 아래 "매출" / "행사 정보" 두 탭 구성.
/// - 매출 탭: 기간 경과율(날짜 경과율) + 목표·달성 금액(달성률) + 내 일별 매출.
/// - 행사 정보 탭: 행사번호~기타상품 기본 정보 + 배정 조원 목록(모바일 확장).
class PromotionDetailPage extends ConsumerStatefulWidget {
  final int promotionId;

  const PromotionDetailPage({super.key, required this.promotionId});

  @override
  ConsumerState<PromotionDetailPage> createState() =>
      _PromotionDetailPageState();
}

class _PromotionDetailPageState extends ConsumerState<PromotionDetailPage>
    with ThrottledTapMixin, SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(promotionDetailProvider.notifier)
          .loadPromotion(widget.promotionId);
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(promotionDetailProvider);

    ref.listen<String?>(
      promotionDetailProvider.select((s) => s.errorMessage),
      (prev, next) {
        if (next != null) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(next)));
          ref.read(promotionDetailProvider.notifier).clearError();
          if (next.contains('권한')) {
            Navigator.of(context).pop();
          }
        }
      },
    );

    return Scaffold(
      appBar: AppBar(title: const Text('행사 상세')),
      body: _buildBody(state),
    );
  }

  Future<void> _openDailySales(PromotionEmployee employee) async {
    final result = await AppRouter.navigateTo<bool>(
      context,
      AppRouter.promotionDailySales,
      arguments: employee.id,
    );
    if (result == true && mounted) {
      ref
          .read(promotionDetailProvider.notifier)
          .loadPromotion(widget.promotionId);
    }
  }

  Widget _buildBody(PromotionDetailState state) {
    if (state.isLoading && state.detail == null) {
      return const LoadingIndicator(message: '행사 정보를 불러오는 중...');
    }

    if (state.detail == null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.grey[300]),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '행사 정보를 불러올 수 없습니다',
              style: AppTypography.bodyMedium
                  .copyWith(color: AppColors.textSecondary),
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () => ref
                  .read(promotionDetailProvider.notifier)
                  .loadPromotion(widget.promotionId),
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    final detail = state.detail!;
    return Column(
      children: [
        _buildHeaderCard(detail),
        _buildTabBar(),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [
              _buildSalesTab(detail),
              _buildInfoTab(detail),
            ],
          ),
        ),
      ],
    );
  }

  // ─── 헤더 + 탭 ──────────────────────────────────────────────

  Widget _buildHeaderCard(PromotionDetail detail) {
    return Container(
      width: double.infinity,
      color: AppColors.background,
      padding: AppSpacing.cardPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(_promotionTitle(detail), style: AppTypography.headlineSmall),
          const SizedBox(height: AppSpacing.xs),
          Text(
            '기간 ${detail.startDate} ~ ${detail.endDate}',
            style: AppTypography.bodySmall
                .copyWith(color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }

  /// `[행사유형] 행사명` (레거시 `['+PromotionType+'] '+PromotionName`).
  String _promotionTitle(PromotionDetail detail) {
    final type = detail.promotionType;
    final name = detail.promotionName ?? '';
    return (type != null && type.isNotEmpty) ? '[$type] $name' : name;
  }

  Widget _buildTabBar() {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.background,
        border: Border(bottom: BorderSide(color: AppColors.border)),
      ),
      child: TabBar(
        controller: _tabController,
        labelColor: AppColors.secondary,
        unselectedLabelColor: AppColors.textSecondary,
        indicatorColor: AppColors.secondary,
        indicatorWeight: AppSpacing.tabIndicatorWeight,
        labelStyle:
            AppTypography.labelLarge.copyWith(fontWeight: FontWeight.w700),
        unselectedLabelStyle: AppTypography.labelLarge,
        tabs: const [
          Tab(text: '매출'),
          Tab(text: '행사 정보'),
        ],
      ),
    );
  }

  // ─── 매출 탭 ────────────────────────────────────────────────

  Widget _buildSalesTab(PromotionDetail detail) {
    return SingleChildScrollView(
      padding: AppSpacing.screenAll,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildProgressSection(detail),
          const SizedBox(height: AppSpacing.xl),
          _buildTargetActualSection(detail),
          const SizedBox(height: AppSpacing.xl),
          _buildDailySalesSection(detail),
        ],
      ),
    );
  }

  /// 기간 경과율 = 날짜 경과율. 레거시 view.jsp: `round(100 - 남은일수/전체일수 * 100)`.
  /// 프로그레스 바 표시 안정성을 위해 0~100 범위로 클램프.
  Widget _buildProgressSection(PromotionDetail detail) {
    final percent = _progressPercent(detail);
    final remaining = _remainingLabel(detail);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionHeader(
          Icons.show_chart,
          '기간 경과율',
          trailing: remaining == null
              ? null
              : Text(
                  remaining,
                  style: AppTypography.bodyMedium
                      .copyWith(color: AppColors.textSecondary),
                ),
        ),
        const SizedBox(height: AppSpacing.md),
        ClipRRect(
          borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
          child: Stack(
            alignment: Alignment.center,
            children: [
              Container(height: 26, color: AppColors.surfaceVariant),
              Align(
                alignment: Alignment.centerLeft,
                child: FractionallySizedBox(
                  widthFactor: percent / 100,
                  child: Container(
                    height: 26,
                    color: AppColors.legacyYellow,
                  ),
                ),
              ),
              Text(
                '$percent%',
                style: AppTypography.labelMedium.copyWith(
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildTargetActualSection(PromotionDetail detail) {
    final rate = detail.achievementRate;
    final rateText = rate != null ? '${rate.round()}' : '0';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionHeader(Icons.adjust, '목표 & 달성 금액'),
        const SizedBox(height: AppSpacing.md),
        IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: _amountBox(
                  icon: Icons.adjust,
                  label: '목표 금액',
                  amount: detail.targetAmount,
                  highlight: false,
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
                child: Center(
                  widthFactor: 1,
                  child: Container(
                    width: 28,
                    height: 28,
                    decoration: const BoxDecoration(
                      color: AppColors.primary,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.arrow_forward,
                      size: 16,
                      color: AppColors.onPrimary,
                    ),
                  ),
                ),
              ),
              Expanded(
                child: _amountBox(
                  icon: Icons.trending_up,
                  label: '달성 금액',
                  amount: detail.actualAmount,
                  highlight: true,
                  subtitle: '($rateText% 달성)',
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _amountBox({
    required IconData icon,
    required String label,
    required int? amount,
    required bool highlight,
    String? subtitle,
  }) {
    final valueColor = highlight ? AppColors.success : AppColors.textPrimary;
    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: highlight ? AppColors.background : AppColors.surface,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(
          color: highlight ? AppColors.success : AppColors.border,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 14, color: AppColors.textSecondary),
              const SizedBox(width: AppSpacing.xs),
              Text(
                label,
                style: AppTypography.bodySmall
                    .copyWith(color: AppColors.textSecondary),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(
            PromotionAmountText.formatAmount(amount),
            textAlign: TextAlign.center,
            style: AppTypography.bodyMedium.copyWith(
              fontWeight: FontWeight.w700,
              color: valueColor,
            ),
          ),
          if (subtitle != null) ...[
            const SizedBox(height: AppSpacing.xxs),
            Text(
              subtitle,
              style: AppTypography.bodySmall.copyWith(color: AppColors.success),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildDailySalesSection(PromotionDetail detail) {
    final rows = _buildDailyRows(detail);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionHeader(Icons.calendar_month, '내 일별 매출'),
        const SizedBox(height: AppSpacing.md),
        Container(
          decoration: BoxDecoration(
            border: Border.all(color: AppColors.border),
            borderRadius: AppSpacing.cardBorderRadius,
          ),
          child: Column(
            children: [
              _dailyHeaderRow(),
              if (rows.isEmpty)
                Padding(
                  padding: AppSpacing.cardPadding,
                  child: Text(
                    '표시할 일별 매출이 없습니다',
                    style: AppTypography.bodySmall
                        .copyWith(color: AppColors.textTertiary),
                  ),
                )
              else
                ListView.separated(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: rows.length,
                  separatorBuilder: (_, _) =>
                      const Divider(height: 1, color: AppColors.border),
                  itemBuilder: (context, index) => _dailyRow(rows[index]),
                ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _dailyHeaderRow() {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(AppSpacing.radiusLg),
          topRight: Radius.circular(AppSpacing.radiusLg),
        ),
      ),
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              '일시',
              style: AppTypography.bodySmall
                  .copyWith(color: AppColors.textSecondary),
            ),
          ),
          Text(
            '판매금액',
            style: AppTypography.bodySmall
                .copyWith(color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }

  Widget _dailyRow(_DailySalesRow row) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              _formatDailyDate(row.date),
              style: AppTypography.bodyMedium,
            ),
          ),
          Text(
            _formatSalesAmount(row.amount),
            style: AppTypography.bodyMedium
                .copyWith(fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }

  // ─── 행사 정보 탭 ───────────────────────────────────────────

  Widget _buildInfoTab(PromotionDetail detail) {
    return SingleChildScrollView(
      padding: AppSpacing.screenAll,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (detail.isClosed) ...[
            _buildClosedBadge(),
            const SizedBox(height: AppSpacing.md),
          ],
          _buildInfoSection(detail),
          const SizedBox(height: AppSpacing.xl),
          _buildProductSection(detail),
          if (detail.employees.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.xl),
            PromotionEmployeeList(
              employees: detail.employees,
              onDailySalesTap: (emp) =>
                  throttledTapAsync(() => _openDailySales(emp)),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildClosedBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: AppColors.textTertiary.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        '마감',
        style: AppTypography.labelMedium.copyWith(
          color: AppColors.textSecondary,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }

  Widget _buildInfoSection(PromotionDetail detail) {
    return Column(
      children: [
        _infoRow('행사번호', detail.promotionNumber),
        if (detail.promotionType != null)
          _infoRow('행사유형', detail.promotionType!),
        if (detail.accountName != null)
          _infoRow('거래처', detail.accountName!),
        _infoRow('기간', '${detail.startDate} ~ ${detail.endDate}'),
        if (detail.standLocation != null)
          _infoRow('매대위치', detail.standLocation!),
      ],
    );
  }

  Widget _buildProductSection(PromotionDetail detail) {
    final hasProduct = detail.primaryProductName != null ||
        detail.otherProduct != null ||
        detail.category != null ||
        detail.message != null;
    if (!hasProduct) return const SizedBox.shrink();

    return Column(
      children: [
        if (detail.primaryProductName != null)
          _infoRow('대표상품', detail.primaryProductName!),
        if (detail.otherProduct != null)
          _infoRow('기타상품', detail.otherProduct!),
        if (detail.category != null) _infoRow('카테고리', detail.category!),
        if (detail.message != null) _infoRow('메시지', detail.message!),
      ],
    );
  }

  // ─── 공통 위젯 ──────────────────────────────────────────────

  Widget _sectionHeader(IconData icon, String title, {Widget? trailing}) {
    return Row(
      children: [
        Icon(icon, size: 18, color: AppColors.secondary),
        const SizedBox(width: AppSpacing.xs),
        Text(title, style: AppTypography.headlineSmall),
        if (trailing != null) ...[
          const Spacer(),
          trailing,
        ],
      ],
    );
  }

  Widget _infoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(
              label,
              style: AppTypography.bodyMedium
                  .copyWith(color: AppColors.textSecondary),
            ),
          ),
          Expanded(
            child: Text(value, style: AppTypography.bodyMedium),
          ),
        ],
      ),
    );
  }

  // ─── 계산 헬퍼 ──────────────────────────────────────────────

  /// 레거시 view.jsp 진행율: `round(100 - (남은일수/전체일수) * 100)`.
  /// `dateCompare` = `ceil(|a-b| / 1day)`. 클라이언트 현재 시각 기준.
  ///
  /// 레거시 공식은 `dateCompare`가 절댓값이라 기간 밖(종료 후/시작 전)에서
  /// 음수·100% 초과가 나온다. 기간이 끝난 행사는 경과율 100%가 의도이므로
  /// 경계를 먼저 처리한 뒤 기간 내에서만 비례 계산한다.
  int _progressPercent(PromotionDetail detail) {
    final start = DateTime.tryParse(detail.startDate);
    final end = DateTime.tryParse(detail.endDate);
    if (start == null || end == null) return 0;

    final now = DateTime.now();
    if (!now.isBefore(end)) return 100; // 종료일 도달/경과 → 100%
    if (now.isBefore(start)) return 0; // 시작 전 → 0%

    final totalDays = _ceilDays(start, end);
    if (totalDays == 0) return 0;
    final remainingDays = _ceilDays(now, end);
    final pct = 100 - (remainingDays / totalDays * 100);
    return pct.round().clamp(0, 100);
  }

  int _ceilDays(DateTime a, DateTime b) {
    final ms = a.difference(b).inMilliseconds.abs();
    return (ms / Duration.millisecondsPerDay).ceil();
  }

  /// 기간 경과율 헤더 우측 라벨. 경과율 바와 경계를 맞춰
  /// 종료(`now >= end`)/시작 전(`now < start`)/진행 중(남은 일수)을 구분.
  String? _remainingLabel(PromotionDetail detail) {
    final start = DateTime.tryParse(detail.startDate);
    final end = DateTime.tryParse(detail.endDate);
    if (start == null || end == null) return null;

    final now = DateTime.now();
    if (!now.isBefore(end)) return '종료';
    if (now.isBefore(start)) return '시작 전';

    final today = DateTime(now.year, now.month, now.day);
    final endDay = DateTime(end.year, end.month, end.day);
    final days = endDay.difference(today).inDays;
    return days <= 0 ? '오늘 종료' : '$days일 남음';
  }

  /// 행사 기간(start~end) 전체 날짜 행 구성. 각 날짜의 조원 실적 합계를 매핑
  /// (실적 미입력 날짜는 빈칸). 레거시 "내 일별 매출" 테이블 정합.
  List<_DailySalesRow> _buildDailyRows(PromotionDetail detail) {
    final start = DateTime.tryParse(detail.startDate);
    final end = DateTime.tryParse(detail.endDate);
    if (start == null || end == null || end.isBefore(start)) return [];

    final byDate = <String, int>{};
    for (final emp in detail.employees) {
      final date = emp.scheduleDate;
      final amount = emp.actualAmount;
      if (date == null || amount == null) continue;
      byDate[date] = (byDate[date] ?? 0) + amount;
    }

    final rows = <_DailySalesRow>[];
    var cur = DateTime(start.year, start.month, start.day);
    final last = DateTime(end.year, end.month, end.day);
    while (!cur.isAfter(last)) {
      rows.add(_DailySalesRow(date: cur, amount: byDate[_isoDate(cur)]));
      cur = cur.add(const Duration(days: 1));
    }
    return rows;
  }

  String _isoDate(DateTime d) {
    final mm = d.month.toString().padLeft(2, '0');
    final dd = d.day.toString().padLeft(2, '0');
    return '${d.year.toString().padLeft(4, '0')}-$mm-$dd';
  }

  /// `08.19 (수)` (레거시 moment `MM.DD` + 한글 요일).
  String _formatDailyDate(DateTime d) {
    const weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final mm = d.month.toString().padLeft(2, '0');
    final dd = d.day.toString().padLeft(2, '0');
    return '$mm.$dd (${weekdays[d.weekday - 1]})';
  }

  /// 일별 매출 금액 포맷: 천단위 콤마(원 단위 없음). null/0 은 빈칸
  /// (레거시 `numberWithCommas(isEmpty(...))` 정합).
  String _formatSalesAmount(int? amount) {
    if (amount == null || amount == 0) return '';
    return amount.toString().replaceAllMapped(
          RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
          (match) => '${match[1]},',
        );
  }
}

/// 내 일별 매출 테이블 한 행 (날짜 + 그날 실적 합계).
class _DailySalesRow {
  final DateTime date;
  final int? amount;

  const _DailySalesRow({required this.date, this.amount});
}
