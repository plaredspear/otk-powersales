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

  /// 배너 안 액션 버튼 — 기본 여백(가로 16)이 두 개 붙으면 안내 문구를 밀어내므로 좁힌다.
  /// 높이는 44px 탭 영역을 유지한다.
  static final ButtonStyle _actionStyle = TextButton.styleFrom(
    padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
    minimumSize: const Size(0, 44),
    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
  );

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
            // 안내 문구가 남은 폭을 차지하고, 좁으면 말줄임한다.
            // 고정폭 Text + Spacer 조합은 좁은 단말에서 버튼을 밀어내 overflow 를 냈다.
            Expanded(
              child: Text(
                '임시 저장 데이터가 있습니다',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.info,
                ),
              ),
            ),
            TextButton(
              onPressed: onLoadDraft,
              style: _actionStyle,
              child: Text(
                '불러오기',
                style: AppTypography.labelMedium.copyWith(
                  color: AppColors.info,
                ),
              ),
            ),
            TextButton(
              onPressed: onNewOrder,
              style: _actionStyle,
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
