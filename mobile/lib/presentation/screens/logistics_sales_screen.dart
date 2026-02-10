import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../domain/entities/logistics_sales.dart';
import '../providers/logistics_sales_provider.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/common/error_view.dart';
import '../widgets/logistics/logistics_sales_table.dart';
import '../widgets/common/sales_chart_widget.dart';

/// 물류매출 조회 화면
class LogisticsSalesScreen extends ConsumerStatefulWidget {
  const LogisticsSalesScreen({super.key});

  @override
  ConsumerState<LogisticsSalesScreen> createState() =>
      _LogisticsSalesScreenState();
}

class _LogisticsSalesScreenState extends ConsumerState<LogisticsSalesScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  String? _yearMonth;

  // 카테고리 탭 목록
  final List<LogisticsCategory> _categories = [
    LogisticsCategory.normal,
    LogisticsCategory.ramen,
    LogisticsCategory.frozen,
  ];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: _categories.length, vsync: this);

    // 탭 변경 리스너
    _tabController.addListener(() {
      if (!_tabController.indexIsChanging) {
        _onTabChanged(_tabController.index);
      }
    });

    // 화면 로드 시 전체 카테고리 조회
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _fetchSales();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  /// 물류매출 조회
  void _fetchSales({LogisticsCategory? category}) {
    ref.read(logisticsSalesProvider.notifier).fetchSales(
          yearMonth: _yearMonth,
          category: category,
        );
  }

  /// 탭 변경 시 카테고리별 조회
  void _onTabChanged(int index) {
    final category = _categories[index];
    _fetchSales(category: category);
  }

  /// 필터 초기화
  void _resetFilter() {
    setState(() {
      _yearMonth = null;
    });
    _tabController.index = 0; // 첫 번째 탭으로 이동
    ref.read(logisticsSalesProvider.notifier).resetFilter();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(logisticsSalesProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('물류매출 조회'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _resetFilter,
            tooltip: '필터 초기화',
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          tabs: _categories.map((category) {
            return Tab(
              text: category.displayName,
              icon: _getCategoryIcon(category),
            );
          }).toList(),
        ),
      ),
      body: Column(
        children: [
          // 필터 영역
          _buildFilterSection(),

          const Divider(height: 1),

          // 탭뷰 영역
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: _categories.map((category) {
                return _buildCategoryTab(state, category);
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  /// 카테고리별 아이콘
  Icon _getCategoryIcon(LogisticsCategory category) {
    switch (category) {
      case LogisticsCategory.normal:
        return const Icon(Icons.inventory_2);
      case LogisticsCategory.ramen:
        return const Icon(Icons.ramen_dining);
      case LogisticsCategory.frozen:
        return const Icon(Icons.ac_unit);
    }
  }

  /// 필터 섹션 UI
  Widget _buildFilterSection() {
    final state = ref.watch(logisticsSalesProvider);

    return Container(
      padding: const EdgeInsets.all(16),
      color: Colors.grey[50],
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 년월 선택기
          _buildYearMonthPicker(),

          const SizedBox(height: 12),

          // 당월/이전월 상태 표시
          _buildMonthTypeIndicator(state),

          const SizedBox(height: 16),

          // 조회 버튼
          ElevatedButton.icon(
            onPressed: () => _fetchSales(
              category: _categories[_tabController.index],
            ),
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

  /// 년월 선택기
  Widget _buildYearMonthPicker() {
    final state = ref.watch(logisticsSalesProvider);
    final displayYearMonth = _yearMonth ?? state.filter.yearMonth;

    return Row(
      children: [
        const Icon(Icons.calendar_today, size: 20),
        const SizedBox(width: 8),
        const Text(
          '조회 년월',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: InkWell(
            onTap: () => _selectYearMonth(context),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey),
                borderRadius: BorderRadius.circular(4),
              ),
              child: Text(
                _formatYearMonth(displayYearMonth),
                style: const TextStyle(fontSize: 16),
              ),
            ),
          ),
        ),
      ],
    );
  }

  /// 년월 포맷팅
  String _formatYearMonth(String yearMonth) {
    if (yearMonth.length != 6) return yearMonth;
    final year = yearMonth.substring(0, 4);
    final month = yearMonth.substring(4, 6);
    return '$year년 $month월';
  }

  /// 년월 선택 다이얼로그
  Future<void> _selectYearMonth(BuildContext context) async {
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
      firstDate: DateTime(now.year - 3, 1),
      lastDate: now,
      initialDatePickerMode: DatePickerMode.year,
    );

    if (picked != null) {
      setState(() {
        _yearMonth = '${picked.year}${picked.month.toString().padLeft(2, '0')}';
      });
    }
  }

  /// 당월/이전월 상태 표시
  Widget _buildMonthTypeIndicator(dynamic state) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: state.isCurrentMonth ? Colors.green[50] : Colors.blue[50],
        borderRadius: BorderRadius.circular(4),
        border: Border.all(
          color: state.isCurrentMonth ? Colors.green : Colors.blue,
        ),
      ),
      child: Row(
        children: [
          Icon(
            state.isCurrentMonth ? Icons.access_time : Icons.check_circle,
            size: 16,
            color: state.isCurrentMonth ? Colors.green : Colors.blue,
          ),
          const SizedBox(width: 8),
          Text(
            state.isCurrentMonth ? '당월 물류예상실적' : '이전월 ABC물류배부 마감실적',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.bold,
              color: state.isCurrentMonth ? Colors.green[900] : Colors.blue[900],
            ),
          ),
        ],
      ),
    );
  }

  /// 카테고리별 탭 내용
  Widget _buildCategoryTab(dynamic state, LogisticsCategory category) {
    // 로딩 중
    if (state.isLoading) {
      return const Center(child: LoadingIndicator());
    }

    // 에러 발생
    if (state.errorMessage != null) {
      return ErrorView(
        message: state.errorMessage!,
        onRetry: () => _fetchSales(category: category),
      );
    }

    // 데이터가 없는 경우
    if (state.sales.isEmpty) {
      return const Center(
        child: Text(
          '조회된 데이터가 없습니다.',
          style: TextStyle(fontSize: 16, color: Colors.grey),
        ),
      );
    }

    // 카테고리별 데이터 필터링
    final categorySales =
        state.sales.where((s) => s.category == category).toList();

    if (categorySales.isEmpty) {
      return Center(
        child: Text(
          '${category.displayName} 카테고리 데이터가 없습니다.',
          style: const TextStyle(fontSize: 16, color: Colors.grey),
        ),
      );
    }

    return SingleChildScrollView(
      child: Column(
        children: [
          // 합계 카드
          _buildSummaryCard(categorySales),

          const Divider(height: 1),

          // 전년 대비 차트
          _buildChart(categorySales),

          const Divider(height: 1),

          // 물류매출 테이블
          LogisticsSalesTable(
            salesList: categorySales,
            onTap: (sales) {
              // 향후 상세 화면으로 이동 가능
              debugPrint('Tapped: ${sales.category.displayName} ${sales.yearMonth}');
            },
          ),
        ],
      ),
    );
  }

  /// 전년 대비 차트
  Widget _buildChart(List<LogisticsSales> salesList) {
    if (salesList.isEmpty) {
      return const SizedBox.shrink();
    }

    // 차트 데이터 변환
    final chartData = salesList.map((sales) {
      return SalesChartData(
        label: _formatYearMonth(sales.yearMonth),
        currentAmount: sales.currentAmount.toDouble(),
        previousYearAmount: sales.previousYearAmount.toDouble(),
      );
    }).toList();

    final category = salesList.first.category;

    return SalesChartWidget(
      data: chartData,
      title: '${category.displayName} 전년 대비 실적',
      height: 250,
      currentColor: Colors.blue[700]!,
      previousYearColor: Colors.grey[400]!,
    );
  }

  /// 합계 카드
  Widget _buildSummaryCard(List<LogisticsSales> salesList) {
    final totalCurrent =
        salesList.fold<int>(0, (sum, s) => sum + s.currentAmount);
    final totalPrevious =
        salesList.fold<int>(0, (sum, s) => sum + s.previousYearAmount);
    final totalDiff = salesList.fold<int>(0, (sum, s) => sum + s.difference);
    final growthRate = totalPrevious > 0
        ? ((totalCurrent - totalPrevious) / totalPrevious) * 100
        : null;

    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.blue[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.blue[200]!),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '합계',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),
          _buildSummaryRow('당해 실적', totalCurrent),
          const SizedBox(height: 8),
          _buildSummaryRow('전년 실적', totalPrevious),
          const SizedBox(height: 8),
          _buildSummaryRow(
            '증감',
            totalDiff,
            color: totalDiff >= 0 ? Colors.red : Colors.blue,
          ),
          if (growthRate != null) ...[
            const SizedBox(height: 8),
            _buildGrowthRateRow(growthRate),
          ],
        ],
      ),
    );
  }

  /// 합계 행
  Widget _buildSummaryRow(String label, int amount, {Color? color}) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(fontSize: 14)),
        Text(
          NumberFormat('#,###').format(amount),
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }

  /// 증감율 행
  Widget _buildGrowthRateRow(double rate) {
    final isPositive = rate >= 0;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        const Text('증감율', style: TextStyle(fontSize: 14)),
        Row(
          children: [
            Icon(
              isPositive ? Icons.arrow_upward : Icons.arrow_downward,
              size: 16,
              color: isPositive ? Colors.red : Colors.blue,
            ),
            const SizedBox(width: 4),
            Text(
              '${rate.toStringAsFixed(2)}%',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: isPositive ? Colors.red : Colors.blue,
              ),
            ),
          ],
        ),
      ],
    );
  }
}
