import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';

/// 주문서 작성 액션 버튼 (삭제/임시저장/승인요청)
class OrderFormActionButtons extends StatelessWidget {
  final VoidCallback onDelete;
  final VoidCallback onSaveDraft;
  final VoidCallback onSubmit;
  final bool isSubmitting;
  final bool hasItems;

  const OrderFormActionButtons({
    super.key,
    required this.onDelete,
    required this.onSaveDraft,
    required this.onSubmit,
    required this.isSubmitting,
    required this.hasItems,
  });

  @override
  Widget build(BuildContext context) {
    // 레거시 write.jsp 하단 고정 바: 삭제(회색) / 임시저장(다크) / 승인요청(옐로) 풀폭 3분할.
    final bool submitEnabled = !isSubmitting && hasItems;

    return SafeArea(
      top: false,
      child: SizedBox(
        height: 60,
        child: Row(
          children: [
            _Segment(
              flex: 2,
              label: '삭제',
              backgroundColor: AppColors.surfaceVariant,
              foregroundColor: AppColors.textSecondary,
              onPressed: isSubmitting ? null : onDelete,
            ),
            _Segment(
              flex: 3,
              label: '임시저장',
              backgroundColor: AppColors.legacyTextSub,
              foregroundColor: AppColors.white,
              loading: isSubmitting,
              onPressed: isSubmitting ? null : onSaveDraft,
            ),
            _Segment(
              flex: 3,
              label: '승인요청',
              backgroundColor: AppColors.legacyYellow,
              foregroundColor: AppColors.onPrimary,
              disabledBackgroundColor: AppColors.surfaceVariant,
              disabledForegroundColor: AppColors.textTertiary,
              loading: isSubmitting,
              onPressed: submitEnabled ? onSubmit : null,
            ),
          ],
        ),
      ),
    );
  }
}

/// 하단 고정 바의 단일 세그먼트 (풀-블리드, 모서리 없음).
class _Segment extends StatelessWidget {
  final int flex;
  final String label;
  final Color backgroundColor;
  final Color foregroundColor;
  final Color? disabledBackgroundColor;
  final Color? disabledForegroundColor;
  final bool loading;
  final VoidCallback? onPressed;

  const _Segment({
    required this.flex,
    required this.label,
    required this.backgroundColor,
    required this.foregroundColor,
    this.disabledBackgroundColor,
    this.disabledForegroundColor,
    this.loading = false,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    final bool enabled = onPressed != null;
    final Color bg = enabled
        ? backgroundColor
        : (disabledBackgroundColor ?? backgroundColor);
    final Color fg = enabled
        ? foregroundColor
        : (disabledForegroundColor ?? foregroundColor);

    return Expanded(
      flex: flex,
      child: Material(
        color: bg,
        child: InkWell(
          onTap: onPressed,
          child: Center(
            child: loading
                ? SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(fg),
                    ),
                  )
                : Text(
                    label,
                    style: AppTypography.headlineSmall.copyWith(color: fg),
                  ),
          ),
        ),
      ),
    );
  }
}
