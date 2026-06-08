import 'package:flutter/material.dart';
import '../../../domain/entities/pos_sales.dart';

/// POS 매출 테이블 위젯
///
/// 제품별 POS 매출(거래처/제품명/제품코드/바코드/수량/금액)을 표 형식으로 표시한다.
/// 레거시 `posmain.jsp` 의 제품별 명세(`SALES_QTY`/`SALES_AMT`) 정합.
class PosSalesTable extends StatelessWidget {
  /// 표시할 POS 매출 목록
  final List<PosSales> salesList;

  /// 아이템 탭 콜백
  final void Function(PosSales)? onTap;

  const PosSalesTable({
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
            '조회된 POS 매출이 없습니다',
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
          rows: _buildRows(),
        ),
      ),
    );
  }

  /// 테이블 컬럼 정의
  List<DataColumn> _buildColumns() {
    const headingStyle = TextStyle(fontWeight: FontWeight.bold, fontSize: 13);
    return const [
      DataColumn(label: Text('거래처', style: headingStyle)),
      DataColumn(label: Text('제품명', style: headingStyle)),
      DataColumn(label: Text('제품코드', style: headingStyle)),
      DataColumn(label: Text('바코드', style: headingStyle)),
      DataColumn(label: Text('수량', style: headingStyle), numeric: true),
      DataColumn(label: Text('금액', style: headingStyle), numeric: true),
    ];
  }

  /// 테이블 행 생성
  List<DataRow> _buildRows() {
    return salesList.map((sales) {
      return DataRow(
        onSelectChanged: onTap != null ? (_) => onTap!(sales) : null,
        cells: [
          DataCell(
            Text(sales.customerName, style: const TextStyle(fontSize: 13)),
          ),
          DataCell(
            Text(sales.productName, style: const TextStyle(fontSize: 13)),
          ),
          DataCell(
            Text(
              _formatProductCode(sales.productCode),
              style: TextStyle(fontSize: 12, color: Colors.grey[600]),
            ),
          ),
          DataCell(
            Text(
              sales.barcode ?? '-',
              style: TextStyle(fontSize: 12, color: Colors.grey[600]),
            ),
          ),
          DataCell(
            Text(
              '${sales.quantity}',
              style: const TextStyle(fontSize: 13),
              textAlign: TextAlign.right,
            ),
          ),
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
        ],
      );
    }).toList();
  }

  /// 제품코드 앞자리 0 제거 (레거시 `posmain.jsp` 의 ITEM_CD leading-zero strip 정합).
  String _formatProductCode(String code) {
    final stripped = code.replaceFirst(RegExp(r'^0+'), '');
    return stripped.isEmpty ? '0' : stripped;
  }

  /// 숫자를 통화 형식으로 포맷
  String _formatCurrency(int amount) {
    return amount.toString().replaceAllMapped(
          RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
          (Match m) => '${m[1]},',
        );
  }
}
