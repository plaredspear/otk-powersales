import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 클레임 상태 뱃지 위젯.
///
/// 표시 값은 코스모스 조치상태(`Claim.actionStatus`, 미회신 시 '미확인') 다. SF 상 picklist 가 아닌
/// 텍스트(10) 자유값이라 코스모스 회신 원문을 그대로 노출하고, 색만 문구로 추정해 입힌다.
class ClaimStatusBadge extends StatelessWidget {
  /// 표시 문구 (= 서버가 내려주는 actionStatusLabel).
  final String label;

  const ClaimStatusBadge({
    super.key,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    final color = _statusColor();
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: 2,
      ),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Text(
        label,
        style: AppTypography.labelSmall.copyWith(
          color: color,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }

  /// 값 집합이 고정돼 있지 않아 포함 문자열로 판정한다.
  /// 물류클레임 조치상태 picklist(미확인/조치중/조치 완료/중복접수) 어휘를 기준으로 삼되,
  /// 코스모스가 다른 문구를 보내면 진행 중(주황) 으로 본다.
  Color _statusColor() {
    if (label.contains('완료')) return Colors.green;
    if (label.contains('미확인') || label.contains('중복')) {
      return AppColors.textSecondary;
    }
    if (label.contains('실패') || label.contains('반려')) return AppColors.error;
    return Colors.orange;
  }
}
