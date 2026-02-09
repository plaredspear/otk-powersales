import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../providers/target_provider.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/common/error_view.dart';

/// 목표/진도율 대시보드 화면
///
/// 거래처별 월 목표금액 및 진도율을 표시합니다.
class TargetDashboardScreen extends ConsumerStatefulWidget {
  const TargetDashboardScreen({super.key});

  @override
  ConsumerState<TargetDashboardScreen> createState() =>
      _TargetDashboardScreenState();
}

class _TargetDashboardScreenState
    extends ConsumerState<TargetDashboardScreen> {
  final NumberFormat currencyFormat = NumberFormat('#,###');

  @override
  void initState() {
    super.initState();
    // 화면 진입 시 데이터 로드
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(targetProvider.notifier).fetchTargets();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(targetProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('목표/진도율'),
        actions: [
          // 새로고침 버튼
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              ref.read(targetProvider.notifier).refresh();
            },
            tooltip: '새로고침',
          ),
          // 필터 초기화 버튼
          IconButton(
            icon: const Icon(Icons.filter_alt_off),
            onPressed: state.filter.category != null ||
                    state.filter.customerCode != null ||
                    state.filter.onlyInsufficient
                ? () {
                    ref.read(targetProvider.notifier).clearFilter();
                  }
                : null,
            tooltip: '필터 초기화',
          ),
        ],
      ),
      body: Column(
        children: [
          // 년월 선택 및 필터
          _buildFilterSection(state),
          // 통계 요약
          _buildStatisticsSummary(state),
          // 목표 목록
          Expanded(
            child: _buildTargetList(state),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // TODO: 목표 추가 화면으로 이동
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('목표 추가 기능 준비 중')),
          );
        },
        tooltip: '목표 추가',
        child: const Icon(Icons.add),
      ),
    );
  }

  /// 필터 섹션
  Widget _buildFilterSection(state) {
    return Container(
      padding: const EdgeInsets.all(16),
      color: Colors.grey[100],
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 년월 선택
          Row(
            children: [
              const Icon(Icons.calendar_today, size: 20),
              const SizedBox(width: 8),
              Text(
                '${state.filter.yearMonth.substring(0, 4)}년 ${state.filter.yearMonth.substring(4, 6)}월',
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const Spacer(),
              IconButton(
                icon: const Icon(Icons.chevron_left),
                onPressed: () => _changeMonth(-1),
                tooltip: '이전 달',
              ),
              IconButton(
                icon: const Icon(Icons.chevron_right),
                onPressed: () => _changeMonth(1),
                tooltip: '다음 달',
              ),
            ],
          ),
          const SizedBox(height: 12),
          // 필터 칩
          Wrap(
            spacing: 8,
            children: [
              // 카테고리 필터
              FilterChip(
                label: Text(state.filter.category ?? '전체 카테고리'),
                selected: state.filter.category != null,
                onSelected: (selected) {
                  _showCategoryFilter();
                },
              ),
              // 진도율 부족 필터
              FilterChip(
                label: const Text('진도율 부족'),
                selected: state.filter.onlyInsufficient,
                onSelected: (selected) {
                  ref.read(targetProvider.notifier).toggleInsufficientFilter();
                },
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// 통계 요약 카드
  Widget _buildStatisticsSummary(state) {
    if (state.isLoading || state.overallProgress == null) {
      return const SizedBox.shrink();
    }

    return Container(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          // 전체 진도율
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  const Text(
                    '전체 진도율',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '${state.overallProgress!.formattedPercentage}%',
                    style: TextStyle(
                      fontSize: 32,
                      fontWeight: FontWeight.bold,
                      color: state.overallProgress!.color,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      _buildStatItem(
                        '목표',
                        currencyFormat.format(state.totalTargetAmount),
                        Colors.blue,
                      ),
                      _buildStatItem(
                        '실적',
                        currencyFormat.format(state.totalActualAmount),
                        Colors.green,
                      ),
                      _buildStatItem(
                        '차액',
                        currencyFormat.format(state.overallProgress!.difference),
                        state.overallProgress!.color,
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          // 통계 카드
          Row(
            children: [
              Expanded(
                child: _buildCountCard(
                  '초과',
                  state.exceededCount,
                  Colors.green,
                  Icons.trending_up,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildCountCard(
                  '달성',
                  state.achievedCount,
                  Colors.blue,
                  Icons.check_circle,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildCountCard(
                  '부족',
                  state.insufficientCount,
                  Colors.red,
                  Icons.trending_down,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// 통계 항목
  Widget _buildStatItem(String label, String value, Color color) {
    return Column(
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            color: Colors.grey,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }

  /// 개수 카드
  Widget _buildCountCard(String label, int count, Color color, IconData icon) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Icon(icon, color: color, size: 24),
            const SizedBox(height: 4),
            Text(
              label,
              style: const TextStyle(fontSize: 12),
            ),
            Text(
              '$count건',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 목표 목록
  Widget _buildTargetList(state) {
    if (state.isLoading) {
      return const LoadingIndicator();
    }

    if (state.errorMessage != null) {
      return ErrorView(
        message: state.errorMessage!,
        onRetry: () {
          ref.read(targetProvider.notifier).fetchTargets();
        },
      );
    }

    final targets = state.filteredTargets;

    if (targets.isEmpty) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.inbox, size: 64, color: Colors.grey),
            SizedBox(height: 16),
            Text(
              '목표가 없습니다',
              style: TextStyle(fontSize: 16, color: Colors.grey),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      itemCount: targets.length,
      itemBuilder: (context, index) {
        final target = targets[index];
        final progress = state.progressList[target.id];

        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: progress?.color ?? Colors.grey,
              child: Text(
                progress?.formattedPercentage ?? '0.0',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            title: Text(
              target.customerName,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (target.category != null)
                  Text(
                    target.category!,
                    style: const TextStyle(fontSize: 12),
                  ),
                const SizedBox(height: 4),
                Text(
                  '목표: ${currencyFormat.format(target.targetAmount)}원',
                  style: const TextStyle(fontSize: 12),
                ),
                Text(
                  '실적: ${currencyFormat.format(target.actualAmount)}원',
                  style: const TextStyle(fontSize: 12),
                ),
              ],
            ),
            trailing: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  progress?.statusDisplayName ?? '-',
                  style: TextStyle(
                    color: progress?.color ?? Colors.grey,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  '${currencyFormat.format(progress?.difference ?? 0)}원',
                  style: TextStyle(
                    fontSize: 12,
                    color: progress?.color ?? Colors.grey,
                  ),
                ),
              ],
            ),
            onTap: () {
              // TODO: 목표 상세 화면으로 이동
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('${target.customerName} 상세 (준비 중)')),
              );
            },
          ),
        );
      },
    );
  }

  /// 년월 변경
  void _changeMonth(int delta) {
    final currentYearMonth = ref.read(targetProvider).filter.yearMonth;
    final year = int.parse(currentYearMonth.substring(0, 4));
    final month = int.parse(currentYearMonth.substring(4, 6));

    final newDate = DateTime(year, month + delta, 1);
    final newYearMonth =
        '${newDate.year}${newDate.month.toString().padLeft(2, '0')}';

    ref.read(targetProvider.notifier).changeYearMonth(newYearMonth);
  }

  /// 카테고리 필터 표시
  void _showCategoryFilter() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('카테고리 선택'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              title: const Text('전체'),
              onTap: () {
                ref.read(targetProvider.notifier).filterByCategory(null);
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: const Text('전산매출'),
              onTap: () {
                ref.read(targetProvider.notifier).filterByCategory('전산매출');
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: const Text('POS매출'),
              onTap: () {
                ref.read(targetProvider.notifier).filterByCategory('POS매출');
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: const Text('물류매출'),
              onTap: () {
                ref.read(targetProvider.notifier).filterByCategory('물류매출');
                Navigator.pop(context);
              },
            ),
          ],
        ),
      ),
    );
  }
}
