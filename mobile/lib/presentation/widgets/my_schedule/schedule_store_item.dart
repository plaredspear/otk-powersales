import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 일정 거래처 아이템 위젯
///
/// 거래처명과 근무 유형(3종)을 표시하며,
/// 등록 탭에서는 등록 상태도 함께 표시합니다.
class ScheduleStoreItem extends StatelessWidget {
  /// 거래처명
  final String storeName;

  /// 근무 유형 1
  final String workType1;

  /// 근무 유형 2
  final String workType2;

  /// 근무 유형 3
  final String workType3;

  /// 등록 여부 (등록 탭에서만 사용)
  final bool? isRegistered;

  /// 등록 상태 표시 여부
  final bool showRegistrationStatus;

  const ScheduleStoreItem({
    super.key,
    required this.storeName,
    required this.workType1,
    required this.workType2,
    required this.workType3,
    this.isRegistered,
    this.showRegistrationStatus = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.md,
      ),
      child: Row(
        children: [
          // 거래처 정보
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 거래처명
                Text(
                  storeName,
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: AppSpacing.xs),
                // 근무 유형
                Text(
                  '$workType1 / $workType2 / $workType3',
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),

          // 등록 상태 표시 (등록 탭에서만)
          if (showRegistrationStatus && isRegistered != null)
            Row(
              children: [
                CircleAvatar(
                  radius: 4,
                  backgroundColor: isRegistered!
                      ? AppColors.success
                      : AppColors.otokiYellow,
                ),
                const SizedBox(width: AppSpacing.xs),
                Text(
                  isRegistered! ? '등록 완료' : '등록 전',
                  style: AppTypography.bodySmall.copyWith(
                    color: isRegistered!
                        ? AppColors.success
                        : AppColors.textSecondary,
                  ),
                ),
              ],
            ),
        ],
      ),
    );
  }
}
