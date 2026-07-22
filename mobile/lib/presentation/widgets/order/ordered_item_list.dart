import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';
import 'cancel_badge_info_sheet.dart';

class OrderedItemList extends StatelessWidget {
  final List<OrderedItem> items;

  const OrderedItemList({
    super.key,
    required this.items,
  });

  /// 취소된 라인 수 — 주문 취소(SAP 상세 확정 isCancelledBySap 또는 마이그레이션 isCancelled) 기준.
  /// 결품(isOutOfStock)은 취소가 아니므로 제외(결품 우선).
  int get _cancelledCount => items
      .where((i) => (i.isCancelledBySap || i.isCancelled) && !i.isOutOfStock)
      .length;

  /// 취소요청/주문 취소 배지가 하나라도 있으면 헤더에 설명 info 아이콘을 노출.
  bool get _hasCancelInfo => items.any((i) =>
      i.isCancelRequested ||
      ((i.isCancelledBySap || i.isCancelled) && !i.isOutOfStock));

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
            children: [
              Expanded(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.baseline,
                  textBaseline: TextBaseline.alphabetic,
                  children: [
                    Flexible(
                      child: Text(
                        '주문한 제품 (${items.length})',
                        style: AppTypography.headlineSmall.copyWith(
                          color: AppColors.textPrimary,
                          fontWeight: FontWeight.bold,
                        ),
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
              ),
              // 취소 배지 설명 — 취소요청/주문 취소 배지가 있을 때만 노출.
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

  /// 취소요청 표식 — 문구 없는 색 동그라미(8px). 제품명 앞에 표시(2026-07-22 사용자 결정).
  Widget _buildDot(Color color) {
    return Container(
      key: const ValueKey('cancelRequestedDot'),
      margin: EdgeInsets.only(right: AppSpacing.xs),
      width: 8,
      height: 8,
      decoration: BoxDecoration(color: color, shape: BoxShape.circle),
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
    // 결품/주문취소(SAP 상세 확정·마이그레이션) 라인은 짙은 회색 처리로 시각 구분(Spec #845 §2).
    final isGrayed =
        item.isCancelledBySap || item.isOutOfStock || item.isCancelled;
    final nameColor =
        isGrayed ? AppColors.textSecondary : AppColors.textPrimary;

    // 배지 2종으로 통합(사용자 결정, 2026-07-22):
    //  - 취소요청 : 해당 제품 SAP 주문취소 API 요청 성공(로컬 흔적, isCancelRequested).
    //  - 주문 취소 : SAP 주문 상세에서 해당 제품이 취소된 경우(확정) — SAP 상세 취소(isCancelledBySap)
    //               또는 마이그레이션 취소('X', isCancelled)를 하나로 묶는다(기존 SAP취소됨 배지 +
    //               [주문 취소] 접두를 단일 '주문 취소' 배지로 대체).
    // 결품이 최우선이며 주문취소와 상호배타(방어적으로 둘 다 true 여도 결품 우선).
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
                    // 취소요청 — SAP 주문취소 API 요청 성공(로컬). 문구 없이 주황색 동그라미.
                    // 의미는 헤더 info(취소 상태 안내)에서 안내.
                    if (item.isCancelRequested)
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: _buildDot(AppColors.warning),
                      ),
                    // 주문 취소 — SAP 주문 상세에서 취소 확정(처리 완료). 빨강.
                    if (showCancelled)
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: _buildBadge('주문 취소', AppColors.error),
                      ),
                    // 결품 — 회색.
                    if (showOutOfStock)
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: _buildBadge('결품', AppColors.textTertiary),
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
}
