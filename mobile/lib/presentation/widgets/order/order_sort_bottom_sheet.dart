import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order.dart';

/// 주문 정렬 BottomSheet 위젯
///
/// 6가지 정렬 옵션을 목록으로 표시하며, 현재 선택된 옵션에 체크 표시합니다.
class OrderSortBottomSheet extends StatelessWidget {
  /// 현재 선택된 정렬 타입
  final OrderSortType currentSortType;

  /// 정렬 선택 콜백
  final ValueChanged<OrderSortType> onSortChanged;

  const OrderSortBottomSheet({
    super.key,
    required this.currentSortType,
    required this.onSortChanged,
  });

  /// BottomSheet 표시
  static Future<void> show(
    BuildContext context, {
    required OrderSortType currentSortType,
    required ValueChanged<OrderSortType> onSortChanged,
  }) {
    return showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusXl),
        ),
      ),
      builder: (sheetContext) => OrderSortBottomSheet(
        currentSortType: currentSortType,
        onSortChanged: (sortType) {
          Navigator.of(sheetContext).pop();
          onSortChanged(sortType);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 핸들 바
          Center(
            child: Container(
              margin: const EdgeInsets.only(top: AppSpacing.sm),
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.divider,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          // 제목
          Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Text(
              '정렬',
              style: AppTypography.headlineSmall,
              textAlign: TextAlign.center,
            ),
          ),
          const Divider(height: 1, color: AppColors.divider),
          // 정렬 옵션 목록
          ...OrderSortType.values.map((sortType) {
            final isSelected = sortType == currentSortType;
            return InkWell(
              onTap: () => onSortChanged(sortType),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.lg,
                  vertical: AppSpacing.md,
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        sortType.displayName,
                        style: AppTypography.bodyMedium.copyWith(
                          color: isSelected
                              ? AppColors.otokiBlue
                              : AppColors.textPrimary,
                          fontWeight: isSelected
                              ? FontWeight.w600
                              : FontWeight.w400,
                        ),
                      ),
                    ),
                    if (isSelected)
                      const Icon(
                        Icons.check,
                        color: AppColors.otokiBlue,
                        size: 20,
                      ),
                  ],
                ),
              ),
            );
          }),
          const SizedBox(height: AppSpacing.lg),
        ],
      ),
    );
  }
}
