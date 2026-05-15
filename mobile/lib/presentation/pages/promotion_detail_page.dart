import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/promotion.dart';
import '../providers/promotion_detail_provider.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/promotion/promotion_amount_text.dart';
import '../widgets/promotion/promotion_employee_list.dart';

/// 행사 상세 페이지
class PromotionDetailPage extends ConsumerStatefulWidget {
  final int promotionId;

  const PromotionDetailPage({super.key, required this.promotionId});

  @override
  ConsumerState<PromotionDetailPage> createState() =>
      _PromotionDetailPageState();
}

class _PromotionDetailPageState extends ConsumerState<PromotionDetailPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(promotionDetailProvider.notifier)
          .loadPromotion(widget.promotionId);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(promotionDetailProvider);

    ref.listen<String?>(
      promotionDetailProvider.select((s) => s.errorMessage),
      (prev, next) {
        if (next != null) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(next)));
          ref.read(promotionDetailProvider.notifier).clearError();
          if (next.contains('권한')) {
            Navigator.of(context).pop();
          }
        }
      },
    );

    return Scaffold(
      appBar: AppBar(title: const Text('행사 상세')),
      body: _buildBody(state),
    );
  }

  Widget _buildBody(PromotionDetailState state) {
    if (state.isLoading && state.detail == null) {
      return const LoadingIndicator(message: '행사 정보를 불러오는 중...');
    }

    if (state.detail == null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.grey[300]),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '행사 정보를 불러올 수 없습니다',
              style: AppTypography.bodyMedium
                  .copyWith(color: AppColors.textSecondary),
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () => ref
                  .read(promotionDetailProvider.notifier)
                  .loadPromotion(widget.promotionId),
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    final detail = state.detail!;
    return SingleChildScrollView(
      padding: AppSpacing.screenAll,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (detail.isClosed) ...[
            _buildClosedBadge(),
            const SizedBox(height: AppSpacing.md),
          ],
          _buildInfoSection(detail),
          const SizedBox(height: AppSpacing.xl),
          _buildProductSection(detail),
          const SizedBox(height: AppSpacing.xl),
          _buildAchievementCard(detail),
          const SizedBox(height: AppSpacing.xl),
          if (detail.employees.isNotEmpty)
            PromotionEmployeeList(employees: detail.employees),
        ],
      ),
    );
  }

  Widget _buildClosedBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: AppColors.textTertiary.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        '마감',
        style: AppTypography.labelMedium.copyWith(
          color: AppColors.textSecondary,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  Widget _buildInfoSection(PromotionDetail detail) {
    return Column(
      children: [
        _infoRow('행사번호', detail.promotionNumber),
        if (detail.promotionType != null)
          _infoRow('행사유형', detail.promotionType!),
        if (detail.accountName != null)
          _infoRow('거래처', detail.accountName!),
        _infoRow('기간', '${detail.startDate} ~ ${detail.endDate}'),
        if (detail.standLocation != null)
          _infoRow('매대위치', detail.standLocation!),
      ],
    );
  }

  Widget _buildProductSection(PromotionDetail detail) {
    final hasProduct = detail.primaryProductName != null ||
        detail.otherProduct != null ||
        detail.category != null ||
        detail.message != null;
    if (!hasProduct) return const SizedBox.shrink();

    return Column(
      children: [
        if (detail.primaryProductName != null)
          _infoRow('대표상품', detail.primaryProductName!),
        if (detail.otherProduct != null)
          _infoRow('기타상품', detail.otherProduct!),
        if (detail.category != null) _infoRow('카테고리', detail.category!),
        if (detail.message != null) _infoRow('메시지', detail.message!),
      ],
    );
  }

  Widget _buildAchievementCard(PromotionDetail detail) {
    final rate = detail.achievementRate;
    final rateText = rate != null ? '${rate.toStringAsFixed(1)}%' : '-';

    return Container(
      width: double.infinity,
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('실적 현황', style: AppTypography.headlineSmall),
          const SizedBox(height: AppSpacing.md),
          _achievementRow(
              '목표', _formatWon(detail.targetAmount)),
          const SizedBox(height: AppSpacing.xs),
          _achievementRow(
              '실적', _formatWon(detail.actualAmount)),
          const SizedBox(height: AppSpacing.xs),
          _achievementRow('달성률', rateText),
        ],
      ),
    );
  }

  Widget _infoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(
              label,
              style: AppTypography.bodyMedium
                  .copyWith(color: AppColors.textSecondary),
            ),
          ),
          Expanded(
            child: Text(value, style: AppTypography.bodyMedium),
          ),
        ],
      ),
    );
  }

  Widget _achievementRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label,
            style: AppTypography.bodyMedium
                .copyWith(color: AppColors.textSecondary)),
        Text(value,
            style: AppTypography.bodyMedium
                .copyWith(fontWeight: FontWeight.w600)),
      ],
    );
  }

  String _formatWon(int? amount) {
    if (amount == null) return '-';
    final formatted = PromotionAmountText.formatAmount(amount);
    // formatAmount already appends "만" or "원", so just add "원" for 만원 unit
    if (formatted.endsWith('만')) return '$formatted원';
    return formatted;
  }
}
