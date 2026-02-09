import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/pos_sales_provider.dart';
import '../providers/pos_sales_state.dart';
import '../widgets/common/date_picker_widget.dart';
import '../widgets/common/search_filter_widget.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/common/error_view.dart';
import '../widgets/pos/pos_sales_item.dart';

/// POS 매출 조회 화면
class PosSalesScreen extends ConsumerStatefulWidget {
  const PosSalesScreen({super.key});

  @override
  ConsumerState<PosSalesScreen> createState() => _PosSalesScreenState();
}

class _PosSalesScreenState extends ConsumerState<PosSalesScreen> {
  DateTime? _startDate;
  DateTime? _endDate;
  String? _storeName;
  String? _productName;

  @override
  void initState() {
    super.initState();
    // 화면 로드 시 기본 필터로 조회
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _fetchSales();
    });
  }

  /// POS 매출 조회
  void _fetchSales() {
    ref.read(posSalesProvider.notifier).fetchSales(
          startDate: _startDate,
          endDate: _endDate,
          storeName: _storeName,
          productName: _productName,
        );
  }

  /// 필터 초기화
  void _resetFilter() {
    setState(() {
      _startDate = null;
      _endDate = null;
      _storeName = null;
      _productName = null;
    });
    ref.read(posSalesProvider.notifier).resetFilter();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(posSalesProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('POS 매출 조회'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _resetFilter,
            tooltip: '필터 초기화',
          ),
        ],
      ),
      body: Column(
        children: [
          // 필터 영역
          _buildFilterSection(),

          const Divider(height: 1),

          // 결과 영역
          Expanded(
            child: _buildResultSection(state),
          ),
        ],
      ),
    );
  }

  /// 필터 섹션 UI
  Widget _buildFilterSection() {
    return Container(
      padding: const EdgeInsets.all(16),
      color: Colors.grey[50],
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 날짜 선택기
          DatePickerWidget(
            initialStartDate: _startDate,
            initialEndDate: _endDate,
            onStartDateChanged: (date) {
              setState(() {
                _startDate = date;
              });
            },
            onEndDateChanged: (date) {
              setState(() {
                _endDate = date;
              });
            },
          ),

          const SizedBox(height: 12),

          // 매장명 필터
          SearchFilterWidget(
            label: '매장명',
            hintText: '매장명 입력 (예: 이마트)',
            filterType: FilterType.textInput,
            onChanged: (value) {
              setState(() {
                _storeName = value.isEmpty ? null : value;
              });
            },
          ),

          const SizedBox(height: 12),

          // 제품명 필터
          SearchFilterWidget(
            label: '제품명',
            hintText: '제품명 입력 (예: 진라면)',
            filterType: FilterType.textInput,
            onChanged: (value) {
              setState(() {
                _productName = value.isEmpty ? null : value;
              });
            },
          ),

          const SizedBox(height: 16),

          // 조회 버튼
          ElevatedButton.icon(
            onPressed: _fetchSales,
            icon: const Icon(Icons.search),
            label: const Text('조회'),
            style: ElevatedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 12),
            ),
          ),
        ],
      ),
    );
  }

  /// 결과 섹션 UI
  Widget _buildResultSection(PosSalesState state) {
    // 로딩 상태
    if (state.isLoading) {
      return const LoadingIndicator(
        message: 'POS 매출 조회 중...',
      );
    }

    // 에러 상태
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

    // 데이터 없음
    if (state.sales.isEmpty) {
      return SingleChildScrollView(
        child: ErrorView.noData(
          message: '조회된 POS 매출이 없습니다',
          description: '다른 조건으로 다시 조회해보세요',
          onRetry: _fetchSales,
          isFullScreen: false,
        ),
      );
    }

    // 결과 표시
    return Column(
      children: [
        // 합계 정보
        _buildSummarySection(state),

        const Divider(height: 1),

        // 매출 목록
        Expanded(
          child: ListView.builder(
            itemCount: state.sales.length,
            itemBuilder: (context, index) {
              final sales = state.sales[index];
              return PosSalesItem(
                sales: sales,
                onTap: () {
                  // TODO: 상세 화면으로 이동 (추후 구현)
                },
              );
            },
          ),
        ),
      ],
    );
  }

  /// 합계 정보 섹션
  Widget _buildSummarySection(PosSalesState state) {
    return Container(
      padding: const EdgeInsets.all(16),
      color: Colors.blue[50],
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildSummaryItem(
            '총 건수',
            '${state.sales.length}건',
            Icons.receipt_long,
          ),
          _buildSummaryItem(
            '총 수량',
            '${state.totalQuantity}개',
            Icons.inventory_2,
          ),
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
      children: [
        Icon(icon, size: 24, color: Colors.blue[700]),
        const SizedBox(height: 4),
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            color: Colors.grey[600],
          ),
        ),
        const SizedBox(height: 2),
        Text(
          value,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
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
