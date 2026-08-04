import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/order_detail.dart';

/// 승인된 품목 수 안내 바텀시트 (2026-08-04 사용자 요청)
///
/// 주문 상세 헤더의 "승인된 품목 수" 옆 info 아이콘 탭 시 표시.
/// 주문 라인 전량([OrderItemCountSummary.orderedCount]) 대비 출고 확정 수를 보여주고,
/// 취소 / 미납 / 반려는 **0 이 아닐 때만** 행으로 나열한다 (해당 없는 분류로 화면을 채우지 않음).
///
/// 각주는 두 가지 어긋남을 설명한다:
///  1. 출고확정 + 취소 + 미납 + 반려 < 주문 → 아직 납품문서가 생성되지 않은 라인이 남아있음.
///  2. 헤더 "주문한 품목 수"(반려·미납 제외 목록 카운트) ≠ 여기의 "주문 N개"(라인 전량).
class OrderItemCountInfoSheet extends StatelessWidget {
  final OrderItemCountSummary summary;

  const OrderItemCountInfoSheet({
    super.key,
    required this.summary,
  });

  /// 바텀시트로 집계 내역 표시
  static void show(BuildContext context, OrderItemCountSummary summary) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => OrderItemCountInfoSheet(summary: summary),
    );
  }

  @override
  Widget build(BuildContext context) {
    // 미집계 = 주문 라인 중 어느 분류에도 배정되지 않은 수 (납품문서 미생성). 음수 방어.
    final unclassified = (summary.orderedCount -
            summary.confirmedCount -
            summary.cancelledCount -
            summary.outOfStockCount -
            summary.rejectedCount)
        .clamp(0, summary.orderedCount);

    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.6,
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 핸들 바
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.divider,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 16),

          const Text(
            '승인된 품목 수 안내',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 16),
          const Divider(height: 1),
          const SizedBox(height: 16),

          Flexible(
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '주문 ${summary.orderedCount}개 중 '
                    '출고 확정 ${summary.confirmedCount}개',
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (summary.cancelledCount > 0)
                    _buildBreakdownRow('취소', summary.cancelledCount),
                  if (summary.outOfStockCount > 0)
                    _buildBreakdownRow('미납', summary.outOfStockCount),
                  if (summary.rejectedCount > 0)
                    _buildBreakdownRow('반려', summary.rejectedCount),
                  if (unclassified > 0)
                    _buildBreakdownRow('출고 확정 전', unclassified),
                  const SizedBox(height: 16),
                  const Text(
                    '※ 납품문서가 생성된 품목만 집계됩니다',
                    style: TextStyle(
                      fontSize: 13,
                      height: 1.4,
                      color: AppColors.textTertiary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    '※ 미납·반려 품목은 위 "주문한 품목" 목록에서 제외되어 각 전용 영역에 표시됩니다',
                    style: TextStyle(
                      fontSize: 13,
                      height: 1.4,
                      color: AppColors.textTertiary,
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                side: const BorderSide(color: AppColors.border),
              ),
              child: const Text(
                '닫기',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 분류 1행 — "- 취소: 2개"
  Widget _buildBreakdownRow(String label, int count) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Text(
        '- $label: $count개',
        style: const TextStyle(
          fontSize: 15,
          height: 1.4,
          color: AppColors.textSecondary,
        ),
      ),
    );
  }
}
