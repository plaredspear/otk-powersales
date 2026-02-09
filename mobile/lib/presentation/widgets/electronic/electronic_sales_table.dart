import 'package:flutter/material.dart';
import '../../../domain/entities/electronic_sales.dart';

/// 전산매출 테이블 위젯
///
/// 전산매출 데이터를 테이블 형식으로 표시합니다.
class ElectronicSalesTable extends StatelessWidget {
  /// 표시할 전산매출 목록
  final List<ElectronicSales> salesList;

  /// 아이템 탭 콜백
  final void Function(ElectronicSales)? onTap;

  const ElectronicSalesTable({
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
            '조회된 전산매출이 없습니다',
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
          headingRowColor: MaterialStateProperty.all(Colors.green[50]),
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
          '거래처',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
      const DataColumn(
        label: Text(
          '제품명',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
      const DataColumn(
        label: Text(
          '제품코드',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
      const DataColumn(
        label: Text(
          '수량',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
        numeric: true,
      ),
      const DataColumn(
        label: Text(
          '금액',
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
          // 거래처명
          DataCell(
            Text(
              sales.customerName,
              style: const TextStyle(fontSize: 13),
            ),
          ),
          // 제품명
          DataCell(
            Text(
              sales.productName,
              style: const TextStyle(fontSize: 13),
            ),
          ),
          // 제품코드
          DataCell(
            Text(
              sales.productCode,
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey[600],
              ),
            ),
          ),
          // 수량
          DataCell(
            Text(
              '${sales.quantity}',
              style: const TextStyle(fontSize: 13),
              textAlign: TextAlign.right,
            ),
          ),
          // 금액
          DataCell(
            Text(
              _formatCurrency(sales.amount),
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
              ),
              textAlign: TextAlign.right,
            ),
          ),
          // 증감율
          DataCell(
            _buildGrowthRateCell(sales.growthRate),
          ),
        ],
      );
    }).toList();
  }

  /// 증감율 셀 위젯
  Widget _buildGrowthRateCell(double? growthRate) {
    if (growthRate == null) {
      return const Text(
        '-',
        style: TextStyle(
          fontSize: 12,
          color: Colors.grey,
        ),
        textAlign: TextAlign.right,
      );
    }

    final isPositive = growthRate >= 0;
    final color = isPositive ? Colors.green[700] : Colors.red[700];
    final icon = isPositive ? Icons.arrow_upward : Icons.arrow_downward;

    return Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Icon(
          icon,
          size: 14,
          color: color,
        ),
        const SizedBox(width: 4),
        Text(
          '${growthRate.toStringAsFixed(1)}%',
          style: TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w500,
            color: color,
          ),
          textAlign: TextAlign.right,
        ),
      ],
    );
  }

  /// 숫자를 통화 형식으로 포맷
  String _formatCurrency(int amount) {
    return amount.toString().replaceAllMapped(
          RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
          (Match m) => '${m[1]},',
        );
  }
}
