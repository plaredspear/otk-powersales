import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/theme/app_colors.dart';
import '../providers/monthly_sales_provider.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../widgets/sales/monthly_sales_chart_widget.dart';

/// 월매출 탭 페이지
///
/// 월매출 통계를 표시하는 탭 페이지입니다.
class MonthlySalesTabPage extends ConsumerStatefulWidget {
  const MonthlySalesTabPage({super.key});

  @override
  ConsumerState<MonthlySalesTabPage> createState() =>
      _MonthlySalesTabPageState();
}

class _MonthlySalesTabPageState extends ConsumerState<MonthlySalesTabPage> {
  /// 선택된 거래처명 (필터 표시용, 미선택 시 전체).
  String? _selectedAccountName;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(monthlySalesProvider.notifier).initialize();
    });
  }

  /// 거래처 선택 — 내 거래처 선택 바텀시트에서 고른 거래처로 필터링
  Future<void> _selectAccount() async {
    final account = await AccountSelectorSheet.show(context);
    if (account == null || !mounted) return;
    setState(() => _selectedAccountName = account.accountName);
    await ref
        .read(monthlySalesProvider.notifier)
        .setCustomer(account.accountId.toString());
  }

  /// 거래처 필터 초기화 (전체)
  void _clearAccount() {
    setState(() => _selectedAccountName = null);
    ref.read(monthlySalesProvider.notifier).clearCustomerFilter();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(monthlySalesProvider);
    final notifier = ref.read(monthlySalesProvider.notifier);

    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('에러: ${state.errorMessage}'),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => notifier.refresh(),
              child: const Text('다시 시도'),
            ),
          ],
        ),
      );
    }

    final monthlySales = state.monthlySales;
    if (monthlySales == null) {
      return const Center(
        child: Text('월매출 데이터가 없습니다'),
      );
    }

    return RefreshIndicator(
      onRefresh: () => notifier.refresh(),
      child: ListView(
        children: [
          // 거래처 선택 필터
          InkWell(
            onTap: _selectAccount,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  const Icon(Icons.store_outlined, size: 20),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _selectedAccountName ?? '전체 거래처',
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  if (_selectedAccountName != null)
                    IconButton(
                      onPressed: _clearAccount,
                      icon: const Icon(Icons.close, size: 18),
                      visualDensity: VisualDensity.compact,
                      tooltip: '전체 보기',
                    )
                  else
                    const Icon(Icons.chevron_right, size: 20),
                ],
              ),
            ),
          ),

          const Divider(height: 1),

          // 연월 네비게이터
          Container(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton(
                  onPressed: state.canGoToPreviousMonth
                      ? () => notifier.goToPreviousMonth()
                      : null,
                  icon: const Icon(Icons.chevron_left),
                ),
                Text(
                  state.displayYearMonth,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                IconButton(
                  onPressed: state.canGoToNextMonth
                      ? () => notifier.goToNextMonth()
                      : null,
                  icon: const Icon(Icons.chevron_right),
                ),
              ],
            ),
          ),

          const Divider(height: 1),

          // ── 당월 매출 진도율 (레거시 list.jsp 진행바 + 기준 진도율) ─────
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '당월 매출 진도율',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
                Stack(
                  alignment: Alignment.center,
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(4),
                      child: LinearProgressIndicator(
                        value:
                            (monthlySales.achievementRate / 100).clamp(0.0, 1.0),
                        minHeight: 26,
                        backgroundColor: AppColors.surface,
                        // 진도율 >= 기준 진도율이면 상승(녹색), 미달이면 하락(적색) — 레거시 is-up/is-down 정합
                        valueColor: AlwaysStoppedAnimation<Color>(
                          monthlySales.achievementRate >= _baseRate
                              ? AppColors.success
                              : AppColors.legacyDanger,
                        ),
                      ),
                    ),
                    Text(
                      '${monthlySales.achievementRate.toStringAsFixed(0)}%',
                      style: const TextStyle(
                          fontSize: 14, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Align(
                  alignment: Alignment.centerRight,
                  child: Text(
                    '기준 진도율 : ${_baseRate.toStringAsFixed(0)}%',
                    style: const TextStyle(
                        fontSize: 12, color: AppColors.legacyDanger),
                  ),
                ),
              ],
            ),
          ),

          // ── 당월 매출 실적 (단위: 만원) ───────────────────────────
          const Padding(
            padding: EdgeInsets.fromLTRB(16, 20, 16, 4),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text('당월 매출 실적',
                    style:
                        TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                Text('단위 : 만원',
                    style:
                        TextStyle(fontSize: 12, color: AppColors.textTertiary)),
              ],
            ),
          ),

          // 목표 금액 / 마감 합계 실적
          _targetActualCard(
            targetLabel: '목표 금액',
            targetAmount: monthlySales.targetAmount,
            actualLabel: '마감 합계 실적',
            actualAmount: monthlySales.achievedAmount,
            achievementRate: monthlySales.achievementRate,
          ),

          // 온도대별 목표 / 실적 (상온 / 라면 / 냉동·냉장 / 유지류)
          ...monthlySales.categorySales.map(
            (c) => _targetActualCard(
              targetLabel: '${_categoryLabel(c.category)} 목표',
              targetAmount: c.targetAmount,
              actualLabel: '${_categoryLabel(c.category)} 실적',
              actualAmount: c.achievedAmount,
              achievementRate: c.achievementRate,
            ),
          ),

          const SizedBox(height: 24),

          // 차트
          MonthlySalesChartWidget(monthlySales: monthlySales),

          const SizedBox(height: 24),
        ],
      ),
    );
  }

  /// 기준 진도율(영업일 기준). 신규 백엔드가 미제공이라 레거시 0% 상태와 동일하게 0 고정.
  static const double _baseRate = 0;

  /// 원 → 만원 환산 (1만원 미만 절사) + 천단위 콤마. 레거시 list.jsp 단위 변환 정합.
  String _man(int won) =>
      NumberFormat('#,###').format((won / 10000).floor());

  /// 백엔드 카테고리 코드 → 온도대 한글 라벨 (레거시 온도대 구분 정합).
  String _categoryLabel(String code) {
    switch (code) {
      case 'AMBIENT':
        return '상온';
      case 'NOODLE':
        return '라면';
      case 'FROZEN_REFRIGERATED':
        return '냉동/냉장';
      case 'OIL_FAT':
        return '유지류';
      default:
        return code;
    }
  }

  /// 좌:목표(회색) / 우:실적(녹색 테두리, N% 달성) 카드 한 쌍 — 레거시 list.jsp box 레이아웃 정합.
  Widget _targetActualCard({
    required String targetLabel,
    required int targetAmount,
    required String actualLabel,
    required int actualAmount,
    required double achievementRate,
  }) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 6, 16, 6),
      child: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 목표 (좌)
            Expanded(
              child: Container(
                padding:
                    const EdgeInsets.symmetric(vertical: 22, horizontal: 12),
                decoration: BoxDecoration(
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(targetLabel,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                            fontSize: 15, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 6),
                    Text('${_man(targetAmount)} 만원',
                        style: const TextStyle(
                            fontSize: 15, color: AppColors.legacyTextMute)),
                  ],
                ),
              ),
            ),
            const SizedBox(width: 12),
            // 실적 (우)
            Expanded(
              child: Container(
                padding:
                    const EdgeInsets.symmetric(vertical: 22, horizontal: 12),
                decoration: BoxDecoration(
                  color: AppColors.white,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: AppColors.success),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(actualLabel,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                            fontSize: 15, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 6),
                    Text('${_man(actualAmount)} 만원',
                        style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: AppColors.successDark)),
                    const SizedBox(height: 2),
                    Text('(${achievementRate.toStringAsFixed(0)}% 달성)',
                        style: const TextStyle(
                            fontSize: 12, color: AppColors.successDark)),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
