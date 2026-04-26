import 'package:flutter/material.dart';

import '../../../domain/entities/monthly_sales.dart';
import '../common/sales_chart_widget.dart';

/// 월매출 차트 위젯
///
/// 월매출 통계를 차트로 표시하는 위젯입니다.
class MonthlySalesChartWidget extends StatelessWidget {
  final MonthlySales monthlySales;

  const MonthlySalesChartWidget({
    super.key,
    required this.monthlySales,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // 전년 동월 비교 차트
        SalesChartWidget(
          title: '전년 동월 비교',
          data: [
            SalesChartData(
              label: '${monthlySales.yearMonth.substring(0, 4)}년',
              currentAmount: monthlySales.achievedAmount.toDouble(),
              previousYearAmount: monthlySales.previousYearSameMonth.toDouble(),
            ),
          ],
          height: 250,
        ),
        const SizedBox(height: 24),

        // 월 평균 실적 차트
        SalesChartWidget(
          title: '월 평균 실적',
          data: [
            SalesChartData(
              label: '평균',
              currentAmount: monthlySales.monthlyAverage.currentYearAverage.toDouble(),
              previousYearAmount: monthlySales.monthlyAverage.previousYearAverage.toDouble(),
            ),
          ],
          height: 250,
        ),
      ],
    );
  }
}
