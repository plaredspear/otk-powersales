import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';
import 'cancel_badge_info_sheet.dart';

class OrderedItemExpandable extends StatelessWidget {
  final List<OrderedItem> items;
  final int itemCount;
  final bool isExpanded;
  final VoidCallback onToggle;

  const OrderedItemExpandable({
    super.key,
    required this.items,
    required this.itemCount,
    required this.isExpanded,
    required this.onToggle,
  });

  /// 취소된 라인 수 — 주문 취소(SAP 상세 확정 isCancelledBySap 또는 마이그레이션 isCancelled) 기준.
  /// 결품(isOutOfStock)은 취소가 아니므로 제외(결품 우선). OrderedItemList 정합.
  int get _cancelledCount => items
      .where((i) => (i.isCancelledBySap || i.isCancelled) && !i.isOutOfStock)
      .length;

  /// 취소요청/주문 취소 배지가 하나라도 있으면 헤더에 설명 info 아이콘을 노출 (OrderedItemList 정합).
  bool get _hasCancelInfo => items.any((i) =>
      i.isCancelRequested ||
      ((i.isCancelledBySap || i.isCancelled) && !i.isOutOfStock));

  /// 취소요청 표식 — 문구 없는 색 동그라미(8px). OrderedItemList._buildDot 정합.
  Widget _buildDot(Color color) {
    return Container(
      key: const ValueKey('cancelRequestedDot'),
      margin: EdgeInsets.only(right: AppSpacing.xs),
      width: 8,
      height: 8,
      decoration: BoxDecoration(color: color, shape: BoxShape.circle),
    );
  }

  /// 인라인 배지 (pill) — OrderedItemList._buildBadge 정합.
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
          InkWell(
            onTap: onToggle,
            borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
            child: Padding(
              padding: EdgeInsets.symmetric(vertical: AppSpacing.xs),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Text(
                        '주문한 제품',
                        style: AppTypography.headlineSmall.copyWith(
                          color: AppColors.textPrimary,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(width: AppSpacing.sm),
                      Text(
                        '$itemCount개',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.textSecondary,
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
                      // 취소 배지 설명 — 토글과 분리된 탭 영역(GestureDetector 가 InkWell 보다 우선).
                      if (_hasCancelInfo)
                        GestureDetector(
                          onTap: () => CancelBadgeInfoSheet.show(context),
                          behavior: HitTestBehavior.opaque,
                          child: Padding(
                            padding: EdgeInsets.only(left: AppSpacing.xs),
                            child: Icon(
                              Icons.info_outline,
                              size: 18,
                              color: AppColors.textTertiary,
                            ),
                          ),
                        ),
                    ],
                  ),
                  Row(
                    children: [
                      Text(
                        isExpanded ? '숨기기' : '제품 보기',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.secondaryDark,
                        ),
                      ),
                      Icon(
                        isExpanded
                            ? Icons.keyboard_arrow_up
                            : Icons.keyboard_arrow_down,
                        color: AppColors.secondaryDark,
                        size: 20,
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          if (isExpanded) ...[
            SizedBox(height: AppSpacing.md),
            Divider(
              height: 1,
              color: AppColors.divider,
            ),
            SizedBox(height: AppSpacing.md),
            _buildItemList(),
          ],
        ],
      ),
    );
  }

  Widget _buildItemList() {
    if (items.isEmpty) {
      return Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: AppSpacing.xl),
          child: Text(
            '주문한 제품이 없습니다',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textTertiary,
            ),
          ),
        ),
      );
    }

    return Column(
      children: items.asMap().entries.map((entry) {
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
      }).toList(),
    );
  }

  Widget _buildItemRow(OrderedItem item) {
    // 취소/결품 제품은 짙은 회색 처리. 배지 체계는 OrderedItemList 와 동일(2배지 통합, 2026-07-22):
    //  - 취소요청 : SAP 주문취소 API 요청 성공(로컬, isCancelRequested).
    //  - 주문 취소 : SAP 주문 상세에서 취소 확정(isCancelledBySap) 또는 마이그레이션 취소(isCancelled).
    //  - 결품 : isOutOfStock (최우선, 주문취소와 상호배타).
    final isGrayed =
        item.isCancelledBySap || item.isOutOfStock || item.isCancelled;
    final nameColor =
        isGrayed ? AppColors.textSecondary : AppColors.textPrimary;

    final showOutOfStock = item.isOutOfStock;
    final showCancelled =
        (item.isCancelledBySap || item.isCancelled) && !item.isOutOfStock;

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
                    // 취소요청 — 문구 없이 주황색 동그라미(OrderedItemList 정합). 의미는 헤더 info 안내.
                    if (item.isCancelRequested)
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: _buildDot(AppColors.warning),
                      ),
                    if (showCancelled)
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: _buildBadge('주문 취소', AppColors.error),
                      ),
                    if (showOutOfStock)
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: _buildBadge('결품', AppColors.textTertiary),
                      ),
                    // 레거시 view.jsp:414 동등 — 제품명 (제품코드) 순서.
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
        if (showOutOfStock && item.outOfStockReason != null) ...[
          SizedBox(height: AppSpacing.xs),
          Text(
            '결품사유: ${item.outOfStockReason}',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ] else if (showCancelled && item.cancelReason != null) ...[
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

  // 레거시 view.jsp:420-438 동등 — 박스 수량은 소수 2자리까지(#,###.##).
  String _formatBoxes(double boxes) {
    return NumberFormat('#,##0.##').format(boxes);
  }

  String _formatPieces(int pieces) {
    return NumberFormat('#,###').format(pieces);
  }
}
