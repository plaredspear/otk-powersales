import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/daily_sales_summary.dart';

/// 일매출 목록 위젯
///
/// 일별 판매금액 목록을 표시하는 위젯입니다.
class DailySalesListWidget extends StatelessWidget {
  final List<DailySalesSummary> dailySales;

  const DailySalesListWidget({
    super.key,
    required this.dailySales,
  });

  @override
  Widget build(BuildContext context) {
    if (dailySales.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(32),
          child: Text(
            '등록된 일매출이 없습니다',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey,
            ),
          ),
        ),
      );
    }

    return Card(
      margin: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.all(16),
            child: Text(
              '일별 판매금액',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const Divider(height: 1),
          ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: dailySales.length,
            separatorBuilder: (context, index) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final sale = dailySales[index];
              return _buildDailySaleItem(sale);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildDailySaleItem(DailySalesSummary sale) {
    final dateFormat = DateFormat('yyyy.MM.dd (E)', 'ko_KR');
    final currencyFormat = NumberFormat('#,###');

    return ListTile(
      leading: Icon(
        sale.status == 'REGISTERED'
            ? Icons.check_circle
            : Icons.edit_note,
        color: sale.status == 'REGISTERED' ? Colors.green : Colors.orange,
      ),
      title: Text(dateFormat.format(sale.salesDate)),
      trailing: Text(
        '${currencyFormat.format(sale.totalAmount)}원',
        style: const TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
