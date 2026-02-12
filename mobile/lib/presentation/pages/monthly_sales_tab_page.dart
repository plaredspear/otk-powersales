import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../providers/monthly_sales_provider.dart';
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
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(monthlySalesProvider.notifier).initialize();
    });
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

    final currencyFormat = NumberFormat('#,###');

    return RefreshIndicator(
      onRefresh: () => notifier.refresh(),
      child: ListView(
        children: [
          // 거래처 선택 (TODO: 구현 필요)
          Container(
            padding: const EdgeInsets.all(16),
            child: const Text('거래처 선택 (TODO)'),
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

          // 매출 진도율
          Card(
            margin: const EdgeInsets.all(16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '매출 진도율',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('목표: ${currencyFormat.format(monthlySales.targetAmount)}원'),
                      Text('달성: ${currencyFormat.format(monthlySales.achievedAmount)}원'),
                    ],
                  ),
                  const SizedBox(height: 8),
                  LinearProgressIndicator(
                    value: monthlySales.achievementRate / 100,
                    backgroundColor: Colors.grey.shade300,
                    valueColor: AlwaysStoppedAnimation<Color>(
                      monthlySales.achievementRate >= 100
                          ? Colors.green
                          : Colors.blue,
                    ),
                    minHeight: 10,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '달성율: ${monthlySales.achievementRate.toStringAsFixed(1)}%',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: monthlySales.achievementRate >= 100
                          ? Colors.green
                          : Colors.orange,
                    ),
                  ),
                ],
              ),
            ),
          ),

          // 제품 유형별 매출
          Card(
            margin: const EdgeInsets.symmetric(horizontal: 16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '제품 유형별 매출',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 16),
                  ...monthlySales.categorySales.map((category) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            category.category,
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 8),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text('목표: ${currencyFormat.format(category.targetAmount)}원'),
                              Text('실적: ${currencyFormat.format(category.achievedAmount)}원'),
                            ],
                          ),
                          const SizedBox(height: 4),
                          LinearProgressIndicator(
                            value: category.achievementRate / 100,
                            backgroundColor: Colors.grey.shade300,
                            minHeight: 8,
                          ),
                          const SizedBox(height: 4),
                          Text('${category.achievementRate.toStringAsFixed(1)}%'),
                        ],
                      ),
                    );
                  }),
                ],
              ),
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
}
