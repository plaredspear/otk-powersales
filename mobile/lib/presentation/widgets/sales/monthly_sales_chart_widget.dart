import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/monthly_sales.dart';

/// 월매출 차트 위젯
///
/// 레거시 `promotion/month/list.jsp` 의 "전년 대비 동월 실적" / "전년 대비 월 평균 실적"
/// 콤보차트 정합 — 전년(좌)·당해(우) 실적을 연·월 라벨로 나란히 표시하고, 단위는 백만원이다.
/// 레거시는 두 막대가 단색이지만, 모바일은 당해/전년 색 구분(파랑/회색)을 유지한다.
class MonthlySalesChartWidget extends StatelessWidget {
  final MonthlySales monthlySales;

  const MonthlySalesChartWidget({
    super.key,
    required this.monthlySales,
  });

  @override
  Widget build(BuildContext context) {
    final yearMonth = monthlySales.yearMonth;
    final currentYear = int.parse(yearMonth.substring(0, 4));
    final previousYear = currentYear - 1;
    final month = int.parse(yearMonth.substring(4, 6));
    final monthPadded = yearMonth.substring(4, 6);

    return Column(
      children: [
        // 전년 대비 동월 실적 (레거시 chart_div) — x축: 전년월(좌) / 당해월(우)
        _LegacyComparisonChart(
          title: '전년 대비 동월 실적',
          bars: [
            _ComparisonBar(
              label: '$previousYear년 $month월',
              amount: monthlySales.previousYearSameMonth.toDouble(),
              isCurrentYear: false,
            ),
            _ComparisonBar(
              label: '$currentYear년 $month월',
              amount: monthlySales.achievedAmount.toDouble(),
              isCurrentYear: true,
            ),
          ],
        ),
        const SizedBox(height: 24),

        // 전년 대비 월 평균 실적 (레거시 chart_div2) — x축: 전년(좌) / 당해(우)
        _LegacyComparisonChart(
          title: '전년 대비 월 평균 실적 (01월 ~ $monthPadded월)',
          bars: [
            _ComparisonBar(
              label: '$previousYear년',
              amount: monthlySales.monthlyAverage.previousYearAverage.toDouble(),
              isCurrentYear: false,
            ),
            _ComparisonBar(
              label: '$currentYear년',
              amount: monthlySales.monthlyAverage.currentYearAverage.toDouble(),
              isCurrentYear: true,
            ),
          ],
        ),
      ],
    );
  }
}

/// 막대 1개(전년 또는 당해)의 데이터.
class _ComparisonBar {
  final String label;
  final double amount;

  /// 당해 연도 막대면 true(파랑), 전년이면 false(회색).
  final bool isCurrentYear;

  const _ComparisonBar({
    required this.label,
    required this.amount,
    required this.isCurrentYear,
  });
}

/// 레거시 전년/당해 비교 막대 차트 한 개 (제목 + 단위 캡션 + 범례 + 막대).
class _LegacyComparisonChart extends StatelessWidget {
  final String title;
  final List<_ComparisonBar> bars;

  const _LegacyComparisonChart({
    required this.title,
    required this.bars,
  });

  static const Color _currentColor = Colors.blue;
  static const Color _previousColor = Colors.grey;

  @override
  Widget build(BuildContext context) {
    double maxAmount = 0;
    for (final b in bars) {
      if (b.amount > maxAmount) maxAmount = b.amount;
    }
    // 여유 10%, 0일 때도 차트가 보이도록 최소 1백만원.
    final maxY = maxAmount > 0 ? maxAmount * 1.1 : 1000000.0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 제목 + 단위 캡션 (레거시 tit_icon + g_caption "단위 : 백만원")
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const Text(
                '단위 : 백만원',
                style: TextStyle(fontSize: 12, color: AppColors.textTertiary),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),

        // 범례 (당해 실적 / 전년 실적)
        _buildLegend(),
        const SizedBox(height: 16),

        SizedBox(
          height: 220,
          child: Padding(
            padding: const EdgeInsets.only(right: 16, left: 8, bottom: 8),
            child: BarChart(_buildChartData(maxY)),
          ),
        ),
      ],
    );
  }

  Widget _buildLegend() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        _legendItem('당해 실적', _currentColor),
        const SizedBox(width: 24),
        _legendItem('전년 실적', _previousColor),
      ],
    );
  }

  Widget _legendItem(String label, Color color) {
    return Row(
      children: [
        Container(
          width: 16,
          height: 16,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(4),
          ),
        ),
        const SizedBox(width: 8),
        Text(label, style: const TextStyle(fontSize: 12)),
      ],
    );
  }

  BarChartData _buildChartData(double maxY) {
    return BarChartData(
      alignment: BarChartAlignment.spaceAround,
      maxY: maxY,
      minY: 0,
      barTouchData: BarTouchData(
        enabled: true,
        touchTooltipData: BarTouchTooltipData(
          getTooltipItem: (group, groupIndex, rod, rodIndex) {
            final bar = bars[groupIndex];
            return BarTooltipItem(
              '${bar.label}\n${NumberFormat('#,###').format(bar.amount)}',
              const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 12,
              ),
            );
          },
        ),
      ),
      titlesData: FlTitlesData(
        show: true,
        bottomTitles: AxisTitles(
          sideTitles: SideTitles(
            showTitles: true,
            reservedSize: 28,
            getTitlesWidget: (value, meta) {
              final index = value.toInt();
              if (index < 0 || index >= bars.length) {
                return const SizedBox.shrink();
              }
              return Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  bars[index].label,
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
              );
            },
          ),
        ),
        leftTitles: AxisTitles(
          sideTitles: SideTitles(
            showTitles: true,
            reservedSize: 40,
            interval: maxY / 5,
            getTitlesWidget: (value, meta) {
              // 백만원 단위 정수 (단위 캡션 "단위 : 백만원" 과 정합)
              return Text(
                NumberFormat('#,###').format((value / 1000000).floor()),
                style: const TextStyle(
                    fontSize: 10, color: AppColors.textSecondary),
              );
            },
          ),
        ),
        topTitles: const AxisTitles(
          sideTitles: SideTitles(showTitles: false),
        ),
        rightTitles: const AxisTitles(
          sideTitles: SideTitles(showTitles: false),
        ),
      ),
      gridData: FlGridData(
        show: true,
        drawVerticalLine: false,
        horizontalInterval: maxY / 5,
        getDrawingHorizontalLine: (value) => const FlLine(
          color: AppColors.divider,
          strokeWidth: 0.5,
        ),
      ),
      borderData: FlBorderData(
        show: true,
        border: const Border(
          bottom: BorderSide(color: AppColors.divider),
          left: BorderSide(color: AppColors.divider),
        ),
      ),
      barGroups: [
        for (var i = 0; i < bars.length; i++)
          BarChartGroupData(
            x: i,
            barRods: [
              BarChartRodData(
                toY: bars[i].amount,
                color: bars[i].isCurrentYear ? _currentColor : _previousColor,
                width: 28,
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(4),
                  topRight: Radius.circular(4),
                ),
              ),
            ],
          ),
      ],
    );
  }
}
