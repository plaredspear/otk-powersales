import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

class OrderedItemList extends StatelessWidget {
  final List<OrderedItem> items;

  const OrderedItemList({
    super.key,
    required this.items,
  });

  /// 취소된 라인 수 (isCancelled). 결품(isOutOfStock)은 취소가 아니므로 제외.
  int get _cancelledCount => items.where((i) => i.isCancelled).length;

  // 레거시 view.jsp:274 동등 — 박스 수량은 소수 2자리까지(#,###.##).
  String _formatBoxes(double boxes) {
    return NumberFormat('#,##0.##').format(boxes);
  }

  String _formatPieces(int pieces) {
    return NumberFormat('#,###').format(pieces);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(
          color: AppColors.border,
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                '주문한 제품 (${items.length})',
                style: AppTypography.headlineSmall.copyWith(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.bold,
                ),
              ),
              // 취소 건수 — 취소 라인이 1건 이상일 때만 노출 (예: "중 취소 3건").
              if (_cancelledCount > 0) ...[
                SizedBox(width: AppSpacing.sm),
                Text(
                  '중 취소 $_cancelledCount건',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.error,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ],
          ),
          SizedBox(height: AppSpacing.md),
          if (items.isEmpty)
            Center(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: AppSpacing.xl),
                child: Text(
                  '주문한 제품이 없습니다',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),
              ),
            )
          else
            ...items.asMap().entries.map((entry) {
              final index = entry.key;
              final item = entry.value;
              return Column(
                children: [
                  if (index > 0)
                    Divider(
                      height: AppSpacing.lg,
                      color: AppColors.divider,
                    ),
                  _buildItemRow(item),
                ],
              );
            }),
        ],
      ),
    );
  }

  /// 인라인 배지 (pill) — 제품명 앞에 표시. 배지/사유 판정은 서버 필드만 사용(코드 분류 금지, Spec #845 §3).
  Widget _buildBadge(String label, Color background) {
    return Container(
      margin: EdgeInsets.only(right: AppSpacing.xs),
      padding: EdgeInsets.symmetric(
        horizontal: AppSpacing.xs,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        label,
        style: AppTypography.labelSmall.copyWith(
          color: AppColors.white,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }

  Widget _buildItemRow(OrderedItem item) {
    // 결품/SAP취소됨(실제)·마이그레이션 취소 라인은 짙은 회색 처리로 시각 구분(Spec #845 §2).
    final isGrayed =
        item.isCancelledBySap || item.isOutOfStock || item.isCancelled;
    final nameColor =
        isGrayed ? AppColors.textSecondary : AppColors.textPrimary;

    // 결품/SAP취소됨 은 상호배타(서버가 한 라인당 하나만 채움). 방어적으로 둘 다 true 여도 결품 우선.
    final showOutOfStock = item.isOutOfStock;
    final showCancelledBySap = item.isCancelledBySap && !item.isOutOfStock;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              flex: 3,
              child: RichText(
                text: TextSpan(
                  children: [
                    // 취소요청(로컬 흔적) — SAP 반영 여부와 독립. 주황(경고).
                    if (item.isCancelRequested)
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: _buildBadge('취소요청', AppColors.warning),
                      ),
                    // SAP 실제 취소 — 네이비.
                    if (showCancelledBySap)
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: _buildBadge('SAP취소됨', AppColors.secondary),
                      ),
                    // 결품 — 회색.
                    if (showOutOfStock)
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: _buildBadge('결품', AppColors.textTertiary),
                      ),
                    // 마이그레이션 과거 취소('X') — 기존 '[주문 취소]' 접두 유지.
                    if (item.isCancelled)
                      TextSpan(
                        text: '[주문 취소] ',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.error,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    // 레거시 view.jsp:273 동등 — 제품명 (제품코드) 순서.
                    TextSpan(
                      text: '${item.productName} (${item.productCode})',
                      style: AppTypography.bodyMedium.copyWith(
                        color: nameColor,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            SizedBox(width: AppSpacing.md),
            Expanded(
              flex: 2,
              child: Text(
                '${_formatBoxes(item.totalQuantityBoxes)} BOX (${_formatPieces(item.totalQuantityPieces)}개)',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
                textAlign: TextAlign.right,
              ),
            ),
          ],
        ),
        // 하단 사유 1줄 — 서버가 '{코드} {설명}' 완성 문자열 전달(모바일 라벨만 부착).
        // 결품/취소 상호배타이나 방어적으로 결품 우선.
        if (showOutOfStock && item.outOfStockReason != null) ...[
          SizedBox(height: AppSpacing.xs),
          Text(
            '결품사유: ${item.outOfStockReason}',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ] else if (showCancelledBySap && item.cancelReason != null) ...[
          SizedBox(height: AppSpacing.xs),
          Text(
            '취소사유: ${item.cancelReason}',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ],
    );
  }
}
