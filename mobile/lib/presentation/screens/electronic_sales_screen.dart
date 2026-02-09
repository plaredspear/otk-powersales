import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../providers/electronic_sales_provider.dart';
import '../providers/electronic_sales_state.dart';
import '../widgets/common/search_filter_widget.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/common/error_view.dart';
import '../widgets/electronic/electronic_sales_table.dart';

/// 전산매출 조회 화면
class ElectronicSalesScreen extends ConsumerStatefulWidget {
  const ElectronicSalesScreen({super.key});

  @override
  ConsumerState<ElectronicSalesScreen> createState() =>
      _ElectronicSalesScreenState();
}

class _ElectronicSalesScreenState
    extends ConsumerState<ElectronicSalesScreen> {
  String? _yearMonth;
  String? _customerName;
  String? _productName;
  String? _productCode;

  @override
  void initState() {
    super.initState();
    // 화면 로드 시 기본 필터(현재 월)로 조회
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _fetchSales();
    });
  }

  /// 전산매출 조회
  void _fetchSales() {
    ref.read(electronicSalesProvider.notifier).fetchSales(
          yearMonth: _yearMonth,
          customerName: _customerName,
          productName: _productName,
          productCode: _productCode,
        );
  }

  /// 필터 초기화
  void _resetFilter() {
    setState(() {
      _yearMonth = null;
      _customerName = null;
      _productName = null;
      _productCode = null;
    });
    ref.read(electronicSalesProvider.notifier).resetFilter();
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
      padding: const EdgeInsets.all(12),
      color: Colors.grey[50],
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 년월 선택기
          _buildYearMonthPicker(),

          const SizedBox(height: 10),

          // 거래처명 필터
          SearchFilterWidget(
            label: '거래처명',
            hintText: '거래처명 입력 (예: 농협)',
            filterType: FilterType.textInput,
            onChanged: (value) {
              setState(() {
                _customerName = value.isEmpty ? null : value;
              });
            },
          ),

          const SizedBox(height: 10),

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

          const SizedBox(height: 10),

          // 제품 코드 필터
          SearchFilterWidget(
            label: '제품 코드',
            hintText: '제품 코드 입력 (예: P001)',
            filterType: FilterType.textInput,
            onChanged: (value) {
              setState(() {
                _productCode = value.isEmpty ? null : value;
              });
            },
          ),

          const SizedBox(height: 12),

          // 조회 버튼
          ElevatedButton.icon(
            onPressed: _fetchSales,
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

  /// 년월 선택기
  Widget _buildYearMonthPicker() {
    final displayYearMonth = _yearMonth ?? _getCurrentYearMonth();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '년월',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: Colors.grey[700],
          ),
        ),
        const SizedBox(height: 6),
        InkWell(
          onTap: () async {
            final selected = await _showYearMonthPicker();
            if (selected != null) {
              setState(() {
                _yearMonth = selected;
              });
            }
          },
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey[300]!),
              borderRadius: BorderRadius.circular(8),
              color: Colors.white,
            ),
            child: Row(
              children: [
                Icon(Icons.calendar_today, size: 20, color: Colors.grey[600]),
                const SizedBox(width: 12),
                Text(
                  _formatYearMonth(displayYearMonth),
                  style: const TextStyle(fontSize: 16),
                ),
                const Spacer(),
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
    final initialDate = _yearMonth != null
        ? DateTime(
            int.parse(_yearMonth!.substring(0, 4)),
            int.parse(_yearMonth!.substring(4, 6)),
          )
        : now;

    final picked = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(now.year + 1, 12),
      helpText: '년월 선택',
      cancelText: '취소',
      confirmText: '확인',
      locale: const Locale('ko', 'KR'),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            datePickerTheme: DatePickerThemeData(
              headerBackgroundColor: Theme.of(context).primaryColor,
              headerForegroundColor: Colors.white,
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      return DateFormat('yyyyMM').format(picked);
    }
    return null;
  }

  /// 년월 포맷팅 (202601 -> 2026년 01월)
  String _formatYearMonth(String yearMonth) {
    if (yearMonth.length != 6) return yearMonth;
    final year = yearMonth.substring(0, 4);
    final month = yearMonth.substring(4, 6);
    return '$year년 $month월';
  }

  /// 현재 년월 가져오기 (예: 202602)
  String _getCurrentYearMonth() {
    return DateFormat('yyyyMM').format(DateTime.now());
  }

  /// 결과 섹션 UI
  Widget _buildResultSection(ElectronicSalesState state) {
    // 로딩 상태
    if (state.isLoading) {
      return const LoadingIndicator(
        message: '전산매출 조회 중...',
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
          message: '조회된 전산매출이 없습니다',
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

        // 매출 테이블
        Expanded(
          child: ElectronicSalesTable(
            salesList: state.sales,
            onTap: (sales) {
              // TODO: 상세 화면으로 이동 (추후 구현)
            },
          ),
        ),
      ],
    );
  }

  /// 합계 정보 섹션
  Widget _buildSummarySection(ElectronicSalesState state) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      color: Colors.green[50],
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
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
          if (state.averageGrowthRate != null) ...[
            const SizedBox(height: 6),
            const Divider(height: 1),
            const SizedBox(height: 6),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  state.averageGrowthRate! >= 0
                      ? Icons.trending_up
                      : Icons.trending_down,
                  size: 18,
                  color: state.averageGrowthRate! >= 0
                      ? Colors.green[700]
                      : Colors.red[700],
                ),
                const SizedBox(width: 6),
                Text(
                  '평균 증감율: ${state.averageGrowthRate!.toStringAsFixed(2)}%',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                    color: state.averageGrowthRate! >= 0
                        ? Colors.green[700]
                        : Colors.red[700],
                  ),
                ),
              ],
            ),
          ],
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
        Text(
          label,
          style: TextStyle(
            fontSize: 11,
            color: Colors.grey[600],
          ),
        ),
        const SizedBox(height: 1),
        Text(
          value,
          style: const TextStyle(
            fontSize: 14,
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
