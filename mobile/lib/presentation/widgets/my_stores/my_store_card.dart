import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/my_store.dart';

/// 내 거래처 카드 위젯
///
/// 거래처명(코드), 주소, 대표자, 전화 버튼을 표시합니다.
/// 카드를 탭하면 거래처 상세 팝업이 열립니다.
class MyStoreCard extends StatelessWidget {
  /// 거래처 정보
  final MyStore store;

  /// 카드 탭 콜백 (거래처 선택)
  final VoidCallback onTap;

  /// 전화 버튼 탭 콜백
  final VoidCallback? onPhoneTap;

  const MyStoreCard({
    super.key,
    required this.store,
    required this.onTap,
    this.onPhoneTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        child: Container(
          padding: const EdgeInsets.all(AppSpacing.md),
          decoration: BoxDecoration(
            color: AppColors.white,
            borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
            border: Border.all(color: AppColors.border, width: 1),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 거래처 정보 영역
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // 거래처명(코드)
                    Text(
                      '${store.storeName}(${store.storeCode})',
                      style: AppTypography.headlineSmall,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    // 주소
                    Text(
                      store.address,
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: AppSpacing.xxs),
                    // 대표자
                    Text(
                      '대표자: ${store.representativeName}',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ],
                ),
              ),
              // 전화 버튼
              if (store.phoneNumber != null && store.phoneNumber!.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.only(left: AppSpacing.sm),
                  child: IconButton(
                    onPressed: onPhoneTap,
                    icon: const Icon(
                      Icons.phone,
                      color: AppColors.otokiBlue,
                      size: 24,
                    ),
                    tooltip: '전화 걸기',
                    constraints: const BoxConstraints(
                      minWidth: 40,
                      minHeight: 40,
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
