import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

/// 매출 차트 데이터 모델
class SalesChartData {
  /// 레이블 (예: 년월, 카테고리명)
  final String label;

  /// 당해 실적
  final double currentAmount;

  /// 전년 실적
  final double previousYearAmount;

  const SalesChartData({
    required this.label,
    required this.currentAmount,
    required this.previousYearAmount,
  });
}

/// 매출 차트 위젯
///
/// fl_chart 기반 전년 대비 바 차트 위젯입니다.
/// 당해 실적과 전년 실적을 나란히 표시합니다.
class SalesChartWidget extends StatelessWidget {
  /// 차트 데이터 목록
  final List<SalesChartData> data;

  /// 차트 제목
  final String? title;

  /// 차트 높이
  final double height;

  /// 당해 실적 색상
  final Color currentColor;

  /// 전년 실적 색상
  final Color previousYearColor;

  const SalesChartWidget({
    super.key,
    required this.data,
    this.title,
    this.height = 300,
    this.currentColor = Colors.blue,
    this.previousYearColor = Colors.grey,
  });

  @override
  Widget build(BuildContext context) {
    if (data.isEmpty) {
      return SizedBox(
        height: height,
        child: const Center(
          child: Text(
            '차트 데이터가 없습니다',
            style: TextStyle(fontSize: 14, color: Colors.grey),
          ),
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (title != null) ...[
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Text(
              title!,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0),
          child: _buildLegend(),
        ),
        const SizedBox(height: 16),
        SizedBox(
          height: height,
          child: Padding(
            padding: const EdgeInsets.only(right: 16, left: 8, bottom: 16),
            child: BarChart(
              _buildBarChartData(),
            ),
          ),
        ),
      ],
    );
  }

  /// 범례 위젯
  Widget _buildLegend() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        _buildLegendItem('당해 실적', currentColor),
        const SizedBox(width: 24),
        _buildLegendItem('전년 실적', previousYearColor),
      ],
    );
  }

  /// 범례 아이템
  Widget _buildLegendItem(String label, Color color) {
    return Row(
      children: [
        Container(
          width: 16,
          height: 16,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(4),
          ),
        ),
        const SizedBox(width: 8),
        Text(
          label,
          style: const TextStyle(fontSize: 12),
        ),
      ],
    );
  }

  /// BarChart 데이터 생성
  BarChartData _buildBarChartData() {
    // 최대값 계산 (Y축 범위 설정용)
    double maxY = 0;
    for (var item in data) {
      final max =
          item.currentAmount > item.previousYearAmount ? item.currentAmount : item.previousYearAmount;
      if (max > maxY) maxY = max;
    }

    // 여유 공간 추가 (최대값의 110%)
    // 최소값 1000000 (1M) 설정하여 0일 때도 차트가 보이도록
    maxY = maxY > 0 ? maxY * 1.1 : 1000000;

    return BarChartData(
      alignment: BarChartAlignment.spaceAround,
      maxY: maxY,
      minY: 0,
      barTouchData: BarTouchData(
        enabled: true,
        touchTooltipData: BarTouchTooltipData(
          getTooltipItem: (group, groupIndex, rod, rodIndex) {
            final item = data[groupIndex];
            final isCurrent = rodIndex == 0;
            final amount = isCurrent ? item.currentAmount : item.previousYearAmount;
            final label = isCurrent ? '당해' : '전년';

            return BarTooltipItem(
              '$label\n${NumberFormat('#,###').format(amount)}',
              const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 12,
              ),
            );
          },
        ),
      ),
      titlesData: FlTitlesData(
        show: true,
        bottomTitles: AxisTitles(
          sideTitles: SideTitles(
            showTitles: true,
            reservedSize: 40,
            getTitlesWidget: (value, meta) {
              final index = value.toInt();
              if (index < 0 || index >= data.length) {
                return const SizedBox.shrink();
              }
              return Padding(
                padding: const EdgeInsets.only(top: 8.0),
                child: Text(
                  data[index].label,
                  style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
              );
            },
          ),
        ),
        leftTitles: AxisTitles(
          sideTitles: SideTitles(
            showTitles: true,
            reservedSize: 60,
            getTitlesWidget: (value, meta) {
              // 백만 단위로 표시
              final million = value / 1000000;
              return Text(
                '${million.toStringAsFixed(0)}M',
                style: const TextStyle(fontSize: 10),
              );
            },
          ),
        ),
        topTitles: const AxisTitles(
          sideTitles: SideTitles(showTitles: false),
        ),
        rightTitles: const AxisTitles(
          sideTitles: SideTitles(showTitles: false),
        ),
      ),
      gridData: FlGridData(
        show: true,
        drawVerticalLine: false,
        horizontalInterval: maxY / 5,
        getDrawingHorizontalLine: (value) {
          return const FlLine(
            color: Colors.grey,
            strokeWidth: 0.5,
          );
        },
      ),
      borderData: FlBorderData(
        show: true,
        border: const Border(
          bottom: BorderSide(color: Colors.grey),
          left: BorderSide(color: Colors.grey),
        ),
      ),
      barGroups: _buildBarGroups(),
    );
  }

  /// 바 그룹 생성
  List<BarChartGroupData> _buildBarGroups() {
    return data.asMap().entries.map((entry) {
      final index = entry.key;
      final item = entry.value;

      return BarChartGroupData(
        x: index,
        barRods: [
          // 당해 실적
          BarChartRodData(
            toY: item.currentAmount,
            color: currentColor,
            width: 12,
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(4),
              topRight: Radius.circular(4),
            ),
          ),
          // 전년 실적
          BarChartRodData(
            toY: item.previousYearAmount,
            color: previousYearColor,
            width: 12,
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(4),
              topRight: Radius.circular(4),
            ),
          ),
        ],
      );
    }).toList();
  }
}
