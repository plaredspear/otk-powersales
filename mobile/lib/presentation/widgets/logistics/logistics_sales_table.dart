import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../domain/entities/logistics_sales.dart';

/// 물류매출 테이블 위젯
///
/// 물류매출 데이터를 테이블 형식으로 표시합니다.
class LogisticsSalesTable extends StatelessWidget {
  /// 표시할 물류매출 목록
  final List<LogisticsSales> salesList;

  /// 아이템 탭 콜백
  final void Function(LogisticsSales)? onTap;

  const LogisticsSalesTable({
    super.key,
    required this.salesList,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    if (salesList.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(32.0),
          child: Text(
            '조회된 물류매출이 없습니다',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey,
            ),
          ),
        ),
      );
    }

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: SingleChildScrollView(
        child: DataTable(
          headingRowColor: WidgetStateProperty.all(Colors.blue[50]),
          columnSpacing: 16,
          horizontalMargin: 12,
          dataRowMinHeight: 48,
          dataRowMaxHeight: 64,
          columns: _buildColumns(),
          rows: _buildRows(context),
        ),
      ),
    );
  }

  /// 테이블 컬럼 정의
  List<DataColumn> _buildColumns() {
    return [
      const DataColumn(
        label: Text(
          '년월',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
      const DataColumn(
        label: Text(
          '카테고리',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
      const DataColumn(
        label: Text(
          '구분',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
      const DataColumn(
        label: Text(
          '당해 실적',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
        numeric: true,
      ),
      const DataColumn(
        label: Text(
          '전년 실적',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
        numeric: true,
      ),
      const DataColumn(
        label: Text(
          '증감',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
        numeric: true,
      ),
      const DataColumn(
        label: Text(
          '증감율',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
        numeric: true,
      ),
    ];
  }

  /// 테이블 행 생성
  List<DataRow> _buildRows(BuildContext context) {
    return salesList.map((sales) {
      return DataRow(
        onSelectChanged: onTap != null ? (_) => onTap!(sales) : null,
        cells: [
          // 년월
          DataCell(
            Text(
              _formatYearMonth(sales.yearMonth),
              style: const TextStyle(fontSize: 13),
            ),
          ),
          // 카테고리
          DataCell(
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                _getCategoryIcon(sales.category),
                const SizedBox(width: 4),
                Text(
                  sales.category.displayName,
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
          ),
          // 당월/이전월 구분
          DataCell(
            _buildMonthTypeChip(sales.isCurrentMonth),
          ),
          // 당해 실적
          DataCell(
            Text(
              NumberFormat('#,###').format(sales.currentAmount),
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          // 전년 실적
          DataCell(
            Text(
              NumberFormat('#,###').format(sales.previousYearAmount),
              style: TextStyle(
                fontSize: 13,
                color: Colors.grey[600],
              ),
            ),
          ),
          // 증감
          DataCell(
            Text(
              NumberFormat('+#,###;-#,###').format(sales.difference),
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.bold,
                color: sales.difference >= 0 ? Colors.red : Colors.blue,
              ),
            ),
          ),
          // 증감율
          DataCell(
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  sales.growthRate >= 0
                      ? Icons.arrow_upward
                      : Icons.arrow_downward,
                  size: 14,
                  color: sales.growthRate >= 0 ? Colors.red : Colors.blue,
                ),
                const SizedBox(width: 4),
                Text(
                  '${sales.growthRate.toStringAsFixed(2)}%',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                    color: sales.growthRate >= 0 ? Colors.red : Colors.blue,
                  ),
                ),
              ],
            ),
          ),
        ],
      );
    }).toList();
  }

  /// 년월 포맷팅
  String _formatYearMonth(String yearMonth) {
    if (yearMonth.length != 6) return yearMonth;
    final year = yearMonth.substring(0, 4);
    final month = yearMonth.substring(4, 6);
    return '$year-$month';
  }

  /// 카테고리 아이콘
  Icon _getCategoryIcon(LogisticsCategory category) {
    IconData iconData;
    Color color;

    switch (category) {
      case LogisticsCategory.normal:
        iconData = Icons.inventory_2;
        color = Colors.brown;
        break;
      case LogisticsCategory.ramen:
        iconData = Icons.ramen_dining;
        color = Colors.orange;
        break;
      case LogisticsCategory.frozen:
        iconData = Icons.ac_unit;
        color = Colors.blue;
        break;
    }

    return Icon(iconData, size: 18, color: color);
  }

  /// 당월/이전월 구분 칩
  Widget _buildMonthTypeChip(bool isCurrentMonth) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: isCurrentMonth ? Colors.green[100] : Colors.blue[100],
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        isCurrentMonth ? '당월' : '마감',
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.bold,
          color: isCurrentMonth ? Colors.green[900] : Colors.blue[900],
        ),
      ),
    );
  }
}
