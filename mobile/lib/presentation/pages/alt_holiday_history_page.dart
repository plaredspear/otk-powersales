import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../domain/entities/alternative_holiday.dart';
import '../providers/alt_holiday_provider.dart';

/// 대체휴무 신청 이력 페이지
class AltHolidayHistoryPage extends ConsumerStatefulWidget {
  const AltHolidayHistoryPage({super.key});

  @override
  ConsumerState<AltHolidayHistoryPage> createState() =>
      _AltHolidayHistoryPageState();
}

class _AltHolidayHistoryPageState
    extends ConsumerState<AltHolidayHistoryPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(altHolidayHistoryProvider.notifier).loadHistory();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(altHolidayHistoryProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('대체휴무 신청 이력'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: _buildBody(state),
    );
  }

  Widget _buildBody(AltHolidayHistoryState state) {
    if (state.isLoading && !state.hasLoaded) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state.errorMessage != null && state.items.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 48, color: AppColors.textTertiary),
            const SizedBox(height: AppSpacing.md),
            Text(state.errorMessage!, style: const TextStyle(color: AppColors.textSecondary)),
            const SizedBox(height: AppSpacing.md),
            TextButton(
              onPressed: () =>
                  ref.read(altHolidayHistoryProvider.notifier).loadHistory(),
              child: const Text('다시 시도'),
            ),
          ],
        ),
      );
    }
    if (state.isEmpty) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.event_busy, size: 48, color: AppColors.textTertiary),
            SizedBox(height: AppSpacing.md),
            Text(
              '신청 이력이 없습니다',
              style: TextStyle(
                fontSize: 16,
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: () =>
          ref.read(altHolidayHistoryProvider.notifier).loadHistory(),
      child: ListView.builder(
        padding: const EdgeInsets.all(AppSpacing.lg),
        itemCount: state.items.length,
        itemBuilder: (context, index) {
          return Padding(
            padding: const EdgeInsets.only(bottom: AppSpacing.sm),
            child: _AltHolidayCard(item: state.items[index]),
          );
        },
      ),
    );
  }
}

/// 대체휴무 이력 카드
class _AltHolidayCard extends StatelessWidget {
  final AlternativeHoliday item;

  const _AltHolidayCard({required this.item});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '대상일: ${_formatDate(item.actualWorkDate)}',
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textPrimary,
                ),
              ),
              _StatusBadge(status: item.status),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            '신청일: ${_formatDate(item.targetAltHolidayDate)}',
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.textSecondary,
            ),
          ),
          if (item.confirmAltHolidayDate != null) ...[
            const SizedBox(height: 4),
            Text(
              '확정일: ${_formatDate(item.confirmAltHolidayDate!)}',
              style: const TextStyle(
                fontSize: 13,
                color: AppColors.textSecondary,
              ),
            ),
          ],
          if (item.changeReason != null && item.changeReason!.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              '사유: ${item.changeReason}',
              style: const TextStyle(
                fontSize: 12,
                color: AppColors.textSecondary,
                fontStyle: FontStyle.italic,
              ),
            ),
          ],
        ],
      ),
    );
  }

  String _formatDate(DateTime date) {
    final weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final weekday = weekdays[date.weekday - 1];
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')} ($weekday)';
  }
}

/// 상태 배지 위젯
class _StatusBadge extends StatelessWidget {
  final String status;

  const _StatusBadge({required this.status});

  @override
  Widget build(BuildContext context) {
    final (bgColor, textColor) = _getColors();
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        status,
        style: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: textColor,
        ),
      ),
    );
  }

  (Color, Color) _getColors() {
    switch (status) {
      case '승인':
        return (const Color(0xFFE8F5E9), AppColors.success);
      case '반려':
        return (const Color(0xFFFFEBEE), AppColors.error);
      case '조정':
        return (const Color(0xFFFFF3E0), AppColors.warning);
      default: // 신규
        return (const Color(0xFFF5F5F5), AppColors.textSecondary);
    }
  }
}
