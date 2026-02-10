import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 임시저장 안내 배너
class DraftBanner extends StatelessWidget {
  final VoidCallback onLoadDraft;
  final VoidCallback onNewOrder;

  const DraftBanner({
    super.key,
    required this.onLoadDraft,
    required this.onNewOrder,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      // ignore: deprecated_member_use
      color: AppColors.info.withOpacity(0.1),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      ),
      child: Padding(
        padding: AppSpacing.cardPadding,
        child: Row(
          children: [
            Icon(
              Icons.save_outlined,
              color: AppColors.info,
              size: 20,
            ),
            const SizedBox(width: AppSpacing.sm),
            Text(
              '임시 저장 데이터가 있습니다',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.info,
              ),
            ),
            const Spacer(),
            TextButton(
              onPressed: onLoadDraft,
              child: Text(
                '불러오기',
                style: AppTypography.labelMedium.copyWith(
                  color: AppColors.info,
                ),
              ),
            ),
            TextButton(
              onPressed: onNewOrder,
              child: Text(
                '새로 작성',
                style: AppTypography.labelMedium.copyWith(
                  color: AppColors.info,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
