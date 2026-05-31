import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../providers/electronic_sales_provider.dart';
import '../providers/electronic_sales_state.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/common/error_view.dart';
import '../widgets/electronic/electronic_sales_table.dart';

/// 전산매출(ABC) 조회 화면.
///
/// 레거시 `promotion/month/abcmain.jsp` 동등 — 거래처 1곳 + 연월 선택 후 제품별 실적 조회.
class ElectronicSalesScreen extends ConsumerStatefulWidget {
  const ElectronicSalesScreen({super.key});

  @override
  ConsumerState<ElectronicSalesScreen> createState() =>
      _ElectronicSalesScreenState();
}

class _ElectronicSalesScreenState
    extends ConsumerState<ElectronicSalesScreen> {
  int? _customerId;
  String? _customerName;
  late String _yearMonth;

  @override
  void initState() {
    super.initState();
    _yearMonth = _getCurrentYearMonth();
  }

  /// 거래처 선택 — 내 거래처 선택 바텀시트 → 선택 시 자동 조회
  Future<void> _selectCustomer() async {
    final account = await AccountSelectorSheet.show(context);
    if (account == null || !mounted) return;
    setState(() {
      _customerId = account.accountId;
      _customerName = account.accountName;
    });
    _fetchSales();
  }

  /// 전산매출 조회 (거래처 선택 필요)
  void _fetchSales() {
    final customerId = _customerId;
    final customerName = _customerName;
    if (customerId == null || customerName == null) return;
    ref.read(electronicSalesProvider.notifier).fetchSales(
          customerId: customerId,
          customerName: customerName,
          yearMonth: _yearMonth,
        );
  }

  /// 초기화 (거래처/연월 선택 해제)
  void _reset() {
    setState(() {
      _customerId = null;
      _customerName = null;
      _yearMonth = _getCurrentYearMonth();
    });
    ref.read(electronicSalesProvider.notifier).reset();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(electronicSalesProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('전산매출 조회'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _reset,
            tooltip: '초기화',
          ),
        ],
      ),
      body: Column(
        children: [
          _buildFilterSection(),
          const Divider(height: 1),
          Expanded(child: _buildResultSection(state)),
        ],
      ),
    );
  }

  /// 필터 섹션 (거래처 선택 + 연월)
  Widget _buildFilterSection() {
    return Container(
      padding: const EdgeInsets.all(12),
      color: Colors.grey[50],
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildSelectorRow(
            icon: Icons.store_outlined,
            label: '거래처',
            value: _customerName ?? '거래처를 선택하세요',
            onTap: _selectCustomer,
          ),
          const SizedBox(height: 10),
          _buildSelectorRow(
            icon: Icons.calendar_today,
            label: '년월',
            value: _formatYearMonth(_yearMonth),
            onTap: () async {
              final selected = await _showYearMonthPicker();
              if (selected != null) {
                setState(() => _yearMonth = selected);
                _fetchSales();
              }
            },
          ),
          const SizedBox(height: 12),
          ElevatedButton.icon(
            onPressed: _customerId != null ? _fetchSales : null,
            icon: const Icon(Icons.search),
            label: const Text('조회'),
            style: ElevatedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 10),
            ),
          ),
        ],
      ),
    );
  }

  /// 거래처/연월 공통 선택 행
  Widget _buildSelectorRow({
    required IconData icon,
    required String label,
    required String value,
    required VoidCallback onTap,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: Colors.grey[700],
          ),
        ),
        const SizedBox(height: 6),
        InkWell(
          onTap: onTap,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey[300]!),
              borderRadius: BorderRadius.circular(8),
              color: Colors.white,
            ),
            child: Row(
              children: [
                Icon(icon, size: 20, color: Colors.grey[600]),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(value, style: const TextStyle(fontSize: 16)),
                ),
                Icon(Icons.arrow_drop_down, color: Colors.grey[600]),
              ],
            ),
          ),
        ),
      ],
    );
  }

  /// 년월 선택 다이얼로그
  Future<String?> _showYearMonthPicker() async {
    final now = DateTime.now();
    final initialDate = DateTime(
      int.parse(_yearMonth.substring(0, 4)),
      int.parse(_yearMonth.substring(4, 6)),
    );

    final picked = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(now.year + 1, 12),
      helpText: '년월 선택',
      cancelText: '취소',
      confirmText: '확인',
      locale: const Locale('ko', 'KR'),
    );

    return picked != null ? DateFormat('yyyyMM').format(picked) : null;
  }

  /// 년월 포맷팅 (202601 -> 2026년 01월)
  String _formatYearMonth(String yearMonth) {
    if (yearMonth.length != 6) return yearMonth;
    return '${yearMonth.substring(0, 4)}년 ${yearMonth.substring(4, 6)}월';
  }

  /// 현재 년월 (예: 202602)
  String _getCurrentYearMonth() => DateFormat('yyyyMM').format(DateTime.now());

  /// 결과 섹션
  Widget _buildResultSection(ElectronicSalesState state) {
    // 거래처 미선택 — 안내
    if (_customerId == null) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(32),
          child: Text(
            '거래처를 선택하면 전산매출을 조회합니다',
            style: TextStyle(fontSize: 14, color: Colors.grey),
          ),
        ),
      );
    }

    if (state.isLoading) {
      return const LoadingIndicator(message: '전산매출 조회 중...');
    }

    if (state.errorMessage != null) {
      return SingleChildScrollView(
        child: ErrorView(
          message: '조회 중 오류가 발생했습니다',
          description: state.errorMessage,
          onRetry: _fetchSales,
          isFullScreen: false,
        ),
      );
    }

    if (state.sales.isEmpty) {
      return SingleChildScrollView(
        child: ErrorView.noData(
          message: '조회된 전산매출이 없습니다',
          description: '다른 거래처/연월로 다시 조회해보세요',
          onRetry: _fetchSales,
          isFullScreen: false,
        ),
      );
    }

    return Column(
      children: [
        _buildSummarySection(state),
        const Divider(height: 1),
        Expanded(
          child: ElectronicSalesTable(salesList: state.sales),
        ),
      ],
    );
  }

  /// 합계 정보 섹션
  Widget _buildSummarySection(ElectronicSalesState state) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      color: Colors.green[50],
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildSummaryItem('총 건수', '${state.sales.length}건', Icons.receipt_long),
          _buildSummaryItem('총 수량', '${state.totalQuantity}개', Icons.inventory_2),
          _buildSummaryItem(
            '총 금액',
            '${_formatCurrency(state.totalAmount)}원',
            Icons.payments,
          ),
        ],
      ),
    );
  }

  /// 합계 항목 위젯
  Widget _buildSummaryItem(String label, String value, IconData icon) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 20, color: Colors.green[700]),
        const SizedBox(height: 2),
        Text(label, style: TextStyle(fontSize: 11, color: Colors.grey[600])),
        const SizedBox(height: 1),
        Text(
          value,
          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
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
