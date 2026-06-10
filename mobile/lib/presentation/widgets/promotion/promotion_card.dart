import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/promotion.dart';

/// 행사 목록 카드 위젯.
///
/// 레거시(heroku `promotion/event/list.jsp`) 카드 정합 — 두 줄 구성:
/// - 1행: `[행사타입] 행사명` (bold). 행사명은 SF formula `DKRetail__PromotionName__c`
///   (`제품온도타입(대표제품명)`) 파생값으로 백엔드에서 내려온다.
/// - 2행: `기간 YYYY.MM.DD(요일) ~ YYYY.MM.DD(요일)`
/// - 탭 시 상세("행사 내용 상세") 진입.
///
/// 레거시 리스트 카드는 행사번호·거래처명·매대·실적/목표·마감 상태를 노출하지 않는다
/// (해당 정보는 상세화면 전용).
class PromotionCard extends StatelessWidget {
  final PromotionItem item;
  final VoidCallback? onTap;

  const PromotionCard({super.key, required this.item, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: AppSpacing.cardBorderRadius,
        child: Container(
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: AppSpacing.cardBorderRadius,
            border: Border.all(color: AppColors.border),
          ),
          padding: AppSpacing.cardPadding,
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _title(),
                      style: AppTypography.headlineSmall,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      '기간 ${_formatDate(item.startDate)} ~ ${_formatDate(item.endDate)}',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: AppSpacing.xs),
              const Icon(Icons.chevron_right,
                  size: AppSpacing.iconSize, color: AppColors.textTertiary),
            ],
          ),
        ),
      ),
    );
  }

  /// `[행사타입] 행사명` (레거시: `['+PromotionType+'] '+PromotionName`).
  String _title() {
    final type = item.promotionType;
    final name = item.promotionName ?? '';
    return (type != null && type.isNotEmpty) ? '[$type] $name' : name;
  }

  /// `2026-06-01` → `2026.06.01(월)` (레거시 moment 포맷 + 한글 요일 정합).
  String _formatDate(String isoDate) {
    final date = DateTime.tryParse(isoDate);
    if (date == null) return isoDate;
    const weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final dotted = isoDate.replaceAll('-', '.');
    return '$dotted(${weekdays[date.weekday - 1]})';
  }
}
