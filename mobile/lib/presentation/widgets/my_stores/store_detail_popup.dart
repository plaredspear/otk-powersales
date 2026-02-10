import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/my_store.dart';

/// 거래처 상세 팝업 위젯
///
/// 거래처 선택 시 노출되는 팝업.
/// 거래처명/코드 표시 + 주문서 현황 / 매출 현황 이동 옵션.
class StoreDetailPopup extends StatelessWidget {
  /// 거래처 정보
  final MyStore store;

  /// 주문서 현황 탭 콜백
  final VoidCallback? onOrderStatusTap;

  /// 매출 현황 탭 콜백
  final VoidCallback? onSalesStatusTap;

  const StoreDetailPopup({
    super.key,
    required this.store,
    this.onOrderStatusTap,
    this.onSalesStatusTap,
  });

  /// 팝업 표시
  static Future<void> show(
    BuildContext context, {
    required MyStore store,
    VoidCallback? onOrderStatusTap,
    VoidCallback? onSalesStatusTap,
  }) {
    return showDialog(
      context: context,
      builder: (dialogContext) => Dialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        ),
        child: StoreDetailPopup(
          store: store,
          onOrderStatusTap: () {
            Navigator.of(dialogContext).pop();
            onOrderStatusTap?.call();
          },
          onSalesStatusTap: () {
            Navigator.of(dialogContext).pop();
            onSalesStatusTap?.call();
          },
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.xl),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 닫기 버튼
          Align(
            alignment: Alignment.topRight,
            child: IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.close, size: 24),
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(
                minWidth: 24,
                minHeight: 24,
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          // 거래처명
          Text(
            store.storeName,
            style: AppTypography.headlineMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: AppSpacing.xs),
          // 거래처 코드
          Text(
            '(${store.storeCode})',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: AppSpacing.xl),
          // 주문서 현황 버튼
          _buildActionButton(
            icon: Icons.description_outlined,
            label: '주문서 현황',
            onTap: onOrderStatusTap,
          ),
          const SizedBox(height: AppSpacing.sm),
          // 매출 현황 버튼
          _buildActionButton(
            icon: Icons.notifications_outlined,
            label: '매출 현황',
            onTap: onSalesStatusTap,
          ),
          const SizedBox(height: AppSpacing.md),
        ],
      ),
    );
  }

  Widget _buildActionButton({
    required IconData icon,
    required String label,
    VoidCallback? onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        decoration: BoxDecoration(
          border: Border.all(color: AppColors.border),
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        ),
        child: Row(
          children: [
            Icon(
              icon,
              size: 22,
              color: AppColors.textSecondary,
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Text(
                label,
                style: AppTypography.bodyLarge.copyWith(
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            const Icon(
              Icons.chevron_right,
              color: AppColors.textTertiary,
              size: 22,
            ),
          ],
        ),
      ),
    );
  }
}
