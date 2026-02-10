import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 검색 안내/빈 결과 위젯
///
/// 초기 상태에서는 검색 안내 메시지를, 검색 후 결과가 없으면 빈 결과 메시지를 표시합니다.
class EmptySearchGuide extends StatelessWidget {
  /// 검색 실행 여부 (true: 검색 후 빈 결과, false: 초기 상태)
  final bool hasSearched;

  /// 바코드 검색 버튼 탭 콜백
  final VoidCallback? onBarcodeTap;

  const EmptySearchGuide({
    super.key,
    this.hasSearched = false,
    this.onBarcodeTap,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // 아이콘
            Icon(
              hasSearched ? Icons.search_off : Icons.search,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.md),

            // 안내 메시지
            Text(
              hasSearched
                  ? '검색 결과가 없습니다'
                  : '제품명 또는 제품코드를 입력 후\n검색하세요',
              textAlign: TextAlign.center,
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),

            // 바코드 검색 버튼 (초기 상태에서만 표시)
            if (!hasSearched && onBarcodeTap != null) ...[
              const SizedBox(height: AppSpacing.lg),
              OutlinedButton.icon(
                onPressed: onBarcodeTap,
                icon: const Icon(Icons.qr_code_scanner, size: 20),
                label: const Text('바코드로 검색'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.textSecondary,
                  side: const BorderSide(color: AppColors.border),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(
                      AppSpacing.radiusMd,
                    ),
                  ),
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.lg,
                    vertical: AppSpacing.sm,
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
