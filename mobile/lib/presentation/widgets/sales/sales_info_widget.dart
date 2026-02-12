import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/event_sales_info.dart';

/// 매출 정보 위젯
///
/// 목표/달성/진행율 정보를 표시하는 위젯입니다.
class SalesInfoWidget extends StatelessWidget {
  final EventSalesInfo salesInfo;

  const SalesInfoWidget({
    super.key,
    required this.salesInfo,
  });

  @override
  Widget build(BuildContext context) {
    final currencyFormat = NumberFormat('#,###');

    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 진행율
            Row(
              children: [
                const Icon(Icons.timeline, color: Colors.blue),
                const SizedBox(width: 8),
                Text(
                  '진행율: ${salesInfo.progressRate.toStringAsFixed(1)}%',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // 목표 금액
            _buildAmountRow(
              '목표 금액',
              salesInfo.targetAmount,
              currencyFormat,
              Icons.flag,
            ),
            const SizedBox(height: 8),

            // 달성 금액
            _buildAmountRow(
              '달성 금액',
              salesInfo.achievedAmount,
              currencyFormat,
              Icons.check_circle,
            ),
            const SizedBox(height: 16),

            // 달성율 progress bar
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      '달성율',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      '${salesInfo.achievementRate.toStringAsFixed(1)}%',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: salesInfo.achievementRate >= 100
                            ? Colors.green
                            : Colors.orange,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                LinearProgressIndicator(
                  value: salesInfo.achievementRate / 100,
                  backgroundColor: Colors.grey.shade300,
                  valueColor: AlwaysStoppedAnimation<Color>(
                    salesInfo.achievementRate >= 100
                        ? Colors.green
                        : Colors.blue,
                  ),
                  minHeight: 10,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAmountRow(
    String label,
    int amount,
    NumberFormat formatter,
    IconData icon,
  ) {
    return Row(
      children: [
        Icon(icon, size: 20, color: Colors.grey),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            label,
            style: const TextStyle(fontSize: 14),
          ),
        ),
        Text(
          '${formatter.format(amount)}원',
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }
}
